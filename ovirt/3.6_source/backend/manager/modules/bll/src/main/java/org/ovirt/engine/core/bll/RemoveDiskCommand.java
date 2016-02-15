package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.storage.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.storage.DiskValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.RemoveCinderDiskParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.action.RemoveImageParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.SubjectEntity;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.ImageDbOperationScope;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.LunDisk;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.BaseDiskDao;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.DiskImageDao;
import org.ovirt.engine.core.dao.DiskImageDynamicDao;
import org.ovirt.engine.core.dao.VmDeviceDao;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@DisableInPrepareMode
@NonTransactiveCommandAttribute
public class RemoveDiskCommand<T extends RemoveDiskParameters> extends CommandBase<T>
        implements QuotaStorageDependent {

    private Disk disk;
    private List<PermissionSubject> permsList = null;
    private List<VM> listVms;
    private String cachedDiskIsBeingRemovedLockMessage;

    public RemoveDiskCommand(T parameters) {
        this(parameters, null);
    }

    public RemoveDiskCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        setStorageDomainId(getParameters().getStorageDomainId());
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__REMOVE);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM_DISK);
    }

    @Override
    protected boolean canDoAction() {
        if (getDisk() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_IMAGE_DOES_NOT_EXIST);
        }

        DiskValidator oldDiskValidator = new DiskValidator(getDisk());

        return validate(oldDiskValidator.validateNotHostedEngineDisk()) && validateAllVmsForDiskAreDown() &&
                canRemoveDiskBasedOnStorageTypeCheck();
    }

    private boolean validateAllVmsForDiskAreDown() {
        if (getDisk().getVmEntityType() != null && getDisk().getVmEntityType().isVmType()) {
            for (VM vm : getVmsForDiskId()) {
                if (vm.getStatus() != VMStatus.Down) {
                    VmDevice vmDevice = getVmDeviceDao().get(new VmDeviceId(getDisk().getId(), vm.getId()));
                    if (vmDevice.getIsPlugged()) {
                        addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_VM_IS_NOT_DOWN);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean canRemoveDiskBasedOnStorageTypeCheck() {
        // currently, only images have specific checks.
        // In the future, if LUNs get specific checks,
        // or additional storage types are added, other else-if clauses should be added.
        if (getDisk().getDiskStorageType() == DiskStorageType.IMAGE ||
                getDisk().getDiskStorageType() == DiskStorageType.CINDER) {
            return canRemoveDiskBasedOnImageStorageCheck();
        }

        return true;
    }

    private boolean canRemoveDiskBasedOnImageStorageCheck() {
        boolean retValue = true;
        DiskImage diskImage = getDiskImage();
        DiskImagesValidator diskImagesValidator = new DiskImagesValidator(Collections.singletonList(diskImage));

        if (diskImage.isOvfStore()
                && !validate(diskImagesValidator.disksInStatus(ImageStatus.ILLEGAL,
                        EngineMessage.ACTION_TYPE_FAILED_OVF_DISK_NOT_IN_APPLICABLE_STATUS))) {
            return false;
        }

        boolean isVmTemplateType = diskImage.getVmEntityType() != null &&
                diskImage.getVmEntityType().isTemplateType();

        if (Guid.isNullOrEmpty(getParameters().getStorageDomainId())) {
            if (isVmTemplateType) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_CANT_DELETE_TEMPLATE_DISK_WITHOUT_SPECIFYING_DOMAIN);
            }
            getParameters().setStorageDomainId(diskImage.getStorageIds().get(0));
            setStorageDomainId(diskImage.getStorageIds().get(0));
        }

        if (isVmTemplateType) {
            diskImage.setStorageIds(getDiskImageDao().get(diskImage.getImageId()).getStorageIds());
        }

        if (!diskImage.getStorageIds().contains(getParameters().getStorageDomainId())) {
            retValue = false;
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_IS_WRONG);
        }

        StorageDomainValidator validator = new StorageDomainValidator(getStorageDomain());
        retValue =
                retValue && validate(validator.isDomainExistAndActive())
                        && validate(validator.domainIsValidDestination());

        if (retValue && diskImage.getImageStatus() == ImageStatus.LOCKED) {
            retValue = false;
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED);
        }
        if (retValue && getDisk().getVmEntityType() != null) {
            if (getDisk().getVmEntityType().isVmType()) {
                retValue = canRemoveVmImageDisk();
            } else if (getDisk().getVmEntityType().isTemplateType()) {
                retValue = canRemoveTemplateDisk();
            }
        }

        return retValue;
    }

    /**
     * Set the parent parameter vmTemplateId, based on the disk image id.
     */
    private void setVmTemplateIdParameter() {
        Map<Boolean, VmTemplate> templateMap =
                // Disk image is the only disk type that can be part of the template disks.
                getVmTemplateDao().getAllForImage(getDiskImage().getImageId());

        if (!templateMap.isEmpty()) {
            setVmTemplateId(templateMap.values().iterator().next().getId());
        }
    }

    /**
     * Cache method to retrieve all the VMs related to image
     * @return List of Vms.
     */
    private List<VM> getVmsForDiskId() {
        if (listVms == null) {
            listVms = getVmDao().getVmsListForDisk((Guid) getParameters().getDiskId(), true);
        }
        return listVms;
    }

    private boolean canRemoveTemplateDisk() {
        if (getVmTemplate().getStatus() == VmTemplateStatus.Locked) {
            return failCanDoAction(EngineMessage.VM_TEMPLATE_IMAGE_IS_LOCKED);
        }

        DiskImage diskImage = getDiskImage();

        if (diskImage.getStorageIds().size() == 1) {
            return failCanDoAction(EngineMessage.VM_TEMPLATE_IMAGE_LAST_DOMAIN);
        }

        if (!checkDerivedVmFromTemplateExists(diskImage) || !checkDerivedDisksFromDiskNotExist(diskImage)){
            return false;
        }

        return true;
    }

    private boolean checkDerivedVmFromTemplateExists(DiskImage diskImage) {
        List<String> vmNames = getNamesOfDerivedVmsFromTemplate(diskImage);
        if (!vmNames.isEmpty()) {
            addCanDoActionMessage(EngineMessage.VMT_CANNOT_REMOVE_DETECTED_DERIVED_VM);
            addCanDoActionMessageVariable("vmsList", StringUtils.join(vmNames, ","));
            return false;
        }
        return true;
    }

    private DiskImagesValidator createDiskImagesValidator(DiskImage disk) {
      return new DiskImagesValidator(Arrays.asList(disk));
    }

    protected boolean checkDerivedDisksFromDiskNotExist(DiskImage diskImage) {
        return validate(createDiskImagesValidator(diskImage).diskImagesHaveNoDerivedDisks(getParameters().getStorageDomainId()));
    }

    private List<String> getNamesOfDerivedVmsFromTemplate(DiskImage diskImage) {
        List<String> result = new ArrayList<>();
        for (VM vm : getVmDao().getAllWithTemplate(getVmTemplateId())) {
            for (Disk vmDisk : getDiskDao().getAllForVm(vm.getId())) {
                if (vmDisk.getDiskStorageType() == DiskStorageType.IMAGE) {
                    DiskImage vmDiskImage = (DiskImage) vmDisk;
                    if (vmDiskImage.getImageTemplateId().equals(diskImage.getImageId())) {
                        if (vmDiskImage.getStorageIds().contains(getParameters().getStorageDomainId())) {
                            result.add(vm.getName());
                        }
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean canRemoveVmImageDisk() {
        if (!listVms.isEmpty()) {
            Guid storagePoolId = listVms.get(0).getStoragePoolId();
            StoragePool sp = getStoragePoolDao().get(storagePoolId);
            if (!validate(new StoragePoolValidator(sp).isUp())) {
                return false;
            }

            List<DiskImage> diskList = ImagesHandler.filterImageDisks(Arrays.asList(getDisk()), true, false, true);
            DiskImagesValidator diskImagesValidator = new DiskImagesValidator(diskList);
            if (!validate(diskImagesValidator.diskImagesNotLocked())) {
                return false;
            }
        }

        SnapshotsValidator snapshotsValidator = new SnapshotsValidator();
        for (VM vm : listVms) {
            if (!validate(snapshotsValidator.vmNotDuringSnapshot(vm.getId())) ||
                    !validate(snapshotsValidator.vmNotInPreview(vm.getId()))) {
                return false;
            }
        }
        return true;
    }

    protected VmDeviceDao getVmDeviceDao() {
        return DbFacade.getInstance()
                .getVmDeviceDao();
    }

    protected DiskImageDao getDiskImageDao() {
        return DbFacade.getInstance().getDiskImageDao();
    }

    public DiskDao getDiskDao() {
        return DbFacade.getInstance().getDiskDao();
    }

    @Override
    protected void executeCommand() {
        switch (getDisk().getDiskStorageType()) {
            case IMAGE:
                VdcReturnValueBase vdcReturnValue =
                        runInternalActionWithTasksContext(VdcActionType.RemoveImage,
                                buildRemoveImageParameters(getDiskImage()));
                if (vdcReturnValue.getSucceeded()) {
                    incrementVmsGeneration();
                    getReturnValue().getVdsmTaskIdList().addAll(vdcReturnValue.getInternalVdsmTaskIdList());
                    setSucceeded(true);
                }
                break;
            case LUN:
                removeLunDisk();
                break;
            case CINDER:
                RemoveCinderDiskParameters params = new RemoveCinderDiskParameters(getParameters().getDiskId());
                if ( ((CinderDisk)getDisk()).getImageStatus() == ImageStatus.ILLEGAL) {
                    params.setFaultTolerant(true);
                }

                Future<VdcReturnValueBase> future = CommandCoordinatorUtil.executeAsyncCommand(
                        VdcActionType.RemoveCinderDisk,
                        params,
                        cloneContextAndDetachFromParent(),
                        new SubjectEntity(VdcObjectType.Storage, getParameters().getStorageDomainId()));
                try {
                    setReturnValue(future.get());
                    setSucceeded(getReturnValue().getSucceeded());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error removing Cinder disk '{}': {}",
                            getDiskImage().getDiskAlias(),
                            e.getMessage());
                    log.debug("Exception", e);
                }
                break;
        }
    }

    private RemoveImageParameters buildRemoveImageParameters(DiskImage diskImage) {
        RemoveImageParameters result = new RemoveImageParameters(diskImage.getImageId());
        result.setTransactionScopeOption(TransactionScopeOption.Suppress);
        result.setDiskImage(diskImage);
        result.setParentCommand(VdcActionType.RemoveDisk);
        result.setEntityInfo(new EntityInfo(VdcObjectType.Disk, getParameters().getDiskId()));
        result.setParentParameters(getParameters());
        result.setRemoveFromSnapshots(true);
        result.setStorageDomainId(getParameters().getStorageDomainId());
        result.setForceDelete(getParameters().getForceDelete());
        if (diskImage.getStorageIds().size() > 1) {
            result.setDbOperationScope(ImageDbOperationScope.MAPPING);
        }
        return result;
    }

    private void removeLunDisk() {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                ImagesHandler.removeLunDisk((LunDisk) getDisk());
                return null;
            }
        });
        setSucceeded(true);
    }

    @Override
    protected void endSuccessfully() {
        setSucceeded(true);
    }

    @Override
    protected void endWithFailure() {
        setSucceeded(true);
    }

    private void incrementVmsGeneration() {
        List<VM> listVms = getVmsForDiskId();
        for (VM vm : listVms) {
            getVmStaticDao().incrementDbGeneration(vm.getId());
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            if (getDisk().getDiskStorageType() == DiskStorageType.LUN) {
                return getSucceeded() ? AuditLogType.USER_FINISHED_REMOVE_DISK_NO_DOMAIN
                        : AuditLogType.USER_FINISHED_FAILED_REMOVE_DISK_NO_DOMAIN;
            } else if (getDisk().getDiskStorageType() == DiskStorageType.CINDER) {
                return getSucceeded() ? AuditLogType.USER_REMOVE_DISK_INITIATED
                        : AuditLogType.USER_FINISHED_FAILED_REMOVE_DISK;
            }
            return getSucceeded() ? AuditLogType.USER_FINISHED_REMOVE_DISK
                    : AuditLogType.USER_FINISHED_FAILED_REMOVE_DISK;
        default:
            return AuditLogType.UNASSIGNED;
        }
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        if (permsList == null && getDisk() != null) {
            permsList = new ArrayList<>();
            permsList.add(new PermissionSubject(getDisk().getId(),
                    VdcObjectType.Disk,
                    ActionGroup.DELETE_DISK));
        }
        return permsList;
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.singletonMap(getParameters().getDiskId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.DISK, getDiskIsBeingRemovedLockMessage()));
    }

    private String getDiskIsBeingRemovedLockMessage() {
        if (cachedDiskIsBeingRemovedLockMessage == null) {
            cachedDiskIsBeingRemovedLockMessage = new StringBuilder(EngineMessage.ACTION_TYPE_FAILED_DISK_IS_BEING_REMOVED.name())
            .append(String.format("$DiskName %1$s", getDiskAlias()))
            .toString();
        }
        return cachedDiskIsBeingRemovedLockMessage;
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        if (getDisk() == null || getDisk().getVmEntityType() == null) {
            return null;
        }

        if (getDisk().getVmEntityType().isVmType()) {
            return createSharedLocksForVmDisk();
        }

        if (getDisk().getVmEntityType().isTemplateType()) {
            return createSharedLocksForTemplateDisk();
        }

        log.warn("No shared locks are taken while removing disk of entity: {}",
                getDisk().getVmEntityType());
        return null;
    }

    private Map<String, Pair<String, String>> createSharedLocksForVmDisk() {
        List<VM> listVms = getVmsForDiskId();
        if (listVms.isEmpty()) {
            return null;
        }

        Map<String, Pair<String, String>> result = new HashMap<>();
        for (VM vm : listVms) {
            result.put(vm.getId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM, getDiskIsBeingRemovedLockMessage()));
        }
        return result;
    }

    private Map<String, Pair<String, String>> createSharedLocksForTemplateDisk() {
        setVmTemplateIdParameter();
        return Collections.singletonMap(getVmTemplateId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.TEMPLATE, getDiskIsBeingRemovedLockMessage()));
    }

    protected Disk getDisk() {
        if (disk == null) {
            disk = getDiskDao().get(getParameters().getDiskId());
        }

        return disk;
    }

    protected DiskImage getDiskImage() {
        return (DiskImage) getDisk();
    }

    public String getDiskAlias() {
        if (getDisk() != null) {
            return getDisk().getDiskAlias();
        }
        return "";
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = super.getJobMessageProperties();
            jobProperties.put("diskalias", getDiskAlias());
        }
        return jobProperties;
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();
        if (getDisk() != null
                && getDisk().getDiskStorageType().isInternal()
                && getQuotaId() != null
                && !Guid.Empty.equals(getQuotaId())) {
            list.add(new QuotaStorageConsumptionParameter(
                    getQuotaId(),
                    null,
                    QuotaConsumptionParameter.QuotaAction.RELEASE,
                    getStorageDomainId(),
                    (double) getDiskImage().getSizeInGigabytes()));
        }
        return list;
    }

    private Guid getQuotaId() {
        if (getDisk() != null && getDisk().getDiskStorageType().isInternal()) {
            return getDiskImage().getQuotaId();
        }
        return null;
    }

    @Override
    public void addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList) {
    }

    protected BaseDiskDao getBaseDiskDao() {
        return getDbFacade().getBaseDiskDao();
    }

    protected DiskImageDynamicDao getDiskImageDynamicDao() {
        return getDbFacade().getDiskImageDynamicDao();
    }
}
