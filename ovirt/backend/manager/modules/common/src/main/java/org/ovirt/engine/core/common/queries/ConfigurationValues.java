package org.ovirt.engine.core.common.queries;

public enum ConfigurationValues {
    MaxNumOfVmCpus(ConfigAuthType.User),
    MaxNumOfVmSockets(ConfigAuthType.User),
    MaxNumOfCpuPerSocket(ConfigAuthType.User),
    VdcVersion(ConfigAuthType.User),
    // GetAllAdDomains,
    SSLEnabled(ConfigAuthType.User),
    CipherSuite(ConfigAuthType.User),
    VmPoolLeaseDays(ConfigAuthType.User),
    VmPoolLeaseStartTime(ConfigAuthType.User),
    VmPoolLeaseEndTime(ConfigAuthType.User),
    MaxVmsInPool(ConfigAuthType.User),
    MaxVdsMemOverCommit(ConfigAuthType.User),
    MaxVdsMemOverCommitForServers(ConfigAuthType.User),
    AdUserName,
    // TODO remove remarks and AdUserPassword completely in version 3.1.
    // AdUserPassword field format has been changed.
    // AdUserPassword,
    LocalAdminPassword,
    ValidNumOfMonitors(ConfigAuthType.User),
    EnableUSBAsDefault(ConfigAuthType.User),
    SpiceSecureChannels(ConfigAuthType.User),
    ConsoleReleaseCursorKeys(ConfigAuthType.User),
    ConsoleToggleFullScreenKeys(ConfigAuthType.User),
    SpiceProxyDefault(ConfigAuthType.User),
    ClientModeSpiceDefault(ConfigAuthType.User),
    ClientModeVncDefault(ConfigAuthType.User),
    ClientModeRdpDefault(ConfigAuthType.User),
    UseFqdnForRdpIfAvailable(ConfigAuthType.User),
    WebSocketProxy(ConfigAuthType.User),
    WebSocketProxyTicketValiditySeconds(ConfigAuthType.User),
    HighUtilizationForEvenlyDistribute(ConfigAuthType.User),
    SpiceUsbAutoShare(ConfigAuthType.User),
    ImportDefaultPath,
    ComputerADPaths(ConfigAuthType.User),
    VdsSelectionAlgorithm,
    LowUtilizationForEvenlyDistribute,
    LowUtilizationForPowerSave,
    HighUtilizationForPowerSave,
    CpuOverCommitDurationMinutes,
    InstallVds,
    AsyncTaskPollingRate,
    FenceProxyDefaultPreferences,
    VcpuConsumptionPercentage(ConfigAuthType.User),
    SearchResultsLimit(ConfigAuthType.User),
    MaxBlockDiskSize(ConfigAuthType.User),
    EnableSpiceRootCertificateValidation(ConfigAuthType.User),
    VMMinMemorySizeInMB(ConfigAuthType.User),
    VM32BitMaxMemorySizeInMB(ConfigAuthType.User),
    VM64BitMaxMemorySizeInMB(ConfigAuthType.User),
    VmPriorityMaxValue(ConfigAuthType.User),
    StorageDomainNameSizeLimit(ConfigAuthType.User),
    ImportDataStorageDomain,
    HostedEngineStorageDomainName,
    StoragePoolNameSizeLimit(ConfigAuthType.User),
    SANWipeAfterDelete(ConfigAuthType.User),
    AuthenticationMethod(ConfigAuthType.User),
    UserDefinedVMProperties(ConfigAuthType.User),
    PredefinedVMProperties(ConfigAuthType.User),
    VdsFenceOptionTypes,
    FenceAgentMapping,
    FenceAgentDefaultParams,
    FenceAgentDefaultParamsForPPC,
    VdsFenceOptionMapping,
    VdsFenceType,
    SupportedClusterLevels(ConfigAuthType.User),
    OvfUpdateIntervalInMinutes,
    OvfItemsCountPerUpdate,
    ProductRPMVersion(ConfigAuthType.User),
    RhevhLocalFSPath,
    HotPlugEnabled(ConfigAuthType.User),
    HotPlugCpuSupported(ConfigAuthType.User),
    NetworkLinkingSupported(ConfigAuthType.User),
    SupportBridgesReportByVDSM(ConfigAuthType.User),
    ManagementNetwork,
    ApplicationMode(ConfigAuthType.User),
    ShareableDiskEnabled(ConfigAuthType.User),
    DirectLUNDiskEnabled(ConfigAuthType.User),
    WANDisableEffects(ConfigAuthType.User),
    WANColorDepth(ConfigAuthType.User),
    SupportForceCreateVG,
    NetworkConnectivityCheckTimeoutInSeconds,
    AllowClusterWithVirtGlusterEnabled,
    MTUOverrideSupported(ConfigAuthType.User),
    GlusterVolumeOptionGroupVirtValue,
    GlusterVolumeOptionOwnerUserVirtValue,
    GlusterVolumeOptionOwnerGroupVirtValue,
    CpuPinningEnabled,
    CpuPinMigrationEnabled,
    MigrationSupportForNativeUsb(ConfigAuthType.User),
    MigrationNetworkEnabled,
    VncKeyboardLayout(ConfigAuthType.User),
    VncKeyboardLayoutValidValues(ConfigAuthType.User),
    SupportCustomDeviceProperties,
    CustomDeviceProperties(ConfigAuthType.User),
    NetworkCustomPropertiesSupported,
    PreDefinedNetworkCustomProperties,
    UserDefinedNetworkCustomProperties,
    MultipleGatewaysSupported,
    VirtIoScsiEnabled(ConfigAuthType.User),
    OvfStoreOnAnyDomain,
    SshSoftFencingCommand,
    MemorySnapshotSupported(ConfigAuthType.User),
    HostNetworkQosSupported,
    StorageQosSupported(ConfigAuthType.User),
    CpuQosSupported(ConfigAuthType.User),
    MaxAverageNetworkQoSValue,
    MaxPeakNetworkQoSValue,
    MaxBurstNetworkQoSValue,
    UserMessageOfTheDay(ConfigAuthType.User),
    QoSInboundAverageDefaultValue,
    QoSInboundPeakDefaultValue,
    QoSInboundBurstDefaultValue,
    QoSOutboundAverageDefaultValue,
    QoSOutboundPeakDefaultValue,
    QoSOutboundBurstDefaultValue,
    MaxVmNameLengthWindows(ConfigAuthType.User),
    MaxVmNameLengthNonWindows(ConfigAuthType.User),
    AttestationServer,
    DefaultGeneralTimeZone,
    DefaultWindowsTimeZone,
    SpeedOptimizationSchedulingThreshold,
    SchedulerAllowOverBooking,
    SchedulerOverBookingThreshold,
    UserSessionTimeOutInterval(ConfigAuthType.User),
    DefaultMaximumMigrationDowntime,
    IsMigrationSupported(ConfigAuthType.User),
    IsMemorySnapshotSupported(ConfigAuthType.User),
    IsSuspendSupported(ConfigAuthType.User),
    SerialNumberPolicySupported(ConfigAuthType.User),
    IscsiMultipathingSupported,
    BootMenuSupported(ConfigAuthType.User),
    MixedDomainTypesInDataCenter,
    KeystoneAuthUrl,
    VirtIoRngDeviceSupported(ConfigAuthType.User),
    ClusterRequiredRngSourcesDefault(ConfigAuthType.User),
    SpiceFileTransferToggleSupported(ConfigAuthType.User),
    SpiceCopyPasteToggleSupported(ConfigAuthType.User),
    DefaultMTU,
    LiveMergeSupported(ConfigAuthType.User),
    SkipFencingIfSDActiveSupported,
    JsonProtocolSupported(ConfigAuthType.User),
    MaxThroughputUpperBoundQosValue,
    MaxReadThroughputUpperBoundQosValue,
    MaxWriteThroughputUpperBoundQosValue,
    MaxIopsUpperBoundQosValue,
    MaxReadIopsUpperBoundQosValue,
    MaxWriteIopsUpperBoundQosValue,
    MaxCpuLimitQosValue;

    public static enum ConfigAuthType {
        Admin,
        User
    }

    private ConfigAuthType authType;

    private ConfigurationValues(ConfigAuthType authType) {
        this.authType = authType;
    }

    private ConfigurationValues() {
        this(ConfigAuthType.Admin);
    }

    public ConfigAuthType getConfigAuthType() {
        return authType;
    }

    public boolean isAdmin() {
        return ConfigAuthType.Admin == authType;
    }

    public int getValue() {
        return ordinal();
    }

    public static ConfigurationValues forValue(int value) {
        return values()[value];
    }
}
