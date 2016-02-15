package org.ovirt.engine.core.common.businessentities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.ovirt.engine.core.common.businessentities.comparators.BusinessEntityComparator;
import org.ovirt.engine.core.common.businessentities.storage.DiskImageDynamic;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;

public class VmDynamic implements BusinessEntityWithStatus<Guid, VMStatus>, Comparable<VmDynamic> {
    private static final long serialVersionUID = 7789482445091432555L;

    private Guid id;
    private VMStatus status;
    private String vmIp;
    private String vmFQDN;
    @UnchangeableByVdsm
    private String vmHost;
    private Integer vmPid;
    @UnchangeableByVdsm
    private Date lastStartTime;
    @UnchangeableByVdsm
    private Date lastStopTime;
    private String guestCurUserName;
    @UnchangeableByVdsm
    private String consoleCurrentUserName;
    @UnchangeableByVdsm
    private Guid consoleUserId;
    private String guestOs;
    @UnchangeableByVdsm
    private Guid migratingToVds;
    @UnchangeableByVdsm
    private Guid runOnVds;
    private String appList;
    private Boolean acpiEnabled;
    private SessionState session;
    private String vncKeyboardLayout;
    private Boolean kvmEnable;
    private Integer utcDiff;
    @UnchangeableByVdsm
    private Guid lastVdsRunOn;
    private String clientIp;
    private Integer guestRequestedMemory;
    @UnchangeableByVdsm
    private BootSequence bootSequence;
    private VmExitStatus exitStatus;
    private VmPauseStatus pauseStatus;
    private String hash;
    private int guestAgentNicsHash;
    @UnchangeableByVdsm
    private String exitMessage;
    @UnchangeableByVdsm
    private ArrayList<DiskImageDynamic> disks;
    private boolean win2kHackEnabled;
    private Long lastWatchdogEvent;
    private String lastWatchdogAction;
    @UnchangeableByVdsm
    private boolean runOnce;
    @UnchangeableByVdsm
    private String cpuName;
    @UnchangeableByVdsm
    private GuestAgentStatus guestAgentStatus;
    @UnchangeableByVdsm
    private String emulatedMachine;
    private String currentCd;
    @UnchangeableByVdsm
    private String stopReason;
    private VmExitReason exitReason;
    private int guestCpuCount;
    private Map<GraphicsType, GraphicsInfo> graphicsInfos;
    private Long guestMemoryCached;
    private Long guestMemoryBuffered;
    private Long guestMemoryFree;
    private String guestOsVersion;
    private String guestOsDistribution;
    private String guestOsCodename;
    private ArchitectureType guestOsArch;
    private OsType guestOsType;
    private String guestOsKernelVersion;
    private String guestOsTimezoneName;
    private int guestOsTimezoneOffset;

