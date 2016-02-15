package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Range;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.validation.annotation.HostnameOrIp;
import org.ovirt.engine.core.common.validation.annotation.ValidNameWithDot;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.PowerManagementCheck;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;

public class VdsStatic implements BusinessEntity<Guid>, Commented {

    private static final long serialVersionUID = -1425566208615075937L;
    private static final int HOST_DEFAULT_SPM_PRIORITY = 5;
    private static final int DEFAULT_SSH_PORT = 22;
    private static final String DEFAULT_SSH_USERNAME = "root";

    private Guid id;

    @EditableField
    @Size(min = 1, max = BusinessEntitiesDefinitions.HOST_NAME_SIZE)
    @ValidNameWithDot(message = "VALIDATION_VDS_NAME_INVALID", groups = { CreateEntity.class, UpdateEntity.class })
    private String name;

    @EditableField
    private String comment;

    @EditableField
    @HostnameOrIp(message = "VALIDATION.VDS.POWER_MGMT.ADDRESS.HOSTNAME_OR_IP", groups = PowerManagementCheck.class)
    @Size(max = BusinessEntitiesDefinitions.HOST_IP_SIZE)
    private String managementIp;

    @EditableField
    @HostnameOrIp(message = "VALIDATION.VDS.CONSOLEADDRESSS.HOSTNAME_OR_IP",
            groups = { CreateEntity.class, UpdateEntity.class })
    @Size(max = BusinessEntitiesDefinitions.CONSOLE_ADDRESS_SIZE)
    private String consoleAddress;

    @Size(max = BusinessEntitiesDefinitions.HOST_UNIQUE_ID_SIZE)
    private String uniqueId;

    @EditableOnVdsStatus
    @HostnameOrIp(message = "VALIDATION.VDS.HOSTNAME.HOSTNAME_OR_IP",
            groups = { CreateEntity.class, UpdateEntity.class })
    @NotNull(groups = { CreateEntity.class, UpdateEntity.class })
    @Size(max = BusinessEntitiesDefinitions.HOST_HOSTNAME_SIZE)
    private String hostName;

    @EditableField
    @Range(min = BusinessEntitiesDefinitions.NETWORK_MIN_LEGAL_PORT,
            max = BusinessEntitiesDefinitions.NETWORK_MAX_LEGAL_PORT,
            message = "VALIDATION.VDS.PORT.RANGE")
    private int port;

    @EditableField
    private VdsProtocol protocol;

    @EditableOnVdsStatus
    @Range(min = BusinessEntitiesDefinitions.NETWORK_MIN_LEGAL_PORT,
            max = BusinessEntitiesDefinitions.NETWORK_MAX_LEGAL_PORT,
            message = "VALIDATION.VDS.SSH_PORT.RANGE")
    private int sshPort;

    @EditableField
    @Size(min = 1, max = BusinessEntitiesDefinitions.HOST_NAME_SIZE)
    @ValidNameWithDot(message = "VALIDATION_VDS_SSH_USERNAME_INVALID", groups = { CreateEntity.class,
            UpdateEntity.class })
    private String sshUsername;

    @EditableOnVdsStatus
    private Guid vdsGroupId;

    private Boolean serverSslEnabled;

    private VDSType vdsType;

