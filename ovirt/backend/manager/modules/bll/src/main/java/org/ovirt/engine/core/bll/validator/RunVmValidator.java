package org.ovirt.engine.core.bll.validator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.ImagesHandler;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.scheduling.SchedulingManager;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.ImageFileType;
import org.ovirt.engine.core.common.businessentities.RepoImage;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.queries.GetImagesListParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.common.vdscommands.IsVmDuringInitiatingVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.VdsDynamicDAO;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.dao.network.VmNicDao;
import org.ovirt.engine.core.utils.NetworkUtils;

public class RunVmValidator {

    private VM vm;
    private RunVmParams runVmParam;
    private boolean isInternalExecution;
    private Guid activeIsoDomainId;
    private OsRepository osRepository;

    private List<Disk> cachedVmDisks;
    private List<DiskImage> cachedVmImageDisks;
    private Set<String> cachedInterfaceNetworkNames;
    private List<Network> cachedClusterNetworks;
    private Set<String> cachedClusterNetworksNames;

    public RunVmValidator(VM vm, RunVmParams rumVmParam, boolean isInternalExecution, Guid activeIsoDomainId) {
        this.vm = vm;
        this.runVmParam = rumVmParam;
        this.isInternalExecution = isInternalExecution;
        this.activeIsoDomainId = activeIsoDomainId;
        this.osRepository = SimpleDependecyInjector.getInstance().get(OsRepository.class);
    }

    /**
     * Used for testings
     */
    protected RunVmValidator() {
    }

    /**
     * A general method for run vm validations. used in runVmCommand and in VmPoolCommandBase
     *
     * @param messages
     * @param vmDisks
     * @param storagePool
     * @param vdsBlackList
     *            - hosts that we already tried to run on
     * @param vdsWhiteList
     *            - initial host list, mainly runOnSpecificHost (runOnce/migrateToHost)
     * @param destVds
     * @param vdsGroup
     * @return
     */
    public boolean canRunVm(List<String> messages, StoragePool storagePool, List<Guid> vdsBlackList,
            List<Guid> vdsWhiteList, Guid destVds, VDSGroup vdsGroup) {

        if (vm.getStatus() == VMStatus.Paused) {
            // if the VM is paused, we should only check the VDS status
            // as the rest of the checks were already checked before
            return validate(validateVdsStatus(vm), messages);
        }

        return
                validateVmProperties(vm, runVmParam.getCustomProperties(), messages) &&
                validate(validateBootSequence(vm, runVmParam.getBootSequence(), getVmDisks(), activeIsoDomainId), messages) &&
                validate(validateDisplayType(), messages) &&
                validate(new VmValidator(vm).vmNotLocked(), messages) &&
                validate(getSnapshotValidator().vmNotDuringSnapshot(vm.getId()), messages) &&
                validate(validateVmStatusUsingMatrix(vm), messages) &&
                validate(validateStoragePoolUp(vm, storagePool, getVmImageDisks()), messages) &&
                validate(validateIsoPath(vm, runVmParam.getDiskPath(), runVmParam.getFloppyPath(), activeIsoDomainId), messages)  &&
                validate(vmDuringInitialization(vm), messages) &&
                validate(validateStatelessVm(vm, runVmParam.getRunAsStateless()), messages) &&
                validate(validateStorageDomains(vm, isInternalExecution, getVmImageDisks()), messages) &&
                validate(validateImagesForRunVm(vm, getVmImageDisks()), messages) &&
                validate(validateMemorySize(vm), messages) &&
                SchedulingManager.getInstance().canSchedule(
                        vdsGroup, vm, vdsBlackList, vdsWhiteList, destVds, messages);
    }

