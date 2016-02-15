package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ovirt.engine.core.bll.profiles.DiskProfileHelper;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.ConvertVmParameters;
import org.ovirt.engine.core.common.action.ImportVmFromExternalProviderParameters;
import org.ovirt.engine.core.common.action.RemoveVmParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

@DisableInPrepareMode
@NonTransactiveCommandAttribute(forceCompensation = true)
public class ImportVmFromExternalProviderCommand<T extends ImportVmFromExternalProviderParameters> extends ImportVmCommandBase<T>
implements QuotaStorageDependent {

    private static final Pattern VMWARE_DISK_NAME_PATTERN = Pattern.compile("\\[.*?\\] .*/(.*).vmdk");

    public ImportVmFromExternalProviderCommand(Guid cmdId) {
        super(cmdId);
    }

    protected ImportVmFromExternalProviderCommand(T parameters) {
        super(parameters, null);
    }

    @Override
    protected void init() {
        super.init();
        setVdsId(getParameters().getProxyHostId());
        setStorageDomainId(getParameters().getDestDomainId());
        setStoragePoolId(getVdsGroup() != null ? getVdsGroup().getStoragePoolId() : null);
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        if (getStorageDomain() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_NOT_EXIST);
        }

        if (!getStorageDomain().getStoragePoolId().equals(getStoragePoolId())) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_AND_CLUSTER_IN_DIFFERENT_POOL);
        }

        if (getStoragePool().getStatus() != StoragePoolStatus.Up) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_STATUS_ILLEGAL);
        }

        if (getStorageDomain().getStatus() != StorageDomainStatus.Active) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL);
        }

        if (getVdsId() != null && !validate(validateRequestedProxyHost())) {
            return false;
        }

        if (!validateBallonDevice()) {
            return false;
        }

        if (!validateNoDuplicateVm()) {
            return false;
        }

        if (!validateUniqueVmName()) {
            return false;
        }

        if (!validateVmArchitecture()) {
            return false;
        }

        if (!validateVdsCluster()) {
            return false;
        }

        if (!setAndValidateCpuProfile()) {
            return false;
        }

        if (!setAndValidateDiskProfiles()) {
            return false;
        }

        if (!validateStorageSpace()) {
            return false;
        }

        if (getParameters().getVirtioIsoName() != null && getActiveIsoDomainId() == null) {
            return failCanDoAction(EngineMessage.ERROR_CANNOT_FIND_ISO_IMAGE_PATH);
        }

        return true;
    }

    protected boolean validateStorageSpace() {
        List<DiskImage> dummiesDisksList = createDiskDummiesForSpaceValidations(getVm().getImages());
        return validate(getImportValidator().validateSpaceRequirements(dummiesDisksList));
    }

    protected boolean setAndValidateDiskProfiles() {
        Map<DiskImage, Guid> map = new HashMap<>();
        for (DiskImage diskImage : getVm().getImages()) {
            map.put(diskImage, getStorageDomainId());
        }
        return validate(DiskProfileHelper.setAndValidateDiskProfiles(map,
                getStoragePool().getCompatibilityVersion(), getCurrentUser()));
    }

    private ValidationResult validateRequestedProxyHost() {
        if (getVds() == null) {
            return new ValidationResult(EngineMessage.VDS_DOES_NOT_EXIST);
        }

        if (!getStoragePoolId().equals(getVds().getStoragePoolId())) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_VDS_NOT_IN_DEST_STORAGE_POOL);
        }

        if (getVds().getStatus() != VDSStatus.Up) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_VDS_STATUS_ILLEGAL);
        }

        return ValidationResult.VALID;
    }

    @Override
    protected List<DiskImage> createDiskDummiesForSpaceValidations(List<DiskImage> disksList) {
        List<DiskImage> dummies = new ArrayList<>(disksList.size());
        ArrayList<Guid> emptyStorageIds = new ArrayList<>();
        for (DiskImage image : disksList) {
            image.setStorageIds(emptyStorageIds);
            dummies.add(ImagesHandler.createDiskImageWithExcessData(image, getStorageDomainId()));
        }
        return dummies;
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();
        for (DiskImage diskImage : getVm().getImages()) {
            list.add(new QuotaStorageConsumptionParameter(
                    diskImage.getQuotaId(),
                    null,
                    QuotaConsumptionParameter.QuotaAction.CONSUME,
                    getStorageDomainId(),
                    (double)diskImage.getSizeInGigabytes()));
        }
        return list;
    }

    @Override
    protected void processImages() {
        ArrayList<Guid> diskIds = new ArrayList<>();
        for (DiskImage image : getVm().getImages()) {
            Guid diskId = createDisk(image);
            diskIds.add(diskId);
        }
        getParameters().setDisks(diskIds);

        setSucceeded(true);
    }

    @Override
    protected VmDynamic createVmDynamic() {
        VmDynamic vmDynamic = super.createVmDynamic();
        vmDynamic.setStatus(VMStatus.Down);
        return vmDynamic;
    }

    @Override
    protected void addVmStatic() {
        super.addVmStatic();
        new SnapshotsManager().addActiveSnapshot(
                Guid.newGuid(), getVm(), "", getCompensationContext());
    }

    @Override
    protected void addVmInterfaces() {
        super.addVmInterfaces();
        for (VmNetworkInterface iface : getVm().getInterfaces()) {
            VmDeviceUtils.addInterface(getVmId(), iface.getId(), iface.isPlugged(), false);
        }
    }

    private Guid createDisk(DiskImage image) {
        image.setDiskAlias(renameDiskAlias(image.getDiskAlias()));
        image.setDiskInterface(DiskInterface.VirtIO);

        AddDiskParameters diskParameters = new AddDiskParameters(getVmId(), image);
        diskParameters.setStorageDomainId(getStorageDomainId());
        diskParameters.setParentCommand(getActionType());
        diskParameters.setParentParameters(getParameters());
        diskParameters.setShouldRemainIllegalOnFailedExecution(true);
        diskParameters.setStorageDomainId(getParameters().getDestDomainId());
        VdcReturnValueBase vdcReturnValueBase =
                runInternalActionWithTasksContext(VdcActionType.AddDisk, diskParameters);

        if (!vdcReturnValueBase.getSucceeded()) {
            throw new EngineException(vdcReturnValueBase.getFault().getError(),
                    "Failed to create disk!");
        }

        getTaskIdList().addAll(vdcReturnValueBase.getInternalVdsmTaskIdList());
        return vdcReturnValueBase.getActionReturnValue();
    }

    protected static String renameDiskAlias(String alias) {
        Matcher vmwareMatcher = VMWARE_DISK_NAME_PATTERN.matcher(alias);
        if (vmwareMatcher.matches()) {
            return vmwareMatcher.group(1);
        }

        return alias;
    }

    @Override
    protected void endSuccessfully() {
        endActionOnDisks();

        // Lock will be acquired by the convert command.
        // Note that the VM is not locked for a short period of time. This should be fixed
        // when locks that are passed by caller could be released by command's callback.
        freeLock();
        convert();

        setSucceeded(true);
    }

    protected void convert() {
        CommandCoordinatorUtil.executeAsyncCommand(
                VdcActionType.ConvertVm,
                buildConvertVmParameters(),
                cloneContextAndDetachFromParent());
    }

    private ConvertVmParameters buildConvertVmParameters() {
        ConvertVmParameters parameters = new ConvertVmParameters(getVmId());
        parameters.setUrl(getParameters().getUrl());
        parameters.setUsername(getParameters().getUsername());
        parameters.setPassword(getParameters().getPassword());
        parameters.setVmName(getVmName());
        parameters.setDisks(getDisks());
        parameters.setStoragePoolId(getStoragePoolId());
        parameters.setStorageDomainId(getStorageDomainId());
        parameters.setProxyHostId(getParameters().getProxyHostId());
        parameters.setVdsGroupId(getVdsGroupId());
        parameters.setVirtioIsoName(getParameters().getVirtioIsoName());
        return parameters;
    }

    @Override
    protected void endWithFailure() {
        // Since AddDisk is called internally, its audit log on end-action will not be logged
        auditLog(this, AuditLogType.ADD_DISK_INTERNAL_FAILURE);
        endActionOnDisks();
        removeVm();
        setSucceeded(true);
    }

    private void removeVm() {
        runInternalActionWithTasksContext(
                VdcActionType.RemoveVm,
                new RemoveVmParameters(getVmId(), true));
    }

    protected void endActionOnDisks() {
        for (VdcActionParametersBase parameters : getParametersForChildCommand()) {
            getBackend().endAction(
                    parameters.getCommandType(),
                    parameters,
                    getContext().clone().withoutCompensationContext().withoutExecutionContext().withoutLock());
        }
    }

    protected List<DiskImage> getDisks() {
        List<DiskImage> disks = new ArrayList<>();
        for (Guid diskId : getParameters().getDisks()) {
            disks.add(getDisk(diskId));
        }
        return disks;
    }

    private DiskImage getDisk(Guid diskId) {
        return runInternalQuery(
                VdcQueryType.GetDiskByDiskId,
                new IdQueryParameters(diskId))
                .getReturnValue();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ?
                    AuditLogType.IMPORTEXPORT_STARTING_IMPORT_VM
                    : AuditLogType.IMPORTEXPORT_IMPORT_VM_FAILED;
        case END_FAILURE:
            return AuditLogType.IMPORTEXPORT_IMPORT_VM_FAILED;
        case END_SUCCESS:
        default:
            return super.getAuditLogTypeValue();
        }
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        Set<PermissionSubject> permissionSet = new HashSet<>();
        // Destination domain
        permissionSet.add(new PermissionSubject(getStorageDomainId(),
                VdcObjectType.Storage,
                getActionType().getActionGroup()));
        return new ArrayList<>(permissionSet);
    }

    protected Guid getActiveIsoDomainId() {
        return getIsoDomainListSyncronizer().findActiveISODomain(getStoragePoolId());
    }

    protected IsoDomainListSyncronizer getIsoDomainListSyncronizer() {
        return IsoDomainListSyncronizer.getInstance();
    }
}