    public static final String APPLICATIONS_LIST_FIELD_NAME = "appList";
    public static final String STATUS_FIELD_NAME = "status";

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((acpiEnabled == null) ? 0 : acpiEnabled.hashCode());
        result = prime * result + ((appList == null) ? 0 : appList.hashCode());
        result = prime * result + bootSequence.hashCode();
        result = prime * result + ((clientIp == null) ? 0 : clientIp.hashCode());
        result = prime * result + ((vncKeyboardLayout == null) ? 0 : vncKeyboardLayout.hashCode());
        result = prime * result + ((consoleCurrentUserName == null) ? 0 : consoleCurrentUserName.hashCode());
        result = prime * result + ((guestCurUserName == null) ? 0 : guestCurUserName.hashCode());
        result = prime * result + ((consoleUserId == null) ? 0 : consoleUserId.hashCode());
        result = prime * result + ((guestOs == null) ? 0 : guestOs.hashCode());
        result = prime * result + ((guestRequestedMemory == null) ? 0 : guestRequestedMemory.hashCode());
        result = prime * result + ((kvmEnable == null) ? 0 : kvmEnable.hashCode());
        result = prime * result + ((lastVdsRunOn == null) ? 0 : lastVdsRunOn.hashCode());
        result = prime * result + ((disks == null) ? 0 : disks.hashCode());
        result = prime * result + ((exitMessage == null) ? 0 : exitMessage.hashCode());
        result = prime * result + exitStatus.hashCode();
        result = prime * result + (win2kHackEnabled ? 1231 : 1237);
        result = prime * result + ((migratingToVds == null) ? 0 : migratingToVds.hashCode());
        result = prime * result + ((pauseStatus == null) ? 0 : pauseStatus.hashCode());
        result = prime * result + ((runOnVds == null) ? 0 : runOnVds.hashCode());
        result = prime * result + session.hashCode();
        result = prime * result + status.hashCode();
        result = prime * result + ((utcDiff == null) ? 0 : utcDiff.hashCode());
        result = prime * result + ((vmHost == null) ? 0 : vmHost.hashCode());
        result = prime * result + ((vmIp == null) ? 0 : vmIp.hashCode());
        result = prime * result + ((vmFQDN == null) ? 0 : vmFQDN.hashCode());
        result = prime * result + ((lastStartTime == null) ? 0 : lastStartTime.hashCode());
        result = prime * result + ((lastStopTime == null) ? 0 : lastStopTime.hashCode());
        result = prime * result + ((vmPid == null) ? 0 : vmPid.hashCode());
        result = prime * result + (lastWatchdogEvent == null ? 0 : lastWatchdogEvent.hashCode());
        result = prime * result + (lastWatchdogAction == null ? 0 : lastWatchdogAction.hashCode());
        result = prime * result + (runOnce ? 1231 : 1237);
        result = prime * result + (cpuName == null ? 0 : cpuName.hashCode());
        result = prime * result + (guestAgentStatus == null ? 0 : guestAgentStatus.hashCode());
        result = prime * result + (currentCd == null ? 0 : currentCd.hashCode());
        result = prime * result + (stopReason == null ? 0 : stopReason.hashCode());
        result = prime * result + exitReason.hashCode();
        result = prime * result + (emulatedMachine == null ? 0 : emulatedMachine.hashCode());
        result = prime * result + graphicsInfos.hashCode();
        result = prime * result + (guestMemoryFree == null ? 0 : guestMemoryFree.hashCode());
        result = prime * result + (guestMemoryBuffered == null ? 0 : guestMemoryBuffered.hashCode());
        result = prime * result + (guestMemoryCached == null ? 0 : guestMemoryCached.hashCode());
        result = prime * result + (guestOsTimezoneName == null ? 0 : guestOsTimezoneName.hashCode());
        result = prime * result + guestOsTimezoneOffset;
        result = prime * result + guestOsArch.hashCode();
        result = prime * result + (guestOsCodename == null ? 0 : guestOsCodename.hashCode());
        result = prime * result + (guestOsDistribution == null ? 0 : guestOsDistribution.hashCode());
        result = prime * result + (guestOsKernelVersion == null ? 0 : guestOsKernelVersion.hashCode());
        result = prime * result + (guestOsVersion == null ? 0 : guestOsVersion.hashCode());
        result = prime * result + guestOsType.hashCode();
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        VmDynamic other = (VmDynamic) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && ObjectUtils.objectsEqual(acpiEnabled, other.acpiEnabled)
                && ObjectUtils.objectsEqual(appList, other.appList)
                && bootSequence == other.bootSequence
                && ObjectUtils.objectsEqual(clientIp, other.clientIp)
                && ObjectUtils.objectsEqual(vncKeyboardLayout, other.vncKeyboardLayout)
                && ObjectUtils.objectsEqual(consoleCurrentUserName, other.consoleCurrentUserName)
                && ObjectUtils.objectsEqual(guestCurUserName, other.guestCurUserName)
                && ObjectUtils.objectsEqual(consoleUserId, other.consoleUserId)
                && ObjectUtils.objectsEqual(guestOs, other.guestOs)
                && ObjectUtils.objectsEqual(guestRequestedMemory, other.guestRequestedMemory)
                && ObjectUtils.objectsEqual(kvmEnable, other.kvmEnable)
                && ObjectUtils.objectsEqual(lastVdsRunOn, other.lastVdsRunOn)
                && ObjectUtils.objectsEqual(disks, other.disks)
                && ObjectUtils.objectsEqual(exitMessage, other.exitMessage)
                && exitStatus == other.exitStatus
                && win2kHackEnabled == other.win2kHackEnabled
                && ObjectUtils.objectsEqual(migratingToVds, other.migratingToVds)
                && pauseStatus == other.pauseStatus
                && ObjectUtils.objectsEqual(runOnVds, other.runOnVds)
                && session == other.session
                && status == other.status
                && ObjectUtils.objectsEqual(utcDiff, other.utcDiff)
                && ObjectUtils.objectsEqual(vmHost, other.vmHost)
                && ObjectUtils.objectsEqual(vmIp, other.vmIp)
                && ObjectUtils.objectsEqual(vmFQDN, other.vmFQDN)
                && ObjectUtils.objectsEqual(lastStartTime, other.lastStartTime)
                && ObjectUtils.objectsEqual(lastStopTime, other.lastStopTime)
                && ObjectUtils.objectsEqual(vmPid, other.vmPid)
                && ObjectUtils.objectsEqual(lastWatchdogEvent, other.lastWatchdogEvent)
                && ObjectUtils.objectsEqual(lastWatchdogAction, other.lastWatchdogAction)
                && runOnce == other.runOnce
                && ObjectUtils.objectsEqual(cpuName, other.cpuName)
                && ObjectUtils.objectsEqual(guestAgentStatus, other.guestAgentStatus)
                && ObjectUtils.objectsEqual(currentCd, other.currentCd)
                && ObjectUtils.objectsEqual(stopReason, other.stopReason)
                && exitReason == other.exitReason
                && ObjectUtils.objectsEqual(emulatedMachine, other.emulatedMachine))
                && ObjectUtils.objectsEqual(graphicsInfos, other.getGraphicsInfos())
                && ObjectUtils.objectsEqual(guestMemoryBuffered, other.guestMemoryBuffered)
                && ObjectUtils.objectsEqual(guestMemoryCached, other.guestMemoryCached)
                && ObjectUtils.objectsEqual(guestMemoryFree, other.guestMemoryFree)
                && ObjectUtils.objectsEqual(guestOsTimezoneName, other.guestOsTimezoneName)
                && guestOsTimezoneOffset == other.guestOsTimezoneOffset
                && ObjectUtils.objectsEqual(guestOsVersion, other.guestOsVersion)
                && ObjectUtils.objectsEqual(guestOsDistribution, other.guestOsDistribution)
                && ObjectUtils.objectsEqual(guestOsCodename, other.guestOsCodename)
                && ObjectUtils.objectsEqual(guestOsKernelVersion, other.guestOsKernelVersion)
                && ObjectUtils.objectsEqual(guestOsArch, other.guestOsArch)
                && ObjectUtils.objectsEqual(guestOsType, other.guestOsType);
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public void setExitMessage(String value) {
        exitMessage = value;
    }

    public VmExitStatus getExitStatus() {
        return this.exitStatus;
    }

    public void setExitStatus(VmExitStatus value) {
        exitStatus = value;
    }

    public ArrayList<DiskImageDynamic> getDisks() {
        return disks;
    }

    public void setDisks(ArrayList<DiskImageDynamic> value) {
        disks = value;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getGuestAgentNicsHash() {
        return guestAgentNicsHash;
    }

    public void setGuestAgentNicsHash(int guestAgentNicsHash) {
        this.guestAgentNicsHash = guestAgentNicsHash;
    }

    public boolean getWin2kHackEnable() {
        return win2kHackEnabled;
    }

    public void setWin2kHackEnable(boolean value) {
        win2kHackEnabled = value;
    }

    public VmDynamic() {
        id = Guid.Empty;
        status = VMStatus.Down;
        pauseStatus = VmPauseStatus.NONE;
        exitStatus = VmExitStatus.Normal;
        win2kHackEnabled = false;
        acpiEnabled = true;
        kvmEnable = true;
        session = SessionState.Unknown;
        bootSequence = BootSequence.C;
        exitReason = VmExitReason.Unknown;
        graphicsInfos = new HashMap<GraphicsType, GraphicsInfo>();
        guestAgentStatus = GuestAgentStatus.DoesntExist;
        guestOsTimezoneName = "";
        guestOsTimezoneOffset = 0;
        guestOsVersion = "";
        guestOsDistribution = "";
        guestOsCodename = "";
        guestOsKernelVersion = "";
        guestOsArch = ArchitectureType.undefined;
        guestOsType = OsType.Other;
        disks = new ArrayList<>();
    }

    public VmDynamic(VmDynamic template) {
        id = template.getId();
        status = template.getStatus();
        vmIp = template.getVmIp();
        vmFQDN = template.getVmFQDN();
        vmHost = template.getVmHost();
        vmPid = template.getVmPid();
        lastStartTime = template.getLastStartTime();
        lastStopTime = template.getLastStopTime();
        guestCurUserName = template.getGuestCurrentUserName();
        consoleCurrentUserName = template.getConsoleCurrentUserName();
        consoleUserId = template.getConsoleUserId();
        guestOs = template.getGuestOs();
        migratingToVds = template.getMigratingToVds();
        runOnVds = template.getRunOnVds();
        appList = template.getAppList();
        acpiEnabled = template.getAcpiEnable();
        session = template.getSession();
        vncKeyboardLayout = template.getVncKeyboardLayout();
        kvmEnable = template.getKvmEnable();
        utcDiff = template.getUtcDiff();
        lastVdsRunOn = template.getLastVdsRunOn();
        clientIp = template.getClientIp();
        guestRequestedMemory = template.getGuestRequestedMemory();
        bootSequence = template.getBootSequence();
        exitStatus = template.getExitStatus();
        pauseStatus = template.getPauseStatus();
        hash = template.getHash();
        guestAgentNicsHash = template.getGuestAgentNicsHash();
        exitMessage = template.getExitMessage();
        disks = new ArrayList<>(template.getDisks());
        win2kHackEnabled = template.getWin2kHackEnable();
        lastWatchdogEvent = template.getLastWatchdogEvent();
        lastWatchdogAction = template.getLastWatchdogAction();
        runOnce = template.isRunOnce();
        cpuName = template.getCpuName();
        guestAgentStatus = template.getGuestAgentStatus();
        emulatedMachine = template.getEmulatedMachine();
        currentCd = template.getCurrentCd();
        stopReason = template.getStopReason();
        exitReason = template.getExitReason();
        guestCpuCount = template.getGuestCpuCount();
        graphicsInfos = new HashMap<>(template.getGraphicsInfos());
        guestMemoryCached = template.getGuestMemoryCached();
        guestMemoryBuffered = template.getGuestMemoryBuffered();
        guestMemoryFree = template.getGuestMemoryFree();
        guestOsVersion = template.getGuestOsVersion();
        guestOsDistribution = template.getGuestOsDistribution();
        guestOsCodename = template.getGuestOsCodename();
        guestOsArch = template.getGuestOsArch();
        guestOsType = template.getGuestOsType();
        guestOsKernelVersion = template.getGuestOsKernelVersion();
        guestOsTimezoneName = template.getGuestOsTimezoneName();
        guestOsTimezoneOffset = template.getGuestOsTimezoneOffset();
    }

    public String getAppList() {
        return this.appList;
    }

    public void setAppList(String value) {
        this.appList = value;
    }

    public String getConsoleCurrentUserName() {
        return consoleCurrentUserName;
    }

    public void setConsoleCurrentUserName(String consoleCurUserName) {
        this.consoleCurrentUserName = consoleCurUserName;
    }

    public String getGuestCurrentUserName() {
        return this.guestCurUserName;
    }

    public void setGuestCurrentUserName(String value) {
        this.guestCurUserName = value;
    }

    public Guid getConsoleUserId() {
        return this.consoleUserId;
    }

    public void setConsoleUserId(Guid value) {
        this.consoleUserId = value;
    }

    public String getGuestOs() {
        return this.guestOs;
    }

    public void setGuestOs(String value) {
        this.guestOs = value;
    }

    public Guid getMigratingToVds() {
        return this.migratingToVds;
    }

    public void setMigratingToVds(Guid value) {
        this.migratingToVds = value;
    }

    public Guid getRunOnVds() {
        return this.runOnVds;
    }

    public void setRunOnVds(Guid value) {
        this.runOnVds = value;
    }

    @Override
    public VMStatus getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(VMStatus value) {
        this.status = value;
    }

    @Override
    public Guid getId() {
        return this.id;
    }

    @Override
    public void setId(Guid value) {
        this.id = value;
    }

    public String getVmHost() {
        return this.vmHost;
    }

    public void setVmHost(String value) {
        this.vmHost = value;
    }

    public String getVmFQDN() {
        return this.vmFQDN;
    }

    public void setVmFQDN(String fqdn) {
        this.vmFQDN = fqdn;
    }

    public String getVmIp() {
        return this.vmIp;
    }

    public void setVmIp(String value) {
        this.vmIp = value;
    }

    public Date getLastStartTime() {
        return this.lastStartTime;
    }

    public void setLastStartTime(Date value) {
        this.lastStartTime = value;
    }

    public Date getLastStopTime() {
        return this.lastStopTime;
    }

    public void setLastStopTime(Date value) {
        this.lastStopTime = value;
    }

    public Integer getVmPid() {
        return this.vmPid;
    }

    public void setVmPid(Integer value) {
        this.vmPid = value;
    }

    public Map<GraphicsType, GraphicsInfo> getGraphicsInfos() {
        return graphicsInfos;
    }

    /*
     * DON'T use this setter. It's here only for serizalization.
     */
    public void setGraphicsInfos(Map<GraphicsType, GraphicsInfo> graphicsInfos) {
        this.graphicsInfos = graphicsInfos;
    }

    public Boolean getAcpiEnable() {
        return this.acpiEnabled;
    }

    public void setAcpiEnable(Boolean value) {
        this.acpiEnabled = value;
    }

    public String getVncKeyboardLayout() {
        return vncKeyboardLayout;
    }

    public void setVncKeyboardLayout(String vncKeyboardLayout) {
        this.vncKeyboardLayout = vncKeyboardLayout;
    }

    public Boolean getKvmEnable() {
        return this.kvmEnable;
    }

    public void setKvmEnable(Boolean value) {
        this.kvmEnable = value;
    }

    public SessionState getSession() {
        return this.session;
    }

    public void setSession(SessionState value) {
        this.session = value;
    }

    public BootSequence getBootSequence() {
        return this.bootSequence;
    }

    public void setBootSequence(BootSequence value) {
        this.bootSequence = value;
    }

    public Integer getUtcDiff() {
        return this.utcDiff;
    }

    public void setUtcDiff(Integer value) {
        this.utcDiff = value;
    }

    public Guid getLastVdsRunOn() {
        return this.lastVdsRunOn;
    }

    public void setLastVdsRunOn(Guid value) {
        this.lastVdsRunOn = value;
    }

    public String getClientIp() {
        return this.clientIp;
    }

    public void setClientIp(String value) {
        this.clientIp = value;
    }

    public Integer getGuestRequestedMemory() {
        return this.guestRequestedMemory;
    }

    public void setGuestRequestedMemory(Integer value) {
        this.guestRequestedMemory = value;
    }

    public void setPauseStatus(VmPauseStatus pauseStatus) {
        this.pauseStatus = pauseStatus;
    }

    public VmPauseStatus getPauseStatus() {
        return this.pauseStatus;
    }

    @Override
    public int compareTo(VmDynamic o) {
        return BusinessEntityComparator.<VmDynamic, Guid>newInstance().compare(this, o);
    }

    public Long getLastWatchdogEvent() {
        return lastWatchdogEvent;
    }

    public void setLastWatchdogEvent(Long lastWatchdogEvent) {
        this.lastWatchdogEvent = lastWatchdogEvent;
    }

    public String getLastWatchdogAction() {
        return lastWatchdogAction;
    }

    public void setLastWatchdogAction(String lastWatchdogAction) {
        this.lastWatchdogAction = lastWatchdogAction;
    }

    public boolean isRunOnce() {
        return runOnce;
    }

    public void setRunOnce(boolean runOnce) {
        this.runOnce = runOnce;
    }
    public String getCpuName() {
        return cpuName;
    }

    public void setCpuName(String cpuName) {
        this.cpuName = cpuName;
    }
    public GuestAgentStatus getGuestAgentStatus() {
        return guestAgentStatus;
    }

    public void setGuestAgentStatus(GuestAgentStatus guestAgentStatus) {
        this.guestAgentStatus = guestAgentStatus;
    }

    public String getCurrentCd() {
        return currentCd;
    }

    public void setCurrentCd(String currentCd) {
        this.currentCd = currentCd;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public VmExitReason getExitReason() {
        return exitReason;
    }

    public void setExitReason(VmExitReason value) {
        exitReason = value;
    }

    public void setGuestCpuCount(int guestCpuCount) {
        this.guestCpuCount = guestCpuCount;
    }

    public int getGuestCpuCount() {
        return guestCpuCount;
    }

    public String getEmulatedMachine() {
        return emulatedMachine;
    }

    public void setEmulatedMachine(String emulatedMachine) {
        this.emulatedMachine = emulatedMachine;
    }

    public Long getGuestMemoryCached() {
        return guestMemoryCached;
    }

    public void setGuestMemoryCached(Long guestMemoryCached) {
        this.guestMemoryCached = guestMemoryCached;
    }

    public Long getGuestMemoryBuffered() {
        return guestMemoryBuffered;
    }

    public void setGuestMemoryBuffered(Long guestMemoryBuffered) {
        this.guestMemoryBuffered = guestMemoryBuffered;
    }

    public Long getGuestMemoryFree() {
        return guestMemoryFree;
    }

    public void setGuestMemoryFree(Long guestMemoryFree) {
        this.guestMemoryFree = guestMemoryFree;
    }

    public int getGuestOsTimezoneOffset() {
        return guestOsTimezoneOffset;
    }

    public void setGuestOsTimezoneOffset(int guestOsTimezoneOffset) {
        this.guestOsTimezoneOffset = guestOsTimezoneOffset;
    }

    public String getGuestOsTimezoneName() {
        return guestOsTimezoneName;
    }

    public void setGuestOsTimezoneName(String guestOsTimezoneName) {
        this.guestOsTimezoneName = guestOsTimezoneName;
    }

    public String getGuestOsVersion() {
        return guestOsVersion;
    }

    public void setGuestOsVersion(String guestOsVersion) {
        this.guestOsVersion = guestOsVersion;
    }

    public String getGuestOsDistribution() {
        return guestOsDistribution;
    }

    public void setGuestOsDistribution(String guestOsDistribution) {
        this.guestOsDistribution = guestOsDistribution;
    }

    public String getGuestOsCodename() {
        return guestOsCodename;
    }

    public void setGuestOsCodename(String guestOsCodename) {
        this.guestOsCodename = guestOsCodename;
    }

    public ArchitectureType getGuestOsArch() {
        return guestOsArch;
    }

    public void setGuestOsArch(ArchitectureType guestOsArch) {
        this.guestOsArch = guestOsArch;
    }

    @JsonIgnore
    public void setGuestOsArch(Integer arch) {
        this.guestOsArch = ArchitectureType.forValue(arch);
    }

    @JsonIgnore
    public void setGuestOsArch(String arch) {
        this.guestOsArch = ArchitectureType.valueOf(arch);
    }

    public OsType getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(OsType guestOsType) {
        this.guestOsType = guestOsType;
    }

    @JsonIgnore
    public void setGuestOsType(String osType) {
        this.guestOsType = EnumUtils.valueOf(OsType.class, osType, true);
    }

    public String getGuestOsKernelVersion() {
        return guestOsKernelVersion;
    }

    public void setGuestOsKernelVersion(String guestOsKernelVersion) {
        this.guestOsKernelVersion = guestOsKernelVersion;
    }
}
