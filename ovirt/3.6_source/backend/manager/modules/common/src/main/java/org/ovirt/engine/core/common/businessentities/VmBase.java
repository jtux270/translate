package org.ovirt.engine.core.common.businessentities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.ovirt.engine.core.common.businessentities.OvfExportOnlyField.ExportOption;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.common.validation.annotation.IntegerContainedInConfigValueList;
import org.ovirt.engine.core.common.validation.annotation.NullOrStringContainedInConfigValueList;
import org.ovirt.engine.core.common.validation.annotation.SizeFromConfigValue;
import org.ovirt.engine.core.common.validation.annotation.ValidDescription;
import org.ovirt.engine.core.common.validation.annotation.ValidI18NExtraName;
import org.ovirt.engine.core.common.validation.annotation.ValidSerialNumberPolicy;
import org.ovirt.engine.core.common.validation.annotation.ValidTimeZone;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.CreateVm;
import org.ovirt.engine.core.common.validation.group.ImportEntity;
import org.ovirt.engine.core.common.validation.group.StartEntity;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.common.validation.group.UpdateVm;
import org.ovirt.engine.core.compat.Guid;

@ValidTimeZone(groups = {CreateEntity.class, UpdateEntity.class, ImportEntity.class, StartEntity.class})
@ValidSerialNumberPolicy(groups = {CreateEntity.class, UpdateEntity.class, ImportEntity.class, StartEntity.class})
public class VmBase implements IVdcQueryable, BusinessEntity<Guid>, Nameable, Commented, HasSerialNumberPolicy, HasMigrationOptions {
    private static final long serialVersionUID = 1078548170257965614L;

    @EditableField
    private String name;

    @EditableField
    private ArrayList<DiskImage> images;

    @EditableField
    private List<VmNetworkInterface> interfaces;

    @EditableField
    private ArrayList<DiskImage> diskList;
    private Map<Guid, VmDevice> managedDeviceMap;
    private List<VmDevice> unmanagedDeviceList;

    private Guid id;

    @EditableOnVmStatusField
    @EditableOnTemplate
    private Guid vdsGroupId;

    @CopyOnNewVersion
    @EditableField
    private int osId;

    @CopyOnNewVersion
    @EditableField
    private Guid smallIconId;

    @CopyOnNewVersion
    @EditableField
    private Guid largeIconId;

