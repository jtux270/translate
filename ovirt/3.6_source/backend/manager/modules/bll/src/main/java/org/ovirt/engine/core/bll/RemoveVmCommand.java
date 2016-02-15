package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.memory.MemoryUtils;
import org.ovirt.engine.core.bll.network.ExternalNetworkManager;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.CINDERStorageHelper;
import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.tasks.TaskHandlerCommand;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.bll.validator.storage.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.storage.MultipleStorageDomainsValidator;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.RemoveAllVmCinderDisksParameters;
import org.ovirt.engine.core.common.action.RemoveAllVmImagesParameters;
import org.ovirt.engine.core.common.action.RemoveMemoryVolumesParameters;
import org.ovirt.engine.core.common.action.RemoveVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.LunDisk;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmIconDao;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@DisableInPrepareMode
@NonTransactiveCommandAttribute(forceCompensation = true)
public class RemoveVmCommand<T extends RemoveVmParameters> extends VmCommand<T> implements QuotaStorageDependent, TaskHandlerCommand<RemoveVmParameters> {

    @Inject
    private Event<Guid> vmDeleted;

    @Inject
    private VmIconDao vmIconDao;

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected RemoveVmCommand(Guid commandId) {
        super(commandId);
    }

    public RemoveVmCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VM, getVmId()));
        if (getVm() != null) {
            setStoragePoolId(getVm().getStoragePoolId());
        }
    }

    public RemoveVmCommand(T parameters) {
        this(parameters, null);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution);
    }

    @Override
    protected void executeVmCommand() {
        if (getVm().getStatus() != VMStatus.ImageLocked) {
            VmHandler.lockVm(getVm().getDynamicData(), getCompensationContext());
        }
        freeLock();
        setSucceeded(removeVm());
    }

    private boolean removeVm() {
        final List<DiskImage> diskImages = ImagesHandler.filterImageDisks(getVm().getDiskList(),
                true,
                false,
                true);

        final List<LunDisk> lunDisks =
                ImagesHandler.filterDiskBasedOnLuns(getVm().getDiskMap().values(), false);

        for (VmNic nic : getInterfaces()) {
            new ExternalNetworkManager(nic).deallocateIfExternal();
        }

        removeMemoryVolumes();

        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                removeVmFromDb();
                if (getParameters().isRemoveDisks()) {
                    for (DiskImage image : diskImages) {
                        getCompensationContext().snapshotEntityStatus(image.getImage(), ImageStatus.ILLEGAL);
                        ImagesHandler.updateImageStatus(image.getImage().getId(), ImageStatus.LOCKED);
                    }

                    for (LunDisk lunDisk : lunDisks) {
                        ImagesHandler.removeLunDisk(lunDisk);
                    }

                    getCompensationContext().stateChanged();
                }
                else {
                    for (DiskImage image : diskImages) {
                        getImageDao().updateImageVmSnapshotId(image.getImageId(), null);
                    }
                }
                return null;
            }
        });

        Collection<DiskImage> unremovedDisks = Collections.emptyList();
        if (getParameters().isRemoveDisks()) {
            if (!diskImages.isEmpty()) {
                unremovedDisks = (Collection<DiskImage>) removeVmImages(diskImages).getActionReturnValue();
            }
            unremovedDisks.addAll(removeCinderDisks());
            if (!unremovedDisks.isEmpty()) {
                processUnremovedDisks(unremovedDisks);
                return false;
            }
        }

        vmDeleted.fire(getVmId());
        return true;
    }

    private void removeMemoryVolumes() {
        Set<String> memoryStates =
                MemoryUtils.getMemoryVolumesFromSnapshots(getDbFacade().getSnapshotDao().getAll(getVmId()));
        for (String memoryState : memoryStates) {
            VdcReturnValueBase retVal = runInternalAction(
                    VdcActionType.RemoveMemoryVolumes,
                    buildRemoveMemoryVolumesParameters(memoryState, getVmId()),
                    cloneContextAndDetachFromParent());

            if (!retVal.getSucceeded()) {
                log.error("Failed to remove memory volumes while removing vm '{}' (volumes: '{}')",
                        getVmId(), memoryState);
            }
        }
    }

    private RemoveMemoryVolumesParameters buildRemoveMemoryVolumesParameters(String memoryState, Guid vmId) {
        RemoveMemoryVolumesParameters params = new RemoveMemoryVolumesParameters(memoryState, vmId);
        params.setEntityInfo(getParameters().getEntityInfo());
        return params;
    }

    @Override
    protected boolean canDoAction() {
        if (getVm() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        if (getVm().isDeleteProtected()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_DELETE_PROTECTION_ENABLED);
        }

        VmHandler.updateDisksFromDb(getVm());

        if (!getParameters().isRemoveDisks() && !canRemoveVmWithDetachDisks()) {
            return false;
        }

        switch (getVm().getStatus()) {
            case Unassigned:
            case Down:
            case ImageIllegal:
            case ImageLocked:
                break;
            case Suspended:
                return failCanDoAction(EngineMessage.VM_CANNOT_REMOVE_VM_WHEN_STATUS_IS_NOT_DOWN);
            default:
                return (getVm().isHostedEngine() && isInternalExecution()) || failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_IS_RUNNING);
        }

        if (getVm().getVmPoolId() != null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_ATTACHED_TO_POOL);
        }

        // enable to remove vms without images
        SnapshotsValidator snapshotsValidator = new SnapshotsValidator();
        if (!validate(snapshotsValidator.vmNotDuringSnapshot(getVmId()))) {
            return false;
        }

        if (!validate(snapshotsValidator.vmNotInPreview(getVmId()))) {
            return false;
        }

        if (!validate(new StoragePoolValidator(getStoragePool()).isUp())) {
            return false;
        }

        Collection<Disk> vmDisks = getVm().getDiskMap().values();
        List<DiskImage> vmImages = ImagesHandler.filterImageDisks(vmDisks, true, false, true);
        vmImages.addAll(ImagesHandler.filterDisksBasedOnCinder(vmDisks));
        if (!vmImages.isEmpty()) {
            Set<Guid> storageIds = ImagesHandler.getAllStorageIdsForImageIds(vmImages);
            MultipleStorageDomainsValidator storageValidator = new MultipleStorageDomainsValidator(getVm().getStoragePoolId(), storageIds);
            if (!validate(storageValidator.allDomainsExistAndActive())) {
                return false;
            }

            DiskImagesValidator diskImagesValidator = new DiskImagesValidator(vmImages);
            if (!getParameters().getForce() && !validate(diskImagesValidator.diskImagesNotLocked())) {
                return false;
            }
        }

        // Handle VM status with ImageLocked
        VmValidator vmValidator = new VmValidator(getVm());
        ValidationResult vmLockedValidatorResult = vmValidator.vmNotLocked();
        if (!vmLockedValidatorResult.isValid()) {
            // without force remove, we can't remove the VM
            if (!getParameters().getForce()) {
                return failCanDoAction(vmLockedValidatorResult.getMessage());
            }

            // If it is force, we cannot remove if there are task
            if (CommandCoordinatorUtil.hasTasksByStoragePoolId(getVm().getStoragePoolId())) {
                return failCanDoAction(EngineMessage.VM_CANNOT_REMOVE_HAS_RUNNING_TASKS);
            }
        }

        if (getParameters().isRemoveDisks() && !validate(vmValidator.vmNotHavingDeviceSnapshotsAttachedToOtherVms(false))) {
            return false;
        }

        return true;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__REMOVE);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM);
    }

    private boolean canRemoveVmWithDetachDisks() {
        if (!Guid.Empty.equals(getVm().getVmtGuid())) {
            return failCanDoAction(EngineMessage.VM_CANNOT_REMOVE_WITH_DETACH_DISKS_BASED_ON_TEMPLATE);
        }

        for (Disk disk : getVm().getDiskList()) {
            List<DiskImage> diskImageList = getDiskImageDao().getAllSnapshotsForImageGroup(disk.getId());
            if (diskImageList.size() > 1) {
                return failCanDoAction(EngineMessage.VM_CANNOT_REMOVE_WITH_DETACH_DISKS_SNAPSHOTS_EXIST);
            }
        }

        return true;
    }

    protected VdcReturnValueBase removeVmImages(List<DiskImage> images) {
        VdcReturnValueBase vdcRetValue =
                runInternalActionWithTasksContext(VdcActionType.RemoveAllVmImages,
                        buildRemoveAllVmImagesParameters(images));

        if (vdcRetValue.getSucceeded()) {
            getReturnValue().getVdsmTaskIdList().addAll(vdcRetValue.getInternalVdsmTaskIdList());
        }

        return vdcRetValue;
    }

    private RemoveAllVmImagesParameters buildRemoveAllVmImagesParameters(List<DiskImage> images) {
        RemoveAllVmImagesParameters params = new RemoveAllVmImagesParameters(getVmId(), images);
        if (getParameters().getParentCommand() == VdcActionType.Unknown) {
            params.setParentCommand(getActionType());
            params.setEntityInfo(getParameters().getEntityInfo());
            params.setParentParameters(getParameters());
        } else {
            params.setParentCommand(getParameters().getParentCommand());
            params.setEntityInfo(getParameters().getParentParameters().getEntityInfo());
            params.setParentParameters(getParameters().getParentParameters());
        }

        return params;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_REMOVE_VM_FINISHED : AuditLogType.USER_REMOVE_VM_FINISHED_WITH_ILLEGAL_DISKS;
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.singletonMap(getVmId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM, EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED));
    }

    protected void removeVmFromDb() {
        removeVmUsers();
        removeVmNetwork();
        removeVmSnapshots();
        removeVmStatic(getParameters().isRemovePermissions());
        removeIcons();
    }

    /**
     * It does just best effort service. There is also global icon cleanup during startup {@link Backend#iconCleanup()}
     */
    private void removeIcons() {
        if (getVm() != null) {
            vmIconDao.removeIfUnused(getVm().getStaticData().getLargeIconId());
            vmIconDao.removeIfUnused(getVm().getStaticData().getSmallIconId());
        }
    }

    /**
     * The following method will perform a removing of all cinder disks from vm. These is only DB operation
     */
    private Collection<CinderDisk> removeCinderDisks() {
        Collection<CinderDisk> failedRemoveCinderDisks = null;
        if (getParameters().isRemoveDisks()) {
            List<CinderDisk> cinderDisks =
                    ImagesHandler.filterDisksBasedOnCinder(getVm().getDiskMap().values());
            if (cinderDisks.isEmpty()) {
                return Collections.emptyList();
            }
            RemoveAllVmCinderDisksParameters param = new RemoveAllVmCinderDisksParameters(getVmId(), cinderDisks);
            param.setParentHasTasks(!getReturnValue().getVdsmTaskIdList().isEmpty());
            Future<VdcReturnValueBase> future = CommandCoordinatorUtil.executeAsyncCommand(
                    VdcActionType.RemoveAllVmCinderDisks,
                    withRootCommandInfo(param, getActionType()),
                    cloneContextAndDetachFromParent(),
                    CINDERStorageHelper.getStorageEntities(cinderDisks));
            try {
                failedRemoveCinderDisks = future.get().getActionReturnValue();
            } catch (InterruptedException | ExecutionException e) {
                failedRemoveCinderDisks = cinderDisks;
                log.error("Exception", e);
            }
        }
        return failedRemoveCinderDisks;
    }

    @Override
    protected void endVmCommand() {
        // no audit log print here as the vm was already removed during the execute phase.
        setCommandShouldBeLogged(false);

        setSucceeded(true);
    }

    private void processUnremovedDisks(Collection<? extends DiskImage> diskImages) {
        List<String> disksLeftInVm = new ArrayList<>();
        for (DiskImage diskImage : diskImages) {
            disksLeftInVm.add(diskImage.getDiskAlias());
        }
        addCustomValue("DisksNames", StringUtils.join(disksLeftInVm, ","));
    }

    @Override
    public void addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList) {
        //
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        if (getParameters().isRemoveDisks()) {
            List<QuotaConsumptionParameter> list = new ArrayList<>();
            ImagesHandler.fillImagesBySnapshots(getVm());
            for (DiskImage disk : getVm().getDiskList()) {
                for (DiskImage snapshot : disk.getSnapshots()) {
                    if (snapshot.getQuotaId() != null && !Guid.Empty.equals(snapshot.getQuotaId())) {
                        if (snapshot.getActive()) {
                            list.add(new QuotaStorageConsumptionParameter(
                                    snapshot.getQuotaId(),
                                    null,
                                    QuotaStorageConsumptionParameter.QuotaAction.RELEASE,
                                    disk.getStorageIds().get(0),
                                    (double) snapshot.getSizeInGigabytes()));
                        } else {
                            list.add(new QuotaStorageConsumptionParameter(
                                    snapshot.getQuotaId(),
                                    null,
                                    QuotaStorageConsumptionParameter.QuotaAction.RELEASE,
                                    disk.getStorageIds().get(0),
                                    snapshot.getActualSize()));
                        }
                    }
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    ///////////////////////////////////////
    // TaskHandlerCommand Implementation //
    ///////////////////////////////////////

    @Override
    public T getParameters() {
        return super.getParameters();
    }

    @Override
    public VdcActionType getActionType() {
        return super.getActionType();
    }

    @Override
    public VdcReturnValueBase getReturnValue() {
        return super.getReturnValue();
    }

    @Override
    public Guid createTask(Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand,
            VdcObjectType entityType,
            Guid... entityIds) {
        return super.createTaskInCurrentTransaction(taskId, asyncTaskCreationInfo, parentCommand, entityType, entityIds);
    }

    @Override
    public Guid createTask(Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand) {
        return super.createTask(taskId, asyncTaskCreationInfo, parentCommand);
    }

    @Override
    public ArrayList<Guid> getTaskIdList() {
        return super.getTaskIdList();
    }

    @Override
    public void taskEndSuccessfully() {
        // Not implemented
    }

    @Override
    public void preventRollback() {
        throw new NotImplementedException();
    }

    @Override
    public Guid persistAsyncTaskPlaceHolder() {
        return super.persistAsyncTaskPlaceHolder(getActionType());
    }

    @Override
    public Guid persistAsyncTaskPlaceHolder(String taskKey) {
        return super.persistAsyncTaskPlaceHolder(getActionType(), taskKey);
    }

}