    protected ValidationResult validateMemorySize(VM vm) {
        final ConfigValues configKey = getOsRepository().get64bitOss().contains(vm.getOs())
                ? ConfigValues.VM64BitMaxMemorySizeInMB
                : ConfigValues.VM32BitMaxMemorySizeInMB;
        final int maxSize = Config.getValue(configKey, vm.getVdsGroupCompatibilityVersion().getValue());
        if (vm.getMemSizeMb() > maxSize) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_MEMORY_EXCEEDS_SUPPORTED_LIMIT);
        }

        return ValidationResult.VALID;
    }

    /**
     * @return true if all VM network interfaces are valid
     */
    public ValidationResult validateNetworkInterfaces() {
        ValidationResult validationResult = validateInterfacesConfigured(vm);
        if (!validationResult.isValid()) {
            return validationResult;
        }

        validationResult = validateInterfacesAttachedToClusterNetworks(vm, getClusterNetworksNames(), getInterfaceNetworkNames());
        if (!validationResult.isValid()) {
            return validationResult;
        }

        validationResult = validateInterfacesAttachedToVmNetworks(getClusterNetworks(), getInterfaceNetworkNames());
        if (!validationResult.isValid()) {
            return validationResult;
        }

        return ValidationResult.VALID;
    }

    protected ValidationResult validateDisplayType() {

        DisplayType selectedDisplayType = runVmParam.getUseVnc() == null ?
                vm.getDefaultDisplayType() : (runVmParam.getUseVnc() ? DisplayType.vnc : DisplayType.qxl);

        if (!VmValidationUtils.isDisplayTypeSupported(vm.getOs(),
                vm.getVdsGroupCompatibilityVersion(),
                selectedDisplayType)) {
            return new ValidationResult(
                    VdcBllMessages.ACTION_TYPE_FAILED_ILLEGAL_VM_DISPLAY_TYPE_IS_NOT_SUPPORTED_BY_OS);
        }

        return ValidationResult.VALID;
    }

    protected boolean validateVmProperties(VM vm, String runOnceCustomProperties, List<String> messages) {
        String customProperties = runOnceCustomProperties != null ?
                runOnceCustomProperties : vm.getCustomProperties();
        return getVmPropertiesUtils().validateVmProperties(
                        vm.getVdsGroupCompatibilityVersion(),
                        customProperties,
                        messages);
    }

    protected ValidationResult validateBootSequence(VM vm, BootSequence runOnceBootSequence,
            List<Disk> vmDisks, Guid activeIsoDomainId) {
        BootSequence bootSequence = runOnceBootSequence != null ?
                runOnceBootSequence : vm.getDefaultBootSequence();
        // Block from running a VM with no HDD when its first boot device is
        // HD and no other boot devices are configured
        if (bootSequence == BootSequence.C && vmDisks.isEmpty()) {
            return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_FROM_DISK_WITHOUT_DISK);
        }

        // If CD appears as first and there is no ISO in storage
        // pool/ISO inactive - you cannot run this VM
        if (bootSequence == BootSequence.CD && activeIsoDomainId == null) {
            return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_FROM_CD_WITHOUT_ACTIVE_STORAGE_DOMAIN_ISO);
        }

        // if there is network in the boot sequence, check that the
        // vm has network, otherwise the vm cannot be run in vdsm
        if (bootSequence == BootSequence.N
                && getVmNicDao().getAllForVm(vm.getId()).isEmpty()) {
            return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_FROM_NETWORK_WITHOUT_NETWORK);
        }

        return ValidationResult.VALID;
    }

    /**
     * Check storage domains. Storage domain status and disk space are checked only for non-HA VMs.
     *
     * @param vm
     *            The VM to run
     * @param message
     *            The error messages to append to
     * @param isInternalExecution
     *            Command is internal?
     * @param vmImages
     *            The VM's image disks
     * @return <code>true</code> if the VM can be run, <code>false</code> if not
     */
    protected ValidationResult validateStorageDomains(VM vm, boolean isInternalExecution,
            List<DiskImage> vmImages) {
        if (vmImages.isEmpty()) {
            return ValidationResult.VALID;
        }

        if (!vm.isAutoStartup() || !isInternalExecution) {
            Set<Guid> storageDomainIds = ImagesHandler.getAllStorageIdsForImageIds(vmImages);
            MultipleStorageDomainsValidator storageDomainValidator =
                    new MultipleStorageDomainsValidator(vm.getStoragePoolId(), storageDomainIds);

            ValidationResult result = storageDomainValidator.allDomainsExistAndActive();
            if (!result.isValid()) {
                return result;
            }

            result = !vm.isAutoStartup() ? storageDomainValidator.allDomainsWithinThresholds()
                    : ValidationResult.VALID;
            if (!result.isValid()) {
                return result;
            }
        }

        return ValidationResult.VALID;
    }

    /**
     * Check isValid only if VM is not HA VM
     */
    protected ValidationResult validateImagesForRunVm(VM vm, List<DiskImage> vmDisks) {
        if (vmDisks.isEmpty()) {
            return ValidationResult.VALID;
        }

        return !vm.isAutoStartup() ?
                new DiskImagesValidator(vmDisks).diskImagesNotLocked() : ValidationResult.VALID;
    }

    protected ValidationResult validateIsoPath(VM vm, String diskPath, String floppyPath, Guid activeIsoDomainId) {
        if (vm.isAutoStartup()) {
            return ValidationResult.VALID;
        }

        if (StringUtils.isEmpty(vm.getIsoPath()) && StringUtils.isEmpty(diskPath) && StringUtils.isEmpty(floppyPath)) {
            return ValidationResult.VALID;
        }

        if (activeIsoDomainId == null) {
            return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_FROM_CD_WITHOUT_ACTIVE_STORAGE_DOMAIN_ISO);
        }

        if (!StringUtils.isEmpty(diskPath) && !isRepoImageExists(diskPath, activeIsoDomainId, ImageFileType.ISO)) {
            return new ValidationResult(VdcBllMessages.ERROR_CANNOT_FIND_ISO_IMAGE_PATH);
        }

        if (!StringUtils.isEmpty(floppyPath) && !isRepoImageExists(floppyPath, activeIsoDomainId, ImageFileType.Floppy)) {
            return new ValidationResult(VdcBllMessages.ERROR_CANNOT_FIND_FLOPPY_IMAGE_PATH);
        }

        return ValidationResult.VALID;
    }

    protected ValidationResult vmDuringInitialization(VM vm) {
        if (vm.isRunning() || vm.getStatus() == VMStatus.NotResponding ||
                isVmDuringInitiating(vm)) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_RUNNING);
        }

        return ValidationResult.VALID;
    }

    protected ValidationResult validateVdsStatus(VM vm) {
        if (vm.getStatus() == VMStatus.Paused && vm.getRunOnVds() != null &&
                getVdsDynamic(vm.getRunOnVds()).getStatus() != VDSStatus.Up) {
            return new ValidationResult(
                    VdcBllMessages.ACTION_TYPE_FAILED_VDS_STATUS_ILLEGAL,
                    VdcBllMessages.VAR__HOST_STATUS__UP.toString());
        }

        return ValidationResult.VALID;
    }

    protected ValidationResult validateStatelessVm(VM vm, Boolean stateless) {
        // if the VM is not stateless, there is nothing to check
        if (stateless != null ? !stateless : !vm.isStateless()) {
            return ValidationResult.VALID;
        }

        ValidationResult previewValidation = getSnapshotValidator().vmNotInPreview(vm.getId());
        if (!previewValidation.isValid()) {
            return previewValidation;
        }

        // if the VM itself is stateless or run once as stateless
        if (vm.isAutoStartup()) {
            return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_STATELESS_HA);
        }

        ValidationResult hasSpaceValidation = hasSpaceForSnapshots();
        if (!hasSpaceValidation.isValid()) {
            return hasSpaceValidation;
        }

        return ValidationResult.VALID;
    }

    protected ValidationResult validateVmStatusUsingMatrix(VM vm) {
        if (!VdcActionUtils.canExecute(Arrays.asList(vm), VM.class,
                VdcActionType.RunVm)) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL, LocalizedVmStatus.from(vm.getStatus()));
        }

        return ValidationResult.VALID;
    }

    /**
     * check that we can create snapshots for all disks
     * return true if all storage domains have enough space to create snapshots for this VM plugged disks
     */
    protected ValidationResult hasSpaceForSnapshots() {
        Set<Guid> sdIds = ImagesHandler.getAllStorageIdsForImageIds(getVmImageDisks());

        MultipleStorageDomainsValidator msdValidator = getStorageDomainsValidator(sdIds);
        ValidationResult retVal = msdValidator.allDomainsWithinThresholds();
        if (retVal == ValidationResult.VALID) {
            return msdValidator.allDomainsHaveSpaceForNewDisks(getVmImageDisks());
        }
        return retVal;
    }

    protected MultipleStorageDomainsValidator getStorageDomainsValidator(Collection<Guid> sdIds) {
        Guid spId = vm.getStoragePoolId();
        return new MultipleStorageDomainsValidator(spId, sdIds);
    }

    protected ValidationResult validateStoragePoolUp(VM vm, StoragePool storagePool, List<DiskImage> vmImages) {
        if (vmImages.isEmpty() || vm.isAutoStartup()) {
            return ValidationResult.VALID;
        }

        return new StoragePoolValidator(storagePool).isUp();
    }

    /**
     * Checking that the interfaces are all configured, interfaces with no network are allowed only if network linking
     * is supported.
     *
     * @return true if all VM network interfaces are attached to existing cluster networks, or to no network (when
     *         network linking is supported).
     */
    protected ValidationResult validateInterfacesConfigured(VM vm) {
        for (VmNetworkInterface nic : vm.getInterfaces()) {
            if (nic.getVnicProfileId() == null) {
                return FeatureSupported.networkLinking(vm.getVdsGroupCompatibilityVersion()) ?
                        ValidationResult.VALID:
                            new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_INTERFACE_NETWORK_NOT_CONFIGURED);
            }
        }

        return ValidationResult.VALID;
    }

    /**
     * @param clusterNetworksNames
     *            cluster logical networks names
     * @param interfaceNetworkNames
     *            VM interface network names
     * @return true if all VM network interfaces are attached to existing cluster networks
     */
    protected ValidationResult validateInterfacesAttachedToClusterNetworks(VM vm,
            final Set<String> clusterNetworkNames, final Set<String> interfaceNetworkNames) {

        Set<String> result = new HashSet<String>(interfaceNetworkNames);
        result.removeAll(clusterNetworkNames);
        if (FeatureSupported.networkLinking(vm.getVdsGroupCompatibilityVersion())) {
            result.remove(null);
        }

        // If after removing the cluster network names we still have objects, then we have interface on networks that
        // aren't attached to the cluster
        return result.isEmpty() ?
                ValidationResult.VALID
                : new ValidationResult(
                        VdcBllMessages.ACTION_TYPE_FAILED_NETWORK_NOT_IN_CLUSTER,
                        String.format("$networks %1$s", StringUtils.join(result, ",")));
    }

    /**
     * @param clusterNetworks
     *            cluster logical networks
     * @param interfaceNetworkNames
     *            VM interface network names
     * @return true if all VM network interfaces are attached to VM networks
     */
    protected ValidationResult validateInterfacesAttachedToVmNetworks(final List<Network> clusterNetworks,
            Set<String> interfaceNetworkNames) {
        List<String> nonVmNetworkNames =
                NetworkUtils.filterNonVmNetworkNames(clusterNetworks, interfaceNetworkNames);

        return nonVmNetworkNames.isEmpty() ?
                ValidationResult.VALID
                : new ValidationResult(
                        VdcBllMessages.ACTION_TYPE_FAILED_NOT_A_VM_NETWORK,
                        String.format("$networks %1$s", StringUtils.join(nonVmNetworkNames, ",")));
    }

    ///////////////////////
    /// Utility methods ///
    ///////////////////////

    protected boolean validate(ValidationResult validationResult, List<String> message) {
        if (!validationResult.isValid()) {
            message.add(validationResult.getMessage().name());
            if (validationResult.getVariableReplacements() != null) {
                for (String variableReplacement : validationResult.getVariableReplacements()) {
                    message.add(variableReplacement);
                }
            }
        }
        return validationResult.isValid();
    }

    protected NetworkDao getNetworkDao() {
        return DbFacade.getInstance().getNetworkDao();
    }

    protected VdsDynamicDAO getVdsDynamicDao() {
        return DbFacade.getInstance().getVdsDynamicDao();
    }

    protected BackendInternal getBackend() {
        return Backend.getInstance();
    }

    protected VmNicDao getVmNicDao() {
        return DbFacade.getInstance().getVmNicDao();
    }

    protected StorageDomainDAO getStorageDomainDAO() {
        return DbFacade.getInstance().getStorageDomainDao();
    }

    protected VmPropertiesUtils getVmPropertiesUtils() {
        return VmPropertiesUtils.getInstance();
    }

    protected DiskDao getDiskDao() {
        return DbFacade.getInstance().getDiskDao();
    }

    private boolean isRepoImageExists(String repoImagePath, Guid storageDomainId, ImageFileType imageFileType) {
        VdcQueryReturnValue ret = getBackend().runInternalQuery(
                VdcQueryType.GetImagesList,
                new GetImagesListParameters(storageDomainId, imageFileType));

        if (ret != null && ret.getReturnValue() != null && ret.getSucceeded()) {
            for (RepoImage isoFileMetaData : ret.<List<RepoImage>>getReturnValue()) {
                if (repoImagePath.equals(isoFileMetaData.getRepoImageId())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isVmDuringInitiating(VM vm) {
        return (Boolean) getBackend()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.IsVmDuringInitiating,
                        new IsVmDuringInitiatingVDSCommandParameters(vm.getId()))
                .getReturnValue();
    }

    protected SnapshotsValidator getSnapshotValidator() {
        return new SnapshotsValidator();
    }

    private VdsDynamic getVdsDynamic(Guid vdsId) {
        return getVdsDynamicDao().get(vdsId);
    }

    private List<Disk> getVmDisks() {
        if (cachedVmDisks == null) {
            cachedVmDisks = getDiskDao().getAllForVm(vm.getId(), true);
        }

        return cachedVmDisks;
    }

    private List<DiskImage> getVmImageDisks() {
        if (cachedVmImageDisks == null) {
            cachedVmImageDisks = ImagesHandler.filterImageDisks(getVmDisks(), true, false, false);
        }

        return cachedVmImageDisks;
    }

    private Set<String> getInterfaceNetworkNames() {
        if (cachedInterfaceNetworkNames == null) {
            cachedInterfaceNetworkNames = Entities.vmInterfacesByNetworkName(vm.getInterfaces()).keySet();
        }

        return cachedInterfaceNetworkNames;
    }

    private List<Network> getClusterNetworks() {
        if (cachedClusterNetworks == null) {
            cachedClusterNetworks = getNetworkDao().getAllForCluster(vm.getVdsGroupId());
        }

        return cachedClusterNetworks;
    }

    private Set<String> getClusterNetworksNames() {
        if (cachedClusterNetworksNames == null) {
            cachedClusterNetworksNames = Entities.objectNames(getClusterNetworks());
        }

        return cachedClusterNetworksNames;
    }

    public OsRepository getOsRepository() {
        return osRepository;
    }
}
