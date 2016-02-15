package org.ovirt.engine.ui.uicommonweb.models.clusters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.AdditionalFeature;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.MigrateOnErrorOptions;
import org.ovirt.engine.core.common.businessentities.SerialNumberPolicy;
import org.ovirt.engine.core.common.businessentities.ServerCpu;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.SupportedAdditionalClusterFeature;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.scheduling.ClusterPolicy;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.FilteredListModel;
import org.ovirt.engine.ui.uicommonweb.models.HasEntity;
import org.ovirt.engine.ui.uicommonweb.models.HasValidatedTabs;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.TabName;
import org.ovirt.engine.ui.uicommonweb.models.ValidationCompleteEvent;
import org.ovirt.engine.ui.uicommonweb.models.vms.SerialNumberPolicyModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicommonweb.validation.HostWithProtocolAndPortAddressValidation;
import org.ovirt.engine.ui.uicommonweb.validation.I18NNameValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class ClusterModel extends EntityModel<VDSGroup> implements HasValidatedTabs {
    private Map<Guid, PolicyUnit> policyUnitMap;
    private ListModel<ClusterPolicy> clusterPolicy;
    private Map<Guid, Network> defaultManagementNetworkCache = new HashMap<>();
    private Boolean detached;

    private ListModel<String> glusterTunedProfile;

    public ListModel<String> getGlusterTunedProfile() {
        return glusterTunedProfile;
    }

    public void setGlusterTunedProfile(ListModel<String> glusterTunedProfile) {
        this.glusterTunedProfile = glusterTunedProfile;
    }

    public ListModel<ClusterPolicy> getClusterPolicy() {
        return clusterPolicy;
    }

    public void setClusterPolicy(ListModel<ClusterPolicy> clusterPolicy) {
        this.clusterPolicy = clusterPolicy;
    }

    private KeyValueModel customPropertySheet;

    public KeyValueModel getCustomPropertySheet() {
        return customPropertySheet;
    }

    public void setCustomPropertySheet(KeyValueModel customPropertySheet) {
        this.customPropertySheet = customPropertySheet;
    }

    private int privateServerOverCommit;

    public int getServerOverCommit() {
        return privateServerOverCommit;
    }

    public void setServerOverCommit(int value) {
        privateServerOverCommit = value;
    }

    private int privateDesktopOverCommit;

    public int getDesktopOverCommit() {
        return privateDesktopOverCommit;
    }

    public void setDesktopOverCommit(int value) {
        privateDesktopOverCommit = value;
    }

    private int privateDefaultMemoryOvercommit;

    public int getDefaultMemoryOvercommit() {
        return privateDefaultMemoryOvercommit;
    }

    public void setDefaultMemoryOvercommit(int value) {
        privateDefaultMemoryOvercommit = value;
    }

    private boolean privateIsEdit;

    public boolean getIsEdit() {
        return privateIsEdit;
    }

    public void setIsEdit(boolean value) {
        privateIsEdit = value;
    }

    private boolean isCPUinitialized = false;

    private boolean privateIsNew;

    public boolean getIsNew() {
        return privateIsNew;
    }

    public void setIsNew(boolean value) {
        privateIsNew = value;
    }

    private String privateOriginalName;

    public String getOriginalName() {
        return privateOriginalName;
    }

    public void setOriginalName(String value) {
        privateOriginalName = value;
    }

    private Guid privateClusterId;

    public Guid getClusterId() {
        return privateClusterId;
    }

    public void setClusterId(Guid value) {
        privateClusterId = value;
    }

    private EntityModel<String> privateName;

    public EntityModel<String> getName() {
        return privateName;
    }

    public void setName(EntityModel<String> value) {
        privateName = value;
    }

    private EntityModel<String> privateDescription;

    public EntityModel<String> getDescription() {
        return privateDescription;
    }

    public void setDescription(EntityModel<String> value) {
        privateDescription = value;
    }

    private EntityModel<String> privateComment;

    public EntityModel<String> getComment() {
        return privateComment;
    }

    public void setComment(EntityModel<String> value) {
        privateComment = value;
    }

    private ListModel<StoragePool> privateDataCenter;

    public ListModel<StoragePool> getDataCenter() {
        return privateDataCenter;
    }

    public void setDataCenter(ListModel<StoragePool> value) {
        privateDataCenter = value;
    }

    private ListModel<Network> managementNetwork;

    public void setManagementNetwork(ListModel<Network> managementNetwork) {
        this.managementNetwork = managementNetwork;
    }

    public ListModel<Network> getManagementNetwork() {
        return managementNetwork;
    }

    private FilteredListModel<ServerCpu> privateCPU;

    public FilteredListModel<ServerCpu> getCPU() {
        return privateCPU;
    }

    public void setCPU(FilteredListModel<ServerCpu> value) {
        privateCPU = value;
    }

    private EntityModel<Boolean> rngRandomSourceRequired;

    public EntityModel<Boolean> getRngRandomSourceRequired() {
        return rngRandomSourceRequired;
    }

    public void setRngRandomSourceRequired(EntityModel<Boolean> rngRandomSourceRequired) {
        this.rngRandomSourceRequired = rngRandomSourceRequired;
    }

    private EntityModel<Boolean> rngHwrngSourceRequired;

    public EntityModel<Boolean> getRngHwrngSourceRequired() {
        return rngHwrngSourceRequired;
    }

    public void setRngHwrngSourceRequired(EntityModel<Boolean> rngHwrngSourceRequired) {
        this.rngHwrngSourceRequired = rngHwrngSourceRequired;
    }

    private ListModel<Version> privateVersion;

    public ListModel<Version> getVersion() {
        return privateVersion;
    }

    public void setVersion(ListModel<Version> value) {
        privateVersion = value;
    }

    private ListModel<ArchitectureType> privateArchitecture;

    public ListModel<ArchitectureType> getArchitecture() {
        return privateArchitecture;
    }

    public void setArchitecture(ListModel<ArchitectureType> value) {
        privateArchitecture = value;
    }

    private boolean allowClusterWithVirtGlusterEnabled;

    public boolean getAllowClusterWithVirtGlusterEnabled() {
        return allowClusterWithVirtGlusterEnabled;
    }

    public void setAllowClusterWithVirtGlusterEnabled(boolean value) {
        allowClusterWithVirtGlusterEnabled = value;
        if (allowClusterWithVirtGlusterEnabled != value) {
            allowClusterWithVirtGlusterEnabled = value;
            onPropertyChanged(new PropertyChangedEventArgs("AllowClusterWithVirtGlusterEnabled")); //$NON-NLS-1$
        }
    }

    private EntityModel<Boolean> privateEnableOvirtService;

    public EntityModel<Boolean> getEnableOvirtService() {
        return privateEnableOvirtService;
    }

    public void setEnableOvirtService(EntityModel<Boolean> value) {
        this.privateEnableOvirtService = value;
    }

    private EntityModel<Boolean> privateEnableGlusterService;

    public EntityModel<Boolean> getEnableGlusterService() {
        return privateEnableGlusterService;
    }

    public void setEnableGlusterService(EntityModel<Boolean> value) {
        this.privateEnableGlusterService = value;
    }

    private ListModel<List<AdditionalFeature>> additionalClusterFeatures;

    public ListModel<List<AdditionalFeature>> getAdditionalClusterFeatures() {
        return additionalClusterFeatures;
    }

    public void setAdditionalClusterFeatures(ListModel<List<AdditionalFeature>> additionalClusterFeatures) {
        this.additionalClusterFeatures = additionalClusterFeatures;
    }

    private EntityModel<Boolean> isImportGlusterConfiguration;

    public EntityModel<Boolean> getIsImportGlusterConfiguration() {
        return isImportGlusterConfiguration;
    }

    public void setIsImportGlusterConfiguration(EntityModel<Boolean> value) {
        this.isImportGlusterConfiguration = value;
    }

    private EntityModel<String> glusterHostAddress;

    public EntityModel<String> getGlusterHostAddress() {
        return glusterHostAddress;
    }

    public void setGlusterHostAddress(EntityModel<String> glusterHostAddress) {
        this.glusterHostAddress = glusterHostAddress;
    }

    private EntityModel<String> glusterHostFingerprint;

    public EntityModel<String> getGlusterHostFingerprint() {
        return glusterHostFingerprint;
    }

    public void setGlusterHostFingerprint(EntityModel<String> glusterHostFingerprint) {
        this.glusterHostFingerprint = glusterHostFingerprint;
    }

    private Boolean isFingerprintVerified;

    public Boolean isFingerprintVerified() {
        return isFingerprintVerified;
    }

    public void setIsFingerprintVerified(Boolean value) {
        this.isFingerprintVerified = value;
    }

    private EntityModel<String> glusterHostPassword;

    public EntityModel<String> getGlusterHostPassword() {
        return glusterHostPassword;
    }

    public void setGlusterHostPassword(EntityModel<String> glusterHostPassword) {
        this.glusterHostPassword = glusterHostPassword;
    }

    private EntityModel<Integer> privateOptimizationNone;

    public EntityModel<Integer> getOptimizationNone() {
        return privateOptimizationNone;
    }

    public void setOptimizationNone(EntityModel<Integer> value) {
        privateOptimizationNone = value;
    }

    private EntityModel<Integer> privateOptimizationForServer;

    public EntityModel<Integer> getOptimizationForServer() {
        return privateOptimizationForServer;
    }

    public void setOptimizationForServer(EntityModel<Integer> value) {
        privateOptimizationForServer = value;
    }

    private EntityModel<Integer> privateOptimizationForDesktop;

    public EntityModel<Integer> getOptimizationForDesktop() {
        return privateOptimizationForDesktop;
    }

    public void setOptimizationForDesktop(EntityModel<Integer> value) {
        privateOptimizationForDesktop = value;
    }

    private EntityModel<Integer> privateOptimizationCustom;

    public EntityModel<Integer> getOptimizationCustom() {
        return privateOptimizationCustom;
    }

    public void setOptimizationCustom(EntityModel<Integer> value) {
        privateOptimizationCustom = value;
    }

    private EntityModel<Boolean> privateOptimizationNone_IsSelected;

    public EntityModel<Boolean> getOptimizationNone_IsSelected() {
        return privateOptimizationNone_IsSelected;
    }

    public void setOptimizationNone_IsSelected(EntityModel<Boolean> value) {
        privateOptimizationNone_IsSelected = value;
    }

    private EntityModel<Boolean> privateOptimizationForServer_IsSelected;

    public EntityModel<Boolean> getOptimizationForServer_IsSelected() {
        return privateOptimizationForServer_IsSelected;
    }

    public void setOptimizationForServer_IsSelected(EntityModel<Boolean> value) {
        privateOptimizationForServer_IsSelected = value;
    }

    private EntityModel<Boolean> privateOptimizationForDesktop_IsSelected;

    public EntityModel<Boolean> getOptimizationForDesktop_IsSelected() {
        return privateOptimizationForDesktop_IsSelected;
    }

    public void setOptimizationForDesktop_IsSelected(EntityModel<Boolean> value) {
        privateOptimizationForDesktop_IsSelected = value;
    }

    private EntityModel<Boolean> privateOptimizationCustom_IsSelected;

    public EntityModel<Boolean> getOptimizationCustom_IsSelected() {
        return privateOptimizationCustom_IsSelected;
    }

    public void setOptimizationCustom_IsSelected(EntityModel<Boolean> value) {
        privateOptimizationCustom_IsSelected = value;
    }

    private EntityModel<Boolean> privateCountThreadsAsCores;

    public EntityModel<Boolean> getCountThreadsAsCores() {
        return privateCountThreadsAsCores;
    }

    public void setCountThreadsAsCores(EntityModel<Boolean> value) {
        privateCountThreadsAsCores = value;
    }

    private EntityModel<Boolean> privateVersionSupportsCpuThreads;

    public EntityModel<Boolean> getVersionSupportsCpuThreads() {
        return privateVersionSupportsCpuThreads;
    }

    public void setVersionSupportsCpuThreads(EntityModel<Boolean> value) {
        privateVersionSupportsCpuThreads = value;
    }

    /**
     * Mutually exclusive Resilience policy radio button
     * @see #privateMigrateOnErrorOption_YES
     * @see #privateMigrateOnErrorOption_HA_ONLY
     */
    private EntityModel<Boolean> privateMigrateOnErrorOption_NO;

    public EntityModel<Boolean> getMigrateOnErrorOption_NO() {
        return privateMigrateOnErrorOption_NO;
    }

    public void setMigrateOnErrorOption_NO(EntityModel<Boolean> value) {
        privateMigrateOnErrorOption_NO = value;
    }

    /**
     * Mutually exclusive Resilience policy radio button
     * @see #privateMigrateOnErrorOption_NO
     * @see #privateMigrateOnErrorOption_HA_ONLY
     */
    private EntityModel<Boolean> privateMigrateOnErrorOption_YES;

    public EntityModel<Boolean> getMigrateOnErrorOption_YES() {
        return privateMigrateOnErrorOption_YES;
    }

    public void setMigrateOnErrorOption_YES(EntityModel<Boolean> value) {
        privateMigrateOnErrorOption_YES = value;
    }

    /**
     * Mutually exclusive Resilience policy radio button
     * @see #privateMigrateOnErrorOption_YES
     * @see #privateMigrateOnErrorOption_NO
     */
    private EntityModel<Boolean> privateMigrateOnErrorOption_HA_ONLY;

    public EntityModel<Boolean> getMigrateOnErrorOption_HA_ONLY() {
        return privateMigrateOnErrorOption_HA_ONLY;
    }

    public void setMigrateOnErrorOption_HA_ONLY(EntityModel<Boolean> value) {
        privateMigrateOnErrorOption_HA_ONLY = value;
    }

    private EntityModel<Boolean> enableKsm;

    public EntityModel<Boolean> getEnableKsm() {
        return enableKsm;
    }

    public void setEnableKsm(EntityModel<Boolean> enableKsm) {
        this.enableKsm = enableKsm;
    }

    private ListModel<KsmPolicyForNuma> ksmPolicyForNumaSelection;

    public ListModel<KsmPolicyForNuma> getKsmPolicyForNumaSelection() {
        return ksmPolicyForNumaSelection;
    }

    private void setKsmPolicyForNumaSelection(ListModel<KsmPolicyForNuma> value) {
        ksmPolicyForNumaSelection = value;
    }

    private EntityModel<Boolean> enableBallooning;

    public EntityModel<Boolean> getEnableBallooning() {
        return enableBallooning;
    }

    public void setEnableBallooning(EntityModel<Boolean> enableBallooning) {
        this.enableBallooning = enableBallooning;
    }

    private EntityModel<Boolean> optimizeForUtilization;

    public EntityModel<Boolean> getOptimizeForUtilization() {
        return optimizeForUtilization;
    }

    public void setOptimizeForUtilization(EntityModel<Boolean> optimizeForUtilization) {
        this.optimizeForUtilization = optimizeForUtilization;
    }

    private EntityModel<Boolean> optimizeForSpeed;

    public EntityModel<Boolean> getOptimizeForSpeed() {
        return optimizeForSpeed;
    }

    public void setOptimizeForSpeed(EntityModel<Boolean> optimizeForSpeed) {
        this.optimizeForSpeed = optimizeForSpeed;
    }
    private EntityModel<Boolean> guarantyResources;

    public EntityModel<Boolean> getGuarantyResources() {
        return guarantyResources;
    }

    public void setGuarantyResources(EntityModel<Boolean> guarantyResources) {
        this.guarantyResources = guarantyResources;
    }

    private EntityModel<Boolean> allowOverbooking;

    public EntityModel<Boolean> getAllowOverbooking() {
        return allowOverbooking;
    }

    public void setAllowOverbooking(EntityModel<Boolean> allowOverbooking) {
        this.allowOverbooking = allowOverbooking;
    }

    private SerialNumberPolicyModel serialNumberPolicy;

    public SerialNumberPolicyModel getSerialNumberPolicy() {
        return serialNumberPolicy;
    }

    public void setSerialNumberPolicy(SerialNumberPolicyModel serialNumberPolicy) {
        this.serialNumberPolicy = serialNumberPolicy;
    }

    private EntityModel<String> spiceProxy;

    public EntityModel<String> getSpiceProxy() {
        return spiceProxy;
    }

    public void setSpiceProxy(EntityModel<String> spiceProxy) {
        this.spiceProxy = spiceProxy;
    }

    private EntityModel<Boolean> spiceProxyEnabled;

    public EntityModel<Boolean> getSpiceProxyEnabled() {
        return spiceProxyEnabled;
    }

    public void setSpiceProxyEnabled(EntityModel<Boolean> spiceProxyEnabled) {
        this.spiceProxyEnabled = spiceProxyEnabled;
    }

    private MigrateOnErrorOptions migrateOnErrorOption = MigrateOnErrorOptions.values()[0];

    public MigrateOnErrorOptions getMigrateOnErrorOption() {
        if (getMigrateOnErrorOption_NO().getEntity() == true) {
            return MigrateOnErrorOptions.NO;
        }
        else if (getMigrateOnErrorOption_YES().getEntity() == true) {
            return MigrateOnErrorOptions.YES;
        }
        else if (getMigrateOnErrorOption_HA_ONLY().getEntity() == true) {
            return MigrateOnErrorOptions.HA_ONLY;
        }
        return MigrateOnErrorOptions.YES;
    }

    public void setMigrateOnErrorOption(MigrateOnErrorOptions value) {
        if (migrateOnErrorOption != value) {
            migrateOnErrorOption = value;

            // webadmin use.
            switch (migrateOnErrorOption) {
            case NO:
                getMigrateOnErrorOption_NO().setEntity(true);
                getMigrateOnErrorOption_YES().setEntity(false);
                getMigrateOnErrorOption_HA_ONLY().setEntity(false);
                break;
            case YES:
                getMigrateOnErrorOption_NO().setEntity(false);
                getMigrateOnErrorOption_YES().setEntity(true);
                getMigrateOnErrorOption_HA_ONLY().setEntity(false);
                break;
            case HA_ONLY:
                getMigrateOnErrorOption_NO().setEntity(false);
                getMigrateOnErrorOption_YES().setEntity(false);
                getMigrateOnErrorOption_HA_ONLY().setEntity(true);
                break;
            default:
                break;
            }
            onPropertyChanged(new PropertyChangedEventArgs("MigrateOnErrorOption")); //$NON-NLS-1$
        }
    }

    private boolean privateisResiliencePolicyTabAvailable;

    public boolean getisResiliencePolicyTabAvailable() {
        return privateisResiliencePolicyTabAvailable;
    }

    public void setisResiliencePolicyTabAvailable(boolean value) {
        privateisResiliencePolicyTabAvailable = value;
    }

    public boolean getIsResiliencePolicyTabAvailable() {
        return getisResiliencePolicyTabAvailable();
    }

    public void setIsResiliencePolicyTabAvailable(boolean value) {
        if (getisResiliencePolicyTabAvailable() != value) {
            setisResiliencePolicyTabAvailable(value);
            onPropertyChanged(new PropertyChangedEventArgs("IsResiliencePolicyTabAvailable")); //$NON-NLS-1$
        }
    }

    private EntityModel<Boolean> enableOptionalReason;

    public EntityModel<Boolean> getEnableOptionalReason() {
        return enableOptionalReason;
    }

    public void setEnableOptionalReason(EntityModel<Boolean> value) {
        this.enableOptionalReason = value;
    }

    private EntityModel<Boolean> enableHostMaintenanceReason;

    public EntityModel<Boolean> getEnableHostMaintenanceReason() {
        return enableHostMaintenanceReason;
    }

    public void setEnableHostMaintenanceReason(EntityModel<Boolean> value) {
        this.enableHostMaintenanceReason = value;
    }

    private EntityModel<Boolean> privateEnableTrustedService;

    private EntityModel<Boolean> privateEnableHaReservation;

    public EntityModel<Boolean> getEnableHaReservation() {
        return privateEnableHaReservation;
    }

    public void setEnableHaReservation(EntityModel<Boolean> value) {
        this.privateEnableHaReservation = value;
    }

    public EntityModel<Boolean> getEnableTrustedService() {
        return privateEnableTrustedService;
    }

    public void setEnableTrustedService(EntityModel<Boolean> value) {
        this.privateEnableTrustedService = value;
    }

    public int getMemoryOverCommit() {
        if (getOptimizationNone_IsSelected().getEntity()) {
            return getOptimizationNone().getEntity();
        }

        if (getOptimizationForServer_IsSelected().getEntity()) {
            return getOptimizationForServer().getEntity();
        }

        if (getOptimizationForDesktop_IsSelected().getEntity()) {
            return getOptimizationForDesktop().getEntity();
        }

        if (getOptimizationCustom_IsSelected().getEntity()) {
            return getOptimizationCustom().getEntity();
        }

        return AsyncDataProvider.getInstance().getClusterDefaultMemoryOverCommit();
    }

    public String getSchedulerOptimizationInfoMessage() {
        return ConstantsManager.getInstance()
                .getMessages()
                .schedulerOptimizationInfo(AsyncDataProvider.getInstance().getOptimizeSchedulerForSpeedPendingRequests());
    }

    public String getAllowOverbookingInfoMessage() {
        return ConstantsManager.getInstance()
                .getMessages()
                .schedulerAllowOverbookingInfo(AsyncDataProvider.getInstance().getSchedulerAllowOverbookingPendingRequestsThreshold());
    }

    public void setMemoryOverCommit(int value) {
        getOptimizationNone_IsSelected().setEntity(value == getOptimizationNone().getEntity());
        getOptimizationForServer_IsSelected().setEntity(value == getOptimizationForServer().getEntity());
        getOptimizationForDesktop_IsSelected().setEntity(value == getOptimizationForDesktop().getEntity());

        if (!getOptimizationNone_IsSelected().getEntity()
                && !getOptimizationForServer_IsSelected().getEntity()
                && !getOptimizationForDesktop_IsSelected().getEntity()) {
            getOptimizationCustom().setIsAvailable(true);
            getOptimizationCustom().setEntity(value);
            getOptimizationCustom_IsSelected().setIsAvailable(true);
            getOptimizationCustom_IsSelected().setEntity(true);
        }
    }

    private EntityModel<Boolean> fencingEnabledModel;

    public EntityModel<Boolean> getFencingEnabledModel() {
        return fencingEnabledModel;
    }

    public void setFencingEnabledModel(EntityModel<Boolean> fencingEnabledModel) {
        this.fencingEnabledModel = fencingEnabledModel;
    }

    private EntityModel<Boolean> skipFencingIfSDActiveEnabled;

    public EntityModel<Boolean> getSkipFencingIfSDActiveEnabled() {
        return skipFencingIfSDActiveEnabled;
    }

    public void setSkipFencingIfSDActiveEnabled(EntityModel<Boolean> skipFencingIfSDActiveEnabled) {
        this.skipFencingIfSDActiveEnabled = skipFencingIfSDActiveEnabled;
    }

    private EntityModel<Boolean> skipFencingIfConnectivityBrokenEnabled;

    public EntityModel<Boolean> getSkipFencingIfConnectivityBrokenEnabled() {
        return skipFencingIfConnectivityBrokenEnabled;
    }

    public void setSkipFencingIfConnectivityBrokenEnabled(EntityModel<Boolean> skipFencingIfConnectivityBrokenEnabled) {
        this.skipFencingIfConnectivityBrokenEnabled = skipFencingIfConnectivityBrokenEnabled;
    }

    private ListModel<Integer> hostsWithBrokenConnectivityThreshold;

    public ListModel<Integer> getHostsWithBrokenConnectivityThreshold() {
        return hostsWithBrokenConnectivityThreshold;
    }

    public void setHostsWithBrokenConnectivityThreshold(ListModel<Integer> value) {
        hostsWithBrokenConnectivityThreshold = value;
    }

    private ListModel<Boolean> autoConverge;

    public ListModel<Boolean> getAutoConverge() {
        return autoConverge;
    }

    public void setAutoConverge(ListModel<Boolean> autoConverge) {
        this.autoConverge = autoConverge;
    }

    private ListModel<Boolean> migrateCompressed;

    public ListModel<Boolean> getMigrateCompressed() {
        return migrateCompressed;
    }

    public void setMigrateCompressed(ListModel<Boolean> migrateCompressed) {
        this.migrateCompressed = migrateCompressed;
    }

    public ClusterModel() {
        super();
        ListModel<KsmPolicyForNuma> ksmPolicyForNumaSelection = new ListModel<KsmPolicyForNuma>();
        ksmPolicyForNumaSelection.setItems(Arrays.asList(KsmPolicyForNuma.values()));
        setKsmPolicyForNumaSelection(ksmPolicyForNumaSelection);
    }

    public void initTunedProfiles() {
        this.startProgress(null);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterTunedProfiles, new VdcQueryParametersBase(), new AsyncQuery(new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                ClusterModel.this.stopProgress();
                List<String> glusterTunedProfiles = new ArrayList<>();
                if (((VdcQueryReturnValue) returnValue).getSucceeded()) {
                    glusterTunedProfiles.addAll((List<String>)(((VdcQueryReturnValue) returnValue).getReturnValue()));
                }
                glusterTunedProfile.setItems(glusterTunedProfiles, glusterTunedProfile.getSelectedItem());
                glusterTunedProfile.setIsAvailable(glusterTunedProfile.getItems().size() > 0);
            }
        }));
    }

    public void init(final boolean isEdit) {
        setIsEdit(isEdit);
        setName(new EntityModel<String>());
        setDescription(new EntityModel<String>());
        setComment(new EntityModel<String>());
        setEnableTrustedService(new EntityModel<Boolean>(false));
        setEnableHaReservation(new EntityModel<Boolean>(false));
        setEnableOptionalReason(new EntityModel<Boolean>(false));
        getEnableOptionalReason().setIsAvailable(ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly));
        setEnableHostMaintenanceReason(new EntityModel<Boolean>(false));
        setAllowClusterWithVirtGlusterEnabled(true);
        setGlusterTunedProfile(new ListModel<String>());
        AsyncDataProvider.getInstance().getAllowClusterWithVirtGlusterEnabled(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                setAllowClusterWithVirtGlusterEnabled((Boolean) returnValue);
            }
        }));

        setEnableOvirtService(new EntityModel<Boolean>());
        setEnableGlusterService(new EntityModel<Boolean>());
        setAdditionalClusterFeatures(new ListModel<List<AdditionalFeature>>());
        List<List<AdditionalFeature>> additionalFeatures = new ArrayList<List<AdditionalFeature>>();
        additionalFeatures.add(Collections.<AdditionalFeature> emptyList());
        getAdditionalClusterFeatures().setItems(additionalFeatures, null);
        setSpiceProxyEnabled(new EntityModel<Boolean>());
        getSpiceProxyEnabled().setEntity(false);
        getSpiceProxyEnabled().getEntityChangedEvent().addListener(this);

        setSpiceProxy(new EntityModel<String>());
        getSpiceProxy().setIsChangeable(false);

        setFencingEnabledModel(new EntityModel<Boolean>());
        getFencingEnabledModel().setEntity(true);
        getFencingEnabledModel().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                 updateFencingPolicyContent(getVersion() == null ? null : getVersion().getSelectedItem());
            }
        });

        setSkipFencingIfSDActiveEnabled(new EntityModel<Boolean>());
        getSkipFencingIfSDActiveEnabled().setEntity(true);

        setSkipFencingIfConnectivityBrokenEnabled(new EntityModel<Boolean>());
        getSkipFencingIfConnectivityBrokenEnabled().setEntity(true);

        setEnableOvirtService(new EntityModel<Boolean>());
        setEnableGlusterService(new EntityModel<Boolean>());

        setSerialNumberPolicy(new SerialNumberPolicyModel());

        setAutoConverge(new ListModel<Boolean>());
        getAutoConverge().setItems(Arrays.asList(null, true, false));
        setMigrateCompressed(new ListModel<Boolean>());
        getMigrateCompressed().setItems(Arrays.asList(null, true, false));

        getEnableOvirtService().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                refreshAdditionalClusterFeaturesList();
                if (!getAllowClusterWithVirtGlusterEnabled() && getEnableOvirtService().getEntity()) {
                    getEnableGlusterService().setEntity(Boolean.FALSE);
                }
                getEnableGlusterService().setIsChangeable(true);
                getEnableTrustedService().setEntity(false);
                if (getEnableOvirtService().getEntity() != null
                        && getEnableOvirtService().getEntity()) {
                    if (getEnableGlusterService().getEntity() != null
                            && !getEnableGlusterService().getEntity()) {
                        getEnableTrustedService().setIsChangeable(true);
                    }
                    else {
                        getEnableTrustedService().setIsChangeable(false);
                    }

                }
                else {
                    getEnableTrustedService().setIsChangeable(false);
                }
            }
        });
        getEnableOvirtService().setEntity(ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly));
        getEnableOvirtService().setIsAvailable(ApplicationModeHelper.getUiMode() != ApplicationMode.VirtOnly
                && ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly));

        setRngRandomSourceRequired(new EntityModel<Boolean>());
        getRngRandomSourceRequired().setIsAvailable(ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly));
        setRngHwrngSourceRequired(new EntityModel<Boolean>());
        getRngHwrngSourceRequired().setIsAvailable(ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly));

        initImportCluster(isEdit);

        getEnableGlusterService().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {

            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                refreshAdditionalClusterFeaturesList();
                if (!getAllowClusterWithVirtGlusterEnabled() && getEnableGlusterService().getEntity()) {
                    getEnableOvirtService().setEntity(Boolean.FALSE);
                }

                if (!isEdit
                        && getEnableGlusterService().getEntity() != null
                        && getEnableGlusterService().getEntity()) {
                    getIsImportGlusterConfiguration().setIsAvailable(true);
                    getGlusterHostAddress().setIsAvailable(true);
                    getGlusterHostFingerprint().setIsAvailable(true);
                    getGlusterHostPassword().setIsAvailable(true);
                }
                else {
                    getIsImportGlusterConfiguration().setIsAvailable(false);
                    getIsImportGlusterConfiguration().setEntity(false);

                    getGlusterHostAddress().setIsAvailable(false);
                    getGlusterHostFingerprint().setIsAvailable(false);
                    getGlusterHostPassword().setIsAvailable(false);
                }
                if (getEnableGlusterService().getEntity() != null
                        && getEnableGlusterService().getEntity()) {
                    getEnableTrustedService().setEntity(false);
                    getEnableTrustedService().setIsChangeable(false);
                }
                else {
                    if (getEnableOvirtService().getEntity() != null
                            && getEnableOvirtService().getEntity()) {
                        getEnableTrustedService().setIsChangeable(true);
                    }
                    else {
                        getEnableTrustedService().setIsChangeable(false);
                    }
                }

                getGlusterTunedProfile().setIsAvailable(getEnableGlusterService().getEntity());
                if (getEnableGlusterService().getEntity()) {
                    initTunedProfiles();
                }
            }
       });

        getEnableTrustedService().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                if (getEnableTrustedService().getEntity() != null
                        && getEnableTrustedService().getEntity()) {
                    getEnableGlusterService().setEntity(false);
                    getEnableGlusterService().setIsChangeable(false);
                }
                else {
                    getEnableGlusterService().setIsChangeable(true);
                }
            }
        });

        getEnableGlusterService().setEntity(ApplicationModeHelper.getUiMode() == ApplicationMode.GlusterOnly);
        getEnableGlusterService().setIsAvailable(ApplicationModeHelper.getUiMode() != ApplicationMode.GlusterOnly
                && ApplicationModeHelper.isModeSupported(ApplicationMode.GlusterOnly));

        getGlusterTunedProfile().setIsAvailable(getEnableGlusterService().getEntity());

        setOptimizationNone(new EntityModel<Integer>());
        setOptimizationForServer(new EntityModel<Integer>());
        setOptimizationForDesktop(new EntityModel<Integer>());
        setOptimizationCustom(new EntityModel<Integer>());

        EntityModel<Boolean> tempVar = new EntityModel<Boolean>();
        tempVar.setEntity(false);
        setOptimizationNone_IsSelected(tempVar);
        getOptimizationNone_IsSelected().getEntityChangedEvent().addListener(this);
        EntityModel<Boolean> tempVar2 = new EntityModel<Boolean>();
        tempVar2.setEntity(false);
        setOptimizationForServer_IsSelected(tempVar2);
        getOptimizationForServer_IsSelected().getEntityChangedEvent().addListener(this);
        EntityModel<Boolean> tempVar3 = new EntityModel<Boolean>();
        tempVar3.setEntity(false);
        setOptimizationForDesktop_IsSelected(tempVar3);
        getOptimizationForDesktop_IsSelected().getEntityChangedEvent().addListener(this);
        EntityModel<Boolean> tempVar4 = new EntityModel<Boolean>();
        tempVar4.setEntity(false);
        tempVar4.setIsAvailable(false);
        setOptimizationCustom_IsSelected(tempVar4);
        getOptimizationCustom_IsSelected().getEntityChangedEvent().addListener(this);

        EntityModel<Boolean> tempVar5 = new EntityModel<Boolean>();
        tempVar5.setEntity(false);
        setMigrateOnErrorOption_YES(tempVar5);
        getMigrateOnErrorOption_YES().getEntityChangedEvent().addListener(this);
        EntityModel<Boolean> tempVar6 = new EntityModel<Boolean>();
        tempVar6.setEntity(false);
        setMigrateOnErrorOption_NO(tempVar6);
        getMigrateOnErrorOption_NO().getEntityChangedEvent().addListener(this);
        EntityModel<Boolean> tempVar7 = new EntityModel<Boolean>();
        tempVar7.setEntity(false);
        setMigrateOnErrorOption_HA_ONLY(tempVar7);
        getMigrateOnErrorOption_HA_ONLY().getEntityChangedEvent().addListener(this);
        // KSM feature
        setEnableKsm(new EntityModel<Boolean>());
        getEnableKsm().setEntity(false);
        getKsmPolicyForNumaSelection().setIsChangeable(false);
        getEnableKsm().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                if (getEnableKsm().getEntity() == null){
                    return;
                }
                if (getEnableKsm().getEntity() == true){
                    getKsmPolicyForNumaSelection().setIsChangeable(true);
                }
                if (getEnableKsm().getEntity() == false){
                    getKsmPolicyForNumaSelection().setIsChangeable(false);
                }
            }
        });

        setEnableBallooning(new EntityModel<Boolean>());
        getEnableBallooning().setEntity(false);
        // Optimization methods:
        // default value =100;
        setDefaultMemoryOvercommit(AsyncDataProvider.getInstance().getClusterDefaultMemoryOverCommit());

        setCountThreadsAsCores(new EntityModel<Boolean>(AsyncDataProvider.getInstance().getClusterDefaultCountThreadsAsCores()));

        setVersionSupportsCpuThreads(new EntityModel<Boolean>(true));

        setOptimizeForUtilization(new EntityModel<Boolean>());
        setOptimizeForSpeed(new EntityModel<Boolean>());
        getOptimizeForUtilization().setEntity(true);
        getOptimizeForSpeed().setEntity(false);
        getOptimizeForUtilization().getEntityChangedEvent().addListener(this);
        getOptimizeForSpeed().getEntityChangedEvent().addListener(this);

        setGuarantyResources(new EntityModel<Boolean>());
        setAllowOverbooking(new EntityModel<Boolean>());
        getGuarantyResources().setEntity(true);
        getAllowOverbooking().setEntity(false);
        getAllowOverbooking().getEntityChangedEvent().addListener(this);
        getGuarantyResources().getEntityChangedEvent().addListener(this);

        boolean overbookingSupported = AsyncDataProvider.getInstance().getScheudulingAllowOverbookingSupported();
        getAllowOverbooking().setIsAvailable(overbookingSupported);
        if (overbookingSupported) {
            getOptimizeForSpeed().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
                @Override
                public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                    Boolean entity = getOptimizeForSpeed().getEntity();
                    if (entity) {
                        getGuarantyResources().setEntity(true);
                    }
                    getAllowOverbooking().setIsChangeable(!entity);
                }
            });
            getAllowOverbooking().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
                @Override
                public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                    Boolean entity = getAllowOverbooking().getEntity();
                    if (entity) {
                        getOptimizeForUtilization().setEntity(true);
                    }
                    getOptimizeForSpeed().setIsChangeable(!entity);
                }
            });
        }

        setHostsWithBrokenConnectivityThreshold(new ListModel<Integer>());
        getHostsWithBrokenConnectivityThreshold().setIsAvailable(true);
        getHostsWithBrokenConnectivityThreshold().getSelectedItemChangedEvent().addListener(this);
        initHostsWithBrokenConnectivityThreshold();

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                ClusterModel clusterModel = (ClusterModel) model;
                clusterModel.setDesktopOverCommit((Integer) result);
                AsyncQuery _asyncQuery1 = new AsyncQuery();
                _asyncQuery1.setModel(clusterModel);
                _asyncQuery1.asyncCallback = new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object model1, Object result1) {
                        ClusterModel clusterModel1 = (ClusterModel) model1;
                        clusterModel1.setServerOverCommit((Integer) result1);

                        // temp is used for conversion purposes
                        EntityModel temp;

                        temp = clusterModel1.getOptimizationNone();
                        temp.setEntity(clusterModel1.getDefaultMemoryOvercommit());
                        // res1, res2 is used for conversion purposes.
                        boolean res1 = clusterModel1.getDesktopOverCommit() != clusterModel1.getDefaultMemoryOvercommit();
                        boolean res2 = clusterModel1.getServerOverCommit() != clusterModel1.getDefaultMemoryOvercommit();
                        temp = clusterModel1.getOptimizationNone_IsSelected();
                        setIsSelected(res1 && res2);
                        temp.setEntity(getIsSelected());

                        temp = clusterModel1.getOptimizationForServer();
                        temp.setEntity(clusterModel1.getServerOverCommit());
                        temp = clusterModel1.getOptimizationForServer_IsSelected();
                        temp.setEntity(clusterModel1.getServerOverCommit() == clusterModel1.getDefaultMemoryOvercommit());

                        temp = clusterModel1.getOptimizationForDesktop();
                        temp.setEntity(clusterModel1.getDesktopOverCommit());
                        temp = clusterModel1.getOptimizationForDesktop_IsSelected();
                        temp.setEntity(clusterModel1.getDesktopOverCommit() == clusterModel1.getDefaultMemoryOvercommit());

                        temp = clusterModel1.getOptimizationCustom();
                        temp.setIsAvailable(false);
                        temp.setIsChangeable(false);

                        if (clusterModel1.getIsEdit()) {
                            clusterModel1.postInit();
                        }

                    }
                };
                AsyncDataProvider.getInstance().getClusterServerMemoryOverCommit(_asyncQuery1);
            }
        };
        AsyncDataProvider.getInstance().getClusterDesktopMemoryOverCommit(_asyncQuery);

        setDataCenter(new ListModel<StoragePool>());
        getDataCenter().getSelectedItemChangedEvent().addListener(this);
        getDataCenter().setIsAvailable(ApplicationModeHelper.getUiMode() != ApplicationMode.GlusterOnly);

        setArchitecture(new ListModel<ArchitectureType>());
        getArchitecture().setIsAvailable(ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly));

        setManagementNetwork(new ListModel<Network>());
        if (isEdit && !isClusterDetached()) {
            getManagementNetwork().setChangeProhibitionReason(ConstantsManager.getInstance()
                    .getConstants()
                    .prohibitManagementNetworkChangeInEditClusterInfoMessage());
            getManagementNetwork().setIsChangeable(false);
        }

        setCPU(new FilteredListModel<ServerCpu>());
        getCPU().setIsAvailable(ApplicationModeHelper.getUiMode() != ApplicationMode.GlusterOnly);
        getCPU().getSelectedItemChangedEvent().addListener(this);

        setVersion(new ListModel<Version>());
        getVersion().getSelectedItemChangedEvent().addListener(this);
        setMigrateOnErrorOption(MigrateOnErrorOptions.YES);

        getRngRandomSourceRequired().setEntity(false);
        getRngHwrngSourceRequired().setEntity(false);

        setValidTab(TabName.GENERAL_TAB, true);
        setIsResiliencePolicyTabAvailable(true);

        setClusterPolicy(new ListModel<ClusterPolicy>());
        setCustomPropertySheet(new KeyValueModel());
        getClusterPolicy().getSelectedItemChangedEvent().addListener(this);
        Frontend.getInstance().runQuery(VdcQueryType.GetAllPolicyUnits, new VdcQueryParametersBase(), new AsyncQuery(this,
                new INewAsyncCallback() {

                    @Override
                    public void onSuccess(Object model, Object returnValue) {
                        ArrayList<PolicyUnit> policyUnits =
                                ((VdcQueryReturnValue) returnValue).getReturnValue();
                        policyUnitMap = new LinkedHashMap<Guid, PolicyUnit>();
                        for (PolicyUnit policyUnit : policyUnits) {
                            policyUnitMap.put(policyUnit.getId(), policyUnit);
                        }
                        Frontend.getInstance().runQuery(VdcQueryType.GetClusterPolicies,
                                new VdcQueryParametersBase(),
                                new AsyncQuery(model,
                                        new INewAsyncCallback() {

                                            @Override
                                            public void onSuccess(Object model, Object returnValue) {
                                                ClusterModel clusterModel = (ClusterModel) model;
                                                ArrayList<ClusterPolicy> list =
                                                       ((VdcQueryReturnValue) returnValue).getReturnValue();
                                                clusterModel.getClusterPolicy().setItems(list);
                                                ClusterPolicy defaultClusterPolicy = null;
                                                ClusterPolicy selectedClusterPolicy = null;
                                                for (ClusterPolicy clusterPolicy : list) {
                                                    if (clusterModel.getIsEdit() && getEntity() != null
                                                            && clusterPolicy.getId()
                                                            .equals(getEntity().getClusterPolicyId())) {
                                                        selectedClusterPolicy = clusterPolicy;
                                                    }
                                                    if (clusterPolicy.isDefaultPolicy()) {
                                                        defaultClusterPolicy = clusterPolicy;
                                                    }
                                                }
                                                if (selectedClusterPolicy != null) {
                                                    clusterModel.getClusterPolicy()
                                                            .setSelectedItem(selectedClusterPolicy);
                                                } else {
                                                    clusterModel.getClusterPolicy()
                                                            .setSelectedItem(defaultClusterPolicy);
                                                }
                                                clusterPolicyChanged();
                                            }
                                        }));
                    }
                }));
    }

    boolean isClusterDetached() {
        if (detached == null) {
            detached = getEntity().getStoragePoolId() == null;
        }
        return detached;
    }

    private void initSpiceProxy() {
        String proxy = getEntity().getSpiceProxy();
        boolean isProxyAvailable = !StringHelper.isNullOrEmpty(proxy);
        getSpiceProxyEnabled().setEntity(isProxyAvailable);
        getSpiceProxy().setIsChangeable(isProxyAvailable);
        getSpiceProxy().setEntity(proxy);
    }

    private void initImportCluster(boolean isEdit) {
        setGlusterHostAddress(new EntityModel<String>());
        getGlusterHostAddress().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                setIsFingerprintVerified(false);
                if (getGlusterHostAddress().getEntity() == null
                        || (getGlusterHostAddress().getEntity()).trim().length() == 0) {
                    getGlusterHostFingerprint().setEntity(""); //$NON-NLS-1$
                    return;
                }
                fetchFingerprint(getGlusterHostAddress().getEntity());
            }
        });

        setGlusterHostFingerprint(new EntityModel<String>());
        getGlusterHostFingerprint().setEntity(""); //$NON-NLS-1$
        setIsFingerprintVerified(false);
        setGlusterHostPassword(new EntityModel<String>());

        setIsImportGlusterConfiguration(new EntityModel<Boolean>());
        getIsImportGlusterConfiguration().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {

            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                if (getIsImportGlusterConfiguration().getEntity() != null
                        && getIsImportGlusterConfiguration().getEntity()) {
                    getGlusterHostAddress().setIsChangeable(true);
                    getGlusterHostPassword().setIsChangeable(true);
                }
                else {
                    getGlusterHostAddress().setIsChangeable(false);
                    getGlusterHostPassword().setIsChangeable(false);
                }
            }
        });

        getIsImportGlusterConfiguration().setIsAvailable(false);
        getGlusterHostAddress().setIsAvailable(false);
        getGlusterHostFingerprint().setIsAvailable(false);
        getGlusterHostPassword().setIsAvailable(false);

        getIsImportGlusterConfiguration().setEntity(false);
    }

    private void fetchFingerprint(String hostAddress) {
        AsyncQuery aQuery = new AsyncQuery();
        aQuery.setModel(this);
        aQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                String fingerprint = (String) result;
                if (fingerprint != null && fingerprint.length() > 0) {
                    getGlusterHostFingerprint().setEntity((String) result);
                    setIsFingerprintVerified(true);
                }
                else {
                    getGlusterHostFingerprint().setEntity(ConstantsManager.getInstance()
                            .getConstants()
                            .errorLoadingFingerprint());
                    setIsFingerprintVerified(false);
                }
            }
        };
        AsyncDataProvider.getInstance().getHostFingerprint(aQuery, hostAddress);
        getGlusterHostFingerprint().setEntity(ConstantsManager.getInstance().getConstants().loadingFingerprint());
    }

    private void postInit() {
        getDescription().setEntity(getEntity().getDescription());
        getComment().setEntity(getEntity().getComment());

        initSpiceProxy();
        getFencingEnabledModel().setEntity(getEntity().getFencingPolicy().isFencingEnabled());
        getSkipFencingIfSDActiveEnabled().setEntity(getEntity().getFencingPolicy().isSkipFencingIfSDActive());
        getSkipFencingIfConnectivityBrokenEnabled().setEntity(getEntity().getFencingPolicy()
                .isSkipFencingIfConnectivityBroken());
        getHostsWithBrokenConnectivityThreshold().setSelectedItem(getEntity().getFencingPolicy()
                .getHostsWithBrokenConnectivityThreshold());

        setMemoryOverCommit(getEntity().getMaxVdsMemoryOverCommit());

        getCountThreadsAsCores().setEntity(getEntity().getCountThreadsAsCores());
        getEnableBallooning().setEntity(getEntity().isEnableBallooning());
        getEnableKsm().setEntity(getEntity().isEnableKsm());

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                ClusterModel clusterModel = (ClusterModel) model;
                List<StoragePool> dataCenters = (List<StoragePool>) result;

                clusterModel.getDataCenter().setItems(dataCenters);

                clusterModel.getDataCenter().setSelectedItem(null);
                final Guid dataCenterId = clusterModel.getEntity().getStoragePoolId();
                for (StoragePool dataCenter : dataCenters) {
                    if (dataCenterId != null && dataCenter.getId().equals(dataCenterId)) {
                        clusterModel.getDataCenter().setSelectedItem(dataCenter);
                        break;
                    }
                }
                final StoragePool selectedDataCenter = clusterModel.getDataCenter().getSelectedItem();
                clusterModel.getDataCenter().setIsChangeable(selectedDataCenter == null);

                clusterModel.setMigrateOnErrorOption(clusterModel.getEntity().getMigrateOnError());

                if (!clusterModel.getManagementNetwork().getIsChangable()) {
                    loadCurrentClusterManagementNetwork();
                }
            }
        };
        AsyncDataProvider.getInstance().getDataCenterList(_asyncQuery);
        // inactive KsmPolicyForNuma if KSM disabled
        if (getEnableKsm().getEntity() == false)
            getKsmPolicyForNumaSelection().setIsChangeable(false);
        // hide KsmPolicyForNuma is cluseter version bellow 3.4
        Version version = getEntity().getCompatibilityVersion();
        if (version.compareTo(Version.v3_4) < 0)
            getKsmPolicyForNumaSelection().setIsAvailable(false);
    }

    private void loadCurrentClusterManagementNetwork() {
        final AsyncQuery getManagementNetworkQuery = new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                final ClusterModel clusterModel = (ClusterModel) model;
                final Network managementNetwork = (Network) returnValue;
                clusterModel.getManagementNetwork().setSelectedItem(managementNetwork);
            }
        });
        AsyncDataProvider.getInstance().getManagementNetwork(getManagementNetworkQuery, getEntity().getId());
    }

    private void loadDcNetworks(final Guid dataCenterId) {
        if (dataCenterId == null) {
            return;
        }
        final AsyncQuery getAllDataCenterNetworksQuery = new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                final ClusterModel clusterModel = (ClusterModel) model;
                if (clusterModel.getDataCenter().getSelectedItem() == null) {
                    return;
                }
                final List<Network> dcNetworks = (List<Network>) returnValue;
                clusterModel.getManagementNetwork().setItems(dcNetworks);

                if (defaultManagementNetworkCache.containsKey(dataCenterId)) {
                    final Network defaultManagementNetwork = defaultManagementNetworkCache.get(dataCenterId);
                    setSelectedDefaultManagementNetwork(clusterModel, defaultManagementNetwork);
                } else {
                    final AsyncQuery getDefaultManagementNetworkQuery =
                            new AsyncQuery(clusterModel, new INewAsyncCallback() {
                                @Override
                                public void onSuccess(Object model, Object returnValue) {
                                    final Network defaultManagementNetwork = (Network) returnValue;
                                    defaultManagementNetworkCache.put(dataCenterId, defaultManagementNetwork);
                                    setSelectedDefaultManagementNetwork(clusterModel, defaultManagementNetwork);
                                }
                            });
                    AsyncDataProvider.getInstance()
                            .getDefaultManagementNetwork(getDefaultManagementNetworkQuery, dataCenterId);
                }
            }

            private void setSelectedDefaultManagementNetwork(ClusterModel clusterModel,
                    Network defaultManagementNetwork) {
                if (defaultManagementNetwork != null) {
                    clusterModel.getManagementNetwork().setSelectedItem(defaultManagementNetwork);
                }
            }
        });
        AsyncDataProvider.getInstance().getManagementNetworkCandidates(getAllDataCenterNetworksQuery, dataCenterId);
    }

    @Override
    public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(ListModel.selectedItemChangedEventDefinition)) {
            if (sender == getDataCenter()) {
                storagePool_SelectedItemChanged(args);
            }
            else if (sender == getVersion()) {
                version_SelectedItemChanged(args);
            }
            else if (sender == getClusterPolicy()) {
                clusterPolicyChanged();
            }
            else if (sender == getCPU()) {
                CPU_SelectedItemChanged(args);
            }
            else if (sender == getArchitecture()) {
                architectureSelectedItemChanged(args);
            }
        }
        else if (ev.matchesDefinition(HasEntity.entityChangedEventDefinition)) {
            EntityModel senderEntityModel = (EntityModel) sender;

            if (senderEntityModel == getSpiceProxyEnabled()) {
                getSpiceProxy().setIsChangeable(getSpiceProxyEnabled().getEntity());
            } else if ((Boolean) senderEntityModel.getEntity()) {
                if (senderEntityModel == getOptimizationNone_IsSelected()) {
                    getOptimizationForServer_IsSelected().setEntity(false);
                    getOptimizationForDesktop_IsSelected().setEntity(false);
                    getOptimizationCustom_IsSelected().setEntity(false);
                }
                else if (senderEntityModel == getOptimizationForServer_IsSelected()) {
                    getOptimizationNone_IsSelected().setEntity(false);
                    getOptimizationForDesktop_IsSelected().setEntity(false);
                    getOptimizationCustom_IsSelected().setEntity(false);
                }
                else if (senderEntityModel == getOptimizationForDesktop_IsSelected()) {
                    getOptimizationNone_IsSelected().setEntity(false);
                    getOptimizationForServer_IsSelected().setEntity(false);
                    getOptimizationCustom_IsSelected().setEntity(false);
                }
                else if (senderEntityModel == getOptimizationCustom_IsSelected()) {
                    getOptimizationNone_IsSelected().setEntity(false);
                    getOptimizationForServer_IsSelected().setEntity(false);
                    getOptimizationForDesktop_IsSelected().setEntity(false);
                }
                else if (senderEntityModel == getMigrateOnErrorOption_YES()) {
                    getMigrateOnErrorOption_NO().setEntity(false);
                    getMigrateOnErrorOption_HA_ONLY().setEntity(false);
                }
                else if (senderEntityModel == getMigrateOnErrorOption_NO()) {
                    getMigrateOnErrorOption_YES().setEntity(false);
                    getMigrateOnErrorOption_HA_ONLY().setEntity(false);
                }
                else if (senderEntityModel == getMigrateOnErrorOption_HA_ONLY()) {
                    getMigrateOnErrorOption_YES().setEntity(false);
                    getMigrateOnErrorOption_NO().setEntity(false);
                } else if (senderEntityModel == getOptimizeForUtilization()) {
                    getOptimizeForSpeed().setEntity(false);
                } else if (senderEntityModel == getOptimizeForSpeed()) {
                    getOptimizeForUtilization().setEntity(false);
                } else if(senderEntityModel == getGuarantyResources()) {
                    getAllowOverbooking().setEntity(false);
                } else if(senderEntityModel == getAllowOverbooking()) {
                    getGuarantyResources().setEntity(false);
                }
            }
        }
    }

    private void architectureSelectedItemChanged(EventArgs args) {
        filterCpuTypeByArchitecture();
    }

    private void filterCpuTypeByArchitecture() {
        final ArchitectureType selectedArchitecture = getArchitecture().getSelectedItem();
        final FilteredListModel.Filter<ServerCpu> filter = selectedArchitecture == null
                || selectedArchitecture.equals(ArchitectureType.undefined)
                ? null
                : new FilteredListModel.Filter<ServerCpu>() {

                    @Override
                    public boolean filter(ServerCpu cpu) {
                        final ArchitectureType cpuArchitecture = cpu.getArchitecture();
                        final boolean showCpu = selectedArchitecture.equals(cpuArchitecture);
                        return showCpu;
                }
        };
        getCPU().filterItems(filter);
    }

    private void CPU_SelectedItemChanged(EventArgs args) {
        updateMigrateOnError();
    }

    private void version_SelectedItemChanged(EventArgs e) {
        Version version;
        if (getVersion().getSelectedItem() != null) {
            version = getVersion().getSelectedItem();
        }
        else {
            version = getDataCenter().getSelectedItem().getCompatibilityVersion();
        }

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                ClusterModel clusterModel = (ClusterModel) model;
                ArrayList<ServerCpu> cpus = (ArrayList<ServerCpu>) result;

                if (clusterModel.getIsEdit()) {
                    AsyncQuery emptyQuery = new AsyncQuery();

                    emptyQuery.setModel(new Object[] { clusterModel, cpus });
                    emptyQuery.asyncCallback = new INewAsyncCallback() {

                        @Override
                        public void onSuccess(Object model, Object returnValue) {
                            Boolean isEmpty = (Boolean) returnValue;

                            Object[] objArray = (Object[]) model;

                            ClusterModel clusterModel = (ClusterModel) objArray[0];
                            ArrayList<ServerCpu> cpus = (ArrayList<ServerCpu>) objArray[1];

                            if (isEmpty) {
                                populateCPUList(clusterModel, cpus, true);
                            } else {
                                ArrayList<ServerCpu> filteredCpus = new ArrayList<ServerCpu>();

                                for (ServerCpu cpu : cpus) {
                                    if (cpu.getArchitecture() == clusterModel.getEntity().getArchitecture()) {
                                        filteredCpus.add(cpu);
                                    }
                                }

                                populateCPUList(clusterModel, filteredCpus, false);
                            }
                        }
                    };

                    AsyncDataProvider.getInstance().isClusterEmpty(emptyQuery, clusterModel.getEntity().getId());
                } else {
                    populateCPUList(clusterModel, cpus, true);
                }
            }
        };
        AsyncDataProvider.getInstance().getCPUList(_asyncQuery, version);

        // CPU Thread support is only available for clusters of version 3.2 or greater
        getVersionSupportsCpuThreads().setEntity(version.compareTo(Version.v3_2) >= 0);
        getEnableBallooning().setChangeProhibitionReason(ConstantsManager.getInstance().getConstants().ballooningNotAvailable());
        getEnableBallooning().setIsChangeable(version.compareTo(Version.v3_3) >= 0);

        setRngSourcesCheckboxes(version);

        updateFencingPolicyContent(version);

        updateKSMPolicy(version);

        updateMigrateOnError();

        updateMigrationOptions();

        refreshAdditionalClusterFeaturesList();
    }

    private void updateKSMPolicy(Version version) {
        // enable KSM control from version 3.4
        boolean isSmallerThanVersion3_4 = version.compareTo(Version.v3_4) < 0;
        getEnableKsm().setIsChangeable(!isSmallerThanVersion3_4);
        getEnableKsm().setChangeProhibitionReason(ConstantsManager.getInstance().getConstants().ksmNotAvailable());
        // for version 3.3 and lower the default is true.
        if (isSmallerThanVersion3_4) {
            getEnableKsm().setEntity(true);
        }

        // allow KSM with NUMA awareness only from version 3.4
        boolean isLowerVersionThen3_4 = version.compareTo(Version.v3_4) < 0;
        getKsmPolicyForNumaSelection().setIsAvailable(!isLowerVersionThen3_4);
        getKsmPolicyForNumaSelection().setChangeProhibitionReason(ConstantsManager.getInstance()
                .getConstants()
                .ksmWithNumaAwarnessNotAvailable());
        // enable NUMA aware KSM by default (matching kernel's default)
        setKsmPolicyForNuma(true);
    }

    private void refreshAdditionalClusterFeaturesList() {
        if (getVersion() == null || getVersion().getSelectedItem() == null) {
            return;
        }
        Version version = getVersion().getSelectedItem();

        ApplicationMode category = null;
        if (getEnableGlusterService().getEntity() && getEnableOvirtService().getEntity()) {
            category = ApplicationMode.AllModes;
        } else if (getEnableGlusterService().getEntity()) {
            category = ApplicationMode.GlusterOnly;
        } else if (getEnableOvirtService().getEntity()) {
            category = ApplicationMode.VirtOnly;
        }

        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setModel(this);
        // Get all the addtional features avaivalble for the cluster
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                ClusterModel.this.stopProgress();
                final Set<AdditionalFeature> features = (Set<AdditionalFeature>) result;
                // Get the additional features which are already enabled for cluster. Applicable only in case of edit
                // cluster
                if (getIsEdit() && !features.isEmpty()) {
                    AsyncQuery asyncQuery = new AsyncQuery();
                    asyncQuery.setModel(this);
                    asyncQuery.asyncCallback = new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object model, Object returnValue) {
                            ClusterModel.this.stopProgress();
                            Set<SupportedAdditionalClusterFeature> clusterFeatures =
                                    (Set<SupportedAdditionalClusterFeature>) returnValue;
                            Set<AdditionalFeature> featuresEnabled = new HashSet<>();
                            for (SupportedAdditionalClusterFeature feature : clusterFeatures) {
                                if (feature.isEnabled()) {
                                    featuresEnabled.add(feature.getFeature());
                                }
                            }
                            updateAddtionClusterFeatureList(features, featuresEnabled);
                        }
                    };
                    ClusterModel.this.startProgress(null);
                    AsyncDataProvider.getInstance().getClusterFeaturesByClusterId(asyncQuery, getEntity().getId());
                } else {
                    updateAddtionClusterFeatureList(features,
                            Collections.<AdditionalFeature> emptySet());
                }
            }
        };
        this.startProgress(null);
        AsyncDataProvider.getInstance().getClusterFeaturesByVersionAndCategory(asyncQuery, version, category);
    }

    private void updateAddtionClusterFeatureList(Set<AdditionalFeature> featuresAvailable,
            Set<AdditionalFeature> featuresEnabled) {
        List<AdditionalFeature> features = new ArrayList<>();
        List<AdditionalFeature> selectedFeatures = new ArrayList<>();
        for (AdditionalFeature feature : featuresAvailable) {
            features.add(feature);
            if (featuresEnabled.contains(feature)) {
                selectedFeatures.add(feature);
            }
        }
        List<List<AdditionalFeature>> clusterFeatureList = new ArrayList<>();
        clusterFeatureList.add(features);
        getAdditionalClusterFeatures().setItems(clusterFeatureList, selectedFeatures);
    }

    private void updateMigrationOptions() {
        Version version = getVersion().getSelectedItem();
        if (version == null) {
            return;
        }

        autoConverge.updateChangeability(ConfigurationValues.AutoConvergenceSupported, version);
        migrateCompressed.updateChangeability(ConfigurationValues.MigrationCompressionSupported, version);
    }

    private void updateMigrateOnError() {
        ServerCpu cpu = getCPU().getSelectedItem();

        Version version = getVersion().getSelectedItem();

        if (version == null) {
            return;
        }

        if (cpu == null || cpu.getArchitecture() == null) {
            return;
        }

        getMigrateOnErrorOption_NO().setIsAvailable(true);

        if (AsyncDataProvider.getInstance().isMigrationSupported(cpu.getArchitecture(), version)) {
            getMigrateOnErrorOption_YES().setIsAvailable(true);
            getMigrateOnErrorOption_HA_ONLY().setIsAvailable(true);
        } else {
            getMigrateOnErrorOption_YES().setIsAvailable(false);
            getMigrateOnErrorOption_HA_ONLY().setIsAvailable(false);

            setMigrateOnErrorOption(MigrateOnErrorOptions.NO);
        }
    }

    private void setRngSourcesCheckboxes(Version ver) {
        boolean rngSupported = isRngSupportedForClusterVersion(ver);
        getRngRandomSourceRequired().setIsChangeable(rngSupported);
        getRngHwrngSourceRequired().setIsChangeable(rngSupported);

        String defaultRequiredRngSourcesCsv = defaultClusterRngSourcesCsv(ver);

        if (rngSupported) {
            getRngRandomSourceRequired().setEntity(getIsNew()
                    ? defaultRequiredRngSourcesCsv.contains(VmRngDevice.Source.RANDOM.name().toLowerCase())
                    : getEntity().getRequiredRngSources().contains(VmRngDevice.Source.RANDOM));
            getRngHwrngSourceRequired().setEntity(getIsNew()
                    ? defaultRequiredRngSourcesCsv.contains(VmRngDevice.Source.HWRNG.name().toLowerCase())
                    : getEntity().getRequiredRngSources().contains(VmRngDevice.Source.HWRNG));
        } else { // reset
            getRngRandomSourceRequired().setEntity(false);
            getRngHwrngSourceRequired().setEntity(false);
            getRngRandomSourceRequired().setChangeProhibitionReason(ConstantsManager.getInstance().getConstants().rngNotSupportedByClusterCV());
            getRngHwrngSourceRequired().setChangeProhibitionReason(ConstantsManager.getInstance().getConstants().rngNotSupportedByClusterCV());
        }
    }

    private void updateFencingPolicyContent(Version ver) {
        // skipFencingIfConnectivityBroken option is enabled when fencing is enabled for all cluster versions
        getSkipFencingIfConnectivityBrokenEnabled().setIsChangeable(getFencingEnabledModel().getEntity());
        getHostsWithBrokenConnectivityThreshold().setIsChangeable(getFencingEnabledModel().getEntity());

        if (ver == null) {
            if (!getFencingEnabledModel().getEntity()) {
                // fencing is disabled and cluster version not selected yet, so disable skipFencingIfSDActive
                getSkipFencingIfSDActiveEnabled().setIsChangeable(false);
            }
        } else {
            // skipFencingIfSDActive is enabled for supported cluster level if fencing is not disabled
            boolean supported = AsyncDataProvider.getInstance().isSkipFencingIfSDActiveSupported(ver.getValue());
            getSkipFencingIfSDActiveEnabled().setIsChangeable(
                    supported && getFencingEnabledModel().getEntity());
            if (supported) {
                if (getEntity() == null) {
                    // this can happen when creating new cluster and cluster dialog is shown
                    getSkipFencingIfSDActiveEnabled().setEntity(true);
                } else {
                    getSkipFencingIfSDActiveEnabled().setEntity(
                            getEntity().getFencingPolicy().isSkipFencingIfSDActive());
                }
            } else {
                getSkipFencingIfSDActiveEnabled().setEntity(false);
            }
        }
    }

    private void populateCPUList(ClusterModel clusterModel, List<ServerCpu> cpus, boolean canChangeArchitecture) {
        // disable CPU Architecture-Type filtering
        getArchitecture().getSelectedItemChangedEvent().removeListener(this);

        ServerCpu oldSelectedCpu = clusterModel.getCPU().getSelectedItem();
        ArchitectureType oldSelectedArch = clusterModel.getArchitecture().getSelectedItem();

        clusterModel.getCPU().setItems(cpus);
        initSupportedArchitectures(clusterModel);

        clusterModel.getCPU().setSelectedItem(oldSelectedCpu != null ?
                Linq.firstOrDefault(cpus, new Linq.ServerCpuPredicate(oldSelectedCpu.getCpuName())) : null);

        if (clusterModel.getCPU().getSelectedItem() == null || !isCPUinitialized) {
            initCPU();
        }

        if (clusterModel.getIsEdit()) {
            if (!canChangeArchitecture) {
                getArchitecture().setItems(new ArrayList<ArchitectureType>(
                        Arrays.asList(clusterModel.getEntity().getArchitecture())));
            }

            if (oldSelectedArch != null) {
                getArchitecture().setSelectedItem(oldSelectedArch);
            } else {
                if (clusterModel.getEntity() != null) {
                    getArchitecture().setSelectedItem(clusterModel.getEntity().getArchitecture());
                } else {
                    getArchitecture().setSelectedItem(ArchitectureType.undefined);
                }
            }
        } else {
            getArchitecture().setSelectedItem(ArchitectureType.undefined);
        }

        // enable CPU Architecture-Type filtering
        initCpuArchTypeFiltering();
    }

    private void initCpuArchTypeFiltering() {
        filterCpuTypeByArchitecture();
        getArchitecture().getSelectedItemChangedEvent().addListener(this);
    }

    private void initSupportedArchitectures(ClusterModel clusterModel) {
        Collection<ArchitectureType> archsWithSupportingCpus = new HashSet<ArchitectureType>();
        archsWithSupportingCpus.add(ArchitectureType.undefined);
        for (ServerCpu cpu: clusterModel.getCPU().getItems()) {
            archsWithSupportingCpus.add(cpu.getArchitecture());
        }
        clusterModel.getArchitecture().setItems(archsWithSupportingCpus);
    }

    private void initCPU() {
        if (!isCPUinitialized && getIsEdit()) {
            isCPUinitialized = true;
            getCPU().setSelectedItem(null);
            for (ServerCpu a : getCPU().getItems()) {
                if (ObjectUtils.objectsEqual(a.getCpuName(), getEntity().getCpuName())) {
                    getCPU().setSelectedItem(a);
                    break;
                }
            }
        }
    }

    private void initHostsWithBrokenConnectivityThreshold() {
        ArrayList<Integer> values = new ArrayList<Integer>();
        // populating threshold values with {25, 50, 75, 100}
        for (int i = 25; i <= 100; i += 25) {
            values.add(i);
        }
        getHostsWithBrokenConnectivityThreshold().setItems(values);
        getHostsWithBrokenConnectivityThreshold().setSelectedItem(50);
    }

    private void storagePool_SelectedItemChanged(EventArgs e) {
        // possible versions for new cluster (when editing cluster, this event won't occur)
        // are actually the possible versions for the data-center that the cluster is going
        // to be attached to.
        final StoragePool selectedDataCenter = getDataCenter().getSelectedItem();
        if (selectedDataCenter == null) {
            getManagementNetwork().setItems(Collections.<Network>emptyList());
            return;
        }
        if (selectedDataCenter.isLocal()) {
            setIsResiliencePolicyTabAvailable(false);
        }
        else {
            setIsResiliencePolicyTabAvailable(true);
        }

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                ClusterModel clusterModel = (ClusterModel) model;
                ArrayList<Version> versions = (ArrayList<Version>) result;
                Version selectedVersion = clusterModel.getVersion().getSelectedItem();
                clusterModel.getVersion().setItems(versions);
                if (selectedVersion == null ||
                        !versions.contains(selectedVersion) ||
                        selectedVersion.compareTo(selectedDataCenter.getCompatibilityVersion()) > 0) {
                    if(ApplicationModeHelper.getUiMode().equals(ApplicationMode.GlusterOnly)){
                        clusterModel.getVersion().setSelectedItem(Linq.selectHighestVersion(versions));
                    }
                    else {
                        clusterModel.getVersion().setSelectedItem(selectedDataCenter.getCompatibilityVersion());
                    }
                }
                else if (clusterModel.getIsEdit()) {
                    clusterModel.getVersion().setSelectedItem(Linq.firstOrDefault(versions,
                            new Linq.VersionPredicate(clusterModel.getEntity().getCompatibilityVersion())));
                }
                else {
                    clusterModel.getVersion().setSelectedItem(selectedVersion);
                }
            }
        };
        AsyncDataProvider.getInstance().getDataCenterVersions(_asyncQuery,
                ApplicationModeHelper.getUiMode().equals(ApplicationMode.GlusterOnly) ? null
                        : selectedDataCenter.getId());

        if (getManagementNetwork().getIsChangable()) {
            loadDcNetworks(selectedDataCenter.getId());
        }
    }

    private void clusterPolicyChanged() {
        ClusterPolicy clusterPolicy = getClusterPolicy().getSelectedItem();
        Map<String, String> policyProperties = new HashMap<String, String>();
        Map<Guid, PolicyUnit> allPolicyUnits = new HashMap<Guid, PolicyUnit>();
        if (clusterPolicy.getFilters() != null) {
            for (Guid policyUnitId : clusterPolicy.getFilters()) {
                allPolicyUnits.put(policyUnitId, policyUnitMap.get(policyUnitId));
            }
        }
        if (clusterPolicy.getFunctions() != null) {
            for (Pair<Guid, Integer> pair : clusterPolicy.getFunctions()) {
                allPolicyUnits.put(pair.getFirst(), policyUnitMap.get(pair.getFirst()));
            }
        }
        if (clusterPolicy.getBalance() != null) {
            allPolicyUnits.put(clusterPolicy.getBalance(), policyUnitMap.get(clusterPolicy.getBalance()));
        }

        for (PolicyUnit policyUnit : allPolicyUnits.values()) {
            if (policyUnit.getParameterRegExMap() != null) {
                policyProperties.putAll(policyUnit.getParameterRegExMap());
            }
        }
        getCustomPropertySheet().setKeyValueMap(policyProperties);
        if (getIsEdit() &&
                clusterPolicy.getId().equals(getEntity().getClusterPolicyId())) {
            getCustomPropertySheet().deserialize(KeyValueModel.convertProperties(getEntity().getClusterPolicyProperties()));
        } else {
            getCustomPropertySheet().deserialize(KeyValueModel.convertProperties(clusterPolicy.getParameterMap()));
        }
    }

    public boolean validate(boolean validateCpu) {
        return validate(true, validateCpu, true);
    }

    public boolean validate(boolean validateStoragePool, boolean validateCpu, boolean validateCustomProperties) {
        getName().validateEntity(new IValidation[] {
                new NotEmptyValidation(),
                new LengthValidation(40),
                new I18NNameValidation() });

        if (validateStoragePool) {
            getDataCenter().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }

        if (validateCpu) {
            getCPU().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }
        else {
            getCPU().validateSelectedItem(new IValidation[] {});
        }

        if (validateCustomProperties) {
            getCustomPropertySheet().setIsValid(getCustomPropertySheet().validate());
        }
        setValidTab(TabName.CLUSTER_POLICY_TAB, getCustomPropertySheet().getIsValid());

        getVersion().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });

        getManagementNetwork().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });

        validateRngRequiredSource();

        boolean validService = true;
        if (getEnableOvirtService().getIsAvailable() && getEnableGlusterService().getIsAvailable()) {
            validService = getEnableOvirtService().getEntity()
                            || getEnableGlusterService().getEntity();
        }

        getGlusterHostAddress().validateEntity(new IValidation[] { new NotEmptyValidation() });
        getGlusterHostPassword().validateEntity(new IValidation[] { new NotEmptyValidation() });

        if (!validService) {
            setMessage(ConstantsManager.getInstance().getConstants().clusterServiceValidationMsg());
        }
        else if (getIsImportGlusterConfiguration().getEntity() && getGlusterHostAddress().getIsValid()
                && getGlusterHostPassword().getIsValid()
                && !isFingerprintVerified()) {
            setMessage(ConstantsManager.getInstance().getConstants().fingerprintNotVerified());
        }
        else {
            setMessage(null);
        }

        if (getSpiceProxyEnabled().getEntity()) {
            getSpiceProxy().validateEntity(new IValidation[] { new HostWithProtocolAndPortAddressValidation() });
        } else {
            getSpiceProxy().setIsValid(true);
        }
        setValidTab(TabName.CONSOLE_TAB, getSpiceProxy().getIsValid());

        if (getSerialNumberPolicy().getSelectedSerialNumberPolicy() == SerialNumberPolicy.CUSTOM) {
            getSerialNumberPolicy().getCustomSerialNumber().validateEntity(new IValidation[] { new NotEmptyValidation() });
        } else {
            getSerialNumberPolicy().getCustomSerialNumber().setIsValid(true);
        }

        boolean generalTabValid = getName().getIsValid() && getDataCenter().getIsValid() && getCPU().getIsValid()
                && getManagementNetwork().getIsValid()
                && getVersion().getIsValid() && validService && getGlusterHostAddress().getIsValid()
                && getRngRandomSourceRequired().getIsValid()
                && getRngHwrngSourceRequired().getIsValid()
                && getGlusterHostPassword().getIsValid()
                && (getIsImportGlusterConfiguration().getEntity() ? (getGlusterHostAddress().getIsValid()
                && getGlusterHostPassword().getIsValid()
                && getSerialNumberPolicy().getCustomSerialNumber().getIsValid()
                && isFingerprintVerified()) : true);
        setValidTab(TabName.GENERAL_TAB, generalTabValid);
        ValidationCompleteEvent.fire(getEventBus(), this);
        return generalTabValid && getCustomPropertySheet().getIsValid() && getSpiceProxy().getIsValid();
    }

    private void validateRngRequiredSource() {
        Version clusterVersion = getVersion().getSelectedItem();
        boolean rngSupportedForCluster = isRngSupportedForClusterVersion(clusterVersion);

        getRngRandomSourceRequired().setIsValid(rngSupportedForCluster || !getRngRandomSourceRequired().getEntity());
        getRngHwrngSourceRequired().setIsValid(rngSupportedForCluster || !getRngHwrngSourceRequired().getEntity());
    }

    private boolean isRngSupportedForClusterVersion(Version version) {
        if (version == null) {
            return false;
        }

        Boolean supported = (Boolean) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.VirtIoRngDeviceSupported, version.toString());
        return (supported == null)
                ? false
                : supported;
    }

    private String defaultClusterRngSourcesCsv(Version ver) {
        String srcs = (String) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.ClusterRequiredRngSourcesDefault, ver.toString());
        return (srcs == null)
                ? ""
                : srcs;
    }

    public boolean getKsmPolicyForNuma() {
        switch (getKsmPolicyForNumaSelection().getSelectedItem()) {
        case shareAcrossNumaNodes:
            return true;
        case shareInsideEachNumaNode:
            return false;
        }
        return true;
    }

    public void setKsmPolicyForNuma(Boolean ksmPolicyForNumaFlag) {
        if (ksmPolicyForNumaFlag == null)
            return;
        KsmPolicyForNuma ksmPolicyForNuma = KsmPolicyForNuma.shareAcrossNumaNodes;
        if (ksmPolicyForNumaFlag == false)
            ksmPolicyForNuma = KsmPolicyForNuma.shareInsideEachNumaNode;

        getKsmPolicyForNumaSelection().setSelectedItem(ksmPolicyForNuma);
        return;
    }

    public enum KsmPolicyForNuma {

        shareAcrossNumaNodes(ConstantsManager.getInstance().getConstants().shareKsmAcrossNumaNodes()),
        shareInsideEachNumaNode(ConstantsManager.getInstance().getConstants().shareKsmInsideEachNumaNode());

        private String description;

        private KsmPolicyForNuma(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

}
