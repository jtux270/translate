package org.ovirt.engine.core.bll;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.VmInterfaceManager;
import org.ovirt.engine.core.bll.network.cluster.NetworkHelper;
import org.ovirt.engine.core.bll.profiles.DiskProfileHelper;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaSanityParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.quota.QuotaVdsDependent;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.storage.CINDERStorageHelper;
import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.utils.IconUtils;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.IconValidator;
import org.ovirt.engine.core.bll.validator.VmValidationUtils;
import org.ovirt.engine.core.bll.validator.VmWatchdogValidator;
import org.ovirt.engine.core.bll.validator.storage.CinderDisksValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddVmParameters;
import org.ovirt.engine.core.common.action.CloneCinderDisksParameters;
import org.ovirt.engine.core.common.action.CreateSnapshotFromTemplateParameters;
import org.ovirt.engine.core.common.action.GraphicsParameters;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.RngDeviceParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmNumaNodeOperationParameters;
import org.ovirt.engine.core.common.action.WatchdogParameters;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.ImageType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.Permission;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmNumaNode;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.queries.VmIconIdSizePair;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.common.validation.group.CreateVm;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.PermissionDao;
import org.ovirt.engine.core.utils.linq.All;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

/**
 * This class adds a thinly provisioned VM over a template
 */
