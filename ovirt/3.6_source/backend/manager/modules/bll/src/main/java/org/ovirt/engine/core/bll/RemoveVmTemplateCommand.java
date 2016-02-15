package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.storage.CINDERStorageHelper;
import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.validator.storage.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.RemoveAllVmCinderDisksParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmTemplateParametersBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VmIconDao;
import org.ovirt.engine.core.utils.collections.MultiValueMapUtils;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@DisableInPrepareMode
@NonTransactiveCommandAttribute(forceCompensation = true)
public class RemoveVmTemplateCommand<T extends VmTemplateParametersBase> extends VmTemplateCommand<T>
        implements QuotaStorageDependent {

    private List<DiskImage> imageTemplates;
    private final Map<Guid, List<DiskImage>> storageToDisksMap = new HashMap<>();

    @Inject
    private VmIconDao vmIconDao;

    public RemoveVmTemplateCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
        super.setVmTemplateId(parameters.getVmTemplateId());
        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VmTemplate, getVmTemplateId()));
        if (getVmTemplate() != null) {
            setStoragePoolId(getVmTemplate().getStoragePoolId());
        }
    }

    public RemoveVmTemplateCommand(T parameters) {
        this(parameters, null);
    }

    public RemoveVmTemplateCommand(Guid vmTemplateId) {
        super.setVmTemplateId(vmTemplateId);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Command);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__REMOVE);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM_TEMPLATE);
    }

    @Override
    protected boolean canDoAction() {
        Guid vmTemplateId = getVmTemplateId();
        VmTemplate template = getVmTemplate();

        if (!super.canDoAction()) {
            return false;
        }

        boolean isInstanceType = getVmTemplate().getTemplateType() == VmEntityType.INSTANCE_TYPE;

        if (getVdsGroup() == null && !isInstanceType) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_CAN_NOT_BE_EMPTY);
            return false;
        }

        // check template exists
        if (!validate(templateExists())) {
            return false;
        }
        // check not blank template
        if (VmTemplateHandler.BLANK_VM_TEMPLATE_ID.equals(vmTemplateId)) {
            return failCanDoAction(EngineMessage.VMT_CANNOT_REMOVE_BLANK_TEMPLATE);
        }

        // check storage pool valid
        if (!isInstanceType && !validate(new StoragePoolValidator(getStoragePool()).isUp())) {
            return false;
        }

        // check if delete protected
        if (template.isDeleteProtected()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_DELETE_PROTECTION_ENABLED);
        }

        if (!isInstanceType) {
            fetchImageTemplates();
        }

        // populate all the domains of the template
        Set<Guid> allDomainsList = getStorageDomainsByDisks(imageTemplates, true);
        getParameters().setStorageDomainsList(new ArrayList<>(allDomainsList));

        // check template images for selected domains
        ArrayList<String> canDoActionMessages = getReturnValue().getCanDoActionMessages();
        for (Guid domainId : getParameters().getStorageDomainsList()) {
            if (!isVmTemplateImagesReady(getVmTemplate(),
                    domainId,
                    canDoActionMessages,
                    getParameters().getCheckDisksExists(),
                    true,
                    false,
                    true,
                    storageToDisksMap.get(domainId))) {
                return false;
            }
        }

        // check no vms from this template on selected domains
        List<VM> vms = getVmDao().getAllWithTemplate(vmTemplateId);
        List<String> problematicVmNames = new ArrayList<>();
        for (VM vm : vms) {
            problematicVmNames.add(vm.getName());
        }

        if (!problematicVmNames.isEmpty()) {
            return failCanDoAction(EngineMessage.VMT_CANNOT_REMOVE_DETECTED_DERIVED_VM,
                    String.format("$vmsList %1$s", StringUtils.join(problematicVmNames, ",")));
        }

        // for base templates, make sure it has no versions that need to be removed first
        if (vmTemplateId.equals(template.getBaseTemplateId())) {
            List<VmTemplate> templateVersions = getVmTemplateDao().getTemplateVersionsForBaseTemplate(vmTemplateId);
            if (!templateVersions.isEmpty()) {
                List<String> templateVersionsNames = new ArrayList<>();
                for (VmTemplate version : templateVersions) {
                    templateVersionsNames.add(version.getName());
                }

                return failCanDoAction(EngineMessage.VMT_CANNOT_REMOVE_BASE_WITH_VERSIONS,
                        String.format("$versionsList %1$s", StringUtils.join(templateVersionsNames, ",")));
            }
        }

        if (!isInstanceType && !validate(checkNoDisksBasedOnTemplateDisks())) {
            return false;
        }

        return true;
    }

    private ValidationResult checkNoDisksBasedOnTemplateDisks() {
        return new DiskImagesValidator(imageTemplates).diskImagesHaveNoDerivedDisks(null);
    }

    private void fetchImageTemplates() {
        if (imageTemplates == null) {
            List<Disk> allImages = DbFacade.getInstance().getDiskDao().getAllForVm(getVmTemplateId());
            imageTemplates = ImagesHandler.filterImageDisks(allImages, false, false, true);
            imageTemplates.addAll(ImagesHandler.filterDisksBasedOnCinder(allImages, true));
        }
    }

    /**
     * Get a list of all domains id that the template is on
     */
    private Set<Guid> getStorageDomainsByDisks(List<DiskImage> disks, boolean isFillStorageTodDiskMap) {
        Set<Guid> domainsList = new HashSet<>();
        if (disks != null) {
            for (DiskImage disk : disks) {
                domainsList.addAll(disk.getStorageIds());
                if (isFillStorageTodDiskMap) {
                    for (Guid storageDomainId : disk.getStorageIds()) {
                        MultiValueMapUtils.addToMap(storageDomainId, disk, storageToDisksMap);
                    }
                }
            }
        }
        return domainsList;
    }

    @Override
    protected void executeCommand() {
        final List<CinderDisk> cinderDisks =
                ImagesHandler.filterDisksBasedOnCinder(DbFacade.getInstance()
                        .getDiskDao()
                        .getAllForVm(getVmTemplateId()));
        // Set VM to lock status immediately, for reducing race condition.
        VmTemplateHandler.lockVmTemplateInTransaction(getVmTemplateId(), getCompensationContext());

        if (!imageTemplates.isEmpty() || !cinderDisks.isEmpty()) {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                    if (!imageTemplates.isEmpty() && removeVmTemplateImages()) {
                        VmHandler.removeVmInitFromDB(getVmTemplate());
                        setSucceeded(true);
                    }
                    if (!cinderDisks.isEmpty()) {
                        removeCinderDisks(cinderDisks);
                        setSucceeded(true);
                    }
                    return null;
                }
            });
        } else {
            // if for some reason template doesn't have images, remove it now and not in end action
            HandleEndAction();
        }
    }

    /**
     * The following method performs a removing of all cinder disks from vm. These is only DB operation
     */
    private void removeCinderDisks(List<CinderDisk> cinderDisks) {
        RemoveAllVmCinderDisksParameters removeParam = new RemoveAllVmCinderDisksParameters(getVmTemplateId(), cinderDisks);
        removeParam.setParentHasTasks(!getReturnValue().getVdsmTaskIdList().isEmpty());
        Future<VdcReturnValueBase> future =
                CommandCoordinatorUtil.executeAsyncCommand(VdcActionType.RemoveAllVmCinderDisks,
                        withRootCommandInfo(removeParam, getActionType()),
                        cloneContextAndDetachFromParent(),
                        CINDERStorageHelper.getStorageEntities(cinderDisks));
        try {
            future.get().getActionReturnValue();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception", e);
        }
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        if (getVmTemplate() != null) {
            return Collections.singletonMap(getVmTemplateId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.TEMPLATE, getTemplateExclusiveLockMessage()));
        }
        return null;
    }

    private String getTemplateExclusiveLockMessage() {
        return new StringBuilder(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_IS_BEING_REMOVED.name())
        .append(String.format("$TemplateName %1$s", getVmTemplate().getName()))
        .toString();
    }

    private void removeTemplateFromDb() {
        removeNetwork();
        DbFacade.getInstance().getVmTemplateDao().remove(getVmTemplate().getId());
        vmIconDao.removeIfUnused(getVmTemplate().getSmallIconId());
        vmIconDao.removeIfUnused(getVmTemplate().getLargeIconId());
    }

    protected boolean removeVmTemplateImages() {
        getParameters().setEntityInfo(getParameters().getEntityInfo());
        getParameters().setParentCommand(getActionType());
        getParameters().setParentParameters(getParameters());
        VdcReturnValueBase vdcReturnValue = runInternalActionWithTasksContext(
                VdcActionType.RemoveAllVmTemplateImageTemplates,
                getParameters());

        if (!vdcReturnValue.getSucceeded()) {
            setSucceeded(false);
            getReturnValue().setFault(vdcReturnValue.getFault());
            return false;
        }

        getReturnValue().getVdsmTaskIdList().addAll(vdcReturnValue.getInternalVdsmTaskIdList());
        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? AuditLogType.USER_REMOVE_VM_TEMPLATE : AuditLogType.USER_FAILED_REMOVE_VM_TEMPLATE;
        case END_FAILURE:
        case END_SUCCESS:
        default:
            return AuditLogType.USER_REMOVE_VM_TEMPLATE_FINISHED;
        }
    }

    @Override
    protected void endSuccessfully() {
        HandleEndAction();
    }

    @Override
    protected void endWithFailure() {
        HandleEndAction();
    }

    private void HandleEndAction() {
        try {
            removeTemplateFromDb();
            setSucceeded(true);
        } catch (RuntimeException e) {
            // Set the try again of task to false, to prevent log spam and audit log spam.
            getReturnValue().setEndActionTryAgain(false);
            log.error("Encountered a problem removing template from DB, setting the action not to retry.");
        }
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();
        fetchImageTemplates();
        if (imageTemplates != null) {
            for (DiskImage disk : imageTemplates) {
                if (disk.getQuotaId() != null && !Guid.Empty.equals(disk.getQuotaId())) {
                    for (Guid storageId : disk.getStorageIds()) {
                        list.add(new QuotaStorageConsumptionParameter(
                                disk.getQuotaId(),
                                null,
                                QuotaStorageConsumptionParameter.QuotaAction.RELEASE,
                                storageId,
                                (double) disk.getSizeInGigabytes()));
                    }
                }
            }
        }
        return list;
    }
}