    private Integer vdsStrength;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.HOST_PM_TYPE_SIZE)
    private String pmType;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.HOST_PM_USER_SIZE)
    private String pmUser;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.HOST_PM_PASSWD_SIZE)
    private String pmPassword;

    @EditableField
    private Integer pmPort;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    private String pmOptions;

    @EditableField
    private boolean pmEnabled;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.GENERAL_NAME_SIZE)
    private String pmProxyPreferences;

    @EditableField
    @HostnameOrIp(message = "VALIDATION.VDS.POWER_MGMT.ADDRESS.HOSTNAME_OR_IP", groups = PowerManagementCheck.class)
    @Size(max = BusinessEntitiesDefinitions.HOST_IP_SIZE)
    private String pmSecondaryIp;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.HOST_PM_TYPE_SIZE)
    private String pmSecondaryType;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.HOST_PM_USER_SIZE)
    private String pmSecondaryUser;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.HOST_PM_PASSWD_SIZE)
    private String pmSecondaryPassword;

    @EditableField
    private Integer pmSecondaryPort;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    private String pmSecondaryOptions;

    @EditableField
    private boolean pmSecondaryConcurrent;

    @EditableField
    private HashMap<String, String> pmOptionsMap;

    @EditableField
    private HashMap<String, String> pmSecondaryOptionsMap;

    @EditableField
    private boolean pmKdumpDetection;

    /**
     * When this flag is true, the automatic power management
     * is not allowed to touch this host.
     */
    @EditableField
    private boolean disablePowerManagementPolicy;

    @EditableField
    private long otpValidity;

    @EditableField
    @Min(BusinessEntitiesDefinitions.HOST_MIN_SPM_PRIORITY)
    @Max(BusinessEntitiesDefinitions.HOST_MAX_SPM_PRIORITY)
    private int vdsSpmPriority;

    private boolean autoRecoverable;

    @EditableField
    @Size(max = BusinessEntitiesDefinitions.SSH_KEY_FINGERPRINT_SIZE)
    private String sshKeyFingerprint;

    private Guid hostProviderId;

    public boolean isAutoRecoverable() {
        return autoRecoverable;
    }

    public void setAutoRecoverable(boolean autoRecoverable) {
        this.autoRecoverable = autoRecoverable;
    }

    public VdsStatic() {
        serverSslEnabled = false;
        vdsStrength = 100;
        this.setPmOptions("");
        this.setPmSecondaryOptions("");
        this.vdsSpmPriority = HOST_DEFAULT_SPM_PRIORITY;
        this.sshPort = DEFAULT_SSH_PORT;
        this.sshUsername = DEFAULT_SSH_USERNAME;
        name = "";
        comment = "";
        vdsType = VDSType.VDS;
        autoRecoverable = true;
        disablePowerManagementPolicy = false;
        pmKdumpDetection = true;
        this.hostProviderId = null;
    }

    public VdsStatic(String host_name, String ip, String uniqueId, int port, int ssh_port, String ssh_username, Guid vds_group_id, Guid vds_id,
            String vds_name, boolean server_SSL_enabled, VDSType vds_type, Guid host_provider_id) {
        this();
        this.hostName = host_name;
        this.managementIp = ip;
        this.uniqueId = uniqueId;
        this.port = port;
        if (ssh_port > 0) {
            this.sshPort = ssh_port;
        }
        if (ssh_username != null) {
            this.sshUsername = ssh_username;
        }
        this.vdsGroupId = vds_group_id;
        this.id = vds_id;
        this.name = vds_name;
        this.serverSslEnabled = server_SSL_enabled;
        this.setVdsType(vds_type);
        this.hostProviderId = host_provider_id;
    }

    public boolean isServerSslEnabled() {
        return serverSslEnabled;
    }

    public void setServerSslEnabled(boolean value) {
        serverSslEnabled = value;
    }

    public String getHostName() {
        return this.hostName;
    }

    public void setHostName(String value) {
        this.hostName = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        comment = value;
    }

    public String getManagementIp() {
        return this.managementIp;
    }

    public void setManagementIp(String value) {
        this.managementIp = value;
    }

    public String getUniqueID() {
        return uniqueId;
    }

    public void setUniqueID(String value) {
        uniqueId = value;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int value) {
        this.port = value;
    }

    public VdsProtocol getProtocol() {
        return this.protocol;
    }

    public void setProtocol(VdsProtocol value) {
        this.protocol = value;
    }

    public int getSshPort() {
        return this.sshPort;
    }

    public void setSshPort(int value) {
        this.sshPort = value;
    }

    public String getSshUsername() {
        return this.sshUsername;
    }

    public void setSshUsername(String value) {
        this.sshUsername = value;
    }

    public Guid getVdsGroupId() {
        return this.vdsGroupId;
    }

    public void setVdsGroupId(Guid value) {
        this.vdsGroupId = value;
    }

    @Override
    public Guid getId() {
        return this.id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setVdsName(String value) {
        this.name = value;
    }

    public VDSType getVdsType() {
        return this.vdsType;
    }

    public void setVdsType(VDSType value) {
        this.vdsType = value;
    }

    public int getVdsStrength() {
        return this.vdsStrength;
    }

    public void setVdsStrength(int value) {
        // strength should be between 1 and 100
        this.vdsStrength = value < 1 ? 1 : value > 100 ? 100 : value;
    }

    public String getPmType() {
        return pmType;
    }

    public void setPmType(String value) {
        pmType = value;
    }

    public String getPmUser() {
        return pmUser;
    }

    public void setPmUser(String value) {
        pmUser = value;
    }

    public String getPmPassword() {
        return pmPassword;
    }

    public void setPmPassword(String value) {
        pmPassword = value;
    }

    public Integer getPmPort() {
        return pmPort;
    }

    public void setPmPort(Integer value) {
        pmPort = value;
    }

    public String getPmOptions() {
        return pmOptions;
    }

    public void setPmOptions(String value) {
        pmOptions = value;
        // set pmOptionsMap value content to match the given string.
        pmOptionsMap = pmOptionsStringToMap(value);
    }

    public HashMap<String, String> getPmOptionsMap() {
        return pmOptionsMap;
    }

    public void setPmOptionsMap(HashMap<String, String> value) {
        pmOptionsMap = value;
        pmOptions = pmOptionsMapToString(value);
    }

    public boolean isPmEnabled() {
        return pmEnabled;
    }

    public void setPmEnabled(boolean value) {
        pmEnabled = value;
    }

    public String getPmProxyPreferences() {
        return pmProxyPreferences;
    }

    public void setPmProxyPreferences(String pmProxyPreferences) {
        this.pmProxyPreferences = pmProxyPreferences;
    }
    public String getPmSecondaryIp() {
        return this.pmSecondaryIp;
    }

    public void setPmSecondaryIp(String value) {
        this.pmSecondaryIp = value;
    }

    public String getPmSecondaryType() {
        return pmSecondaryType;
    }

    public void setPmSecondaryType(String value) {
        pmSecondaryType = value;
    }

    public String getPmSecondaryUser() {
        return pmSecondaryUser;
    }

    public void setPmSecondaryUser(String value) {
        pmSecondaryUser = value;
    }

    public String getPmSecondaryPassword() {
        return pmSecondaryPassword;
    }

    public void setPmSecondaryPassword(String value) {
        pmSecondaryPassword = value;
    }

    public Integer getPmSecondaryPort() {
        return pmSecondaryPort;
    }

    public void setPmSecondaryPort(Integer value) {
        pmSecondaryPort = value;
    }

    public String getPmSecondaryOptions() {
        return pmSecondaryOptions;
    }

    public void setPmSecondaryOptions(String value) {
        pmSecondaryOptions = value;
        // set pmSecondaryOptionsMap value content to match the given string.
        pmSecondaryOptionsMap = pmOptionsStringToMap(value);
    }

    public boolean isPmSecondaryConcurrent() {
        return pmSecondaryConcurrent;
    }

    public void setPmSecondaryConcurrent(boolean value) {
        pmSecondaryConcurrent = value;
    }

    public HashMap<String, String> getPmSecondaryOptionsMap() {
        return pmSecondaryOptionsMap;
    }

    public void setPmSecondaryOptionsMap(HashMap<String, String> value) {
        pmSecondaryOptionsMap = value;
        pmSecondaryOptions = pmOptionsMapToString(value);
    }

    public boolean isPmKdumpDetection() {
        return pmKdumpDetection;
    }

    public void setPmKdumpDetection(boolean pmKdumpDetection) {
        this.pmKdumpDetection = pmKdumpDetection;
    }

    public boolean isDisablePowerManagementPolicy() {
        return disablePowerManagementPolicy;
    }

    public void setDisablePowerManagementPolicy(boolean disablePowerManagementPolicy) {
        this.disablePowerManagementPolicy = disablePowerManagementPolicy;
    }

    public long getOtpValidity() {
        return otpValidity;
    }

    public void setOtpValidity(long otpValidity) {
        this.otpValidity = otpValidity;
    }

    public int getVdsSpmPriority() {
        return vdsSpmPriority;
    }

    public void setVdsSpmPriority(int value) {
        this.vdsSpmPriority = value;
    }

    public String getSshKeyFingerprint() {
        return sshKeyFingerprint;
    }

    public void setSshKeyFingerprint(String sshKeyFingerprint) {
        this.sshKeyFingerprint = sshKeyFingerprint;
    }

    public String getConsoleAddress() {
        return consoleAddress;
    }

    public void setConsoleAddress(String consoleAddress) {
        this.consoleAddress = consoleAddress;
    }

    public void setHostProviderId (Guid hostProviderId) { this.hostProviderId = hostProviderId; }

    public Guid getHostProviderId () { return hostProviderId; }

    /**
     * Converts a PM Options map to string
     *
     * @param map
     * @return
     */
    public static String pmOptionsMapToString(HashMap<String, String> map) {
        String result = "";
        String seperator = "";
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pairs = it.next();
            String value = pairs.getValue();
            result +=
                    seperator + pairs.getKey()
                            + ((value != null && value.length() > 0) ? "=" + value : "");
            seperator = ",";
        }
        return result;
    }

    /**
     * Converts a PM Options string to a map.
     *
     * <b<Note:</b> A {@link HashMap} is used instead of the interface
     * {@link Map}, as this method is used by the frontend, and requires
     * GWT compilation.
     *
     * @param pmOptions String representation of the map
     * @return A parsed map
     */
    public static HashMap<String, String> pmOptionsStringToMap(String pmOptions) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (pmOptions == null || pmOptions.equals("")) {
            return map;
        }
        String[] tokens = pmOptions.split(",");
        for (String token : tokens) {
            String[] pair = token.split("=");
            if (pair.length == 2) { // key=value setting
                pair[1] = (pair[1] == null ? "" : pair[1]);
                // ignore illegal settings
                if (pair[0].trim().length() > 0 && pair[1].trim().length() > 0)
                    map.put(pair[0], pair[1]);
            } else { // only key setting
                map.put(pair[0], "");
            }
        }
        return map;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((consoleAddress == null) ? 0 : consoleAddress.hashCode());
        result = prime * result + ((managementIp == null) ? 0 : managementIp.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (otpValidity ^ (otpValidity >>> 32));
        result = prime * result + (pmEnabled ? 1231 : 1237);
        result = prime * result + ((pmOptions == null) ? 0 : pmOptions.hashCode());
        result = prime * result + ((pmOptionsMap == null) ? 0 : pmOptionsMap.hashCode());
        result = prime * result + ((pmPassword == null) ? 0 : pmPassword.hashCode());
        result = prime * result + ((pmPort == null) ? 0 : pmPort.hashCode());
        result = prime * result + ((pmType == null) ? 0 : pmType.hashCode());
        result = prime * result + ((pmUser == null) ? 0 : pmUser.hashCode());
        result = prime * result + ((pmSecondaryIp == null) ? 0 : managementIp.hashCode());
        result = prime * result + (pmSecondaryConcurrent ? 1231 : 1237);
        result = prime * result + ((pmSecondaryOptions == null) ? 0 : pmOptions.hashCode());
        result = prime * result + ((pmSecondaryOptionsMap == null) ? 0 : pmSecondaryOptionsMap.hashCode());
        result = prime * result + ((pmSecondaryPassword == null) ? 0 : pmSecondaryPassword.hashCode());
        result = prime * result + ((pmSecondaryPort == null) ? 0 : pmSecondaryPort.hashCode());
        result = prime * result + ((pmSecondaryType == null) ? 0 : pmSecondaryType.hashCode());
        result = prime * result + ((pmSecondaryUser == null) ? 0 : pmSecondaryUser.hashCode());
        result = prime * result + (pmKdumpDetection ? 1 : 0);
        result = prime * result + port;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + sshPort;
        result = prime * result + ((sshUsername == null) ? 0 : sshUsername.hashCode());
        result = prime * result + ((serverSslEnabled == null) ? 0 : serverSslEnabled.hashCode());
        result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result + ((vdsGroupId == null) ? 0 : vdsGroupId.hashCode());
        result = prime * result + ((vdsStrength == null) ? 0 : vdsStrength.hashCode());
        result = prime * result + ((vdsType == null) ? 0 : vdsType.hashCode());
        result = prime * result + (disablePowerManagementPolicy ? 0 : 1);
        result = prime * result + ((hostProviderId == null) ? 0 : hostProviderId.hashCode());
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
        VdsStatic other = (VdsStatic) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && ObjectUtils.objectsEqual(hostName, other.hostName)
                && ObjectUtils.objectsEqual(consoleAddress, other.consoleAddress)
                && ObjectUtils.objectsEqual(managementIp, other.managementIp)
                && ObjectUtils.objectsEqual(name, other.name)
                && otpValidity == other.otpValidity
                && pmEnabled == other.pmEnabled
                && ObjectUtils.objectsEqual(pmOptions, other.pmOptions)
                && ObjectUtils.objectsEqual(pmOptionsMap, other.pmOptionsMap)
                && ObjectUtils.objectsEqual(pmPassword, other.pmPassword)
                && ObjectUtils.objectsEqual(pmPort, other.pmPort)
                && ObjectUtils.objectsEqual(pmType, other.pmType)
                && ObjectUtils.objectsEqual(pmUser, other.pmUser)
                && ObjectUtils.objectsEqual(pmSecondaryIp, other.pmSecondaryIp)
                && pmSecondaryConcurrent == other.pmSecondaryConcurrent
                && ObjectUtils.objectsEqual(pmSecondaryOptions, other.pmSecondaryOptions)
                && ObjectUtils.objectsEqual(pmSecondaryOptionsMap, other.pmSecondaryOptionsMap)
                && ObjectUtils.objectsEqual(pmSecondaryPassword, other.pmSecondaryPassword)
                && ObjectUtils.objectsEqual(pmSecondaryPort, other.pmSecondaryPort)
                && ObjectUtils.objectsEqual(pmSecondaryType, other.pmSecondaryType)
                && ObjectUtils.objectsEqual(pmSecondaryUser, other.pmSecondaryUser)
                && pmKdumpDetection == other.isPmKdumpDetection()
                && port == other.port
                && protocol == other.protocol
                && sshPort == other.sshPort
                && ObjectUtils.objectsEqual(sshUsername, other.sshUsername)
                && ObjectUtils.objectsEqual(serverSslEnabled, other.serverSslEnabled)
                && ObjectUtils.objectsEqual(uniqueId, other.uniqueId)
                && ObjectUtils.objectsEqual(vdsGroupId, other.vdsGroupId)
                && ObjectUtils.objectsEqual(vdsStrength, other.vdsStrength)
                && vdsType == other.vdsType
                && ObjectUtils.objectsEqual(sshKeyFingerprint, other.sshKeyFingerprint))
                && disablePowerManagementPolicy == other.disablePowerManagementPolicy
                && ObjectUtils.objectsEqual(hostProviderId, other.hostProviderId);
    }
}