    private Date creationDate;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.VM_DESCRIPTION_SIZE)
    @ValidDescription(message = "ACTION_TYPE_FAILED_DESCRIPTION_MAY_NOT_CONTAIN_SPECIAL_CHARS",
            groups = { CreateEntity.class, UpdateEntity.class })
    private String description;

    @EditableField
    private String comment;

    @CopyOnNewVersion
    @EditableOnVmStatusField(isHotsetAllowed = true)
    @EditableOnTemplate
    private int memSizeMb;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @SizeFromConfigValue(
            maxConfig = ConfigValues.MaxIoThreadsPerVm,
            min = 0,
            groups = { CreateEntity.class, UpdateEntity.class, CreateVm.class, UpdateVm.class },
            message = "ACTION_TYPE_FAILED_NUM_OF_IOTHREADS_INCORRECT"
    )
    private int numOfIoThreads;

    @EditableOnVmStatusField(isHotsetAllowed = true)
    @EditableOnTemplate
    @CopyOnNewVersion
    private int numOfSockets;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private int cpuPerSocket;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @Size(max = BusinessEntitiesDefinitions.VM_EMULATED_MACHINE_SIZE)
    @ValidI18NExtraName(message = "ACTION_TYPE_FAILED_EMULATED_MACHINE_MAY_NOT_CONTAIN_SPECIAL_CHARS",
            groups = { CreateEntity.class, UpdateEntity.class })
    private String customEmulatedMachine;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @Size(max = BusinessEntitiesDefinitions.VM_CPU_NAME_SIZE)
    @ValidI18NExtraName(message = "ACTION_TYPE_FAILED_CPU_NAME_MAY_NOT_CONTAIN_SPECIAL_CHARS",
            groups = { CreateEntity.class, UpdateEntity.class })
    private String customCpuName; // overrides cluster cpu. (holds the actual vdsVerb)

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @IntegerContainedInConfigValueList(configValue = ConfigValues.ValidNumOfMonitors,
            message = "VALIDATION_VM_NUM_OF_MONITORS_EXCEEDED")
    private int numOfMonitors;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private boolean singleQxlPci;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @Size(max = BusinessEntitiesDefinitions.GENERAL_TIME_ZONE_SIZE)
    private String timeZone;

    @CopyOnNewVersion
    @EditableField
    private VmType vmType;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private UsbPolicy usbPolicy;

    @CopyOnNewVersion
    private boolean failBack;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private BootSequence defaultBootSequence;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    private int niceLevel;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private int cpuShares;

    @CopyOnNewVersion
    @EditableField
    private int priority;

    @CopyOnNewVersion
    @EditableField
    private boolean autoStartup;

    @EditableOnVmStatusField
    @EditableOnTemplate
    private boolean stateless;

    @CopyOnNewVersion
    @EditableField
    private boolean deleteProtected;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private SsoMethod ssoMethod;

    @EditableField
    private long dbGeneration;

    @CopyOnNewVersion
    @EditableField
    private boolean smartcardEnabled;

    @CopyOnNewVersion
    @EditableField
    @Pattern(regexp = ValidationUtils.ISO_SUFFIX_PATTERN, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "ACTION_TYPE_FAILED_INVALID_CDROM_DISK_FORMAT")
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    private String isoPath;

    private OriginType origin;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    @Pattern(regexp = ValidationUtils.NO_TRIMMING_WHITE_SPACES_PATTERN,
            message = "ACTION_TYPE_FAILED_LINUX_BOOT_PARAMS_MAY_NOT_CONTAIN_TRIMMING_WHITESPACES", groups = { CreateEntity.class,
                    UpdateEntity.class })
    private String kernelUrl;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    @Pattern(regexp = ValidationUtils.NO_TRIMMING_WHITE_SPACES_PATTERN,
            message = "ACTION_TYPE_FAILED_LINUX_BOOT_PARAMS_MAY_NOT_CONTAIN_TRIMMING_WHITESPACES", groups = { CreateEntity.class,
                    UpdateEntity.class })
    private String kernelParams;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    @Pattern(regexp = ValidationUtils.NO_TRIMMING_WHITE_SPACES_PATTERN,
            message = "ACTION_TYPE_FAILED_LINUX_BOOT_PARAMS_MAY_NOT_CONTAIN_TRIMMING_WHITESPACES", groups = { CreateEntity.class,
                    UpdateEntity.class })
    private String initrdUrl;

    @CopyOnNewVersion
    @EditableField
    private boolean allowConsoleReconnect;

    /**
     * if this field is null then value should be taken from cluster
     */
    @CopyOnNewVersion
    @EditableField
    private Boolean tunnelMigration;

    /**
     * this field is used to save the ovf version,
     * in case the vm object was built from ovf.
     * not persisted to db.
     */
    private String ovfVersion;

    // not persisted to db
    private Date exportDate;

    /**
     * Maximum allowed downtime for live migration in milliseconds.
     * Value of null indicates that the {@link ConfigValues.DefaultMaximumMigrationDowntime} value will be used.
     *
     * Special value of 0 for migration downtime specifies that no value will be sent to VDSM and the default
     * VDSM behavior will be used.
     */
    @EditableField
    @Min(value = 0, message = "VALIDATION_VM_MIGRATION_DOWNTIME_RANGE")
    private Integer migrationDowntime;

    @EditableField
    private NumaTuneMode numaTuneMode;

    @EditableField
    private List<VmNumaNode> vNumaNodeList;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @OvfExportOnlyField(exportOption = ExportOption.EXPORT_NON_IGNORED_VALUES)
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    private String userDefinedProperties;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @OvfExportOnlyField(exportOption = ExportOption.EXPORT_NON_IGNORED_VALUES)
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    private String predefinedProperties;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private String customProperties;

    @CopyOnNewVersion
    @EditableField
    private ConsoleDisconnectAction consoleDisconnectAction;

    public VmBase() {
        name = "";
        interfaces = new ArrayList<VmNetworkInterface>();
        images = new ArrayList<DiskImage>();
        diskList = new ArrayList<DiskImage>();
        managedDeviceMap = new HashMap<Guid, VmDevice>();
        unmanagedDeviceList = new ArrayList<VmDevice>();
        id = Guid.Empty;
        creationDate = new Date(0);
        numOfSockets = 1;
        cpuPerSocket = 1;
        usbPolicy = UsbPolicy.DISABLED;
        isoPath = "";
        defaultBootSequence = BootSequence.C;
        migrationSupport = MigrationSupport.MIGRATABLE;
        vmType = VmType.Desktop;
        defaultDisplayType = DisplayType.qxl;
        ssoMethod = SsoMethod.GUEST_AGENT;
        singleQxlPci = true;
        spiceFileTransferEnabled = true;
        spiceCopyPasteEnabled = true;
        setNumaTuneMode(NumaTuneMode.INTERLEAVE);
        vNumaNodeList = new ArrayList<VmNumaNode>();
        customProperties = "";
        consoleDisconnectAction = ConsoleDisconnectAction.LOCK_SCREEN;
    }

    @EditableField
    private Guid quotaId;


    /** Transient field for GUI presentation purposes. */
    @EditableField
    private String quotaName;

    @EditableField
    /** Transient field for GUI presentation purposes. */
    private boolean quotaDefault;

    /** Transient field for GUI presentation purposes. */
    @EditableField
    private QuotaEnforcementTypeEnum quotaEnforcementType;

    @CopyOnNewVersion
    @EditableField
    @EditableOnTemplate
    @OvfExportOnlyField(valueToIgnore = "MIGRATABLE", exportOption = ExportOption.EXPORT_NON_IGNORED_VALUES)
    private MigrationSupport migrationSupport;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private List<Guid> dedicatedVmForVdsList;

    @EditableOnVmStatusField
    @EditableOnTemplate
    private DisplayType defaultDisplayType;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @NullOrStringContainedInConfigValueList(configValue = ConfigValues.VncKeyboardLayoutValidValues,
        groups = { CreateEntity.class, UpdateEntity.class },
        message = "VALIDATION_VM_INVALID_KEYBOARD_LAYOUT")
    private String vncKeyboardLayout;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private int minAllocatedMem;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private boolean runAndPause;

    private Guid createdByUserId;

    @EditableField
    @CopyOnNewVersion
    private VmInit vmInit;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private SerialNumberPolicy serialNumberPolicy;

    /**
     * Serial number used when {@link serialNumberPolicy} is set to {@link SerialNumberPolicy.CUSTOM}
     */
    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    @Size(max = BusinessEntitiesDefinitions.VM_SERIAL_NUMBER_SIZE)
    private String customSerialNumber;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private boolean bootMenuEnabled;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private boolean spiceFileTransferEnabled;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private boolean spiceCopyPasteEnabled;

    @CopyOnNewVersion
    @EditableOnVmStatusField
    @EditableOnTemplate
    private Guid cpuProfileId;

    @CopyOnNewVersion
    @EditableField
    private Boolean autoConverge;

    @CopyOnNewVersion
    @EditableField
    private Boolean migrateCompressed;

    public VmBase(VmBase vmBase) {
        this(vmBase.getName(),
                vmBase.getId(),
                vmBase.getVdsGroupId(),
                vmBase.getOsId(),
                vmBase.getCreationDate(),
                vmBase.getDescription(),
                vmBase.getComment(),
                vmBase.getMemSizeMb(),
                vmBase.getNumOfSockets(),
                vmBase.getCpuPerSocket(),
                vmBase.getNumOfMonitors(),
                vmBase.getSingleQxlPci(),
                vmBase.getTimeZone(),
                vmBase.getVmType(),
                vmBase.getUsbPolicy(),
                vmBase.isFailBack(),
                vmBase.getDefaultBootSequence(),
                vmBase.getNiceLevel(),
                vmBase.getCpuShares(),
                vmBase.getPriority(),
                vmBase.isAutoStartup(),
                vmBase.isStateless(),
                vmBase.getIsoPath(),
                vmBase.getOrigin(),
                vmBase.getKernelUrl(),
                vmBase.getKernelParams(),
                vmBase.getInitrdUrl(),
                vmBase.getQuotaId(),
                vmBase.isSmartcardEnabled(),
                vmBase.isDeleteProtected(),
                vmBase.getSsoMethod(),
                vmBase.getTunnelMigration(),
                vmBase.getVncKeyboardLayout(),
                vmBase.getMinAllocatedMem(),
                vmBase.isRunAndPause(),
                vmBase.getCreatedByUserId(),
                vmBase.getMigrationSupport(),
                vmBase.isAllowConsoleReconnect(),
                vmBase.getDedicatedVmForVdsList(),
                vmBase.getDefaultDisplayType(),
                vmBase.getMigrationDowntime(),
                vmBase.getVmInit(),
                vmBase.getSerialNumberPolicy(),
                vmBase.getCustomSerialNumber(),
                vmBase.isBootMenuEnabled(),
                vmBase.isSpiceFileTransferEnabled(),
                vmBase.isSpiceCopyPasteEnabled(),
                vmBase.getCpuProfileId(),
                vmBase.getNumaTuneMode(),
                vmBase.getAutoConverge(),
                vmBase.getMigrateCompressed(),
                vmBase.getUserDefinedProperties(),
                vmBase.getPredefinedProperties(),
                vmBase.getCustomProperties(),
                vmBase.getCustomEmulatedMachine(),
                vmBase.getCustomCpuName(),
                vmBase.getSmallIconId(),
                vmBase.getLargeIconId(),
                vmBase.getNumOfIoThreads(),
                vmBase.getConsoleDisconnectAction());
    }

    public VmBase(
            String name,
            Guid id,
            Guid vdsGroupId,
            int osId,
            Date creationDate,
            String description,
            String comment,
            int memSizeMb,
            int numOfSockets,
            int cpusPerSocket,
            int numOfMonitors,
            boolean singleQxlPci,
            String timezone,
            VmType vmType,
            UsbPolicy usbPolicy,
            boolean failBack,
            BootSequence defaultBootSequence,
            int niceLevel,
            int cpuShares,
            int priority,
            boolean autoStartup,
            boolean stateless,
            String isoPath,
            OriginType origin,
            String kernelUrl,
            String kernelParams,
            String initrdUrl,
            Guid quotaId,
            boolean smartcardEnabled,
            boolean deleteProtected,
            SsoMethod ssoMethod,
            Boolean tunnelMigration,
            String vncKeyboardLayout,
            int minAllocatedMem,
            boolean runAndPause,
            Guid createdByUserId,
            MigrationSupport migrationSupport,
            boolean allowConsoleReconnect,
            List<Guid> dedicatedVmForVdsList,
            DisplayType defaultDisplayType,
            Integer migrationDowntime,
            VmInit vmInit,
            SerialNumberPolicy serialNumberPolicy,
            String customSerialNumber,
            boolean bootMenuEnabled,
            boolean spiceFileTransferEnabled,
            boolean spiceCopyPasteEnabled,
            Guid cpuProfileId,
            NumaTuneMode numaTuneMode,
            Boolean autoConverge,
            Boolean migrateCompressed,
            String userDefinedProperties,
            String predefinedProperties,
            String customProperties,
            String customEmulatedMachine,
            String customCpuName,
            Guid smallIconId,
            Guid largeIconId,
            int numOfIoThreads,
            ConsoleDisconnectAction consoleDisconnectAction) {
        this();
        this.name = name;
        this.id = id;
        this.vdsGroupId = vdsGroupId;
        this.osId = osId;
        this.creationDate = creationDate;
        this.description = description;
        this.comment = comment;
        this.memSizeMb = memSizeMb;
        this.numOfSockets = numOfSockets;
        this.cpuPerSocket = cpusPerSocket;
        this.numOfMonitors = numOfMonitors;
        this.singleQxlPci = singleQxlPci;
        this.timeZone = timezone;
        this.vmType = vmType;
        this.usbPolicy = usbPolicy;
        this.failBack = failBack;
        this.defaultBootSequence = defaultBootSequence;
        this.niceLevel = niceLevel;
        this.cpuShares = cpuShares;
        this.priority = priority;
        this.autoStartup = autoStartup;
        this.stateless = stateless;
        this.isoPath = isoPath;
        this.origin = origin;
        this.kernelUrl = kernelUrl;
        this.kernelParams = kernelParams;
        this.initrdUrl = initrdUrl;
        this.smartcardEnabled = smartcardEnabled;
        this.deleteProtected = deleteProtected;
        this.ssoMethod = ssoMethod;
        this.tunnelMigration = tunnelMigration;
        this.vncKeyboardLayout = vncKeyboardLayout;
        this.minAllocatedMem = minAllocatedMem;
        this.runAndPause = runAndPause;
        this.createdByUserId = createdByUserId;
        this.defaultDisplayType = defaultDisplayType;
        setQuotaId(quotaId);
        this.migrationSupport = migrationSupport;
        this.allowConsoleReconnect = allowConsoleReconnect;
        this.dedicatedVmForVdsList = dedicatedVmForVdsList;
        this.migrationDowntime = migrationDowntime;
        this.vmInit = vmInit;
        this.serialNumberPolicy = serialNumberPolicy;
        this.customSerialNumber = customSerialNumber;
        this.bootMenuEnabled = bootMenuEnabled;
        this.spiceFileTransferEnabled = spiceFileTransferEnabled;
        this.numaTuneMode = numaTuneMode;
        this.spiceCopyPasteEnabled = spiceCopyPasteEnabled;
        this.cpuProfileId = cpuProfileId;
        this.autoConverge = autoConverge;
        this.migrateCompressed = migrateCompressed;
        this.userDefinedProperties = userDefinedProperties;
        this.predefinedProperties = predefinedProperties;
        this.customProperties = customProperties;
        this.customEmulatedMachine = customEmulatedMachine;
        this.customCpuName = customCpuName;
        this.smallIconId = smallIconId;
        this.largeIconId = largeIconId;
        this.numOfIoThreads = numOfIoThreads;
        this.consoleDisconnectAction = consoleDisconnectAction;
    }

    @Override
    public Object getQueryableId() {
        return getId();
    }

    public long getDbGeneration() {
        return dbGeneration;
    }

    public void setDbGeneration(long value) {
        this.dbGeneration = value;
    }

    public List<VmNetworkInterface> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<VmNetworkInterface> value) {
        interfaces = value;
    }

    public ArrayList<DiskImage> getImages() {
        return images;
    }

    public void setImages(ArrayList<DiskImage> value) {
        images = value;
    }

    @JsonIgnore
    public ArrayList<DiskImage> getDiskList() {
        return diskList;
    }

    public void setDiskList(ArrayList<DiskImage> diskList) {
        this.diskList = diskList;
    }

    public Map<Guid, VmDevice> getManagedDeviceMap() {
        return managedDeviceMap;
    }

    public void setManagedDeviceMap(Map<Guid, VmDevice> map) {
        this.managedDeviceMap = map;
    }

    public List<VmDevice> getUnmanagedDeviceList() {
        return unmanagedDeviceList;
    }

    public void setUnmanagedDeviceList(List<VmDevice> list) {
        this.unmanagedDeviceList = list;
    }

    public int getNumOfCpus() {
        return this.getCpuPerSocket() * this.getNumOfSockets();
    }

    @Override
    public Guid getId() {
        return id;
    }

    @Override
    public void setId(Guid value) {
        this.id = value;
    }

    public Guid getVdsGroupId() {
        return vdsGroupId;
    }

    public void setVdsGroupId(Guid value) {
        this.vdsGroupId = value;
    }

    public int getOsId() {
        return osId;
    }

    public void setOsId(int value) {
        osId = value;
    }

    public Guid getSmallIconId() {
        return smallIconId;
    }

    public void setSmallIconId(Guid smallIconId) {
        this.smallIconId = smallIconId;
    }

    public Guid getLargeIconId() {
        return largeIconId;
    }

    public void setLargeIconId(Guid largeIconId) {
        this.largeIconId = largeIconId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date value) {
        this.creationDate = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        comment = value;
    }

    public int getMemSizeMb() {
        return memSizeMb;
    }

    public int getNumOfIoThreads() {
        return numOfIoThreads;
    }

    public void setNumOfIoThreads(int numOfIoThreads) {
        this.numOfIoThreads = numOfIoThreads;
    }

    public void setMemSizeMb(int value) {
        this.memSizeMb = value;
    }

    public int getNumOfSockets() {
        return numOfSockets;
    }

    public void setNumOfSockets(int value) {
        this.numOfSockets = value;
    }

    public int getCpuPerSocket() {
        return cpuPerSocket;
    }

    public void setCpuPerSocket(int value) {
        this.cpuPerSocket = value;
    }

    public int getNumOfMonitors() {
        return numOfMonitors;
    }

    public void setNumOfMonitors(int value) {
        numOfMonitors = value;
    }

    public boolean getSingleQxlPci() {
        return singleQxlPci;
    }

    public void setSingleQxlPci(boolean value) {
        singleQxlPci = value;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String value) {
        timeZone = value;
    }

    public VmType getVmType() {
        return vmType;
    }

    public void setVmType(VmType value) {
        vmType = value;
    }

    public UsbPolicy getUsbPolicy() {
        return usbPolicy;
    }

    public void setUsbPolicy(UsbPolicy value) {
        usbPolicy = value;
    }

    public boolean isFailBack() {
        return failBack;
    }

    public void setFailBack(boolean value) {
        failBack = value;
    }

    public BootSequence getDefaultBootSequence() {
        return defaultBootSequence;
    }

    public void setDefaultBootSequence(BootSequence value) {
        defaultBootSequence = value;
    }

    public int getNiceLevel() {
        return niceLevel;
    }

    public void setNiceLevel(int value) {
        niceLevel = value;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int value) {
        priority = value;
    }

    public boolean isAutoStartup() {
        return autoStartup;
    }

    public void setAutoStartup(boolean value) {
        autoStartup = value;
    }

    public boolean isStateless() {
        return stateless;
    }

    public void setStateless(boolean value) {
        stateless = value;
    }

    public String getIsoPath() {
        return isoPath;
    }

    public void setIsoPath(String value) {
        isoPath = value;
    }

    public OriginType getOrigin() {
        return origin;
    }

    public void setOrigin(OriginType value) {
        origin = value;
    }

    public String getKernelUrl() {
        return kernelUrl;
    }

    public void setKernelUrl(String value) {
        kernelUrl = value;
    }

    public String getKernelParams() {
        return kernelParams;
    }

    public void setKernelParams(String value) {
        kernelParams = value;
    }

    public String getInitrdUrl() {
        return initrdUrl;
    }

    public void setInitrdUrl(String value) {
        initrdUrl = value;
    }

    public boolean isAllowConsoleReconnect() {
        return allowConsoleReconnect;
    }

    public void setAllowConsoleReconnect(boolean value) {
        allowConsoleReconnect = value;
    }

    public void setExportDate(Date value) {
        this.exportDate = value;
    }

    public Date getExportDate() {
        return exportDate;
    }

    public boolean isSmartcardEnabled() {
        return smartcardEnabled;
    }

    public void setSmartcardEnabled(boolean smartcardEnabled) {
        this.smartcardEnabled = smartcardEnabled;
    }

    public boolean isDeleteProtected() {
        return deleteProtected;
    }

    public void setDeleteProtected(boolean deleteProtected) {
        this.deleteProtected = deleteProtected;
    }

    public String getVncKeyboardLayout() {
        return vncKeyboardLayout;
    }

    public void setVncKeyboardLayout(String vncKeyboardLayout) {
        this.vncKeyboardLayout = vncKeyboardLayout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (autoStartup ? 1231 : 1237);
        result = prime * result + cpuPerSocket;
        result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
        result = prime * result + ((defaultBootSequence == null) ? 0 : defaultBootSequence.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + (failBack ? 1231 : 1237);
        result = prime * result + ((initrdUrl == null) ? 0 : initrdUrl.hashCode());
        result = prime * result + ((isoPath == null) ? 0 : isoPath.hashCode());
        result = prime * result + ((kernelParams == null) ? 0 : kernelParams.hashCode());
        result = prime * result + ((kernelUrl == null) ? 0 : kernelUrl.hashCode());
        result = prime * result + osId;
        result = prime * result + memSizeMb;
        result = prime * result + niceLevel;
        result = prime * result + cpuShares;
        result = prime * result + numOfSockets;
        result = prime * result + numOfMonitors;
        result = prime * result + ((origin == null) ? 0 : origin.hashCode());
        result = prime * result + priority;
        result = prime * result + (stateless ? 1231 : 1237);
        result = prime * result + (smartcardEnabled ? 1231 : 1237);
        result = prime * result + ((timeZone == null) ? 0 : timeZone.hashCode());
        result = prime * result + ((usbPolicy == null) ? 0 : usbPolicy.hashCode());
        result = prime * result + ((vdsGroupId == null) ? 0 : vdsGroupId.hashCode());
        result = prime * result + ((vmType == null) ? 0 : vmType.hashCode());
        result = prime * result + ((quotaId == null) ? 0 : quotaId.hashCode());
        result = prime * result + (allowConsoleReconnect ? 1231 : 1237);
        result = prime * result + ((dedicatedVmForVdsList == null) ? 0 : dedicatedVmForVdsList.hashCode());
        result = prime * result + ((migrationSupport == null) ? 0 : migrationSupport.hashCode());
        result = prime * result + ((tunnelMigration == null) ? 0 : tunnelMigration.hashCode());
        result = prime * result + ((vncKeyboardLayout == null) ? 0 : vncKeyboardLayout.hashCode());
        result = prime * result + ((createdByUserId == null) ? 0 : createdByUserId.hashCode());
        result = prime * result + ((defaultDisplayType == null) ? 0 : defaultDisplayType.hashCode());
        result = prime * result + ((migrationDowntime == null) ? 0 : migrationDowntime.hashCode());
        result = prime * result + ((serialNumberPolicy == null) ? 0 : serialNumberPolicy.hashCode());
        result = prime * result + ((customSerialNumber == null) ? 0 : customSerialNumber.hashCode());
        result = prime * result + (bootMenuEnabled ? 1231 : 1237);
        result = prime * result + (spiceFileTransferEnabled ? 1231 : 1237);
        result = prime * result + (spiceCopyPasteEnabled ? 1231 : 1237);
        result = prime * result + ((cpuProfileId == null) ? 0 : cpuProfileId.hashCode());
        result = prime * result + ((numaTuneMode == null) ? 0 : numaTuneMode.getValue().hashCode());
        result = prime * result + ((vNumaNodeList == null) ? 0 : vNumaNodeList.hashCode());
        result = prime * result + (autoConverge == null ? 0 : autoConverge.hashCode());
        result = prime * result + (migrateCompressed == null ? 0 : migrateCompressed.hashCode());
        result = prime * result + ((predefinedProperties == null) ? 0 : predefinedProperties.hashCode());
        result = prime * result + ((userDefinedProperties == null) ? 0 : userDefinedProperties.hashCode());
        result = prime * result + ((customEmulatedMachine == null) ? 0 : customEmulatedMachine.hashCode());
        result = prime * result + ((customCpuName== null) ? 0 : customCpuName.hashCode());
        result = prime * result + ((smallIconId == null) ? 0 : smallIconId.hashCode());
        result = prime * result + ((largeIconId == null) ? 0 : largeIconId.hashCode());
        result = prime * result + ((consoleDisconnectAction == null) ? 0 : consoleDisconnectAction.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof VmBase)) {
            return false;
        }
        VmBase other = (VmBase) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && autoStartup == other.autoStartup
                && cpuPerSocket == other.cpuPerSocket
                && ObjectUtils.objectsEqual(creationDate, other.creationDate)
                && defaultBootSequence == other.defaultBootSequence
                && ObjectUtils.objectsEqual(description, other.description)
                && failBack == other.failBack
                && ObjectUtils.objectsEqual(initrdUrl, other.initrdUrl)
                && ObjectUtils.objectsEqual(isoPath, other.isoPath)
                && ObjectUtils.objectsEqual(kernelParams, other.kernelParams)
                && ObjectUtils.objectsEqual(kernelUrl, other.kernelUrl)
                && osId == other.osId
                && memSizeMb == other.memSizeMb
                && niceLevel == other.niceLevel
                && numOfSockets == other.numOfSockets
                && numOfMonitors == other.numOfMonitors
                && singleQxlPci == other.singleQxlPci
                && origin == other.origin
                && priority == other.priority
                && stateless == other.stateless
                && smartcardEnabled == other.smartcardEnabled
                && deleteProtected == other.deleteProtected
                && ObjectUtils.objectsEqual(timeZone, other.timeZone)
                && usbPolicy == other.usbPolicy
                && ObjectUtils.objectsEqual(vdsGroupId, other.vdsGroupId)
                && vmType == other.vmType
                && ObjectUtils.objectsEqual(quotaId, other.quotaId)
                && allowConsoleReconnect == other.allowConsoleReconnect
                && ObjectUtils.objectsEqual(dedicatedVmForVdsList, other.dedicatedVmForVdsList)
                && migrationSupport == other.migrationSupport
                && ObjectUtils.objectsEqual(tunnelMigration, other.tunnelMigration)
                && ObjectUtils.objectsEqual(vncKeyboardLayout, other.vncKeyboardLayout)
                && ObjectUtils.objectsEqual(createdByUserId, other.createdByUserId)
                && cpuShares == other.cpuShares
                && ObjectUtils.objectsEqual(migrationDowntime, other.migrationDowntime)
                && serialNumberPolicy == other.serialNumberPolicy
                && ObjectUtils.objectsEqual(customSerialNumber, other.customSerialNumber)
                && bootMenuEnabled == other.bootMenuEnabled
                && spiceFileTransferEnabled == other.spiceFileTransferEnabled
                && spiceCopyPasteEnabled == other.spiceCopyPasteEnabled
                && ObjectUtils.objectsEqual(cpuProfileId, other.cpuProfileId)
                && ObjectUtils.objectsEqual(numaTuneMode.getValue(), other.numaTuneMode.getValue())
                && ObjectUtils.objectsEqual(vNumaNodeList, other.vNumaNodeList))
                && ObjectUtils.objectsEqual(autoConverge, other.autoConverge)
                && ObjectUtils.objectsEqual(migrateCompressed, other.migrateCompressed)
                && ObjectUtils.objectsEqual(predefinedProperties, other.predefinedProperties)
                && ObjectUtils.objectsEqual(userDefinedProperties, other.userDefinedProperties)
                && ObjectUtils.objectsEqual(customEmulatedMachine, other.customEmulatedMachine)
                && ObjectUtils.objectsEqual(customCpuName, other.customCpuName)
                && Objects.equals(smallIconId, other.smallIconId)
                && Objects.equals(largeIconId, other.largeIconId)
                && ObjectUtils.objectsEqual(consoleDisconnectAction, other.consoleDisconnectAction);
    }

    public Guid getQuotaId() {
        return quotaId;
    }

    public void setQuotaId(Guid quotaId) {
        this.quotaId = quotaId;
    }

    public String getQuotaName() {
        return quotaName;
    }

    public void setQuotaName(String quotaName) {
        this.quotaName = quotaName;
    }

    public boolean isQuotaDefault() {
        return quotaDefault;
    }

    public void setQuotaDefault(boolean isQuotaDefault) {
        this.quotaDefault = isQuotaDefault;
    }

    public QuotaEnforcementTypeEnum getQuotaEnforcementType() {
        return quotaEnforcementType;
    }

    public void setQuotaEnforcementType(QuotaEnforcementTypeEnum quotaEnforcementType) {
        this.quotaEnforcementType = quotaEnforcementType;
    }

    public MigrationSupport getMigrationSupport() {
        return migrationSupport;
    }

    public void setMigrationSupport(MigrationSupport migrationSupport) {
        this.migrationSupport = migrationSupport;
    }

    public Guid fetchDedicatedVmForSingleHost(){
        if(getDedicatedVmForVdsList().size() == 0){
            return null;
        }
        return getDedicatedVmForVdsList().get(0);
    }
    public List<Guid> getDedicatedVmForVdsList() {
        if (dedicatedVmForVdsList == null){
            dedicatedVmForVdsList = new LinkedList<Guid>();
        }
        return dedicatedVmForVdsList;
    }

    @JsonIgnore
    public void setDedicatedVmForVdsList(List<Guid> value) {
        dedicatedVmForVdsList = value;
    }

    public void setDedicatedVmForVdsList(Guid value) {
        dedicatedVmForVdsList = new LinkedList<Guid>();
        dedicatedVmForVdsList.add(value);
    }

    public DisplayType getDefaultDisplayType() {
        return defaultDisplayType;
    }

    public void setDefaultDisplayType(DisplayType value) {
        defaultDisplayType = value;
    }

    public String getOvfVersion() {
        return ovfVersion;
    }

    public void setOvfVersion(String ovfVersion) {
        this.ovfVersion = ovfVersion;
    }

    public Boolean getTunnelMigration() {
        return tunnelMigration;
    }

    public void setTunnelMigration(Boolean value) {
        tunnelMigration = value;
    }

    public ConsoleDisconnectAction getConsoleDisconnectAction() {
        return consoleDisconnectAction;
    }

    public void setConsoleDisconnectAction(ConsoleDisconnectAction consoleDisconnectAction) {
        this.consoleDisconnectAction = consoleDisconnectAction;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public int getMinAllocatedMem() {
        return minAllocatedMem;
    }

    public void setMinAllocatedMem(int value) {
        minAllocatedMem = value;
    }

    public boolean isRunAndPause() {
        return runAndPause;
    }

    public void setRunAndPause(boolean runAndPause) {
        this.runAndPause = runAndPause;
    }

    public Guid getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Guid createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public int getCpuShares() {
        return cpuShares;
    }

    public void setCpuShares(int cpuShares) {
        this.cpuShares = cpuShares;
    }

    public SsoMethod getSsoMethod() {
        return ssoMethod;
    }

    public void setSsoMethod(SsoMethod ssoMethod) {
        this.ssoMethod = ssoMethod;
    }

    public void setMigrationDowntime(Integer migrationDowntime) {
        this.migrationDowntime = migrationDowntime;
    }

    public Integer getMigrationDowntime() {
        return this.migrationDowntime;
    }

    public VmInit getVmInit() {
        return vmInit;
    }

    public void setVmInit(VmInit vmInit) {
        this.vmInit = vmInit;
    }

    public SerialNumberPolicy getSerialNumberPolicy() {
        return serialNumberPolicy;
    }

    public void setSerialNumberPolicy(SerialNumberPolicy serialNumberPolicy) {
        this.serialNumberPolicy = serialNumberPolicy;
    }

    public String getCustomSerialNumber() {
        return customSerialNumber;
    }

    public void setCustomSerialNumber(String customSerialNumber) {
        this.customSerialNumber = customSerialNumber;
    }

    public boolean isBootMenuEnabled() {
        return bootMenuEnabled;
    }

    public void setBootMenuEnabled(boolean bootMenuEnabled) {
        this.bootMenuEnabled = bootMenuEnabled;
    }

    public boolean isSpiceFileTransferEnabled() {
        return spiceFileTransferEnabled;
    }

    public void setSpiceFileTransferEnabled(boolean spiceFileTransferEnabled) {
        this.spiceFileTransferEnabled = spiceFileTransferEnabled;
    }

    public boolean isSpiceCopyPasteEnabled() {
        return spiceCopyPasteEnabled;
    }

    public void setSpiceCopyPasteEnabled(boolean spiceCopyPasteEnabled) {
        this.spiceCopyPasteEnabled = spiceCopyPasteEnabled;
    }

    public Guid getCpuProfileId() {
        return cpuProfileId;
    }

    public void setCpuProfileId(Guid cpuProfileId) {
        this.cpuProfileId = cpuProfileId;
    }

    public NumaTuneMode getNumaTuneMode() {
        return numaTuneMode;
    }

    public void setNumaTuneMode(NumaTuneMode numaTuneMode) {
        this.numaTuneMode = numaTuneMode;
    }

    public List<VmNumaNode> getvNumaNodeList() {
        return vNumaNodeList;
    }

    public void setvNumaNodeList(List<VmNumaNode> vNumaNodeList) {
        this.vNumaNodeList = vNumaNodeList;
    }

    public Boolean getAutoConverge() {
        return autoConverge;
    }

    public void setAutoConverge(Boolean autoConverge) {
        this.autoConverge = autoConverge;
    }

    public Boolean getMigrateCompressed() {
        return migrateCompressed;
    }

    public void setMigrateCompressed(Boolean migrateCompressed) {
        this.migrateCompressed = migrateCompressed;
    }

    public String getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(String customProperties) {
        this.customProperties = customProperties;
    }

    public String getPredefinedProperties() {
        return predefinedProperties;
    }

    public void setPredefinedProperties(String predefinedProperties) {
        this.predefinedProperties = predefinedProperties;
    }

    public String getUserDefinedProperties() {
        return userDefinedProperties;
    }

    public void setUserDefinedProperties(String userDefinedProperties) {
        this.userDefinedProperties = userDefinedProperties;
    }

    public String getCustomEmulatedMachine() {
        return customEmulatedMachine;
    }

    public void setCustomEmulatedMachine(String customEmulatedMachine) {
        this.customEmulatedMachine = ((customEmulatedMachine == null || customEmulatedMachine.trim().isEmpty()) ? null : customEmulatedMachine);
    }

    public String getCustomCpuName() {
        return customCpuName;
    }

    public void setCustomCpuName(String customCpuName) {
        this.customCpuName = ((customCpuName==null || customCpuName.trim().isEmpty()) ? null : customCpuName);
    }

}