@DisableInPrepareMode
@NonTransactiveCommandAttribute
public class AddVmCommand<T extends AddVmParameters> extends VmManagementCommandBase<T>
        implements QuotaStorageDependent, QuotaVdsDependent {

    private static final Base64 BASE_64 = new Base64(0, null);
    protected HashMap<Guid, DiskImage> diskInfoDestinationMap;
    protected Map<Guid, StorageDomain> destStorages = new HashMap<>();
    protected Map<Guid, List<DiskImage>> storageToDisksMap;
    private String cachedDiskSharedLockMessage;
    protected Guid imageTypeId;
    protected ImageType imageType;
    private Guid vmInterfacesSourceId;
    protected VmTemplate vmDisksSource;
    private Guid vmDevicesSourceId;
    private List<StorageDomain> poolDomains;

    private Map<Guid, Guid> srcDiskIdToTargetDiskIdMapping = new HashMap<>();
    private Map<Guid, Guid> srcVmNicIdToTargetVmNicIdMapping = new HashMap<>();

    public AddVmCommand(T parameters) {
        this(parameters, null);
    }

    protected AddVmCommand(Guid commandId) {
        super(commandId);
    }

    public AddVmCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        // if we came from endAction the VmId is not null
        setVmId((parameters.getVmId().equals(Guid.Empty)) ? Guid.newGuid() : parameters.getVmId());
        setVmName(parameters.getVm().getName());
        parameters.setVmId(getVmId());
        setStorageDomainId(getParameters().getStorageDomainId());
    }

    @Override
    protected void init() {
        T parameters = getParameters();
        if (parameters.getVmStaticData() != null) {
            Guid templateIdToUse = getParameters().getVmStaticData().getVmtGuid();

            if (parameters.getVmStaticData().isUseLatestVersion()) {
                VmTemplate latest = getVmTemplateDao().getTemplateWithLatestVersionInChain(templateIdToUse);

                if (latest != null) {
                    // if not using original template, need to override storage mappings
                    // as it may have different set of disks
                    if (!templateIdToUse.equals(latest.getId())) {
                        getParameters().setDiskInfoDestinationMap(new HashMap<Guid, DiskImage>());
                    }

                    setVmTemplate(latest);
                    templateIdToUse = latest.getId();
                    getParameters().getVmStaticData().setVmtGuid(templateIdToUse);
                }
            }

            setVmTemplateId(templateIdToUse);

            // API backward compatibility
            if (parameters.isSoundDeviceEnabled() == null) {
                parameters.setSoundDeviceEnabled(parameters.getVmStaticData().getVmType() == VmType.Desktop);
            }

            if (parameters.isConsoleEnabled() == null) {
                parameters.setConsoleEnabled(false);
            }

            vmDevicesSourceId = (getInstanceTypeId() != null) ?
                    getInstanceTypeId() : parameters.getVmStaticData().getVmtGuid();
            imageTypeId = parameters.getVmStaticData().getImageTypeId();
            vmInterfacesSourceId = parameters.getVmStaticData().getVmtGuid();
            vmDisksSource = getVmTemplate();
        }

        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VM, getVmId()));

        // override values here for canDoACtion to run with correct values, has to come before init-disks
        updateVmObject();

        initTemplateDisks();
        initStoragePoolId();
        diskInfoDestinationMap = getParameters().getDiskInfoDestinationMap();
        if (diskInfoDestinationMap == null) {
            diskInfoDestinationMap = new HashMap<>();
        }
        VmHandler.updateDefaultTimeZone(parameters.getVmStaticData());

        // Fill the migration policy if it was omitted
        if (getParameters().getVmStaticData() != null &&
                getParameters().getVmStaticData().getMigrationSupport() == null) {
            setDefaultMigrationPolicy();
        }
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Command);
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        Map<String, Pair<String, String>> locks = new HashMap<>();
        locks.put(getVmTemplateId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.TEMPLATE, getTemplateSharedLockMessage()));
        for (DiskImage image: getImagesToCheckDestinationStorageDomains()) {
            locks.put(image.getId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.DISK, getDiskSharedLockMessage()));
        }
        if (getParameters().getPoolId() != null) {
            locks.put(getParameters().getPoolId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM_POOL, getPoolSharedLockMessage()));
        }
        return locks;
    }

    private String getTemplateSharedLockMessage() {
        return new StringBuilder(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_IS_USED_FOR_CREATE_VM.name())
                .append(String.format("$VmName %1$s", getVmName()))
                .toString();
    }

    protected String getDiskSharedLockMessage() {
        if (cachedDiskSharedLockMessage == null) {
            cachedDiskSharedLockMessage = new StringBuilder(EngineMessage.ACTION_TYPE_FAILED_DISK_IS_USED_FOR_CREATE_VM.name())
            .append(String.format("$VmName %1$s", getVmName()))
            .toString();
        }
        return cachedDiskSharedLockMessage;
    }

    private String getPoolSharedLockMessage() {
        return new StringBuilder(EngineMessage.ACTION_TYPE_FAILED_VM_POOL_IS_USED_FOR_CREATE_VM.name())
                .append(String.format("$VmName %1$s", getVmName()))
                .toString();
    }

    protected ImageType getImageType() {
        if (imageType == null && imageTypeId != null) {
            imageType = getVmTemplateDao().getImageType(imageTypeId);
        }
        return imageType;
    }

    protected void initStoragePoolId() {
        if (getVdsGroup() != null) {
            setStoragePoolId(getVdsGroup().getStoragePoolId() != null ? getVdsGroup().getStoragePoolId()
                    : Guid.Empty);
        }
    }

    protected void initTemplateDisks() {
        if (vmDisksSource != null) {
            VmTemplateHandler.updateDisksFromDb(vmDisksSource);
        }
    }

    private Guid _vmSnapshotId = Guid.Empty;

    protected Guid getVmSnapshotId() {
        return _vmSnapshotId;
    }

    protected List<VmNic> _vmInterfaces;

    protected List<VmNic> getVmInterfaces() {
        if (_vmInterfaces == null) {
            List<VmNic> vmNetworkInterfaces = getVmNicDao().getAllForTemplate(vmInterfacesSourceId);
            _vmInterfaces = vmNetworkInterfaces == null ? new ArrayList<VmNic>() : vmNetworkInterfaces;
        }
        return _vmInterfaces;
    }

    protected Map<Guid, VmDevice> getVmInterfaceDevices() {
        List<VmDevice> vmInterfaceDevicesList = getVmDeviceDao().getVmDeviceByVmIdAndType(vmInterfacesSourceId, VmDeviceGeneralType.INTERFACE);
        Map<Guid, VmDevice> vmInterfaceDevices = new HashMap<>();
        for (VmDevice device : vmInterfaceDevicesList) {
            vmInterfaceDevices.put(device.getDeviceId(), device);
        }
        return vmInterfaceDevices;
    }

    protected List<? extends Disk> _vmDisks;

    protected List<? extends Disk> getVmDisks() {
        if (_vmDisks == null) {
            _vmDisks =
                    DbFacade.getInstance()
                            .getDiskDao()
                            .getAllForVm(vmDisksSource.getId());
        }

        return _vmDisks;
    }

    protected boolean canAddVm(ArrayList<String> reasons, Collection<StorageDomain> destStorages) {
        VmStatic vmStaticFromParams = getParameters().getVmStaticData();
        if (!canAddVm(reasons, vmStaticFromParams.getName(), getStoragePoolId(), vmStaticFromParams.getPriority())) {
            return false;
        }

        if (!validateCustomProperties(vmStaticFromParams, reasons)) {
            return false;
        }

        // check that template image and vm are on the same storage pool
        if (shouldCheckSpaceInStorageDomains()) {
            if (!getStoragePoolId().equals(getStoragePoolIdFromSourceImageContainer())) {
                reasons.add(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_MATCH.toString());
                return false;
            }
            for (StorageDomain domain : destStorages) {
               StorageDomainValidator storageDomainValidator = new StorageDomainValidator(domain);
               if (!validate(storageDomainValidator.isDomainExistAndActive())) {
                   return false;
               }
            }
            if (!validateSpaceRequirements()) {
                return false;
            }
        }
        return VmHandler.validateDedicatedVdsExistOnSameCluster(vmStaticFromParams,
                getReturnValue().getCanDoActionMessages());
    }

    protected boolean shouldCheckSpaceInStorageDomains() {
        return !getImagesToCheckDestinationStorageDomains().isEmpty()
                && !LinqUtils.firstOrNull(getImagesToCheckDestinationStorageDomains(), new All<DiskImage>())
                        .getImageId().equals(VmTemplateHandler.BLANK_VM_TEMPLATE_ID);
    }

    protected void setDefaultMigrationPolicy() {
        if (getVdsGroup() != null) {
            boolean isMigrationSupported =
                    FeatureSupported.isMigrationSupported(getVdsGroup().getArchitecture(),
                            getVdsGroup().getCompatibilityVersion());

            MigrationSupport migrationSupport =
                    isMigrationSupported ? MigrationSupport.MIGRATABLE : MigrationSupport.PINNED_TO_HOST;

            getParameters().getVmStaticData().setMigrationSupport(migrationSupport);
        }
    }

    protected Guid getStoragePoolIdFromSourceImageContainer() {
        return vmDisksSource.getStoragePoolId();
    }

    protected boolean canDoAddVmCommand() {
        boolean returnValue = false;
        returnValue = areParametersLegal(getReturnValue().getCanDoActionMessages());
        // Check if number of monitors passed is legal
        returnValue = returnValue && checkNumberOfMonitors() && checkSingleQxlDisplay();

        returnValue =
                returnValue
                        && checkPciAndIdeLimit(getParameters().getVm().getOs(),
                                getVdsGroup().getCompatibilityVersion(),
                                getParameters().getVmStaticData().getNumOfMonitors(),
                                getVmInterfaces(),
                                getVmDisks(),
                                isVirtioScsiEnabled(),
                                hasWatchdog(),
                                isBalloonEnabled(),
                                getParameters().isSoundDeviceEnabled(),
                                getReturnValue().getCanDoActionMessages())
                        && canAddVm(getReturnValue().getCanDoActionMessages(), destStorages.values())
                        && hostToRunExist();
        return returnValue;
    }

    /**
     * Check if destination storage has enough space
     * @return
     */
    protected boolean validateSpaceRequirements() {
        for (Map.Entry<Guid, List<DiskImage>> sdImageEntry : storageToDisksMap.entrySet()) {
            StorageDomain destStorageDomain = destStorages.get(sdImageEntry.getKey());
            List<DiskImage> disksList = sdImageEntry.getValue();
            StorageDomainValidator storageDomainValidator = createStorageDomainValidator(destStorageDomain);
            if (!validateDomainsThreshold(storageDomainValidator) ||
                !validateFreeSpace(storageDomainValidator, disksList)) {
                return false;
            }
        }
        return true;
    }

    protected StorageDomainValidator createStorageDomainValidator(StorageDomain storageDomain) {
        return new StorageDomainValidator(storageDomain);
    }

    private boolean validateDomainsThreshold(StorageDomainValidator storageDomainValidator) {
        return validate(storageDomainValidator.isDomainWithinThresholds());
    }

    /**
     * This validation is for thin provisioning, when done differently on other commands, this method should be overridden.
     */
    protected boolean validateFreeSpace(StorageDomainValidator storageDomainValidator, List<DiskImage> disksList)
    {
        Collection<DiskImage> disks = ImagesHandler.getDisksDummiesForStorageAllocations(disksList);
        return validate(storageDomainValidator.hasSpaceForNewDisks(disks));
    }

    protected boolean checkSingleQxlDisplay() {
        if (!getParameters().getVmStaticData().getSingleQxlPci()) {
            return true;
        }
        return (VmHandler.isSingleQxlDeviceLegal(getParameters().getVm().getDefaultDisplayType(),
                        getParameters().getVm().getOs(),
                        getReturnValue().getCanDoActionMessages(),
                        getVdsGroup().getCompatibilityVersion()));
    }

    protected boolean hostToRunExist() {
        List<Guid> dedicatedHostsList = getParameters().getVmStaticData().getDedicatedVmForVdsList();
        if (dedicatedHostsList.isEmpty()){
            return true;
        }
        for (Guid candidateHostGuid : dedicatedHostsList) {
            if (DbFacade.getInstance().getVdsDao().get(candidateHostGuid) == null) {
                addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_HOST_NOT_EXIST);
                return false;
            }
        }
        return true;
    }

    public static boolean checkCpuSockets(int num_of_sockets, int cpu_per_socket, String compatibility_version,
                                          List<String> CanDoActionMessages) {
        boolean retValue = true;
        if (retValue
                && (num_of_sockets * cpu_per_socket) > Config.<Integer> getValue(ConfigValues.MaxNumOfVmCpus,
                        compatibility_version)) {
            CanDoActionMessages.add(EngineMessage.ACTION_TYPE_FAILED_MAX_NUM_CPU.toString());
            retValue = false;
        }
        if (retValue
                && num_of_sockets > Config.<Integer> getValue(ConfigValues.MaxNumOfVmSockets, compatibility_version)) {
            CanDoActionMessages.add(EngineMessage.ACTION_TYPE_FAILED_MAX_NUM_SOCKETS.toString());
            retValue = false;
        }
        if (retValue
                && cpu_per_socket > Config.<Integer> getValue(ConfigValues.MaxNumOfCpuPerSocket, compatibility_version)) {
            CanDoActionMessages.add(EngineMessage.ACTION_TYPE_FAILED_MAX_CPU_PER_SOCKET.toString());
            retValue = false;
        }
        if (retValue && cpu_per_socket < 1) {
            CanDoActionMessages.add(EngineMessage.ACTION_TYPE_FAILED_MIN_CPU_PER_SOCKET.toString());
            retValue = false;
        }
        if (retValue && num_of_sockets < 1) {
            CanDoActionMessages.add(EngineMessage.ACTION_TYPE_FAILED_MIN_NUM_SOCKETS.toString());
            retValue = false;
        }
        return retValue;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateVm.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__ADD);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM);
    }

    @Override
    protected boolean canDoAction() {
        if (getVdsGroup() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_CAN_NOT_BE_EMPTY);
        }

        if (getVmTemplate() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
        }

        if (getVmTemplate().isDisabled()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_IS_DISABLED);
        }

        if (getStoragePool() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_EXIST);
        }

        if (getStoragePool().getStatus() != StoragePoolStatus.Up) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_IMAGE_REPOSITORY_NOT_FOUND);
        }

        if (!isTemplateInValidDc()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_NOT_EXISTS_IN_CURRENT_DC);
        }

        // A VM cannot be added in a cluster without a defined architecture
        if (getVdsGroup().getArchitecture() == ArchitectureType.undefined) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_UNDEFINED_ARCHITECTURE);
        }

        if (verifySourceDomains() && buildAndCheckDestStorageDomains()) {
            chooseDisksSourceDomains();
        } else {
            return false;
        }

        if (isBalloonEnabled() && !osRepository.isBalloonEnabled(getParameters().getVmStaticData().getOsId(),
                getVdsGroup().getCompatibilityVersion())) {
            addCanDoActionMessageVariable("clusterArch", getVdsGroup().getArchitecture());
            return failCanDoAction(EngineMessage.BALLOON_REQUESTED_ON_NOT_SUPPORTED_ARCH);
        }

        // otherwise..
        storageToDisksMap =
                ImagesHandler.buildStorageToDiskMap(getImagesToCheckDestinationStorageDomains(),
                        diskInfoDestinationMap);

        if (!canDoAddVmCommand()) {
            return false;
        }

        VM vmFromParams = getParameters().getVm();

        // check if the selected template is compatible with Cluster architecture.
        if (!getVmTemplate().getId().equals(VmTemplateHandler.BLANK_VM_TEMPLATE_ID)
                && getVdsGroup().getArchitecture() != getVmTemplate().getClusterArch()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_IS_INCOMPATIBLE);
        }

        if (StringUtils.isEmpty(vmFromParams.getName())) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_NAME_MAY_NOT_BE_EMPTY);
        }

        // check that VM name is not too long
        if (!isVmNameValidLength(vmFromParams)) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_NAME_LENGTH_IS_TOO_LONG);
        }

        // check for Vm Payload
        if (getParameters().getVmPayload() != null) {
            if (!checkPayload(getParameters().getVmPayload(),
                    getParameters().getVmStaticData().getIsoPath())) {
                return false;
            }

            // otherwise, we save the content in base64 string
            for (Map.Entry<String, String> entry : getParameters().getVmPayload().getFiles().entrySet()) {
                entry.setValue(new String(BASE_64.encode(entry.getValue().getBytes()), Charset.forName(CharEncoding.UTF_8)));
            }
        }

        // check for Vm Watchdog Model
        if (getParameters().getWatchdog() != null) {
            if (!validate((new VmWatchdogValidator(vmFromParams.getOs(),
                    getParameters().getWatchdog(),
                    getVdsGroup().getCompatibilityVersion())).isValid())) {
                return false;
            }
        }

        // Check that the USB policy is legal
        if (!VmHandler.isUsbPolicyLegal(vmFromParams.getUsbPolicy(), vmFromParams.getOs(),
                getVdsGroup(), getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // check if the OS type is supported
        if (!VmHandler.isOsTypeSupported(vmFromParams.getOs(), getVdsGroup().getArchitecture(),
                getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        if (!isCpuSupported(vmFromParams)) {
            return false;
        }

        // Check if the graphics and display from parameters are supported
        if (!VmHandler.isGraphicsAndDisplaySupported(getParameters().getVmStaticData().getOsId(),
                VmHandler.getResultingVmGraphics(VmDeviceUtils.getGraphicsTypesOfEntity(getVmTemplateId()), getParameters().getGraphicsDevices()),
                vmFromParams.getDefaultDisplayType(),
                getReturnValue().getCanDoActionMessages(),
                getVdsGroup().getCompatibilityVersion())) {
            return false;
        }

        if (!FeatureSupported.isMigrationSupported(getVdsGroup().getArchitecture(), getVdsGroup().getCompatibilityVersion())
                && vmFromParams.getMigrationSupport() != MigrationSupport.PINNED_TO_HOST) {
            return failCanDoAction(EngineMessage.VM_MIGRATION_IS_NOT_SUPPORTED);
        }

        // check cpuPinning if the check haven't failed yet
        if (!isCpuPinningValid(vmFromParams.getCpuPinning(), vmFromParams.getStaticData())) {
            return false;
        }

        if (vmFromParams.isUseHostCpuFlags()
                && vmFromParams.getMigrationSupport() != MigrationSupport.PINNED_TO_HOST) {
            return failCanDoAction(EngineMessage.VM_HOSTCPU_MUST_BE_PINNED_TO_HOST);
        }

        if (getInstanceTypeId() != null && getInstanceType() == null) {
            // invalid instance type
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_INSTANCE_TYPE_DOES_NOT_EXIST);
        }

        if (imageTypeId != null && getImageType() == null) {
            // invalid image type
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_IMAGE_TYPE_DOES_NOT_EXIST);
        }

        if (!checkCpuSockets()){
            return false;
        }

        if (!isCpuSharesValid(vmFromParams)) {
            return failCanDoAction(EngineMessage.QOS_CPU_SHARES_OUT_OF_RANGE);
        }

        if (Boolean.TRUE.equals(getParameters().isVirtioScsiEnabled())) {
            // Verify cluster compatibility
            if (!FeatureSupported.virtIoScsi(getVdsGroup().getCompatibilityVersion())) {
                return failCanDoAction(EngineMessage.VIRTIO_SCSI_INTERFACE_IS_NOT_AVAILABLE_FOR_CLUSTER_LEVEL);
            }

            // Verify OS compatibility
            if (!VmHandler.isOsTypeSupportedForVirtioScsi(vmFromParams.getOs(), getVdsGroup().getCompatibilityVersion(),
                    getReturnValue().getCanDoActionMessages())) {
                return false;
            }
        }

        if (vmFromParams.getMinAllocatedMem() > vmFromParams.getMemSizeMb()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_MIN_MEMORY_CANNOT_EXCEED_MEMORY_SIZE);
        }

        if (!setAndValidateDiskProfiles()) {
            return false;
        }

        if (!setAndValidateCpuProfile()) {
            return false;
        }

        if (!validate(VmHandler.checkNumaPreferredTuneMode(getParameters().getVmStaticData()
                        .getNumaTuneMode(),
                getParameters().getVmStaticData().getvNumaNodeList(), getVmId()))) {
            return false;
        }

        if (getVmId() != null && getVmStaticDao().get(getVmId()) != null) {
            return failCanDoAction(EngineMessage.VM_ID_EXISTS);
        }

        List<CinderDisk> cinderDisks = ImagesHandler.filterDisksBasedOnCinder(diskInfoDestinationMap.values());
        CinderDisksValidator cinderDisksValidator = new CinderDisksValidator(cinderDisks);
        if (!validate(cinderDisksValidator.validateCinderDiskLimits())) {
            return false;
        }

        if (getParameters().getVmLargeIcon() != null && !validate(IconValidator.validate(
                IconValidator.DimensionsType.LARGE_CUSTOM_ICON,
                getParameters().getVmLargeIcon()))) {
            return false;
        }

        if (getSmallIconId() != null
                && !validate(IconValidator.validateIconId(getSmallIconId(), "Small"))) {
            return false;
        }

        if (getLargeIconId() != null
                && !validate(IconValidator.validateIconId(getLargeIconId(), "Large"))) {
            return false;
        }

        // validate NUMA nodes count not more than CPUs
        if (getParameters().getVm().getMigrationSupport() == MigrationSupport.PINNED_TO_HOST &&
                !validate(VmHandler.checkVmNumaNodesIntegrity(getParameters().getVm(),
                        getParameters().getVm(),
                        getParameters().isUpdateNuma()))) {
            return false;
        }

        return true;
    }

    protected Guid getSmallIconId() {
        if (getParameters().getVmStaticData() != null) {
            return getParameters().getVmStaticData().getSmallIconId();
        }
        return null;
    }

    protected Guid getLargeIconId() {
        if (getParameters().getVmStaticData() != null) {
            return getParameters().getVmStaticData().getLargeIconId();
        }
        return null;
    }

    protected boolean setAndValidateDiskProfiles() {
        if (diskInfoDestinationMap != null && !diskInfoDestinationMap.isEmpty()) {
            Map<DiskImage, Guid> map = new HashMap<>();
            List<DiskImage> diskImages = ImagesHandler.filterImageDisks(diskInfoDestinationMap.values(), true, false, true);
            for (DiskImage diskImage : diskImages) {
                map.put(diskImage, diskImage.getStorageIds().get(0));
            }
            return validate(DiskProfileHelper.setAndValidateDiskProfiles(map,
                    getStoragePool().getCompatibilityVersion(), getCurrentUser()));
        }
        return true;
    }

    protected boolean checkTemplateImages(List<String> reasons) {
        if (getParameters().getParentCommand() == VdcActionType.AddVmPoolWithVms) {
            return true;
        }

        for (StorageDomain storage : destStorages.values()) {
            if (!VmTemplateCommand.isVmTemplateImagesReady(vmDisksSource, storage.getId(),
                    reasons, false, false, true, true,
                    storageToDisksMap.get(storage.getId()))) {
                return false;
            }
        }
        return true;
    }

    protected boolean checkCpuSockets() {
        return AddVmCommand.checkCpuSockets(getParameters().getVmStaticData().getNumOfSockets(),
                getParameters().getVmStaticData().getCpuPerSocket(), getVdsGroup().getCompatibilityVersion()
                .toString(), getReturnValue().getCanDoActionMessages());
    }

    protected boolean buildAndCheckDestStorageDomains() {
        boolean retValue = true;
        if (diskInfoDestinationMap.isEmpty()) {
            retValue = fillDestMap();
        } else {
            retValue = validateProvidedDestinations();
        }
        if (retValue && getImagesToCheckDestinationStorageDomains().size() != diskInfoDestinationMap.size()) {
            log.error("Can not find any default active domain for one of the disks of template with id '{}'",
                    vmDisksSource.getId());
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_MISSED_STORAGES_FOR_SOME_DISKS);
            retValue = false;
        }

        return retValue && validateIsImagesOnDomains();
    }

    protected boolean verifySourceDomains() {
        return true;
    }

    protected void chooseDisksSourceDomains() {}

    protected Collection<DiskImage> getImagesToCheckDestinationStorageDomains() {
        return vmDisksSource.getDiskTemplateMap().values();
    }

    private boolean validateProvidedDestinations() {
        for (DiskImage diskImage : diskInfoDestinationMap.values()) {
            if (diskImage.getStorageIds() == null || diskImage.getStorageIds().isEmpty()) {
                diskImage.setStorageIds(new ArrayList<Guid>());
                diskImage.getStorageIds().add(getParameters().getStorageDomainId());
            }
            Guid storageDomainId = diskImage.getStorageIds().get(0);
            if (destStorages.get(storageDomainId) == null) {
                StorageDomain storage = DbFacade.getInstance().getStorageDomainDao().getForStoragePool(
                        storageDomainId, getStoragePoolId());
                StorageDomainValidator validator =
                        new StorageDomainValidator(storage);
                if (!validate(validator.isDomainExistAndActive()) || !validate(validator.domainIsValidDestination())) {
                    return false;
                }
                destStorages.put(storage.getId(), storage);
            }
        }
        return true;
    }

    private boolean fillDestMap() {
        if (getParameters().getStorageDomainId() != null
                && !Guid.Empty.equals(getParameters().getStorageDomainId())) {
            Guid storageId = getParameters().getStorageDomainId();
            for (DiskImage image : getImagesToCheckDestinationStorageDomains()) {
                diskInfoDestinationMap.put(image.getId(), makeNewImage(storageId, image));
            }
            return validateProvidedDestinations();
        }
        fillImagesMapBasedOnTemplate();
        return true;
    }

    protected List<StorageDomain> getPoolDomains() {
        if (poolDomains == null) {
            poolDomains = getStorageDomainDao().getAllForStoragePool(vmDisksSource.getStoragePoolId());
        }
        return poolDomains;
    }

    protected void fillImagesMapBasedOnTemplate() {
        ImagesHandler.fillImagesMapBasedOnTemplate(vmDisksSource,
                getPoolDomains(),
                diskInfoDestinationMap,
                destStorages);
    }

    protected boolean validateIsImagesOnDomains() {
        for (DiskImage image : getImagesToCheckDestinationStorageDomains()) {
            if (!image.getStorageIds().containsAll(diskInfoDestinationMap.get(image.getId()).getStorageIds())) {
                addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_NOT_FOUND_ON_DESTINATION_DOMAIN);
                return false;
            }
        }
        return true;
    }

    private DiskImage makeNewImage(Guid storageId, DiskImage image) {
        DiskImage newImage = new DiskImage();
        newImage.setImageId(image.getImageId());
        newImage.setDiskAlias(image.getDiskAlias());
        newImage.setvolumeFormat(image.getVolumeFormat());
        newImage.setVolumeType(image.getVolumeType());
        ArrayList<Guid> storageIds = new ArrayList<>();
        storageIds.add(storageId);
        newImage.setStorageIds(storageIds);
        newImage.setQuotaId(image.getQuotaId());
        newImage.setDiskProfileId(image.getDiskProfileId());
        return newImage;
    }

    protected boolean canAddVm(List<String> reasons, String name, Guid storagePoolId,
            int vmPriority) {
        // Checking if a desktop with same name already exists
        if (isVmWithSameNameExists(name, storagePoolId)) {
            reasons.add(EngineMessage.ACTION_TYPE_FAILED_NAME_ALREADY_USED.name());
            return false;
        }

        if (!verifyAddVM(reasons, vmPriority)) {
            return false;
        }

        if (!checkTemplateImages(reasons)) {
            return false;
        }

        return true;
    }

    protected boolean verifyAddVM(List<String> reasons, int vmPriority) {
        return VmHandler.verifyAddVm(reasons, getVmInterfaces().size(), vmPriority, getMacPool());
    }

    @Override
    protected void executeVmCommand() {
        VmHandler.warnMemorySizeLegal(getParameters().getVm().getStaticData(), getVdsGroup().getCompatibilityVersion());

        ArrayList<String> errorMessages = new ArrayList<>();
        if (canAddVm(errorMessages, destStorages.values())) {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                    addVmStatic();
                    addVmDynamic();
                    addVmNetwork();
                    addVmNumaNodes();
                    addVmStatistics();
                    addActiveSnapshot();
                    addVmPermission();
                    addVmInit();
                    addVmRngDevice();
                    getCompensationContext().stateChanged();
                    return null;
                }
            });

            if (addVmImages()) {
                TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                    @Override
                    public Void runInTransaction() {
                        copyVmDevices();
                        addDiskPermissions();
                        addVmPayload();
                        updateSmartCardDevices();
                        addVmWatchdog();
                        addGraphicsDevice();
                        setActionReturnValue(getVm().getId());
                        setSucceeded(true);
                        return null;
                    }
                });
            }
        } else {
            log.error("Failed to add vm . The reasons are: {}", StringUtils.join(errorMessages, ','));
        }
    }

    private void addGraphicsDevice() {
        for (GraphicsDevice graphicsDevice : getParameters().getGraphicsDevices().values()) {
            if (graphicsDevice == null) {
                continue;
            }

            graphicsDevice.setVmId(getVmId());
            getBackend().runInternalAction(VdcActionType.AddGraphicsDevice, new GraphicsParameters(graphicsDevice));
        }
    }

    private void updateSmartCardDevices() {
        // if vm smartcard settings is different from device source's
        // add or remove the smartcard according to user request
        boolean smartcardOnDeviceSource = getInstanceTypeId() != null ? getInstanceType().isSmartcardEnabled() : getVmTemplate().isSmartcardEnabled();
        if (getVm().isSmartcardEnabled() != smartcardOnDeviceSource) {
            VmDeviceUtils.updateSmartcardDevice(getVm().getId(), getVm().isSmartcardEnabled());
        }
    }

    protected void addVmWatchdog() {
        VmWatchdog vmWatchdog = getParameters().getWatchdog();
        if (vmWatchdog != null) {
            WatchdogParameters parameters = new WatchdogParameters();
            parameters.setId(getParameters().getVmId());
            parameters.setAction(vmWatchdog.getAction());
            parameters.setModel(vmWatchdog.getModel());
            runInternalAction(VdcActionType.AddWatchdog, parameters, cloneContextAndDetachFromParent());
        }
    }

    private void addVmRngDevice() {
        VmRngDevice rngDev = getParameters().getRngDevice();
        if (rngDev != null) {
            rngDev.setVmId(getVmId());
            RngDeviceParameters params = new RngDeviceParameters(rngDev, true);
            VdcReturnValueBase result = runInternalAction(VdcActionType.AddRngDevice, params, cloneContextAndDetachFromParent());
            if (!result.getSucceeded()) {
                log.error("Couldn't add RNG device for new VM.");
                throw new IllegalArgumentException("Couldn't add RNG device for new VM.");
            }
        }
    }

    protected void addVmPayload() {
        VmPayload payload = getParameters().getVmPayload();

        if (payload != null) {
            VmDeviceUtils.addManagedDevice(new VmDeviceId(Guid.newGuid(), getParameters().getVmId()),
                    VmDeviceGeneralType.DISK,
                    payload.getDeviceType(),
                    payload.getSpecParams(),
                    true,
                    true);
        }
    }

    protected void copyVmDevices() {
        VmDeviceUtils.copyVmDevices(vmDevicesSourceId,
                getVmId(),
                getSrcDeviceIdToTargetDeviceIdMapping(),
                getParameters().isSoundDeviceEnabled(),
                getParameters().isConsoleEnabled(),
                isVirtioScsiEnabled(),
                isBalloonEnabled(),
                getParameters().getGraphicsDevices().keySet(),
                false);

        if (getInstanceTypeId() != null) {
            copyDiskDevicesFromTemplate();
        }
    }

    /**
     * If both the instance type and the template is set, than all the devices has to be copied from instance type except the
     * disk devices which has to be copied from the template (since the instance type has no disks but the template does have).
     */
    private void copyDiskDevicesFromTemplate() {
        List<VmDevice> disks =
                DbFacade.getInstance()
                        .getVmDeviceDao()
                        .getVmDeviceByVmIdTypeAndDevice(vmDisksSource.getId(),
                                VmDeviceGeneralType.DISK,
                                VmDeviceType.DISK.getName());
        VmDeviceUtils.copyDiskDevices(
                getVmId(),
                disks,
                getSrcDeviceIdToTargetDeviceIdMapping()
        );
    }

    protected static boolean isLegalClusterId(Guid clusterId, List<String> reasons) {
        // check given cluster id
        VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(clusterId);
        boolean legalClusterId = (vdsGroup != null);
        if (!legalClusterId) {
            reasons.add(EngineError.VM_INVALID_SERVER_CLUSTER_ID.toString());
        }
        return legalClusterId;
    }

    protected boolean areParametersLegal(List<String> reasons) {
        boolean returnValue = false;
        final VmStatic vmStaticData = getParameters().getVmStaticData();

        if (vmStaticData != null) {

            returnValue = isLegalClusterId(vmStaticData.getVdsGroupId(), reasons);

            if (!validatePinningAndMigration(reasons, vmStaticData, getParameters().getVm().getCpuPinning())) {
                returnValue = false;
            }

        }
        return returnValue;
    }

    protected void addVmNetwork() {
        List<VmNic> nics = getVmInterfaces();
        VmInterfaceManager vmInterfaceManager = new VmInterfaceManager(getMacPool());
        vmInterfaceManager.sortVmNics(nics, getVmInterfaceDevices());

        List<String> macAddresses = getMacPool().allocateMacAddresses(nics.size());

        // Add interfaces from template
        for (int i = 0; i < nics.size(); ++i) {
            VmNic iface = nics.get(i);
            Guid id = Guid.newGuid();
            srcVmNicIdToTargetVmNicIdMapping.put(iface.getId(), id);
            iface.setId(id);
            iface.setMacAddress(macAddresses.get(i));
            iface.setSpeed(VmInterfaceType.forValue(iface.getType()).getSpeed());
            iface.setVmTemplateId(null);
            iface.setVmId(getParameters().getVmStaticData().getId());
            updateProfileOnNic(iface);
            getVmNicDao().save(iface);
            getCompensationContext().snapshotNewEntity(iface);
            DbFacade.getInstance().getVmNetworkStatisticsDao().save(iface.getStatistics());
            getCompensationContext().snapshotNewEntity(iface.getStatistics());
        }
    }

    protected void addVmNumaNodes() {
        List<VmNumaNode> numaNodes = getParameters().getVmStaticData().getvNumaNodeList();
        VmNumaNodeOperationParameters params = new VmNumaNodeOperationParameters(getVmId(), numaNodes);
        params.setNumaTuneMode(getParameters().getVmStaticData().getNumaTuneMode());
        params.setDedicatedHostList(getParameters().getVmStaticData().getDedicatedVmForVdsList());
        params.setMigrationSupport(getParameters().getVmStaticData().getMigrationSupport());
        if (numaNodes == null || numaNodes.isEmpty()) {
            return;
        }
        VdcReturnValueBase returnValueBase = getBackend().runInternalAction(VdcActionType.AddVmNumaNodes, params);
        if (!returnValueBase.getSucceeded()) {
            auditLogDirector.log(this, AuditLogType.NUMA_ADD_VM_NUMA_NODE_FAILED);
        }
    }

    protected void addVmInit() {
        VmHandler.addVmInitToDB(getParameters().getVmStaticData());
    }

    protected void addVmStatic() {
        VmStatic vmStatic = getParameters().getVmStaticData();

        if (vmStatic.getOrigin() == null) {
            vmStatic.setOrigin(OriginType.valueOf(Config.<String> getValue(ConfigValues.OriginType)));
        }
        vmStatic.setId(getVmId());
        vmStatic.setQuotaId(getQuotaId());
        vmStatic.setCreationDate(new Date());
        vmStatic.setCreatedByUserId(getUserId());
        setIconIds(vmStatic);
        // Parses the custom properties field that was filled by frontend to
        // predefined and user defined fields
        VmPropertiesUtils.getInstance().separateCustomPropertiesToUserAndPredefined(
                getVdsGroup().getCompatibilityVersion(), vmStatic);

        updateOriginalTemplate(vmStatic);

        getVmStaticDao().save(vmStatic);
        getCompensationContext().snapshotNewEntity(vmStatic);
    }

    protected void updateOriginalTemplate(VmStatic vmStatic) {
        vmStatic.setOriginalTemplateGuid(vmStatic.getVmtGuid());
        vmStatic.setOriginalTemplateName(getVmTemplate().getName());
    }

    void addVmDynamic() {
        VmDynamic vmDynamic = new VmDynamic();
        vmDynamic.setId(getVmId());
        vmDynamic.setStatus(VMStatus.Down);
        vmDynamic.setVmHost("");
        vmDynamic.setVmIp("");
        vmDynamic.setVmFQDN("");
        vmDynamic.setLastStopTime(new Date());
        getDbFacade().getVmDynamicDao().save(vmDynamic);
        getCompensationContext().snapshotNewEntity(vmDynamic);
    }

    void addVmStatistics() {
        VmStatistics stats = new VmStatistics();
        stats.setId(getVmId());
        DbFacade.getInstance().getVmStatisticsDao().save(stats);
        getCompensationContext().snapshotNewEntity(stats);
    }

    protected boolean addVmImages() {
        if (!vmDisksSource.getDiskTemplateMap().isEmpty()) {
            if (getVm().getStatus() != VMStatus.Down) {
                log.error("Cannot add images. VM is not Down");
                throw new EngineException(EngineError.IRS_IMAGE_STATUS_ILLEGAL);
            }
            VmHandler.lockVm(getVmId());
            Collection<DiskImage> templateDisks = getImagesToCheckDestinationStorageDomains();
            List<DiskImage> diskImages = ImagesHandler.filterImageDisks(templateDisks, true, false, true);
            for (DiskImage image : diskImages) {
                VdcReturnValueBase result = runInternalActionWithTasksContext(
                        VdcActionType.CreateSnapshotFromTemplate,
                        buildCreateSnapshotFromTemplateParameters(image));

                /**
                 * if couldn't create snapshot then stop the transaction and the command
                 */
                if (!result.getSucceeded()) {
                    throw new EngineException(result.getFault().getError());
                } else {
                    getTaskIdList().addAll(result.getInternalVdsmTaskIdList());
                    DiskImage newImage = (DiskImage) result.getActionReturnValue();
                    srcDiskIdToTargetDiskIdMapping.put(image.getId(), newImage.getId());
                }
            }

            // Clone volumes for Cinder disk templates
            addVmCinderDisks(templateDisks);
        }
        return true;
    }

    private CreateSnapshotFromTemplateParameters buildCreateSnapshotFromTemplateParameters(DiskImage image) {
        CreateSnapshotFromTemplateParameters tempVar = new CreateSnapshotFromTemplateParameters(
                image.getImageId(), getParameters().getVmStaticData().getId());
        tempVar.setDestStorageDomainId(diskInfoDestinationMap.get(image.getId()).getStorageIds().get(0));
        tempVar.setDiskAlias(diskInfoDestinationMap.get(image.getId()).getDiskAlias());
        tempVar.setStorageDomainId(image.getStorageIds().get(0));
        tempVar.setVmSnapshotId(getVmSnapshotId());
        tempVar.setParentCommand(VdcActionType.AddVm);
        tempVar.setEntityInfo(getParameters().getEntityInfo());
        tempVar.setParentParameters(getParameters());
        tempVar.setQuotaId(diskInfoDestinationMap.get(image.getId()).getQuotaId());
        tempVar.setDiskProfileId(diskInfoDestinationMap.get(image.getId()).getDiskProfileId());

        return tempVar;
    }

    protected void addVmCinderDisks(Collection<DiskImage> templateDisks) {
        List<CinderDisk> cinderDisks = ImagesHandler.filterDisksBasedOnCinder(templateDisks);
        if (cinderDisks.isEmpty()) {
            return;
        }
        Future<VdcReturnValueBase> future = CommandCoordinatorUtil.executeAsyncCommand(
            VdcActionType.CloneCinderDisks,
            buildCinderChildCommandParameters(cinderDisks, getVmSnapshotId()),
            cloneContextAndDetachFromParent(),
            CINDERStorageHelper.getStorageEntities(cinderDisks));
        try {
            Map<Guid, Guid> diskImageMap = future.get().getActionReturnValue();
            srcDiskIdToTargetDiskIdMapping.putAll(diskImageMap);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error cloning Cinder disks from template disks.", e);
        }
    }

    private CloneCinderDisksParameters buildCinderChildCommandParameters(List<CinderDisk> cinderDisks, Guid vmSnapshotId) {
        CloneCinderDisksParameters createParams = new CloneCinderDisksParameters(cinderDisks, vmSnapshotId, diskInfoDestinationMap);
        createParams.setParentHasTasks(!getReturnValue().getVdsmTaskIdList().isEmpty());
        return withRootCommandInfo(createParams, getActionType());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? (!getReturnValue().getVdsmTaskIdList().isEmpty() ? AuditLogType.USER_ADD_VM_STARTED
                    : AuditLogType.USER_ADD_VM) : AuditLogType.USER_FAILED_ADD_VM;

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.USER_ADD_VM_FINISHED_SUCCESS
                    : AuditLogType.USER_ADD_VM_FINISHED_FAILURE;

        default:
            return AuditLogType.USER_ADD_VM_FINISHED_FAILURE;
        }
    }

    @Override
    protected VdcActionType getChildActionType() {
        return VdcActionType.CreateSnapshotFromTemplate;
    }

    @Override
    protected void endWithFailure() {
        super.endActionOnDisks();
        removeVmRelatedEntitiesFromDb();
        setSucceeded(true);
    }

    protected void removeVmRelatedEntitiesFromDb() {
        removeVmUsers();
        removeVmNetwork();
        // Note that currently newly added vm never have memory state
        // In case it will be changed (clone vm from snapshot will clone the memory state),
        // we'll need to remove the memory state images here as well.
        removeVmSnapshots();
        removeVmStatic();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<>();
        permissionList.add(new PermissionSubject(getVdsGroupId(),
                VdcObjectType.VdsGroups,
                getActionType().getActionGroup()));
        permissionList.add(new PermissionSubject(getVmTemplateId(),
                VdcObjectType.VmTemplate,
                getActionType().getActionGroup()));
        if (getVmTemplate() != null && !getVmTemplate().getDiskList().isEmpty()) {
            for (DiskImage disk : getParameters().getDiskInfoDestinationMap().values()) {
                if (disk.getStorageIds() != null && !disk.getStorageIds().isEmpty()) {
                    permissionList.add(new PermissionSubject(disk.getStorageIds().get(0),
                            VdcObjectType.Storage, ActionGroup.CREATE_DISK));
                }
            }
        }

        addPermissionSubjectForAdminLevelProperties(permissionList);
        return permissionList;
    }

    /**
     * user need permission on each object used: template, instance type, image type.
     */
    @Override
    protected boolean checkPermissions(final List<PermissionSubject> permSubjects) {

        if (getInstanceTypeId() != null && !checkInstanceTypeImagePermissions(getInstanceTypeId())) {
            return false;
        }

        if (imageTypeId != null && !checkInstanceTypeImagePermissions(imageTypeId)) {
            return false;
        }

        for (PermissionSubject permSubject : permSubjects) {
            // if user is using instance type, then create_instance on the cluster is enough
            if (permSubject.getObjectType() == VdcObjectType.VdsGroups && getInstanceTypeId() != null) {
                permSubject.setActionGroup(ActionGroup.CREATE_INSTANCE);
                if (checkSinglePermission(permSubject, getReturnValue().getCanDoActionMessages())) {
                    continue;
                }

                // create_vm is overriding in case no create_instance, try again with it
                permSubject.setActionGroup(getActionType().getActionGroup());
            }

            if (!checkSinglePermission(permSubject, getReturnValue().getCanDoActionMessages())) {
                return false;
            }
        }
        return true;
    }

    /**
     * If using an instance type/image the user needs to have either CREATE_INSTANCE or the specific
     * getActionType().getActionGroup() on the instance type/image
     */
    private boolean checkInstanceTypeImagePermissions(Guid id) {
        Collection<String> createInstanceMessages = new ArrayList<>();
        Collection<String> actionGroupMessages = new ArrayList<>();

        PermissionSubject createInstanceSubject = new PermissionSubject(id, VdcObjectType.VmTemplate, ActionGroup.CREATE_INSTANCE);
        PermissionSubject actionGroupSubject = new PermissionSubject(id, VdcObjectType.VmTemplate, getActionType().getActionGroup());

        // it is enough if at least one of this two permissions are there
        if (!checkSinglePermission(createInstanceSubject, createInstanceMessages) &&
                !checkSinglePermission(actionGroupSubject, actionGroupMessages)) {
            getReturnValue().getCanDoActionMessages().addAll(actionGroupMessages);
            return false;
        }

        return true;
    }

    protected void addPermissionSubjectForAdminLevelProperties(List<PermissionSubject> permissionList) {
        VmStatic vmFromParams = getParameters().getVmStaticData();

        if (vmFromParams != null) {
            // user needs specific permission to change custom properties
            if (!StringUtils.isEmpty(vmFromParams.getCustomProperties())) {
                permissionList.add(new PermissionSubject(getVdsGroupId(),
                        VdcObjectType.VdsGroups, ActionGroup.CHANGE_VM_CUSTOM_PROPERTIES));
            }

            // host-specific parameters can be changed by administration role only
            if (vmFromParams.getDedicatedVmForVdsList().size() > 0 || !StringUtils.isEmpty(vmFromParams.getCpuPinning())) {
                permissionList.add(new PermissionSubject(getVdsGroupId(),
                        VdcObjectType.VdsGroups, ActionGroup.EDIT_ADMIN_VM_PROPERTIES));
            }
        }
    }

    protected void addVmPermission() {
        UniquePermissionsSet permissionsToAdd = new UniquePermissionsSet();
        if (isMakeCreatorExplicitOwner()) {
            permissionsToAdd.addPermission(getCurrentUser().getId(), PredefinedRoles.VM_OPERATOR.getId(),
                    getVmId(), VdcObjectType.VM);
        }

        if (getParameters().isCopyTemplatePermissions() && !getVmTemplateId().equals(VmTemplateHandler.BLANK_VM_TEMPLATE_ID)) {
            copyTemplatePermissions(permissionsToAdd);
        }

        if (!permissionsToAdd.isEmpty()) {
            List<Permission> permissionsList = permissionsToAdd.asPermissionList();
            MultiLevelAdministrationHandler.addPermission(permissionsList.toArray(new Permission[permissionsList.size()]));

            getCompensationContext().snapshotNewEntities(permissionsList);
        }
    }

    private boolean isMakeCreatorExplicitOwner() {
        return getParameters().isMakeCreatorExplicitOwner()
                || (getCurrentUser() != null && !checkUserAuthorization(
                        getCurrentUser().getId(),
                        ActionGroup.MANIPULATE_PERMISSIONS,
                        getVmId(),
                        VdcObjectType.VM));
    }

    private void copyTemplatePermissions(UniquePermissionsSet permissionsToAdd) {
        PermissionDao dao = getDbFacade().getPermissionDao();

        List<Permission> templatePermissions = dao.getAllForEntity(getVmTemplateId(), getEngineSessionSeqId(), false);

        for (Permission templatePermission : templatePermissions) {
            boolean templateOwnerRole = templatePermission.getRoleId().equals(PredefinedRoles.TEMPLATE_OWNER.getId());
            boolean templateUserRole = templatePermission.getRoleId().equals(PredefinedRoles.TEMPLATE_USER.getId());

            if (templateOwnerRole || templateUserRole) {
                continue;
            }

            permissionsToAdd.addPermission(templatePermission.getAdElementId(), templatePermission.getRoleId(),
                    getVmId(), VdcObjectType.VM);
        }

    }

    protected void addDiskPermissions() {
        List<Guid> newDiskImageIds = new ArrayList<>(srcDiskIdToTargetDiskIdMapping.values());
        Permission[] permsArray = new Permission[newDiskImageIds.size()];

        for (int i = 0; i < newDiskImageIds.size(); i++) {
            permsArray[i] =
                    new Permission(getUserIdOfDiskOperator(),
                            PredefinedRoles.DISK_OPERATOR.getId(),
                            newDiskImageIds.get(i),
                            VdcObjectType.Disk);
        }
        MultiLevelAdministrationHandler.addPermission(permsArray);
    }

    private Guid getUserIdOfDiskOperator() {
        Guid diskOperatorIdFromParams = getParameters().getDiskOperatorAuthzPrincipalDbId();
        return diskOperatorIdFromParams != null ? diskOperatorIdFromParams : getCurrentUser().getId();
    }

    protected void addActiveSnapshot() {
        _vmSnapshotId = Guid.newGuid();
        new SnapshotsManager().addActiveSnapshot(_vmSnapshotId, getVm(), getCompensationContext());
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        if (!StringUtils.isBlank(getParameters().getVm().getName())) {
            return Collections.singletonMap(getParameters().getVm().getName(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM_NAME, EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED));
        }
        return null;
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = super.getJobMessageProperties();
            jobProperties.put(VdcObjectType.VM.name().toLowerCase(),
                    (getVmName() == null) ? "" : getVmName());
        }
        return jobProperties;
    }

    private Guid getQuotaId() {
        return getParameters().getVmStaticData().getQuotaId();
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();

        for (DiskImage disk : diskInfoDestinationMap.values()) {
            list.add(new QuotaStorageConsumptionParameter(
                    disk.getQuotaId(),
                    null,
                    QuotaStorageConsumptionParameter.QuotaAction.CONSUME,
                    disk.getStorageIds().get(0),
                    (double)disk.getSizeInGigabytes()));
        }
        return list;
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaVdsConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();
        list.add(new QuotaSanityParameter(getQuotaId(), null));
        return list;
    }

    public Map<Guid, Guid> getSrcDiskIdToTargetDiskIdMapping() {
        return srcDiskIdToTargetDiskIdMapping;
    }

    public Map<Guid, Guid> getSrcDeviceIdToTargetDeviceIdMapping() {
        Map<Guid, Guid> srcDeviceIdToTargetDeviceIdMapping = new HashMap<>();
        srcDeviceIdToTargetDeviceIdMapping.putAll(srcVmNicIdToTargetVmNicIdMapping);
        srcDeviceIdToTargetDeviceIdMapping.putAll(srcDiskIdToTargetDiskIdMapping);
        return srcDeviceIdToTargetDeviceIdMapping;
    }

    protected boolean isVirtioScsiEnabled() {
        Boolean virtioScsiEnabled = getParameters().isVirtioScsiEnabled();
        boolean isOsSupportedForVirtIoScsi = VmValidationUtils.isDiskInterfaceSupportedByOs(
                getParameters().getVm().getOs(), getVdsGroup().getCompatibilityVersion(), DiskInterface.VirtIO_SCSI);

        return virtioScsiEnabled != null ? virtioScsiEnabled :
                FeatureSupported.virtIoScsi(getVdsGroup().getCompatibilityVersion()) && isOsSupportedForVirtIoScsi;
    }

    protected boolean isBalloonEnabled() {
        Boolean balloonEnabled = getParameters().isBalloonEnabled();
        return balloonEnabled != null ? balloonEnabled :
            osRepository.isBalloonEnabled(getParameters().getVmStaticData().getOsId(),
                    getVdsGroup().getCompatibilityVersion());
    }

    protected boolean hasWatchdog() {
        return getParameters().getWatchdog() != null;
    }

    protected boolean isVirtioScsiControllerAttached(Guid vmId) {
        return VmDeviceUtils.hasVirtioScsiController(vmId);
    }

    /**
     * This method override vm values with the instance type values
     * in case instance type is selected for this vm
     */
    private void updateVmObject() {
        updateParametersVmFromInstanceType();

        // set vm interface source id to be the instance type, vm interface are taken from it
        if (getInstanceType() != null) {
            vmInterfacesSourceId = getInstanceTypeId();
        }

        VmStatic vmStatic = getParameters().getVmStaticData();
        ImageType imageType = getImageType();
        if (imageType != null) {
            vmStatic.setOsId(imageType.getOsId());
            vmStatic.setIsoPath(imageType.getIsoPath());
            vmStatic.setInitrdUrl(imageType.getInitrdUrl());
            vmStatic.setKernelUrl(imageType.getKernelUrl());
            vmStatic.setKernelParams(imageType.getKernelParams());
            // set vm disks source to be the image type, vm disks are taken from it
            vmDisksSource = (VmTemplate)imageType;
        }

        OsRepository osRepository = SimpleDependecyInjector.getInstance().get(OsRepository.class);

        // Choose a proper default OS according to the cluster architecture
        if (getParameters().getVmStaticData().getOsId() == OsRepository.AUTO_SELECT_OS) {
            if (getVdsGroup().getArchitecture() != ArchitectureType.undefined) {
                Integer defaultOs = osRepository.getDefaultOSes().get(getVdsGroup().getArchitecture());

                getParameters().getVmStaticData().setOsId(defaultOs);
            }
        }

        // Choose a proper default display type according to the cluster architecture
        VmHandler.autoSelectDefaultDisplayType(getVmTemplateId(),
            getParameters().getVmStaticData(),
            getVdsGroup(),
            getParameters().getGraphicsDevices());
    }

    protected boolean isTemplateInValidDc() {
        return VmTemplateHandler.BLANK_VM_TEMPLATE_ID.equals(getVmTemplateId())
                || getVmTemplate().getStoragePoolId().equals(getStoragePoolId());
    }

    protected void updateProfileOnNic(VmNic iface) {
        Network network = NetworkHelper.getNetworkByVnicProfileId(iface.getVnicProfileId());
        if (network != null && !NetworkHelper.isNetworkInCluster(network, getVdsGroupId())) {
            iface.setVnicProfileId(null);
        }
    }
    protected boolean checkNumberOfMonitors() {
        Collection<GraphicsType> graphicsTypes = VmHandler.getResultingVmGraphics(
                VmDeviceUtils.getGraphicsTypesOfEntity(getVmTemplateId()),
                getParameters().getGraphicsDevices());
        int numOfMonitors = getParameters().getVmStaticData().getNumOfMonitors();

        return VmHandler.isNumOfMonitorsLegal(graphicsTypes,
                numOfMonitors,
                getReturnValue().getCanDoActionMessages());
    }

    /**
     * Icon processing policy:
     * <ul>
     *     <li>If there is an attached icon, it is used as large icon as base for computation of small icon.
     *         Predefined icons should not be sent in parameters.</li>
     *     <li>If there are no icon in parameters && both (small and large) icon ids are set then those ids are used.
     *         </li>
     *     <li>Otherwise (at least one icon id is null) both icon ids are copied from template.</li>
     * </ul>
     * @param vmStatic
     */
    public void setIconIds(VmStatic vmStatic) {
        if (getParameters().getVmLargeIcon() != null){
            final VmIconIdSizePair iconIds =
                    IconUtils.ensureIconPairInDatabase(getParameters().getVmLargeIcon());
            vmStatic.setLargeIconId(iconIds.getLarge());
            vmStatic.setSmallIconId(iconIds.getSmall());
            return;
        } else {
            if (vmStatic.getLargeIconId() == null
                    || vmStatic.getSmallIconId() == null) {
                vmStatic.setSmallIconId(getVmTemplate().getSmallIconId());
                vmStatic.setLargeIconId(getVmTemplate().getLargeIconId());
            }
        }
    }
}
