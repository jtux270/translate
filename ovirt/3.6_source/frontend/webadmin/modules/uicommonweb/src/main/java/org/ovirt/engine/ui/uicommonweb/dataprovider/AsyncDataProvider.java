package org.ovirt.engine.ui.uicommonweb.dataprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ovirt.engine.core.aaa.ProfileEntry;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.EventNotificationEntity;
import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.VdcEventNotificationUtils;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRemoveBricksQueriesParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.AdditionalFeature;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.CertificateInfo;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.Erratum;
import org.ovirt.engine.core.common.businessentities.ExternalComputeResource;
import org.ovirt.engine.core.common.businessentities.ExternalDiscoveredHost;
import org.ovirt.engine.core.common.businessentities.ExternalHostGroup;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.businessentities.NumaTuneMode;
import org.ovirt.engine.core.common.businessentities.Permission;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.ServerCpu;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.SupportedAdditionalClusterFeature;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.businessentities.TagsType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VdsNumaNode;
import org.ovirt.engine.core.common.businessentities.VmGuestAgentInterface;
import org.ovirt.engine.core.common.businessentities.VmPool;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.core.common.businessentities.aaa.AuthzGroup;
import org.ovirt.engine.core.common.businessentities.aaa.DbGroup;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.businessentities.comparators.LexoNumericComparator;
import org.ovirt.engine.core.common.businessentities.comparators.NameableComparator;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterClusterService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepNonEligibilityReason;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSession;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeSnapshotEntity;
import org.ovirt.engine.core.common.businessentities.gluster.ServiceType;
import org.ovirt.engine.core.common.businessentities.network.HostNetworkQos;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkQoS;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VnicProfileView;
import org.ovirt.engine.core.common.businessentities.qos.QosType;
import org.ovirt.engine.core.common.businessentities.storage.CinderVolumeType;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.businessentities.storage.ImageFileType;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType;
import org.ovirt.engine.core.common.businessentities.storage.RepoImage;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.console.ConsoleOptions.WanColorDepth;
import org.ovirt.engine.core.common.console.ConsoleOptions.WanDisableEffects;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.ArchCapabilitiesParameters;
import org.ovirt.engine.core.common.queries.ArchCapabilitiesParameters.ArchCapabilitiesVerb;
import org.ovirt.engine.core.common.queries.ClusterEditParameters;
import org.ovirt.engine.core.common.queries.CommandVersionsInfo;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.GetAgentFenceOptionsQueryParameters;
import org.ovirt.engine.core.common.queries.GetAllAttachableDisksForVmQueryParameters;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.common.queries.GetAllProvidersParameters;
import org.ovirt.engine.core.common.queries.GetAllServerCpuListParameters;
import org.ovirt.engine.core.common.queries.GetClusterFeaturesByVersionAndCategoryParameters;
import org.ovirt.engine.core.common.queries.GetConfigurationValueParameters;
import org.ovirt.engine.core.common.queries.GetConnectionsByDataCenterAndStorageTypeParameters;
import org.ovirt.engine.core.common.queries.GetDataCentersWithPermittedActionOnClustersParameters;
import org.ovirt.engine.core.common.queries.GetEntitiesWithPermittedActionParameters;
import org.ovirt.engine.core.common.queries.GetExistingStorageDomainListParameters;
import org.ovirt.engine.core.common.queries.GetFilteredAttachableDisksParameters;
import org.ovirt.engine.core.common.queries.GetHostListFromExternalProviderParameters;
import org.ovirt.engine.core.common.queries.GetHostsForStorageOperationParameters;
import org.ovirt.engine.core.common.queries.GetImagesListByStoragePoolIdParameters;
import org.ovirt.engine.core.common.queries.GetLunsByVgIdParameters;
import org.ovirt.engine.core.common.queries.GetPermittedStorageDomainsByStoragePoolIdParameters;
import org.ovirt.engine.core.common.queries.GetStorageDomainDefaultWipeAfterDeleteParameters;
import org.ovirt.engine.core.common.queries.GetStorageDomainsByConnectionParameters;
import org.ovirt.engine.core.common.queries.GetStoragePoolsByClusterServiceParameters;
import org.ovirt.engine.core.common.queries.GetSupportedCpuListParameters;
import org.ovirt.engine.core.common.queries.GetTagsByUserGroupIdParameters;
import org.ovirt.engine.core.common.queries.GetTagsByUserIdParameters;
import org.ovirt.engine.core.common.queries.GetTagsByVdsIdParameters;
import org.ovirt.engine.core.common.queries.GetTagsByVmIdParameters;
import org.ovirt.engine.core.common.queries.GetVmChangedFieldsForNextRunParameters;
import org.ovirt.engine.core.common.queries.GetVmFromOvaQueryParameters;
import org.ovirt.engine.core.common.queries.GetVmTemplateParameters;
import org.ovirt.engine.core.common.queries.GetVmsFromExternalProviderQueryParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.InterfaceAndIdQueryParameters;
import org.ovirt.engine.core.common.queries.MultilevelAdministrationsQueriesParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.OsQueryParameters;
import org.ovirt.engine.core.common.queries.OsQueryParameters.OsRepositoryVerb;
import org.ovirt.engine.core.common.queries.ProviderQueryParameters;
import org.ovirt.engine.core.common.queries.QosQueryParameterBase;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.ServerParameters;
import org.ovirt.engine.core.common.queries.StorageDomainsAndStoragePoolIdQueryParameters;
import org.ovirt.engine.core.common.queries.StorageServerConnectionQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.VmIconIdSizePair;
import org.ovirt.engine.core.common.queries.gluster.AddedGlusterServersParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterHookContentQueryParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterHookQueryParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterServersQueryParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterServiceQueryParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeAdvancedDetailsParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeGeoRepEligibilityParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeProfileParameters;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeQueriesParameters;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.IntegerCompat;
import org.ovirt.engine.core.compat.KeyValuePairCompat;
import org.ovirt.engine.core.compat.RefObject;
import org.ovirt.engine.core.compat.RpmVersion;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.searchbackend.OsValueAutoCompleter;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.IAsyncConverter;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.LoginModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NetworkModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NetworkQoSModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.FcpStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.GlusterStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.IStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.ImportFcpStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.ImportIscsiStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.IscsiStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.LocalStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.NfsStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.PosixStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendMultipleQueryAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleQueryAsyncCallback;
import org.ovirt.engine.ui.uicompat.SpiceConstantsManager;

public class AsyncDataProvider {

    //TODO MM: fix duplicity with org.ovirt.engine.core.bll.RunVmCommand.ISO_PREFIX  ?
    public static final String ISO_PREFIX = "iso://";    //$NON-NLS-1$
    private static AsyncDataProvider instance;

    public static AsyncDataProvider getInstance() {
        if (instance == null) {
            instance = new AsyncDataProvider();
        }
        return instance;
    }

    public static void setInstance(AsyncDataProvider provider) {
        instance = provider;
    }

    private static final String GENERAL = "general"; //$NON-NLS-1$

    private static int DEFAULT_OS_ID = 0;

    // dictionary to hold cache of all config values (per version) queried by client, if the request for them succeeded.
    private HashMap<KeyValuePairCompat<ConfigurationValues, String>, Object> cachedConfigValues =
            new HashMap<KeyValuePairCompat<ConfigurationValues, String>, Object>();

    private HashMap<KeyValuePairCompat<ConfigurationValues, String>, Object> cachedConfigValuesPreConvert =
            new HashMap<KeyValuePairCompat<ConfigurationValues, String>, Object>();

    private String _defaultConfigurationVersion = null;

    // cached OS names
    private HashMap<Integer, String> osNames;

    // OS default icons
    private Map<Integer, VmIconIdSizePair> osIdToDefaultIconIdMap;

    /**
     * large-icon-id -> small-icon-id; data comes from {@link #osIdToDefaultIconIdMap}
     */
    private Map<Guid, Guid> largeToSmallOsDefaultIconIdMap;

    // cached list of os ids
    private List<Integer> osIds;

    // cached unique OS names
    private HashMap<Integer, String> uniqueOsNames;

    // cached linux OS
    private List<Integer> linuxOsIds;

    // cached NIC hotplug support map
    private Map<Pair<Integer, Version>, Boolean> nicHotplugSupportMap;

    // cached disk hotpluggable interfaces map
    private Map<Pair<Integer, Version>, Set<String>> diskHotpluggableInterfacesMap;

    // cached os's balloon enabled by default map (given compatibility version)
    private Map<Integer, Map<Version, Boolean>> balloonSupportMap;

    // cached windows OS
    private List<Integer> windowsOsIds;
    // cached OS Architecture
    private HashMap<Integer, ArchitectureType> osArchitectures;
    // default OS per architecture
    private HashMap<ArchitectureType, Integer> defaultOSes;

    // cached os's support for graphics and display types (given compatibility version)
    private Map<Integer, Map<Version, List<Pair<GraphicsType, DisplayType>>>> graphicsAndDisplays;

    // cached architecture support for live migration
    private Map<ArchitectureType, Map<Version, Boolean>> migrationSupport;

    // cached architecture support for memory snapshot
    private Map<ArchitectureType, Map<Version, Boolean>> memorySnapshotSupport;

    // cached architecture support for VM suspend
    private Map<ArchitectureType, Map<Version, Boolean>> suspendSupport;

    // cached custom properties
    private Map<Version, Map<String, String>> customPropertiesList;

    public String getDefaultConfigurationVersion() {
        return _defaultConfigurationVersion;
    }

    private void getDefaultConfigurationVersion(Object target) {
        AsyncQuery callback = new AsyncQuery(target, new INewAsyncCallback() {

            @Override
            public void onSuccess(Object model, Object returnValue) {
                if (returnValue != null) {
                    _defaultConfigurationVersion =
                            ((VdcQueryReturnValue) returnValue).getReturnValue();
                } else {
                    _defaultConfigurationVersion = GENERAL;
                }
                LoginModel loginModel = (LoginModel) model;
                loginModel.getLoggedInEvent().raise(loginModel, EventArgs.EMPTY);
            }
        });
        callback.setHandleFailure(true);
        Frontend.getInstance().runQuery(VdcQueryType.GetDefaultConfigurationVersion,
                new VdcQueryParametersBase(),
                callback);
    }

    public void initCache(LoginModel loginModel) {
        cacheConfigValues(new AsyncQuery(loginModel, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                getDefaultConfigurationVersion(target);
            }
        }));
        initOsNames();
        initOsDefaultIconIds();
        initUniqueOsNames();
        initLinuxOsTypes();
        initWindowsOsTypes();
        initDisplayTypes();
        initBalloonSupportMap();
        initNicHotplugSupportMap();
        initDiskHotpluggableInterfacesMap();
        initOsArchitecture();
        initDefaultOSes();
        initMigrationSupportMap();
        initMemorySnapshotSupportMap();
        initSuspendSupportMap();
        initCustomPropertiesList();
    }

    private void initCustomPropertiesList() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                customPropertiesList = (Map<Version, Map<String, String>>) returnValue;
            }
        };

        callback.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return (source != null) ? (Map<Version, Map<String, String>>) source
                        : new HashMap<Version, Map<String, String>>();
            }


        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmCustomProperties,
                new VdcQueryParametersBase().withoutRefresh(), callback);
    }

    public void initDefaultOSes() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                defaultOSes = ((VdcQueryReturnValue) returnValue)
                        .getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(
                OsRepositoryVerb.GetDefaultOSes), callback);
    }

    public void getStorageDomainsWithAttachedStoragePoolGuid(
            AsyncQuery aQuery, StoragePool storagePool, List<StorageDomain> storageDomains) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source == null ?
                        new ArrayList<StorageDomain>() : (ArrayList<StorageDomain>) source;
            }
        };
        StorageDomainsAndStoragePoolIdQueryParameters parameters =
                new StorageDomainsAndStoragePoolIdQueryParameters(storageDomains, storagePool.getId());
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainsWithAttachedStoragePoolGuid,
                parameters, aQuery);
    }

    public void getStorageDomainsWithAttachedStoragePoolGuid(
            AsyncQuery aQuery, StoragePool storagePool,
            List<StorageDomain> storageDomains, StorageServerConnections storageServerConnection, Guid vdsId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source == null ?
                        new ArrayList<StorageDomain>() : (ArrayList<StorageDomain>) source;
            }
        };

        if (storageDomains != null) {
            // Get file storage domains
            StorageDomainsAndStoragePoolIdQueryParameters parameters =
                    new StorageDomainsAndStoragePoolIdQueryParameters(storageDomains, storagePool.getId(), vdsId);
            Frontend.getInstance().runQuery(VdcQueryType.GetBlockStorageDomainsWithAttachedStoragePoolGuid,
                    parameters, aQuery);
        }
        else {
            // Get block storage domains
            StorageDomainsAndStoragePoolIdQueryParameters parameters =
                    new StorageDomainsAndStoragePoolIdQueryParameters(storageServerConnection, storagePool.getId(), vdsId);
            Frontend.getInstance().runQuery(VdcQueryType.GetFileStorageDomainsWithAttachedStoragePoolGuid,
                    parameters, aQuery);
        }
    }

    public Boolean isMigrationSupported(ArchitectureType architecture, Version version) {
        return migrationSupport.get(architecture).get(version);
    }

    public Boolean isMemorySnapshotSupportedByArchitecture(ArchitectureType architecture, Version version) {
        return memorySnapshotSupport.get(architecture).get(version);
    }

    public Boolean isSuspendSupportedByArchitecture(ArchitectureType architecture, Version version) {
        return suspendSupport.get(architecture).get(version);
    }

    private void initMigrationSupportMap() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                migrationSupport = ((VdcQueryReturnValue) returnValue).getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetArchitectureCapabilities,
                new ArchCapabilitiesParameters(ArchCapabilitiesVerb.GetMigrationSupport),
                callback);
    }

    private void initMemorySnapshotSupportMap() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                memorySnapshotSupport = ((VdcQueryReturnValue) returnValue).getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetArchitectureCapabilities,
                new ArchCapabilitiesParameters(ArchCapabilitiesVerb.GetMemorySnapshotSupport),
                callback);
    }

    private void initSuspendSupportMap() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                suspendSupport = ((VdcQueryReturnValue) returnValue).getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetArchitectureCapabilities,
                new ArchCapabilitiesParameters(ArchCapabilitiesVerb.GetSuspendSupport),
                callback);
    }

    /**
     * Check if memory snapshot is supported
     * @param vm
     * @return
     */
    public boolean isMemorySnapshotSupported(VM vm) {
        if (vm == null) {
            return false;
        }

        boolean archMemorySnapshotSupported = isMemorySnapshotSupportedByArchitecture(
                vm.getClusterArch(),
                vm.getVdsGroupCompatibilityVersion());

        return  ((Boolean) getConfigValuePreConverted(
                ConfigurationValues.MemorySnapshotSupported,
                vm.getVdsGroupCompatibilityVersion().toString()))
                && archMemorySnapshotSupported;
    }

    public boolean canVmsBePaused(List<VM> items) {
        for (VM vm : items) {
            if (!isSuspendSupportedByArchitecture(vm.getClusterArch(),
                vm.getVdsGroupCompatibilityVersion())) {
                return false;
            }
        }

        return true;
    }

    public boolean isLiveMergeSupported(VM vm) {
        return (vm != null && (Boolean) getConfigValuePreConverted(
                ConfigurationValues.LiveMergeSupported,
                vm.getVdsGroupCompatibilityVersion().toString()));
    }

    public void initNicHotplugSupportMap() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                nicHotplugSupportMap = ((VdcQueryReturnValue) returnValue)
                        .getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(
                OsRepositoryVerb.GetNicHotplugSupportMap), callback);
    }

    public Map<Pair<Integer, Version>, Boolean> getNicHotplugSupportMap() {
        return nicHotplugSupportMap;
    }

    public Boolean getNicHotplugSupport(Integer osId, Version version) {
        Pair<Integer, Version> pair = new Pair<Integer, Version>(osId, version);

        if (getNicHotplugSupportMap().containsKey(pair)) {
            return getNicHotplugSupportMap().get(pair);
        }

        return false;
    }

    public Boolean isBalloonEnabled(int osId, Version version) {
        return balloonSupportMap.get(osId).get(version);
    }

    public void initBalloonSupportMap() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                balloonSupportMap = (Map<Integer, Map<Version, Boolean>>) ((VdcQueryReturnValue) returnValue)
                        .getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(
                OsRepositoryVerb.GetBalloonSupportMap), callback);
    }

    public void initDiskHotpluggableInterfacesMap() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                diskHotpluggableInterfacesMap = ((VdcQueryReturnValue) returnValue)
                        .getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(
                OsRepositoryVerb.GetDiskHotpluggableInterfacesMap), callback);
    }

    public Map<Pair<Integer, Version>, Set<String>> getDiskHotpluggableInterfacesMap() {
        return diskHotpluggableInterfacesMap;
    }

    public Collection<DiskInterface> getDiskHotpluggableInterfaces(Integer osId, Version version) {

        Set<String> diskHotpluggableInterfaces = getDiskHotpluggableInterfacesMap()
                .get(new Pair<Integer, Version>(osId, version));
        if (diskHotpluggableInterfaces == null) {
            return Collections.emptySet();
        }

        Collection<DiskInterface> diskInterfaces = new HashSet<DiskInterface>();
        for (String diskHotpluggableInterface : diskHotpluggableInterfaces) {
            diskInterfaces.add(DiskInterface.valueOf(diskHotpluggableInterface));
        }

        return diskInterfaces;
    }

    public void getUserProfile(AsyncQuery aQuery) {
        Frontend.getInstance().runQuery(VdcQueryType.GetUserProfile, new VdcQueryParametersBase(), aQuery);
    }

    public void getAAAProfilesListViaPublic(AsyncQuery aQuery, boolean passwordBasedOnly) {
        convertAAAProfilesResult(aQuery, passwordBasedOnly);
        Frontend.getInstance().runPublicQuery(VdcQueryType.GetAAAProfileList, new VdcQueryParametersBase(), aQuery);
    }

    public static void isFloppySupported(AsyncQuery aQuery, Integer osId, Version version) {
        aQuery.converterCallback = new IAsyncConverter<Boolean>() {
            @Override
            public Boolean Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (Boolean) source : Boolean.FALSE;
            }
        };
        OsQueryParameters params = new OsQueryParameters(OsRepositoryVerb.GetFloppySupport, osId, version);

        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, params, aQuery);
    }

    public void getIsoDomainByDataCenterId(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<StorageDomain> storageDomains = (ArrayList<StorageDomain>) source;
                    for (StorageDomain domain : storageDomains) {
                        if (domain.getStorageDomainType() == StorageDomainType.ISO) {
                            return domain;
                        }
                    }
                }

                return null;
            }
        };

        IdQueryParameters getIsoParams = new IdQueryParameters(dataCenterId);
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainsByStoragePoolId, getIsoParams, aQuery);
    }

    public void getExportDomainByDataCenterId(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<StorageDomain> storageDomains = (ArrayList<StorageDomain>) source;
                for (StorageDomain domain : storageDomains) {
                    if (domain.getStorageDomainType() == StorageDomainType.ImportExport) {
                        return domain;
                    }
                }

                return null;
            }
        };

        IdQueryParameters getExportParams = new IdQueryParameters(dataCenterId);
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainsByStoragePoolId, getExportParams, aQuery);
    }

    public void getIrsImageList(AsyncQuery aQuery, Guid storagePoolId) {
        getIrsImageList(aQuery, storagePoolId, false);
    }

    public void getIrsImageList(AsyncQuery aQuery, Guid storagePoolId, boolean forceRefresh) {
        ImageFileType imageFileType = ImageFileType.ISO;
        getIrsImageList(aQuery, storagePoolId, forceRefresh, imageFileType);
    }

    public void getFloppyImageList(AsyncQuery aQuery, Guid storagePoolId) {
        getIrsImageList(aQuery, storagePoolId, false, ImageFileType.Floppy);
    }

    public void getUnknownImageList(AsyncQuery aQuery, Guid storagePoolId, boolean forceRefresh) {
        getIrsImageList(aQuery,
                storagePoolId,
                forceRefresh,
                ImageFileType.All,
                new RepoImageToImageFileNameAsyncConverter() {


                    @Override
                    protected String transform(ArrayList<String> fileNameList, RepoImage repoImage) {
                        return ISO_PREFIX + super.transform(fileNameList, repoImage);
                    }

                    @Override
                    protected boolean desiredImage(RepoImage repoImage) {
                        return ImageFileType.Unknown == repoImage.getFileType();
                    }
                });
    }

    public void getIrsImageList(AsyncQuery aQuery,
            Guid storagePoolId,
            boolean forceRefresh,
            ImageFileType imageFileType) {


        getIrsImageList(aQuery, storagePoolId, forceRefresh, imageFileType,
                new RepoImageToImageFileNameAsyncConverter());
    }

    private void getIrsImageList(AsyncQuery aQuery,
            Guid storagePoolId,
            boolean forceRefresh,
            ImageFileType imageFileType,
            IAsyncConverter converterCallBack) {

        aQuery.converterCallback = converterCallBack;

        GetImagesListByStoragePoolIdParameters parameters =
                new GetImagesListByStoragePoolIdParameters(storagePoolId, imageFileType);
        parameters.setForceRefresh(forceRefresh);
        Frontend.getInstance().runQuery(VdcQueryType.GetImagesListByStoragePoolId, parameters, aQuery);
    }

    public void getDefaultManagementNetwork(AsyncQuery aQuery, Guid dataCenterId) {
        runQueryByIdParameter(VdcQueryType.GetDefaultManagementNetwork, aQuery, dataCenterId);
    }

    public void getManagementNetwork(AsyncQuery aQuery, Guid clusterId) {
        runQueryByIdParameter(VdcQueryType.GetManagementNetwork, aQuery, clusterId);
    }

    public void isManagementNetwork(AsyncQuery aQuery, Guid networkId) {
        runQueryByIdParameter(VdcQueryType.IsManagementNetwork, aQuery, networkId);
    }

    public void isClusterEmpty(AsyncQuery aQuery, Guid clusterId) {
        runQueryByIdParameter(VdcQueryType.IsClusterEmpty, aQuery, clusterId);
    }

    private void runQueryByIdParameter(VdcQueryType queryType, AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new AsIsAsyncConverter();
        Frontend.getInstance().runQuery(queryType, new IdQueryParameters(id), aQuery);
    }

    public void getHostArchitecture(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter<ArchitectureType>() {
            @Override
            public ArchitectureType Convert(Object source, AsyncQuery _asyncQuery) {
                return (ArchitectureType) source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetHostArchitecture, new IdQueryParameters(id), aQuery);
    }

    public void getClusterById(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVdsGroupById, new IdQueryParameters(id), aQuery);
    }

    public void getClusterListByName(AsyncQuery aQuery, String name) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<VDSGroup>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.Search,
                new SearchParameters("Cluster: name=" + name + " sortby name", SearchType.Cluster), //$NON-NLS-1$ //$NON-NLS-2$
                aQuery);
    }

    public void getAuthzGroupsByUserId(AsyncQuery aQuery, Guid userId) {
        aQuery.converterCallback = new IAsyncConverter<List<AuthzGroup>>() {
            @Override
            public List<AuthzGroup> Convert(Object source, AsyncQuery _asyncQuery) {
                return (List<AuthzGroup>) source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAuthzGroupsByUserId, new IdQueryParameters(userId), aQuery);
    }

    public void getPoolById(AsyncQuery aQuery, Guid poolId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmPoolById, new IdQueryParameters(poolId), aQuery);
    }

    public void getVmById(AsyncQuery aQuery, Guid vmId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmByVmId, new IdQueryParameters(vmId), aQuery);
    }

    public void getVmNextRunConfiguration(AsyncQuery aQuery, Guid vmId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmNextRunConfiguration, new IdQueryParameters(vmId), aQuery);
    }

    public void getVmChangedFieldsForNextRun(VM original, VM updated, VmManagementParametersBase updateVmParameters, AsyncQuery aQuery) {
        Frontend.getInstance().runQuery(VdcQueryType.GetVmChangedFieldsForNextRun,
                new GetVmChangedFieldsForNextRunParameters(original, updated, updateVmParameters), aQuery);
    }

    public void getDataCenterList(AsyncQuery aQuery) {
        getDataCenterList(aQuery, true);
    }

    public void getDataCenterList(AsyncQuery aQuery, boolean doRefresh) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<StoragePool>();
                }
                return source;
            }
        };
        SearchParameters params = new SearchParameters("DataCenter: sortby name", SearchType.StoragePool); //$NON-NLS-1$
        Frontend.getInstance().runQuery(VdcQueryType.Search, doRefresh ? params : params.withoutRefresh(), aQuery);
    }

    public void getDataCenterByClusterServiceList(AsyncQuery aQuery,
            boolean supportsVirtService,
            boolean supportsGlusterService) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<StoragePool>();
                }

                // sort data centers
                final ArrayList<StoragePool> storagePoolList = (ArrayList<StoragePool>) source;
                Collections.sort(storagePoolList, new NameableComparator());
                return source;
            }
        };

        final GetStoragePoolsByClusterServiceParameters parameters = new GetStoragePoolsByClusterServiceParameters();
        parameters.setSupportsVirtService(supportsVirtService);
        parameters.setSupportsGlusterService(supportsGlusterService);

        Frontend.getInstance().runQuery(VdcQueryType.GetStoragePoolsByClusterService, parameters, aQuery);
    }

    public void getDataCenterListByName(AsyncQuery aQuery, String name) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<StoragePool>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.Search,
                new SearchParameters("DataCenter: name=" + name + " sortby name", SearchType.StoragePool), //$NON-NLS-1$ //$NON-NLS-2$
                aQuery);
    }

    public void getSpiceUsbAutoShare(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Boolean) source).booleanValue() : true;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.SpiceUsbAutoShare,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getWANColorDepth(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? WanColorDepth.fromInt(((Integer) source).intValue()) : WanColorDepth.depth16;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.WANColorDepth, getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getWANDisableEffects(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<WanDisableEffects>();
                }

                List<WanDisableEffects> res = new ArrayList<WanDisableEffects>();
                String fromDb = (String) source;
                for (String value : fromDb.split(",")) {//$NON-NLS-1$
                    if (value == null) {
                        continue;
                    }

                    String trimmedValue = value.trim();
                    if ("".equals(trimmedValue)) {
                        continue;
                    }

                    res.add(WanDisableEffects.fromString(trimmedValue));
                }

                return res;

            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.WANDisableEffects,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getMaxVmsInPool(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 1000;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.MaxVmsInPool, getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getMaxNumOfVmSockets(AsyncQuery aQuery, String version) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 1;
            }
        };
        GetConfigurationValueParameters tempVar =
                new GetConfigurationValueParameters(ConfigurationValues.MaxNumOfVmSockets);
        tempVar.setVersion(version);
        getConfigFromCache(tempVar, aQuery);
    }

    public void getMaxNumOfVmCpus(AsyncQuery aQuery, String version) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 1;
            }
        };
        GetConfigurationValueParameters tempVar =
                new GetConfigurationValueParameters(ConfigurationValues.MaxNumOfVmCpus);
        tempVar.setVersion(version);
        getConfigFromCache(tempVar, aQuery);
    }

    public void getMaxNumOfCPUsPerSocket(AsyncQuery aQuery, String version) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 1;
            }
        };
        GetConfigurationValueParameters tempVar =
                new GetConfigurationValueParameters(ConfigurationValues.MaxNumOfCpuPerSocket);
        tempVar.setVersion(version);
        getConfigFromCache(tempVar, aQuery);
    }

    public void getClusterList(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDSGroup> list = (ArrayList<VDSGroup>) source;
                    Collections.sort(list, new NameableComparator());
                    return list;
                }
                return new ArrayList<VDSGroup>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVdsGroupsByStoragePoolId,
                new IdQueryParameters(dataCenterId),
                aQuery);
    }

    public List<VDSGroup> filterByArchitecture(List<VDSGroup> clusters, ArchitectureType targetArchitecture) {
        List<VDSGroup> filteredClusters = new ArrayList<VDSGroup>();

        for (VDSGroup cluster : clusters) {
            if (cluster.getArchitecture().equals(targetArchitecture)) {
                filteredClusters.add(cluster);
            }
        }
        return filteredClusters;
    }

    public List<VDSGroup> filterClustersWithoutArchitecture(List<VDSGroup> clusters) {
        List<VDSGroup> filteredClusters = new ArrayList<VDSGroup>();

        for (VDSGroup cluster : clusters) {
            if (cluster.getArchitecture() != ArchitectureType.undefined) {
                filteredClusters.add(cluster);
            }
        }
        return filteredClusters;
    }

    public void getClusterByServiceList(AsyncQuery aQuery, Guid dataCenterId,
            final boolean supportsVirtService, final boolean supportsGlusterService) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<VDSGroup>();
                }
                final ArrayList<VDSGroup> list = (ArrayList<VDSGroup>) source;
                return getClusterByServiceList(list, supportsVirtService, supportsGlusterService);
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVdsGroupsByStoragePoolId,
                new IdQueryParameters(dataCenterId),
                aQuery);
    }

    public void isSoundcardEnabled(AsyncQuery aQuery, Guid vmId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    return !((List<?>) source).isEmpty();
                }

                return false;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetSoundDevices, new IdQueryParameters(vmId), aQuery);
    }

    public void isVirtioScsiEnabledForVm(AsyncQuery aQuery, Guid vmId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    return !((List<?>) source).isEmpty();
                }

                return false;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVirtioScsiControllers, new IdQueryParameters(vmId), aQuery);
    }

    public void getClusterListByService(AsyncQuery aQuery, final boolean supportsVirtService,
            final boolean supportsGlusterService) {

        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDSGroup> list =
                            getClusterByServiceList((ArrayList<VDSGroup>) source,
                                    supportsVirtService,
                                    supportsGlusterService);
                    Collections.sort(list, new NameableComparator());
                    return list;
                }
                return new ArrayList<VDSGroup>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllVdsGroups, new VdcQueryParametersBase(), aQuery);
    }

    public void getClusterList(AsyncQuery aQuery) {
        getClusterList(aQuery, true);
    }

    public void getClusterList(AsyncQuery aQuery, boolean doRefresh) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDSGroup> list = (ArrayList<VDSGroup>) source;
                    Collections.sort(list, new NameableComparator());
                    return list;
                }
                return new ArrayList<VDSGroup>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllVdsGroups, doRefresh ? new VdcQueryParametersBase() :
                new VdcQueryParametersBase().withoutRefresh(), aQuery);
    }

    public void getTemplateDiskList(AsyncQuery aQuery, Guid templateId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<DiskImage>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmTemplatesDisks, new IdQueryParameters(templateId), aQuery);
    }

    /**
     * Round the priority to the closest value from n (3 for now) values
     *
     * i.e.: if priority entered is 30 and the predefined values are 1,50,100
     *
     * then the return value will be 50 (closest to 50).
     *
     * @param priority
     *            - the current priority of the vm
     * @param maxPriority
     *            - the max priority
     * @return the rounded priority
     */
    public int getRoundedPriority(int priority, int maxPriority) {

        int medium = maxPriority / 2;

        int[] levels = new int[] { 1, medium, maxPriority };

        for (int i = 0; i < levels.length; i++) {
            int lengthToLess = levels[i] - priority;
            int lengthToMore = levels[i + 1] - priority;

            if (lengthToMore < 0) {
                continue;
            }

            return Math.abs(lengthToLess) < lengthToMore ? levels[i] : levels[i + 1];
        }

        return 0;
    }

    public void getTemplateListByDataCenter(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new TemplateConverter();
        Frontend.getInstance().runQuery(VdcQueryType.GetVmTemplatesByStoragePoolId,
                new IdQueryParameters(dataCenterId),
                aQuery);
    }

    public void getTemplateListByStorage(AsyncQuery aQuery, Guid storageId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<VmTemplate> list = new ArrayList<VmTemplate>();
                if (source != null) {
                    for (VmTemplate template : (ArrayList<VmTemplate>) source) {
                        if (template.getStatus() == VmTemplateStatus.OK) {
                            list.add(template);
                        }
                    }

                    Collections.sort(list, new NameableComparator());
                }

                return list;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmTemplatesFromStorageDomain,
                new IdQueryParameters(storageId),
                aQuery);
    }

    public ArrayList<VmTemplate> filterTemplatesByArchitecture(List<VmTemplate> list,
            ArchitectureType architecture) {
        ArrayList<VmTemplate> filteredList = new ArrayList<VmTemplate>();

        for (VmTemplate template : list) {
            if (template.getId().equals(Guid.Empty) ||
                    template.getClusterArch().equals(architecture)) {
                filteredList.add(template);
            }
        }

        return filteredList;
    }

    public void getNumOfMonitorList(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<Integer> nums = new ArrayList<Integer>();
                if (source != null) {
                    Iterable numEnumerable = (Iterable) source;
                    Iterator numIterator = numEnumerable.iterator();
                    while (numIterator.hasNext()) {
                        nums.add(Integer.parseInt(numIterator.next().toString()));
                    }
                }
                return nums;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.ValidNumOfMonitors,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getStorageDomainList(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<StorageDomain>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainsByStoragePoolId,
                new IdQueryParameters(dataCenterId),
                aQuery);
    }

    public void getMaxVmPriority(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return 100;
                }
                return source;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.VmPriorityMaxValue,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getHostById(AsyncQuery aQuery, Guid id) {

        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVdsByVdsId, new IdQueryParameters(id).withoutRefresh(), aQuery);
    }

    public void getHostListByCluster(AsyncQuery aQuery, String clusterName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDS> list = Linq.<VDS> cast((List<IVdcQueryable>) source);
                    return list;
                }

                return new ArrayList<VDS>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.Search, new SearchParameters("Host: cluster = " + clusterName + " sortby name", //$NON-NLS-1$ //$NON-NLS-2$
                SearchType.VDS), aQuery);
    }

    public void getHostListByClusterId(AsyncQuery aQuery, Guid clusterId) {
        Frontend.getInstance().runQuery(VdcQueryType.GetHostsByClusterId, new IdQueryParameters(clusterId), aQuery);
    }

    public void getHostListByDataCenter(AsyncQuery aQuery, Guid spId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    return Linq.<VDS> cast((List<?>) source);
                }

                return new ArrayList<VDS>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllVdsByStoragePool, new IdQueryParameters(spId), aQuery);
    }

    public void getHostDevicesByHostId(AsyncQuery aQuery, Guid hostId) {
        Frontend.getInstance().runQuery(VdcQueryType.GetExtendedHostDevicesByHostId, new IdQueryParameters(hostId), aQuery);
    }

    public void getConfiguredVmHostDevices(AsyncQuery aQuery, Guid vmId) {
        Frontend.getInstance().runQuery(VdcQueryType.GetVmHostDevices, new IdQueryParameters(vmId), aQuery);
    }

    public void getVmDiskList(AsyncQuery aQuery, Guid vmId, boolean isRefresh) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    return source;
                }
                return new ArrayList<DiskImage>();
            }
        };
        IdQueryParameters params = new IdQueryParameters(vmId);
        params.setRefresh(isRefresh);
        Frontend.getInstance().runQuery(VdcQueryType.GetAllDisksByVmId, params, aQuery);
    }

    public HashMap<Integer, String> getOsUniqueOsNames() {
        return uniqueOsNames;
    }

    public void getAAAProfilesList(AsyncQuery aQuery) {
        convertAAAProfilesResult(aQuery, false);
        Frontend.getInstance().runQuery(VdcQueryType.GetAAAProfileList, new VdcQueryParametersBase(), aQuery);
    }

    public void getAAANamespaces(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (HashMap<String, List<String>>) source : new HashMap<String, List<String>>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAvailableNamespaces, new VdcQueryParametersBase(), aQuery);
    }


    public void getAAAProfilesEntriesList(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (Collection<ProfileEntry>) source : new ArrayList<ProfileEntry>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAAAProfileList, new VdcQueryParametersBase(), aQuery);
    }

    public void getRoleList(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<Role>) source : new ArrayList<Role>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllRoles, new MultilevelAdministrationsQueriesParameters(), aQuery);
    }

    public void getStorageDomainById(AsyncQuery aQuery, Guid storageDomainId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (StorageDomain) source : null;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainById,
                new IdQueryParameters(storageDomainId),
                aQuery);
    }

    public void getStorageDomainByName(AsyncQuery aQuery, String storageDomainName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainByName,
                new NameQueryParameters(storageDomainName),
                aQuery);
    }

    public VolumeFormat getDiskVolumeFormat(VolumeType volumeType, StorageType storageType) {
        if (storageType.isFileDomain()) {
            return VolumeFormat.RAW;
        } else if (storageType.isBlockDomain()) {
            switch (volumeType) {
            case Sparse:
                return VolumeFormat.COW;

            case Preallocated:
                return VolumeFormat.RAW;

            default:
                return VolumeFormat.Unassigned;
            }
        } else {
            return VolumeFormat.Unassigned;
        }
    }

    public VolumeType getTemplateVolumeType(VolumeFormat volumeFormat, StorageType storageType) {
        switch (volumeFormat) {
        case COW:
            return VolumeType.Sparse;
        case RAW:
        default:
            return storageType.isFileDomain() ? VolumeType.Sparse : VolumeType.Preallocated;
        }
    }

    public void getClusterNetworkList(AsyncQuery aQuery, Guid clusterId) {
        // do not replace a converter = just add if none provided
        if (aQuery.converterCallback == null) {
            aQuery.converterCallback = new IAsyncConverter() {
                @Override
                public Object Convert(Object source, AsyncQuery _asyncQuery) {
                    if (source == null) {
                        return new ArrayList<Network>();
                    }
                    return source;
                }
            };
        }

        Frontend.getInstance().runQuery(VdcQueryType.GetAllNetworksByClusterId, new IdQueryParameters(clusterId), aQuery);
    }

    public void getAllNetworkQos(Guid dcId, AsyncQuery query) {
        query.converterCallback = new IAsyncConverter<List<NetworkQoS>>() {

            @Override
            public List<NetworkQoS> Convert(Object returnValue, AsyncQuery asyncQuery) {
                List<NetworkQoS> qosList = returnValue == null ? new ArrayList<NetworkQoS>() : (List<NetworkQoS>) returnValue;
                qosList.add(0, NetworkQoSModel.EMPTY_QOS);
                return qosList;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllNetworkQosByStoragePoolId, new IdQueryParameters(dcId), query);
    }

    public void getAllHostNetworkQos(Guid dcId, AsyncQuery query) {
        query.converterCallback = new IAsyncConverter<List<HostNetworkQos>>() {

            @Override
            public List<HostNetworkQos> Convert(Object returnValue, AsyncQuery asyncQuery) {
                List<HostNetworkQos> qosList =
                        (returnValue == null) ? new ArrayList<HostNetworkQos>() : (List<HostNetworkQos>) returnValue;
                qosList.add(0, NetworkModel.EMPTY_HOST_NETWORK_QOS);
                return qosList;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllQosByStoragePoolIdAndType,
                new QosQueryParameterBase(dcId, QosType.HOSTNETWORK),
                query);
    }

    public void getDataCenterById(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetStoragePoolById,
                new IdQueryParameters(dataCenterId).withoutRefresh(), aQuery);
    }

    public void getNetworkLabelsByDataCenterId(Guid dataCenterId, AsyncQuery query) {
        query.converterCallback = new IAsyncConverter<SortedSet<String>>() {
            @Override
            public SortedSet<String> Convert(Object returnValue, AsyncQuery asyncQuery) {
                SortedSet<String> sortedSet = new TreeSet<String>(new LexoNumericComparator());
                sortedSet.addAll((Collection<String>) returnValue);
                return sortedSet;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetNetworkLabelsByDataCenterId,
                new IdQueryParameters(dataCenterId),
                query);
    }

    public void getWatchdogByVmId(AsyncQuery aQuery, Guid vmId) {
        Frontend.getInstance().runQuery(VdcQueryType.GetWatchdog, new IdQueryParameters(vmId), aQuery);
    }

    public void getTemplateById(AsyncQuery aQuery, Guid templateId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmTemplate, new GetVmTemplateParameters(templateId), aQuery);
    }

    public void countAllTemplates(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmTemplateCount, new VdcQueryParametersBase(), aQuery);
    }

    public void getHostList(AsyncQuery aQuery) {
        getHostListByStatus(aQuery, null);
    }

    public void getHostList(AsyncQuery aQuery, boolean doRefresh) {
        getHostListByStatus(aQuery, null, doRefresh);
    }

    public void getHostListByStatus(AsyncQuery aQuery, VDSStatus status) {
        getHostListByStatus(aQuery, status, true);
    }

    public void getHostListByStatus(AsyncQuery aQuery, VDSStatus status, boolean doRefresh) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDS> list = Linq.<VDS> cast((Iterable) source);
                    return list;
                }

                return new ArrayList<VDS>();
            }
        };
        SearchParameters searchParameters =
                new SearchParameters("Host: " + (status == null ? "" : ("status=" + status.name())), SearchType.VDS); //$NON-NLS-1$ //$NON-NLS-2$
        searchParameters.setMaxCount(9999);
        Frontend.getInstance().runQuery(VdcQueryType.Search,
                doRefresh ? searchParameters : searchParameters.withoutRefresh(), aQuery);
    }

    public void getHostsForStorageOperation(AsyncQuery aQuery, Guid storagePoolId, boolean localFsOnly) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    return source;
                }

                return new ArrayList<VDS>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetHostsForStorageOperation,
                new GetHostsForStorageOperationParameters(storagePoolId, localFsOnly),
                aQuery);
    }

    public void getVolumeList(AsyncQuery aQuery, boolean doRefresh) {
        getVolumeList(aQuery, null, doRefresh);
    }

    public void getVolumeList(AsyncQuery aQuery, String clusterName) {
        getVolumeList(aQuery, clusterName, true);
    }

    public void getVolumeList(AsyncQuery aQuery, String clusterName, boolean doRefresh) {

        if ((ApplicationModeHelper.getUiMode().getValue() & ApplicationMode.GlusterOnly.getValue()) == 0) {
            aQuery.asyncCallback.onSuccess(aQuery.model, new ArrayList<GlusterVolumeEntity>());
            return;
        }
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<GlusterVolumeEntity> list =
                            (ArrayList<GlusterVolumeEntity>) source;
                    return list;
                }

                return new ArrayList<GlusterVolumeEntity>();
            }
        };
        SearchParameters searchParameters;
        searchParameters =
                clusterName == null ? new SearchParameters("Volumes:", SearchType.GlusterVolume) //$NON-NLS-1$
                        : new SearchParameters("Volumes: cluster.name=" + clusterName, SearchType.GlusterVolume); //$NON-NLS-1$
        searchParameters.setMaxCount(9999);
        if (!doRefresh) {
            searchParameters.withoutRefresh();
        }
        Frontend.getInstance().runQuery(VdcQueryType.Search, searchParameters, aQuery);
    }

    public void getGlusterVolumeOptionInfoList(AsyncQuery aQuery, Guid clusterId) {
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeOptionsInfo, new GlusterParameters(clusterId), aQuery);
    }

    public void getHostFingerprint(AsyncQuery aQuery, String hostAddress) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (String) source : ""; //$NON-NLS-1$
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetServerSSHKeyFingerprint, new ServerParameters(hostAddress), aQuery);
    }

    public void getHostPublicKey(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (String) source : ""; //$NON-NLS-1$
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetServerSSHPublicKey, new VdcQueryParametersBase(), aQuery);
    }

    public void getGlusterHosts(AsyncQuery aQuery, String hostAddress, String rootPassword, String fingerprint) {
        GlusterServersQueryParameters parameters = new GlusterServersQueryParameters(hostAddress, rootPassword);
        parameters.setFingerprint(fingerprint);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterServersForImport,
                parameters,
                aQuery);
    }

    public void getClusterGlusterServices(AsyncQuery aQuery, Guid clusterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        // Passing empty values for Volume and Brick to get the services of all the volumes/hosts in the cluster
        GlusterVolumeAdvancedDetailsParameters parameters =
                new GlusterVolumeAdvancedDetailsParameters(clusterId, null, null, false); //$NON-NLS-1$ //$NON-NLS-2$
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeAdvancedDetails,
                parameters,
                aQuery);
    }

    public void getGlusterVolumeBrickDetails(AsyncQuery aQuery, Guid clusterId, Guid volumeId, Guid brickId) {
        GlusterVolumeAdvancedDetailsParameters parameters =
                new GlusterVolumeAdvancedDetailsParameters(clusterId, volumeId, brickId, true);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeAdvancedDetails,
                parameters,
                aQuery);
    }

    public void getGlusterHostsNewlyAdded(AsyncQuery aQuery, Guid clusterId, boolean isFingerprintRequired) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAddedGlusterServers,
                new AddedGlusterServersParameters(clusterId, isFingerprintRequired),
                aQuery);
    }

    public void isAnyHostUpInCluster(AsyncQuery aQuery, String clusterName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null && !((List<?>) source).isEmpty()) {
                    return true;
                }
                return false;
            }
        };
        getUpHostListByCluster(aQuery, clusterName, 1);
    }

    public void getGlusterHooks(AsyncQuery aQuery, Guid clusterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : new ArrayList<GlusterHookEntity>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterHooks, new GlusterParameters(clusterId), aQuery);
    }

    public void getGlusterBricksForServer(AsyncQuery aQuery, Guid serverId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : new ArrayList<GlusterBrickEntity>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeBricksByServerId, new IdQueryParameters(serverId), aQuery);
    }

    public void getGlusterVolumeGeoRepStatusForMasterVolume(AsyncQuery aQuery, Guid masterVolumeId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery asyncQuery) {
                return source != null ? source : new ArrayList<GlusterGeoRepSession>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeGeoRepSessions, new IdQueryParameters(masterVolumeId), aQuery);
    }

    public void getGlusterVolumeGeoRepRecommendationViolations(AsyncQuery aQuery,
            Guid masterVolumeId,
            Guid slaveVolumeId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object returnValue, AsyncQuery asyncQuery) {
                return returnValue == null ? new ArrayList<GlusterGeoRepNonEligibilityReason>()
                        : (List<GlusterGeoRepNonEligibilityReason>) returnValue;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetNonEligibilityReasonsOfVolumeForGeoRepSession,
                new GlusterVolumeGeoRepEligibilityParameters(masterVolumeId, slaveVolumeId),
                aQuery);
    }

    public void getGlusterVolumeSnapshotsForVolume(AsyncQuery aQuery, Guid volumeId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery asyncQuery) {
                return source != null ? source : new ArrayList<GlusterVolumeSnapshotEntity>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeSnapshotsByVolumeId, new IdQueryParameters(volumeId), aQuery);
    }

    public void getVolumeSnapshotSchedule(AsyncQuery aQuery, Guid volumeId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeSnapshotScheduleByVolumeId,
                new IdQueryParameters(volumeId),
                aQuery);
    }

    public void getIsGlusterVolumeSnapshotCliScheduleEnabled(AsyncQuery aQuery, Guid clusterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeSnapshotCliScheduleFlag,
                new IdQueryParameters(clusterId),
                aQuery);
    }

    public void getGlusterHook(AsyncQuery aQuery, Guid hookId, boolean includeServerHooks) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterHookById,
                new GlusterHookQueryParameters(hookId, includeServerHooks),
                aQuery);
    }

    public void getGlusterHookContent(AsyncQuery aQuery, Guid hookId, Guid serverId) {

        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : ""; //$NON-NLS-1$
            }
        };
        GlusterHookContentQueryParameters parameters = new GlusterHookContentQueryParameters(hookId);
        parameters.setGlusterServerId(serverId);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterHookContent, parameters, aQuery);
    }

    public void getGlusterSwiftServices(AsyncQuery aQuery, Guid serverId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : new ArrayList<GlusterServerService>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterServerServicesByServerId, new GlusterServiceQueryParameters(serverId,
                ServiceType.GLUSTER_SWIFT), aQuery);
    }

    public void getClusterGlusterSwiftService(AsyncQuery aQuery, Guid clusterId) {

        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    List<GlusterClusterService> serviceList = (List<GlusterClusterService>) source;
                    if (!serviceList.isEmpty()) {
                        return serviceList.get(0);
                    }
                    return null;
                }
                else {
                    return source;
                }
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterClusterServiceByClusterId,
                new GlusterServiceQueryParameters(clusterId,
                        ServiceType.GLUSTER_SWIFT), aQuery);
    }

    public void getGlusterSwiftServerServices(AsyncQuery aQuery, Guid clusterId) {

        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : new ArrayList<GlusterServerService>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterServerServicesByClusterId,
                new GlusterServiceQueryParameters(clusterId,
                        ServiceType.GLUSTER_SWIFT), aQuery);
    }

    public void getGlusterRebalanceStatus(AsyncQuery aQuery, Guid clusterId, Guid volumeId) {
        aQuery.setHandleFailure(true);
        GlusterVolumeQueriesParameters parameters = new GlusterVolumeQueriesParameters(clusterId, volumeId);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeRebalanceStatus, parameters, aQuery);
    }

    public void getGlusterSnapshotConfig(AsyncQuery aQuery, Guid clusterId, Guid volumeId) {
        aQuery.setHandleFailure(true);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeSnapshotConfig, new GlusterVolumeQueriesParameters(clusterId, volumeId), aQuery);
    }

    public void getGlusterVolumeProfilingStatistics(AsyncQuery aQuery, Guid clusterId, Guid volumeId, boolean nfs) {
        aQuery.setHandleFailure(true);
        GlusterVolumeProfileParameters parameters = new GlusterVolumeProfileParameters(clusterId, volumeId, nfs);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeProfileInfo, parameters, aQuery);
    }

    public void getGlusterRemoveBricksStatus(AsyncQuery aQuery,
            Guid clusterId,
            Guid volumeId,
            List<GlusterBrickEntity> bricks) {
        aQuery.setHandleFailure(true);
        GlusterVolumeRemoveBricksQueriesParameters parameters =
                new GlusterVolumeRemoveBricksQueriesParameters(clusterId, volumeId, bricks);
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterVolumeRemoveBricksStatus, parameters, aQuery);
    }

    public void getRpmVersion(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (String) source : ""; //$NON-NLS-1$
            }
        };
        GetConfigurationValueParameters tempVar =
                new GetConfigurationValueParameters(ConfigurationValues.ProductRPMVersion);
        tempVar.setVersion(getDefaultConfigurationVersion());
        getConfigFromCache(tempVar, aQuery);
    }

    public void getUserMessageOfTheDayViaPublic(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (String) source : ""; //$NON-NLS-1$
            }
        };
        Frontend.getInstance().runPublicQuery(VdcQueryType.GetConfigurationValue,
                new GetConfigurationValueParameters(ConfigurationValues.UserMessageOfTheDay,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getSearchResultsLimit(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 100;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.SearchResultsLimit,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public Map<Version, Map<String, String>> getCustomPropertiesList() {
        return customPropertiesList;
    }

    public void getPermissionsByAdElementId(AsyncQuery aQuery, Guid userId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<Permission>) source
                        : new ArrayList<Permission>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetPermissionsByAdElementId,
                new IdQueryParameters(userId),
                aQuery);
    }

    public void getRoleActionGroupsByRoleId(AsyncQuery aQuery, Guid roleId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<ActionGroup>) source
                        : new ArrayList<ActionGroup>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetRoleActionGroupsByRoleId,
                new IdQueryParameters(roleId),
                aQuery);
    }

    public void isTemplateNameUnique(AsyncQuery aQuery, String templateName, Guid datacenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? !((Boolean) source).booleanValue() : false;
            }
        };
        NameQueryParameters params = new NameQueryParameters(templateName);
        params.setDatacenterId(datacenterId);
        Frontend.getInstance().runQuery(VdcQueryType.IsVmTemlateWithSameNameExist,
                params,
                aQuery);
    }

    public void isVmNameUnique(AsyncQuery aQuery, String name, Guid datacenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? !((Boolean) source).booleanValue() : false;
            }
        };
        NameQueryParameters params = new NameQueryParameters(name);
        params.setDatacenterId(datacenterId);
        Frontend.getInstance().runQuery(VdcQueryType.IsVmWithSameNameExist, params, aQuery);
    }

    public void getDataCentersWithPermittedActionOnClusters(AsyncQuery aQuery, ActionGroup actionGroup,
            final boolean supportsVirtService, final boolean supportsGlusterService) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<StoragePool>();
                }
                return source;
            }
        };

        GetDataCentersWithPermittedActionOnClustersParameters getDataCentersWithPermittedActionOnClustersParameters =
                new GetDataCentersWithPermittedActionOnClustersParameters();
        getDataCentersWithPermittedActionOnClustersParameters.setActionGroup(actionGroup);
        getDataCentersWithPermittedActionOnClustersParameters.setSupportsVirtService(supportsVirtService);
        getDataCentersWithPermittedActionOnClustersParameters.setSupportsGlusterService(supportsGlusterService);

        Frontend.getInstance().runQuery(VdcQueryType.GetDataCentersWithPermittedActionOnClusters,
                getDataCentersWithPermittedActionOnClustersParameters,
                aQuery);
    }

    public void getClustersWithPermittedAction(AsyncQuery aQuery, ActionGroup actionGroup,
            final boolean supportsVirtService, final boolean supportsGlusterService) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDSGroup> list = (ArrayList<VDSGroup>) source;
                    return getClusterByServiceList(list, supportsVirtService, supportsGlusterService);
                }
                return new ArrayList<VDSGroup>();
            }
        };

        GetEntitiesWithPermittedActionParameters getEntitiesWithPermittedActionParameters =
                new GetEntitiesWithPermittedActionParameters();
        getEntitiesWithPermittedActionParameters.setActionGroup(actionGroup);
        Frontend.getInstance().runQuery(VdcQueryType.GetClustersWithPermittedAction, getEntitiesWithPermittedActionParameters, aQuery);
    }

    public void getClustersHavingHosts(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDSGroup> list = (ArrayList<VDSGroup>) source;
                    Collections.sort(list, new NameableComparator());
                    return list;
                }
                return new ArrayList<VDSGroup>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllClustersHavingHosts,
                new VdcQueryParametersBase(),
                aQuery);
    }

    public void getAllVmTemplates(AsyncQuery aQuery, final boolean refresh) {
        aQuery.converterCallback = new TemplateConverter();
        VdcQueryParametersBase params = new VdcQueryParametersBase();
        params.setRefresh(refresh);
        Frontend.getInstance().runQuery(VdcQueryType.GetAllVmTemplates, params, aQuery);
    }

    public void getStorageConnectionById(AsyncQuery aQuery, String id, boolean isRefresh) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (StorageServerConnections) source : null;
            }
        };
        StorageServerConnectionQueryParametersBase params = new StorageServerConnectionQueryParametersBase(id);
        params.setRefresh(isRefresh);
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageServerConnectionById, params, aQuery);
    }

    public void getDataCentersByStorageDomain(AsyncQuery aQuery, Guid storageDomainId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<StoragePool>) source : null;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetStoragePoolsByStorageDomainId,
                new IdQueryParameters(storageDomainId),
                aQuery);
    }

    public void getDataCenterVersions(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<Version>();
                }
                else {
                    ArrayList<Version> list = (ArrayList<Version>) source;
                    Collections.sort(list);
                    return list;
                }
            }
        };
        IdQueryParameters tempVar = new IdQueryParameters(dataCenterId);
        Frontend.getInstance().runQuery(VdcQueryType.GetAvailableClusterVersionsByStoragePool, tempVar, aQuery);
    }

    public void getDataCenterMaxNameLength(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 1;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.StoragePoolNameSizeLimit,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getClusterServerMemoryOverCommit(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 0;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.MaxVdsMemOverCommitForServers,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getClusterDesktopMemoryOverCommit(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 0;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.MaxVdsMemOverCommit,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getAllowClusterWithVirtGlusterEnabled(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : Boolean.TRUE;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.AllowClusterWithVirtGlusterEnabled,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getCPUList(AsyncQuery aQuery, Version version) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<ServerCpu>) source : new ArrayList<ServerCpu>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllServerCpuList,
                new GetAllServerCpuListParameters(version),
                aQuery);
    }

    public void getPmTypeList(AsyncQuery aQuery, Version version) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<String> list = new ArrayList<String>();
                if (source != null) {
                    String[] array = ((String) source).split("[,]", -1); //$NON-NLS-1$
                    for (String item : array) {
                        list.add(item);
                    }
                }
                return list;
            }
        };
        GetConfigurationValueParameters param = new GetConfigurationValueParameters(ConfigurationValues.VdsFenceType);
        param.setVersion(version != null ? version.toString() : getDefaultConfigurationVersion());
        Frontend.getInstance().runQuery(VdcQueryType.GetFenceConfigurationValue, param, aQuery);
    }

    public void getPmOptions(AsyncQuery aQuery, String pmType, String version) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                String pmtype = (String) _asyncQuery.data[0];
                HashMap<String, ArrayList<String>> cachedPmMap =
                        new HashMap<String, ArrayList<String>>();
                HashMap<String, HashMap<String, Object>> dict =
                        (HashMap<String, HashMap<String, Object>>) source;
                for (Map.Entry<String, HashMap<String, Object>> pair : dict.entrySet()) {
                    ArrayList<String> list = new ArrayList<String>();
                    for (Map.Entry<String, Object> p : pair.getValue().entrySet()) {
                        list.add(p.getKey());
                    }

                    cachedPmMap.put(pair.getKey(), list);
                }
                return cachedPmMap.get(pmtype);
            }
        };
        aQuery.setData(new Object[] { pmType });
        Frontend.getInstance().runQuery(VdcQueryType.GetAgentFenceOptions, new GetAgentFenceOptionsQueryParameters(version), aQuery);
    }

    public void getNetworkList(AsyncQuery aQuery, Guid dataCenterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<Network>) source : new ArrayList<Network>();
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetAllNetworks, new IdQueryParameters(dataCenterId), aQuery);
    }

    public void getISOStorageDomainList(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<StorageDomain> allStorageDomains =
                            (ArrayList<StorageDomain>) source;
                    ArrayList<StorageDomain> isoStorageDomains = new ArrayList<StorageDomain>();
                    for (StorageDomain storageDomain : allStorageDomains) {
                        if (storageDomain.getStorageDomainType() == StorageDomainType.ISO) {
                            isoStorageDomains.add(storageDomain);
                        }
                    }
                    return isoStorageDomains;
                }
                return new ArrayList<StorageDomain>();
            }
        };

        SearchParameters searchParams = new SearchParameters("Storage:", SearchType.StorageDomain); //$NON-NLS-1$
        searchParams.setMaxCount(9999);

        Frontend.getInstance().runQuery(VdcQueryType.Search, searchParams, aQuery);
    }

    public void getStorageDomainList(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<StorageDomain>) source
                        : new ArrayList<StorageDomain>();
            }
        };

        SearchParameters searchParams = new SearchParameters("Storage:", SearchType.StorageDomain); //$NON-NLS-1$
        searchParams.setMaxCount(9999);

        Frontend.getInstance().runQuery(VdcQueryType.Search, searchParams, aQuery);
    }

    public void getLocalStorageHost(AsyncQuery aQuery, String dataCenterName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    for (IVdcQueryable item : (List<IVdcQueryable>) source) {
                        return item;
                    }
                }
                return null;
            }
        };
        SearchParameters sp = new SearchParameters("hosts: datacenter=" + dataCenterName, SearchType.VDS); //$NON-NLS-1$
        Frontend.getInstance().runQuery(VdcQueryType.Search, sp, aQuery);
    }

    public void getStorageDomainsByConnection(AsyncQuery aQuery, Guid storagePoolId, String connectionPath) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<StorageDomain>) source : null;
            }
        };

        GetStorageDomainsByConnectionParameters param = new GetStorageDomainsByConnectionParameters();
        param.setConnection(connectionPath);
        if (storagePoolId != null) {
            param.setStoragePoolId(storagePoolId);
        }

        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainsByConnection, param, aQuery);
    }

    public void getExistingStorageDomainList(AsyncQuery aQuery,
            Guid hostId,
            StorageDomainType domainType,
            StorageType storageType,
            String path) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<StorageDomain>) source : null;
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetExistingStorageDomainList, new GetExistingStorageDomainListParameters(hostId,
                storageType,
                domainType,
                path), aQuery);
    }

    public void getStorageDomainMaxNameLength(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 1;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.StorageDomainNameSizeLimit,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void isStorageDomainNameUnique(AsyncQuery aQuery, String name) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<StorageDomain> storageDomains = (ArrayList<StorageDomain>) source;
                    return storageDomains.isEmpty();
                }

                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.Search, new SearchParameters("Storage: name=" + name, //$NON-NLS-1$
                SearchType.StorageDomain), aQuery);
    }

    public void getNetworkConnectivityCheckTimeoutInSeconds(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 120;
            }
        };
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.NetworkConnectivityCheckTimeoutInSeconds,
                        getDefaultConfigurationVersion()),
                aQuery);
    }

    public void getMaxSpmPriority(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 0;
            }
        };

        // GetConfigFromCache(
        // new GetConfigurationValueParameters(ConfigurationValues.HighUtilizationForPowerSave,
        // getDefaultConfigurationVersion()),
        // aQuery);

        aQuery.asyncCallback.onSuccess(aQuery.getModel(), 10);
    }

    public void getDefaultSpmPriority(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Integer) source).intValue() : 0;
            }
        };

        // GetConfigFromCache(
        // new GetConfigurationValueParameters(ConfigurationValues.HighUtilizationForPowerSave,
        // getDefaultConfigurationVersion()),
        // aQuery);

        aQuery.asyncCallback.onSuccess(aQuery.getModel(), 5);
    }

    public void getDefaultPmProxyPreferences(AsyncQuery query) {
        getConfigFromCache(
                new GetConfigurationValueParameters(ConfigurationValues.FenceProxyDefaultPreferences,
                        getDefaultConfigurationVersion()),
                query);
    }

    public void getRootTag(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    Tags tag = (Tags) source;

                    Tags root =
                            new Tags(tag.getdescription(),
                                    tag.getparent_id(),
                                    tag.getIsReadonly(),
                                    tag.gettag_id(),
                                    tag.gettag_name());
                    if (tag.getChildren() != null) {
                        fillTagsRecursive(root, tag.getChildren());
                    }

                    return root;
                }

                return new Tags();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetRootTag, new VdcQueryParametersBase(), aQuery);
    }

    private void setAttachedTagsConverter(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<Tags> ret = new ArrayList<Tags>();
                    for (Tags tags : (ArrayList<Tags>) source) {
                        if (tags.gettype() == TagsType.GeneralTag) {
                            ret.add(tags);
                        }
                    }
                    return ret;
                }

                return new Tags();
            }
        };
    }

    public void getAttachedTagsToVm(AsyncQuery aQuery, Guid id) {
        setAttachedTagsConverter(aQuery);

        Frontend.getInstance().runQuery(VdcQueryType.GetTagsByVmId, new GetTagsByVmIdParameters(id.toString()), aQuery);
    }

    public void getAttachedTagsToUser(AsyncQuery aQuery, Guid id) {
        setAttachedTagsConverter(aQuery);

        Frontend.getInstance().runQuery(VdcQueryType.GetTagsByUserId, new GetTagsByUserIdParameters(id.toString()), aQuery);
    }

    public void getAttachedTagsToUserGroup(AsyncQuery aQuery, Guid id) {
        setAttachedTagsConverter(aQuery);

        Frontend.getInstance().runQuery(VdcQueryType.GetTagsByUserGroupId, new GetTagsByUserGroupIdParameters(id.toString()), aQuery);
    }

    public void getAttachedTagsToHost(AsyncQuery aQuery, Guid id) {
        setAttachedTagsConverter(aQuery);

        Frontend.getInstance().runQuery(VdcQueryType.GetTagsByVdsId, new GetTagsByVdsIdParameters(id.toString()), aQuery);
    }

    public void getoVirtISOsList(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? new ArrayList<RpmVersion>((ArrayList<RpmVersion>) source)
                        : new ArrayList<RpmVersion>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetoVirtISOs, new IdQueryParameters(id), aQuery);
    }

    public void getLunsByVgId(AsyncQuery aQuery, String vgId, Guid vdsId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<LUNs>) source : new ArrayList<LUNs>();
            }
        };
        GetLunsByVgIdParameters params = new GetLunsByVgIdParameters(vgId, vdsId);
        Frontend.getInstance().runQuery(VdcQueryType.GetLunsByVgId, params, aQuery);
    }

    public void getAllTemplatesFromExportDomain(AsyncQuery aQuery, Guid storagePoolId, Guid storageDomainId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : new HashMap<VmTemplate, ArrayList<DiskImage>>();
            }
        };
        GetAllFromExportDomainQueryParameters getAllFromExportDomainQueryParamenters =
                new GetAllFromExportDomainQueryParameters(storagePoolId, storageDomainId);
        Frontend.getInstance().runQuery(VdcQueryType.GetTemplatesFromExportDomain, getAllFromExportDomainQueryParamenters, aQuery);
    }

    public void getUpHostListByCluster(AsyncQuery aQuery, String clusterName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? source : Collections.emptyList();
            }
        };
        getUpHostListByCluster(aQuery, clusterName, null);
    }

    public void getUpHostListByCluster(AsyncQuery aQuery, String clusterName, Integer maxCount) {
        SearchParameters searchParameters =
                new SearchParameters("Host: cluster = " + clusterName + " and status = up", SearchType.VDS); //$NON-NLS-1$ //$NON-NLS-2$
        if (maxCount != null) {
            searchParameters.setMaxCount(maxCount);
        }
        Frontend.getInstance().runQuery(VdcQueryType.Search, searchParameters, aQuery);
    }

    public void getVmNicList(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? new ArrayList<VmNetworkInterface>((ArrayList<VmNetworkInterface>) source)
                        : new ArrayList<VmNetworkInterface>();
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetVmInterfacesByVmId, new IdQueryParameters(id), aQuery);
    }

    public void getTemplateNicList(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? new ArrayList<VmNetworkInterface>((ArrayList<VmNetworkInterface>) source)
                        : new ArrayList<VmNetworkInterface>();
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetTemplateInterfacesByTemplateId, new IdQueryParameters(id), aQuery);
    }

    public void getVmSnapshotList(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<Snapshot>) source : new ArrayList<Snapshot>();
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetAllVmSnapshotsByVmId, new IdQueryParameters(id), aQuery);
    }

    public void getVmsRunningOnOrMigratingToVds(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<VM>();
                }
                return source;
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetVmsRunningOnOrMigratingToVds,
                new IdQueryParameters(id),
                aQuery);
    }

    public void getVmsFromExternalServer(AsyncQuery aQuery, Guid dataCenterId, Guid vdsId,
            String url, String username, String password) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<VM>();
                }
                return source;
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetVmsFromExternalProvider,
                new GetVmsFromExternalProviderQueryParameters(url, username, password, vdsId, dataCenterId),
                aQuery);
    }

    public void getVmFromOva(AsyncQuery aQuery, Guid vdsId, String path) {
        aQuery.setHandleFailure(true);
        Frontend.getInstance().runQuery(
                VdcQueryType.GetVmFromOva,
                new GetVmFromOvaQueryParameters(vdsId, path),
                aQuery);
    }

    public void getVmDiskList(AsyncQuery aQuery, Guid id) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<Disk> list = new ArrayList<Disk>();
                if (source != null) {
                    Iterable listEnumerable = (Iterable) source;
                    Iterator listIterator = listEnumerable.iterator();
                    while (listIterator.hasNext()) {
                        list.add((Disk) listIterator.next());
                    }
                }
                return list;
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetAllDisksByVmId, new IdQueryParameters(id).withoutRefresh(), aQuery);
    }

    public void getVmList(AsyncQuery aQuery, String poolName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<VM> vms = Linq.<VM> cast((ArrayList<IVdcQueryable>) source);
                return vms;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.Search, new SearchParameters("Vms: pool=" + poolName, SearchType.VM), aQuery); //$NON-NLS-1$
    }

    public void getVmListByClusterName(AsyncQuery aQuery, String clusterName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<VM> vms = Linq.<VM> cast((ArrayList<IVdcQueryable>) source);
                return vms;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.Search,
                new SearchParameters("Vms: cluster=" + clusterName, SearchType.VM), aQuery); //$NON-NLS-1$
    }

    public void getDiskList(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<DiskImage>) source : new ArrayList<DiskImage>();
            }
        };

        SearchParameters searchParams = new SearchParameters("Disks:", SearchType.Disk); //$NON-NLS-1$
        searchParams.setMaxCount(9999);

        Frontend.getInstance().runQuery(VdcQueryType.Search, searchParams, aQuery);
    }

    public void getNextAvailableDiskAliasNameByVMId(AsyncQuery aQuery, Guid vmId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetNextAvailableDiskAliasNameByVMId,
                new IdQueryParameters(vmId),
                aQuery);
    }

    public void isPoolNameUnique(AsyncQuery aQuery, String name) {

        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    return !(Boolean) source;
                }

                return false;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.IsVmPoolWithSameNameExists,
                new NameQueryParameters(name),
                aQuery);
    }

    public void getVmConfigurationBySnapshot(AsyncQuery aQuery, Guid snapshotSourceId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (VM) source : null;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmConfigurationBySnapshot,
                new IdQueryParameters(snapshotSourceId).withoutRefresh(),
                aQuery);
    }

    public void getAllAttachableDisks(AsyncQuery aQuery, Guid storagePoolId, Guid vmId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<Disk>) source : new ArrayList<Disk>();
            }
        };
        GetAllAttachableDisksForVmQueryParameters params = new GetAllAttachableDisksForVmQueryParameters(storagePoolId);
        params.setVmId(vmId);
        Frontend.getInstance().runQuery(VdcQueryType.GetAllAttachableDisksForVm, params, aQuery);
    }

    public void getFilteredAttachableDisks(AsyncQuery aQuery, Guid storagePoolId, Guid vmId, int os, Version compatibilityVersion) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (ArrayList<Disk>) source : new ArrayList<Disk>();
            }
        };
        GetFilteredAttachableDisksParameters params = new GetFilteredAttachableDisksParameters(storagePoolId);
        params.setVmId(vmId);
        params.setOs(os);
        params.setVdsGroupCompatibilityVersion(compatibilityVersion);
        Frontend.getInstance().runQuery(VdcQueryType.GetFilteredAttachableDisks, params, aQuery);
    }

    public void getPermittedStorageDomainsByStoragePoolId(AsyncQuery aQuery,
            Guid dataCenterId,
            ActionGroup actionGroup) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new java.util.ArrayList<StorageDomain>();
                }
                return source;
            }
        };
        GetPermittedStorageDomainsByStoragePoolIdParameters params =
                new GetPermittedStorageDomainsByStoragePoolIdParameters();

        params.setStoragePoolId(dataCenterId);
        params.setActionGroup(actionGroup);

        Frontend.getInstance().runQuery(VdcQueryType.GetPermittedStorageDomainsByStoragePoolId, params, aQuery);
    }

    public void getStorageDomainDefaultWipeAfterDelete(AsyncQuery aQuery, StorageType storageType) {
        aQuery.converterCallback = new IAsyncConverter<Boolean>() {
            @Override public Boolean Convert(Object returnValue, AsyncQuery asyncQuery) {
                return (Boolean) returnValue;
            }
        };
        GetStorageDomainDefaultWipeAfterDeleteParameters params =
                new GetStorageDomainDefaultWipeAfterDeleteParameters(storageType);
        Frontend.getInstance().runQuery(VdcQueryType.GetStorageDomainDefaultWipeAfterDelete, params, aQuery);
    }

    public void getAllDataCenterNetworks(AsyncQuery aQuery, Guid storagePoolId) {
        aQuery.converterCallback = new ListAsyncConverter();
        IdQueryParameters params = new IdQueryParameters(storagePoolId);
        Frontend.getInstance().runQuery(VdcQueryType.GetNetworksByDataCenterId, params, aQuery);
    }

    public void getManagementNetworkCandidates(AsyncQuery aQuery, Guid storagePoolId) {
        aQuery.converterCallback = new ListAsyncConverter();
        IdQueryParameters params = new IdQueryParameters(storagePoolId);
        Frontend.getInstance().runQuery(VdcQueryType.GetManagementNetworkCandidates, params, aQuery);
    }

    public void getStorageConnectionsByDataCenterIdAndStorageType(AsyncQuery aQuery,
                                                                         Guid storagePoolId,
                                                                         StorageType storageType) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        GetConnectionsByDataCenterAndStorageTypeParameters params = new GetConnectionsByDataCenterAndStorageTypeParameters(storagePoolId, storageType);
        Frontend.getInstance().runQuery(VdcQueryType.GetConnectionsByDataCenterAndStorageType, params, aQuery);
    }

    private HashMap<VdcActionType, CommandVersionsInfo> cachedCommandsCompatibilityVersions;

    public void isCommandCompatible(AsyncQuery aQuery, final VdcActionType vdcActionType,
            final Version cluster, final Version dc) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                cachedCommandsCompatibilityVersions = (HashMap<VdcActionType, CommandVersionsInfo>) source;
                return isCommandCompatible(vdcActionType, cluster, dc);
            }
        };

        if (cachedCommandsCompatibilityVersions != null) {
            aQuery.asyncCallback.onSuccess(aQuery.getModel(), isCommandCompatible(vdcActionType, cluster, dc));
        } else {
            Frontend.getInstance().runQuery(VdcQueryType.GetCommandsCompatibilityVersions,
                    new VdcQueryParametersBase().withoutRefresh(), aQuery);
        }
    }

    private boolean isCommandCompatible(VdcActionType vdcActionType, Version cluster, Version dc) {
        if (cachedCommandsCompatibilityVersions == null || cluster == null || dc == null) {
            return false;
        }

        CommandVersionsInfo commandVersionsInfo = cachedCommandsCompatibilityVersions.get(vdcActionType);
        if (commandVersionsInfo == null) {
            return false;
        }

        Version clusterCompatibility = commandVersionsInfo.getClusterVersion();
        Version dcCompatibility = commandVersionsInfo.getStoragePoolVersion();

        return (clusterCompatibility.compareTo(cluster) <= 0)
                && (dcCompatibility.compareTo(dc) <= 0);
    }

    public CommandVersionsInfo getCommandVersionsInfo(VdcActionType vdcActionType) {
        if (cachedCommandsCompatibilityVersions == null) {
            return null;
        }

        return cachedCommandsCompatibilityVersions.get(vdcActionType);
    }

    /**
     * Cache configuration values [raw (not converted) values from vdc_options table].
     */
    private void cacheConfigValues(AsyncQuery aQuery) {
        getDefaultConfigurationVersion();
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object returnValue, AsyncQuery _asyncQuery) {
                if (returnValue != null) {
                    cachedConfigValuesPreConvert.putAll((HashMap<KeyValuePairCompat<ConfigurationValues, String>, Object>) returnValue);
                }
                return cachedConfigValuesPreConvert;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetConfigurationValues, new VdcQueryParametersBase(), aQuery);
    }

    /**
     * Get configuration value from 'cachedConfigValuesPreConvert' (raw values from vdc_options table).
     *
     * @param version
     */
    public Object getConfigValuePreConverted(ConfigurationValues configValue, String version) {
        KeyValuePairCompat<ConfigurationValues, String> key =
                new KeyValuePairCompat<ConfigurationValues, String>(configValue, version);

        return cachedConfigValuesPreConvert.get(key);
    }


    public boolean supportedForUnitVmModel(ConfigurationValues feature, UnitVmModel model) {
        if (model.getSelectedCluster() == null || model.getSelectedCluster().getCompatibilityVersion() == null) {
            return false;
        }

        String version = model.getSelectedCluster().getCompatibilityVersion().getValue();
        return (Boolean) AsyncDataProvider.getInstance().getConfigValuePreConverted(feature, version);
    }

    /**
     * Get configuration value from 'cachedConfigValuesPreConvert' (raw values from vdc_options table).
     */
    public Object getConfigValuePreConverted(ConfigurationValues configValue) {
        KeyValuePairCompat<ConfigurationValues, String> key =
                new KeyValuePairCompat<ConfigurationValues, String>(configValue, getDefaultConfigurationVersion());

        return cachedConfigValuesPreConvert.get(key);
    }

    /**
     * Get configuration value from using a specified converter.
     */
    public Object getConfigValue(ConfigurationValues configValue, String version, IAsyncConverter converter) {
        if (converter == null) {
            return null;
        }

        KeyValuePairCompat<ConfigurationValues, String> key =
                new KeyValuePairCompat<ConfigurationValues, String>(configValue, version);

        return converter.Convert(cachedConfigValuesPreConvert.get(key), null);
    }

    /**
     * method to get an item from config while caching it (config is not supposed to change during a session)
     *
     * @param aQuery
     *            an async query
     * @param parameters
     *            a converter for the async query
     */
    public void getConfigFromCache(GetConfigurationValueParameters parameters, AsyncQuery aQuery) {
        // cache key
        final KeyValuePairCompat<ConfigurationValues, String> config_key =
                new KeyValuePairCompat<ConfigurationValues, String>(parameters.getConfigValue(),
                        parameters.getVersion());

        Object returnValue = null;

        if (cachedConfigValues.containsKey(config_key)) {
            // cache hit
            returnValue = cachedConfigValues.get(config_key);
        }
        // cache miss: convert configuration value using query's converter
        // and call asyncCallback's onSuccess
        else if (cachedConfigValuesPreConvert.containsKey(config_key)) {
            returnValue = cachedConfigValuesPreConvert.get(config_key);

            // run converter
            if (aQuery.converterCallback != null) {
                returnValue = aQuery.converterCallback.Convert(returnValue, aQuery);
            }
            if (returnValue != null) {
                cachedConfigValues.put(config_key, returnValue);
            }
        }
        aQuery.asyncCallback.onSuccess(aQuery.getModel(), returnValue);
    }

    /**
     * method to get an item from config while caching it (config is not supposed to change during a session)
     *
     * @param configValue
     *            the config value to query
     * @param version
     *            the compatibility version to query
     * @param aQuery
     *            an async query
     */
    public void getConfigFromCache(ConfigurationValues configValue, String version, AsyncQuery aQuery) {
        GetConfigurationValueParameters parameters = new GetConfigurationValueParameters(configValue, version);
        getConfigFromCache(parameters, aQuery);
    }

    public ArrayList<QuotaEnforcementTypeEnum> getQuotaEnforcmentTypes() {
        return new ArrayList<QuotaEnforcementTypeEnum>(Arrays.asList(new QuotaEnforcementTypeEnum[] {
                QuotaEnforcementTypeEnum.DISABLED,
                QuotaEnforcementTypeEnum.SOFT_ENFORCEMENT,
                QuotaEnforcementTypeEnum.HARD_ENFORCEMENT }));
    }

    public void clearCache() {
        cachedConfigValues.clear();
    }

    private static class TemplateConverter implements IAsyncConverter {

        @Override
        public Object Convert(Object source, AsyncQuery asyncQuery) {
            List<VmTemplate> list = new ArrayList<VmTemplate>();
            if (source != null) {
                VmTemplate blankTemplate = null;
                for (VmTemplate template : (List<VmTemplate>) source) {
                    if (template.getId().equals(Guid.Empty)) {
                        blankTemplate = template;
                    } else if (template.getStatus() == VmTemplateStatus.OK) {
                        list.add(template);
                    }
                }

                Collections.sort(list, new NameableComparator());
                if (blankTemplate != null) {
                    list.add(0, blankTemplate);
                }
            }

            return list;
        }
    }

    public void getInterfaceOptionsForEditNetwork(final AsyncQuery asyncQuery,
            final ArrayList<VdsNetworkInterface> interfaceList,
            final VdsNetworkInterface originalInterface,
            Network networkToEdit,
            final Guid vdsID,
            final StringBuilder defaultInterfaceName) {
        final ArrayList<VdsNetworkInterface> ifacesOptions = new ArrayList<VdsNetworkInterface>();
        for (VdsNetworkInterface i : interfaceList) {
            if (StringHelper.isNullOrEmpty(i.getNetworkName()) && StringHelper.isNullOrEmpty(i.getBondName())) {
                ifacesOptions.add(i);
            }
        }

        if (originalInterface.getVlanId() == null) { // no vlan:
            // Filter out the Interfaces that have child vlan Interfaces
            getAllChildVlanInterfaces(vdsID, ifacesOptions, new IFrontendMultipleQueryAsyncCallback() {

                @Override
                public void executed(FrontendMultipleQueryAsyncResult result) {

                    ArrayList<VdsNetworkInterface> ifacesOptionsTemp = new ArrayList<VdsNetworkInterface>();
                    List<VdcQueryReturnValue> returnValueList = result.getReturnValues();

                    for (int i = 0; i < returnValueList.size(); i++) {
                        VdcQueryReturnValue returnValue = returnValueList.get(i);
                        if (returnValue != null && returnValue.getSucceeded() && returnValue.getReturnValue() != null) {
                            ArrayList<VdsNetworkInterface> childVlanInterfaces =
                                    returnValue.getReturnValue();

                            if (childVlanInterfaces.size() == 0) {
                                ifacesOptionsTemp.add(ifacesOptions.get(i));
                            }
                        }
                    }

                    ifacesOptions.clear();
                    ifacesOptions.addAll(ifacesOptionsTemp);

                    if (originalInterface.getBonded() != null && originalInterface.getBonded()) {
                        // eth0 -- \
                        // |---> bond0 -> <networkToEdit>
                        // eth1 -- /
                        // ---------------------------------------
                        // - originalInterface: 'bond0'
                        // --> We want to add 'eth0' and and 'eth1' as optional Interfaces
                        // (note that choosing one of them will break the bond):
                        for (VdsNetworkInterface i : interfaceList) {
                            if (ObjectUtils.objectsEqual(i.getBondName(), originalInterface.getName())) {
                                ifacesOptions.add(i);
                            }
                        }
                    }

                    // add the original interface as an option and set it as the default option:
                    ifacesOptions.add(originalInterface);
                    defaultInterfaceName.append(originalInterface.getName());

                    asyncQuery.asyncCallback.onSuccess(asyncQuery.model, ifacesOptions);
                }
            });

        }

        else { // vlan:
            getVlanParentInterface(vdsID, originalInterface, new AsyncQuery(asyncQuery, new INewAsyncCallback() {

                @Override
                public void onSuccess(Object model, Object returnValue) {
                    final VdsNetworkInterface vlanParent = (VdsNetworkInterface) returnValue;

                    if (vlanParent != null && vlanParent.getBonded() != null && vlanParent.getBonded()) {
                        interfaceHasSiblingVlanInterfaces(vdsID, originalInterface, new AsyncQuery(asyncQuery,
                                new INewAsyncCallback() {

                                    @Override
                                    public void onSuccess(Object model, Object returnValue) {
                                        Boolean interfaceHasSiblingVlanInterfaces = (Boolean) returnValue;

                                        if (!interfaceHasSiblingVlanInterfaces) {
                                            // eth0 -- \
                                            // |--- bond0 ---> bond0.3 -> <networkToEdit>
                                            // eth1 -- /
                                            // ---------------------------------------------------
                                            // - originalInterface: 'bond0.3'
                                            // - vlanParent: 'bond0'
                                            // - 'bond0.3' has no vlan siblings
                                            // --> We want to add 'eth0' and and 'eth1' as optional Interfaces.
                                            // (note that choosing one of them will break the bond):
                                            // ifacesOptions.AddRange(interfaceList.Where(a => a.bond_name ==
                                            // vlanParent.name).ToList());
                                            for (VdsNetworkInterface i : interfaceList) {
                                                if (ObjectUtils.objectsEqual(i.getBondName(), vlanParent.getName())) {
                                                    ifacesOptions.add(i);
                                                }
                                            }
                                        }

                                        // the vlanParent should already be in ifacesOptions
                                        // (since it has no network_name or bond_name).
                                        defaultInterfaceName.append(vlanParent.getName());

                                        asyncQuery.asyncCallback.onSuccess(asyncQuery.model, ifacesOptions);

                                    }
                                }));
                    } else {
                        // the vlanParent should already be in ifacesOptions
                        // (since it has no network_name or bond_name).
                        if (vlanParent != null)
                            defaultInterfaceName.append(vlanParent.getName());
                        asyncQuery.asyncCallback.onSuccess(asyncQuery.model, ifacesOptions);
                    }
                }
            }));
        }
    }

    private void getVlanParentInterface(Guid vdsID, VdsNetworkInterface iface, AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVlanParent, new InterfaceAndIdQueryParameters(vdsID,
                iface), aQuery);
    }

    private void interfaceHasSiblingVlanInterfaces(Guid vdsID, VdsNetworkInterface iface, AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                ArrayList<VdsNetworkInterface> siblingVlanInterfaces = (ArrayList<VdsNetworkInterface>) source;
                return !siblingVlanInterfaces.isEmpty();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllSiblingVlanInterfaces,
                new InterfaceAndIdQueryParameters(vdsID, iface), aQuery);

    }

    public void getExternalProviderHostList(AsyncQuery aQuery,
                                                   Guid providerId,
                                                   boolean filterOutExistingHosts,
                                                   String searchFilter) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<VDS>();
                }
                return source;
            }
        };
        GetHostListFromExternalProviderParameters params = new GetHostListFromExternalProviderParameters();
        params.setFilterOutExistingHosts(filterOutExistingHosts);
        params.setProviderId(providerId);
        params.setSearchFilter(searchFilter);
        Frontend.getInstance().runQuery(VdcQueryType.GetHostListFromExternalProvider,
                params,
                aQuery);
    }

    public void getExternalProviderDiscoveredHostList(AsyncQuery aQuery, Provider provider) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<ExternalDiscoveredHost>();
                }
                return source;
            }
        };
        ProviderQueryParameters params = new ProviderQueryParameters();
        params.setProvider(provider);
        Frontend.getInstance().runQuery(VdcQueryType.GetDiscoveredHostListFromExternalProvider, params, aQuery);
    }

    public void getExternalProviderHostGroupList(AsyncQuery aQuery, Provider provider) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<ExternalHostGroup>();
                }
                return source;
            }
        };

        ProviderQueryParameters params = new ProviderQueryParameters();
        params.setProvider(provider);
        Frontend.getInstance().runQuery(VdcQueryType.GetHostGroupsFromExternalProvider, params, aQuery);
    }

    public void getExternalProviderComputeResourceList(AsyncQuery aQuery, Provider provider) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<ExternalComputeResource>();
                }
                return source;
            }
        };

        ProviderQueryParameters params = new ProviderQueryParameters();
        params.setProvider(provider);
        Frontend.getInstance().runQuery(VdcQueryType.GetComputeResourceFromExternalProvider, params, aQuery);
    }

    public void getAllProviders(AsyncQuery aQuery) {
        getAllProviders(aQuery, true);
    }

    public void getAllProviders(AsyncQuery aQuery, boolean doRefresh) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<Provider>();
                }
                Collections.sort((List<Provider>) source, new NameableComparator());
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllProviders, doRefresh ? new GetAllProvidersParameters() :
                new GetAllProvidersParameters().withoutRefresh(), aQuery);
    }

    public void getAllErrata(AsyncQuery aQuery) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<Erratum>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetErrataForSystem, new VdcQueryParametersBase(), aQuery);
    }

    public void getAllProvidersByProvidedEntity(AsyncQuery query, final VdcObjectType providedEntity) {
        query.converterCallback = new IAsyncConverter<List<Provider>>() {
            @Override
            public List<Provider> Convert(Object returnValue, AsyncQuery asyncQuery) {
                if (returnValue == null) {
                    return new ArrayList<Provider>();
                }
                List<Provider> providers =
                        Linq.toList(Linq.filterProvidersByProvidedType((Collection<Provider>) returnValue, providedEntity));
                Collections.sort(providers, new NameableComparator());
                return providers;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllProviders, new GetAllProvidersParameters(), query);
    }

    public void getAllNetworkProviders(AsyncQuery query) {
        getAllProvidersByProvidedEntity(query, VdcObjectType.Network);
    }

    public void getAllProvidersByType(AsyncQuery aQuery, ProviderType providerType) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<Provider>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllProviders, new GetAllProvidersParameters(providerType), aQuery);
    }

    public void getProviderCertificateChain(AsyncQuery aQuery, Provider provider) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return Collections.<CertificateInfo> emptyList();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetProviderCertificateChain,
                new ProviderQueryParameters(provider),
                aQuery);
    }

    private void getAllChildVlanInterfaces(Guid vdsID,
            List<VdsNetworkInterface> ifaces,
            IFrontendMultipleQueryAsyncCallback callback) {
        ArrayList<VdcQueryParametersBase> parametersList = new ArrayList<VdcQueryParametersBase>();
        ArrayList<VdcQueryType> queryTypeList = new ArrayList<VdcQueryType>();
        for (final VdsNetworkInterface iface : ifaces) {
            queryTypeList.add(VdcQueryType.GetAllChildVlanInterfaces);
            parametersList.add(new InterfaceAndIdQueryParameters(vdsID, iface));
        }
        Frontend.getInstance().runMultipleQueries(queryTypeList, parametersList, callback);
    }

    public void isSupportBridgesReportByVDSM(AsyncQuery aQuery, String version) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? ((Boolean) source).booleanValue() : true;
            }
        };
        GetConfigurationValueParameters tempVar =
                new GetConfigurationValueParameters(ConfigurationValues.SupportBridgesReportByVDSM);
        tempVar.setVersion(version);
        getConfigFromCache(tempVar, aQuery);
    }

    public void fillTagsRecursive(Tags tagToFill, List<Tags> children) {
        ArrayList<Tags> list = new ArrayList<Tags>();

        for (Tags tag : children) {
            // tags child = new tags(tag.description, tag.parent_id, tag.IsReadonly, tag.tag_id, tag.tag_name);
            if (tag.gettype() == TagsType.GeneralTag) {
                list.add(tag);
                if (tag.getChildren() != null) {
                    fillTagsRecursive(tag, tag.getChildren());
                }
            }

        }

        tagToFill.setChildren(list);
    }

    public ArrayList<EventNotificationEntity> getEventNotificationTypeList() {
        ArrayList<EventNotificationEntity> ret = new ArrayList<EventNotificationEntity>();
        // TODO: We can translate it here too
        for (EventNotificationEntity entity : EventNotificationEntity.values()) {
            if (entity != EventNotificationEntity.UNKNOWN) {
                ret.add(entity);
            }
        }
        return ret;
    }

    public Map<EventNotificationEntity, HashSet<AuditLogType>> getAvailableNotificationEvents() {
        return VdcEventNotificationUtils.getNotificationEvents();
    }

    public void getNicTypeList(final int osId, Version version, AsyncQuery asyncQuery) {
        final INewAsyncCallback chainedCallback = asyncQuery.asyncCallback;
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                ArrayList<String> nics = ((VdcQueryReturnValue) returnValue).getReturnValue();
                List<VmInterfaceType> interfaceTypes = new ArrayList<VmInterfaceType>();
                for (String nic : nics) {
                    try {
                        interfaceTypes.add(VmInterfaceType.valueOf(nic));
                    } catch (IllegalArgumentException e) {
                        // ignore if we can't find the enum value.
                    }
                }
                chainedCallback.onSuccess(model, interfaceTypes);
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository,
                new OsQueryParameters(OsRepositoryVerb.GetNetworkDevices, osId, version),
                asyncQuery);
    }

    public void getIsPasswordDelegationPossible(AsyncQuery asyncQuery) {
        final INewAsyncCallback chainedCallback = asyncQuery.asyncCallback;
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                chainedCallback.onSuccess(model, ((VdcQueryReturnValue) returnValue).getReturnValue());
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.IsPasswordDelegationPossible,
                new VdcQueryParametersBase(),
                asyncQuery);
    }

    public VmInterfaceType getDefaultNicType(Collection<VmInterfaceType> items) {
        if (items == null || items.isEmpty()) {
            return null;
        } else if (items.contains(VmInterfaceType.pv)) {
            return VmInterfaceType.pv;
        } else {
            return items.iterator().next();
        }
    }

    public boolean isVersionMatchStorageType(Version version, boolean isLocalType) {
        return version.compareTo(new Version(3, 0)) >= 0;
    }

    public int getClusterDefaultMemoryOverCommit() {
        return 100;
    }

    public boolean getClusterDefaultCountThreadsAsCores() {
        return false;
    }

    public ArrayList<VolumeType> getVolumeTypeList() {
        return new ArrayList<VolumeType>(Arrays.asList(new VolumeType[] {
                VolumeType.Preallocated,
                VolumeType.Sparse
        }));
    }

    public ArrayList<VolumeFormat> getVolumeFormats() {
        return new ArrayList<VolumeFormat>(Arrays.asList(
                VolumeFormat.COW,
                VolumeFormat.RAW
                ));
    }

    public ArrayList<StorageType> getStorageTypeList() {
        return new ArrayList<StorageType>(Arrays.asList(new StorageType[] {
                StorageType.ISCSI,
                StorageType.FCP
        }));
    }

    public void getDiskInterfaceList(int osId, Version clusterVersion, AsyncQuery asyncQuery) {
        final INewAsyncCallback chainedCallback = asyncQuery.asyncCallback;
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                ArrayList<String> interfaces = ((VdcQueryReturnValue) returnValue).getReturnValue();
                List<DiskInterface> interfaceTypes = new ArrayList<DiskInterface>();
                for (String diskIfs : interfaces) {
                    try {
                        interfaceTypes.add(DiskInterface.valueOf(diskIfs));
                    } catch (IllegalArgumentException e) {
                        // ignore if we can't find the enum value.
                    }
                }
                chainedCallback.onSuccess(model, interfaceTypes);
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository,
                new OsQueryParameters(OsRepositoryVerb.GetDiskInterfaces, osId, clusterVersion),
                asyncQuery);
    }

    public ArrayList<DiskInterface> getDiskInterfaceList() {
        ArrayList<DiskInterface> diskInterfaces = new ArrayList<DiskInterface>(
                Arrays.asList(new DiskInterface[] {
                        DiskInterface.IDE,
                        DiskInterface.VirtIO,
                        DiskInterface.VirtIO_SCSI,
                        DiskInterface.SPAPR_VSCSI
                }));

        return diskInterfaces;
    }

    public String getNewNicName(Collection<VmNetworkInterface> existingInterfaces) {
        int maxIfaceNumber = 0;
        if (existingInterfaces != null) {
            for (VmNetworkInterface iface : existingInterfaces) {
                // name of Interface is "eth<n>" (<n>: integer).
                if (iface.getName().length() > 3) {
                    final Integer ifaceNumber = IntegerCompat.tryParse(iface.getName().substring(3));
                    if (ifaceNumber != null && ifaceNumber > maxIfaceNumber) {
                        maxIfaceNumber = ifaceNumber;
                    }
                }
            }
        }

        return "nic" + (maxIfaceNumber + 1); //$NON-NLS-1$
    }

    /**
     * Gets a value composed of "[string1]+[string2]+..." and returns "[string1Translated]+[string2Translated]+..."
     *
     * @param complexValue
     *            string in the form of "[string1]+[string2]+..."
     * @return string in the form of "[string1Translated]+[string2Translated]+..."
     */
    public String getComplexValueFromSpiceRedKeysResource(String complexValue) {
        if (StringHelper.isNullOrEmpty(complexValue)) {
            return ""; //$NON-NLS-1$
        }
        ArrayList<String> values = new ArrayList<String>();

        for (String s : complexValue.split("[+]", -1)) { //$NON-NLS-1$
            try {
                String value =
                    SpiceConstantsManager.getInstance()
                    .getSpiceRedKeys()
                    .getString(s.replaceAll("-", "_")); //$NON-NLS-1$ //$NON-NLS-2$
                values.add(value);
            } catch (MissingResourceException e) {
                values.add(s);
            }

        }

        return StringHelper.join("+", values.toArray(new String[]{})); //$NON-NLS-1$
    }

    public <T extends Guid> T getEntityGuid(BusinessEntity<T> entity) {
        return entity.getId();
    }

    public Guid getEntityGuid(Object entity) {
        if (entity instanceof BusinessEntity) {
            //BusinessEntity can have lot of different ID types, but from this context it cannot be determined.
            Object id = ((BusinessEntity<?>) entity).getId();

            //check whether result can be casted to Guid, otherwise continue with explicit rules.
            if (id instanceof Guid) {
                return (Guid) id;
            }
        }

        if (entity instanceof VmPool) {
            return ((VmPool) entity).getVmPoolId();
        } else if (entity instanceof DbUser) {
            return ((DbUser) entity).getId();
        } else if (entity instanceof DbGroup) {
            return ((DbGroup) entity).getId();
        } else {
            return Guid.Empty;
        }
    }

    public boolean isWindowsOsType(Integer osType) {
        // can be null as a consequence of setItems on ListModel
        if (osType == null) {
            return false;
        }

        return windowsOsIds.contains(osType);
    }

    public boolean isLinuxOsType(Integer osId) {
        // can be null as a consequence of setItems on ListModel
        if (osId == null) {
            return false;
        }

        return linuxOsIds.contains(osId);
    }

    public void initWindowsOsTypes() {

        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                windowsOsIds = (ArrayList<Integer>) ((VdcQueryReturnValue) returnValue).getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(OsRepositoryVerb.GetWindowsOss), callback);
    }

    public void initLinuxOsTypes() {

        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                linuxOsIds = (ArrayList<Integer>) ((VdcQueryReturnValue) returnValue).getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(OsRepositoryVerb.GetLinuxOss), callback);
    }

    public void initUniqueOsNames() {

        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                uniqueOsNames = ((VdcQueryReturnValue) returnValue).getReturnValue();
                // Initialize specific UI dependencies for search
                SimpleDependecyInjector.getInstance().bind(new OsValueAutoCompleter(uniqueOsNames));
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(OsRepositoryVerb.GetUniqueOsNames), callback);
    }

    public void initOsNames() {

        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                osNames = ((VdcQueryReturnValue) returnValue).getReturnValue();
                initOsIds();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(OsRepositoryVerb.GetOsNames), callback);
    }

    private void initOsDefaultIconIds() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                final Map<Integer, VmIconIdSizePair> returnMap = ((VdcQueryReturnValue) returnValue).getReturnValue();
                if (returnMap.get(DEFAULT_OS_ID) == null) {
                    throw new RuntimeException("Engine did not provide icon IDs of default OS."); //$NON-NLS-1$
                }
                osIdToDefaultIconIdMap = Collections.unmodifiableMap(returnMap);
                initializeLargeToSmallIconMap();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmIconDefaults, new VdcQueryParametersBase(), callback);
    }

    private void initializeLargeToSmallIconMap() {
        largeToSmallOsDefaultIconIdMap = new HashMap<>();
        for (VmIconIdSizePair pair : osIdToDefaultIconIdMap.values()) {
            largeToSmallOsDefaultIconIdMap.put(pair.getLarge(), pair.getSmall());
        }
    }

    private void initOsIds() {
        osIds = new ArrayList<Integer>(osNames.keySet());
        Collections.sort(osIds, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return osNames.get(o1).compareTo(osNames.get(o2));
            }
        });
    }

    public void initOsArchitecture() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                osArchitectures = ((VdcQueryReturnValue) returnValue).getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(OsRepositoryVerb.GetOsArchitectures), callback);
    }

    public boolean osNameExists(Integer osId) {
        return osNames.keySet().contains(osId);
    }

    public String getOsName(Integer osId) {
        // can be null as a consequence of setItems on ListModel
        if (osId == null) {
            return "";
        }

        return osNames.get(osId);
    }

    public Guid getOsDefaultIconId(Integer osId, boolean small) {
        if (osId == null) {
            return getDefaultIconId(small);
        }
        final VmIconIdSizePair pair = osIdToDefaultIconIdMap.get(osId);
        if (pair != null) {
            return pair.get(small);
        }
        return getDefaultIconId(small);
    }

    public boolean isCustomIconId(Guid iconId) {
        return (!largeToSmallOsDefaultIconIdMap.containsKey(iconId))
                && (!largeToSmallOsDefaultIconIdMap.containsValue(iconId));
    }

    public Guid getDefaultIconId(boolean small) {
        final VmIconIdSizePair pair = osIdToDefaultIconIdMap.get(DEFAULT_OS_ID);
        if (pair != null) {
            return pair.get(small);
        }
        throw new RuntimeException("Icon of default operating system not found."); //$NON-NLS-1$
    }

    public Guid getSmallByLargeOsDefaultIconId(Guid largeIconId) {
        return largeToSmallOsDefaultIconIdMap.get(largeIconId);
    }

    public boolean hasSpiceSupport(int osId, Version version) {
        for (Pair<GraphicsType, DisplayType> graphicsDisplayPair : getGraphicsAndDisplays(osId, version)) {
            if (graphicsDisplayPair.getFirst() == GraphicsType.SPICE) {
                return true;
            }
        }
        return false;
    }

    public List<Pair<GraphicsType, DisplayType>> getGraphicsAndDisplays(int osId, Version version) {
        return graphicsAndDisplays.get(osId).get(version);
    }

    private void initDisplayTypes() {
        AsyncQuery callback = new AsyncQuery();
        callback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                graphicsAndDisplays = ((VdcQueryReturnValue) returnValue).getReturnValue();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(OsRepositoryVerb.GetDisplayTypes), callback);
    }

    public List<Integer> getOsIds(ArchitectureType architectureType) {

        List<Integer> osIds = new ArrayList<Integer>();

        for (Entry<Integer, ArchitectureType> entry : osArchitectures.entrySet()) {
            if (entry.getValue() == architectureType) {
                osIds.add(entry.getKey());
            }
        }

        Collections.sort(osIds, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return osNames.get(o1).compareTo(osNames.get(o2));
            }
        });

        return osIds;
    }

    public void getOsMaxRam(int osId, Version version, AsyncQuery asyncQuery) {
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository,
                new OsQueryParameters(OsRepositoryVerb.GetMaxOsRam, osId, version),
                asyncQuery);
    }

    public void getVmWatchdogTypes(int osId, Version version,
            AsyncQuery asyncQuery) {
        Frontend.getInstance().runQuery(VdcQueryType.OsRepository, new OsQueryParameters(
                OsRepositoryVerb.GetVmWatchdogTypes, osId, version), asyncQuery);
    }

    public ArrayList<Map.Entry<String, EntityModel<String>>> getBondingOptionList(RefObject<Map.Entry<String, EntityModel<String>>> defaultItem) {
        ArrayList<Map.Entry<String, EntityModel<String>>> list =
                new ArrayList<Map.Entry<String, EntityModel<String>>>();
        EntityModel<String> entityModel = new EntityModel<String>();
        entityModel.setEntity("(Mode 1) Active-Backup"); //$NON-NLS-1$
        list.add(new KeyValuePairCompat<String, EntityModel<String>>("mode=1 miimon=100", entityModel)); //$NON-NLS-1$
        entityModel = new EntityModel<String>();
        entityModel.setEntity("(Mode 2) Load balance (balance-xor)"); //$NON-NLS-1$
        list.add(new KeyValuePairCompat<String, EntityModel<String>>("mode=2 miimon=100", entityModel)); //$NON-NLS-1$
        entityModel = new EntityModel<String>();
        entityModel.setEntity("(Mode 4) Dynamic link aggregation (802.3ad)"); //$NON-NLS-1$
        defaultItem.argvalue = new KeyValuePairCompat<String, EntityModel<String>>("mode=4 miimon=100", entityModel); //$NON-NLS-1$
        list.add(defaultItem.argvalue);
        entityModel = new EntityModel<String>();
        entityModel.setEntity("(Mode 5) Adaptive transmit load balancing (balance-tlb)"); //$NON-NLS-1$
        list.add(new KeyValuePairCompat<String, EntityModel<String>>("mode=5 miimon=100", entityModel)); //$NON-NLS-1$
        entityModel = new EntityModel<String>();
        entityModel.setEntity(""); //$NON-NLS-1$
        list.add(new KeyValuePairCompat<String, EntityModel<String>>("custom", entityModel)); //$NON-NLS-1$
        return list;
    }

    public String getDefaultBondingOption() {
        return "mode=802.3ad miimon=150"; //$NON-NLS-1$
    }

    public int getMaxVmPriority() {
        return (Integer) getConfigValuePreConverted(ConfigurationValues.VmPriorityMaxValue,
                getDefaultConfigurationVersion());
    }

    public int roundPriority(int priority) {
        int max = getMaxVmPriority();
        int medium = max / 2;

        int[] levels = new int[] { 1, medium, max };

        for (int i = 0; i < levels.length; i++) {
            int lengthToLess = levels[i] - priority;
            int lengthToMore = levels[i + 1] - priority;

            if (lengthToMore < 0) {
                continue;
            }

            return Math.abs(lengthToLess) < lengthToMore ? levels[i] : levels[i + 1];
        }

        return 0;
    }

    public void getVmGuestAgentInterfacesByVmId(AsyncQuery aQuery, Guid vmId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<VmGuestAgentInterface>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmGuestAgentInterfacesByVmId,
                new IdQueryParameters(vmId).withoutRefresh(), aQuery);
    }

    public void getVnicProfilesByNetworkId(AsyncQuery aQuery, Guid networkId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new ArrayList<VnicProfileView>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVnicProfilesByNetworkId, new IdQueryParameters(networkId), aQuery);
    }

    public void getVnicProfilesByDcId(AsyncQuery aQuery, Guid dcId) {
        // do not replace a converter = just add if none provided
        if (aQuery.converterCallback == null) {
            aQuery.converterCallback = new IAsyncConverter() {
                @Override
                public Object Convert(Object source, AsyncQuery _asyncQuery) {
                    if (source == null) {
                        return new ArrayList<VnicProfileView>();
                    }
                    return source;
                }
            };
        }
        Frontend.getInstance().runQuery(VdcQueryType.GetVnicProfilesByDataCenterId, new IdQueryParameters(dcId), aQuery);
    }

    public void getNumberOfActiveVmsInCluster(AsyncQuery aQuery, Guid clusterId) {
        // do not replace a converter = just add if none provided
        if (aQuery.converterCallback == null) {
            aQuery.converterCallback = new IAsyncConverter() {
                @Override
                public Object Convert(Object source, AsyncQuery _asyncQuery) {
                    if (source == null) {
                        return Integer.valueOf(0);
                    }
                    return source;
                }
            };
        }
        Frontend.getInstance().runQuery(VdcQueryType.GetNumberOfActiveVmsInVdsGroupByVdsGroupId, new IdQueryParameters(clusterId), aQuery);
    }

    public void getNumberOfVmsInCluster(AsyncQuery aQuery, Guid clusterId) {
        Frontend.getInstance().runQuery(VdcQueryType.GetNumberOfVmsInVdsGroupByVdsGroupId, new IdQueryParameters(clusterId),
                aQuery);
    }

    public boolean isMixedStorageDomainsSupported(Version version) {
        return (Boolean) getConfigValuePreConverted(ConfigurationValues.MixedDomainTypesInDataCenter, version.toString());
    }

    public boolean isLsmBetweenMixedStorageDomainsSupported(Version version) {
        return (Boolean) getConfigValuePreConverted(ConfigurationValues.LiveStorageMigrationBetweenDifferentStorageTypes,
                version.toString());
    }

    public Integer getMaxIoThreadsPerVm() {
        return (Integer) getConfigValuePreConverted(ConfigurationValues.MaxIoThreadsPerVm);
    }

    public ArrayList<VDSGroup> getClusterByServiceList(List<VDSGroup> list,
            boolean supportsVirtService,
            boolean supportsGlusterService) {
        final ArrayList<VDSGroup> filteredList = new ArrayList<VDSGroup>();
        for (VDSGroup cluster : list) {
            if ((supportsVirtService && cluster.supportsVirtService())
                    || (supportsGlusterService && cluster.supportsGlusterService())) {
                filteredList.add(cluster);
            }
        }

        // sort by cluster name
        Collections.sort(filteredList, new NameableComparator());
        return filteredList;
    }

    public String priorityToString(int value) {
        int roundedPriority = roundPriority(value);

        if (roundedPriority == 1) {
            return ConstantsManager.getInstance().getConstants().vmLowPriority();
        }
        else if (roundedPriority == getMaxVmPriority() / 2) {
            return ConstantsManager.getInstance().getConstants().vmMediumPriority();
        }
        else if (roundedPriority == getMaxVmPriority()) {
            return ConstantsManager.getInstance().getConstants().vmHighPriority();
        }
        else {
            return ConstantsManager.getInstance().getConstants().vmUnknownPriority();
        }
    }

    public void getExternalNetworkMap(AsyncQuery aQuery, Guid providerId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source == null) {
                    return new HashMap<Network, Set<Guid>>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllExternalNetworksOnProvider,
                new IdQueryParameters(providerId),
                aQuery);
    }

    public Integer getMaxVmNameLengthWin() {
        Integer maxVmNameLengthWindows = (Integer) getConfigValuePreConverted(ConfigurationValues.MaxVmNameLengthWindows);
        if (maxVmNameLengthWindows == null) {
            return 15;
        }
        return maxVmNameLengthWindows;
    }

    public Integer getMaxVmNameLengthNonWin() {
        Integer maxVmNameLengthNonWindows = (Integer) getConfigValuePreConverted(ConfigurationValues.MaxVmNameLengthNonWindows);
        if (maxVmNameLengthNonWindows == null) {
            return 64;
        }
        return maxVmNameLengthNonWindows;
    }

    public int getOptimizeSchedulerForSpeedPendingRequests() {
        return (Integer) getConfigValuePreConverted(ConfigurationValues.SpeedOptimizationSchedulingThreshold,
                getDefaultConfigurationVersion());
    }

    public boolean getScheudulingAllowOverbookingSupported() {
        return (Boolean) getConfigValuePreConverted(ConfigurationValues.SchedulerAllowOverBooking,
                getDefaultConfigurationVersion());
    }

    public int getSchedulerAllowOverbookingPendingRequestsThreshold() {
        return (Integer) getConfigValuePreConverted(ConfigurationValues.SchedulerOverBookingThreshold,
                getDefaultConfigurationVersion());
    }

    public Integer getDefaultOs (ArchitectureType architectureType) {
        return defaultOSes.get(architectureType);
    }

    public boolean isRebootCommandExecutionAllowed(List<VM> vms) {
        if (vms.isEmpty() || !VdcActionUtils.canExecutePartially(vms, VM.class, VdcActionType.RebootVm)) {
            return false;
        }

        for (VM vm : vms) {
            Version version = vm.getVdsGroupCompatibilityVersion();
            Version anyDcVersion = new Version();
            boolean compatibleCluster = isCommandCompatible(VdcActionType.RebootVm, version, anyDcVersion);
            boolean guestAgentPresent = !StringHelper.isNullOrEmpty(vm.getVmIp());
            boolean acpiEnabled = Boolean.TRUE.equals(vm.getAcpiEnable());
            if (!(compatibleCluster && (guestAgentPresent || acpiEnabled))) {
                return false;
            }
        }
        return true;
    }

    public boolean isSerialNumberPolicySupported(String version) {
        return (Boolean) getConfigValuePreConverted(ConfigurationValues.SerialNumberPolicySupported, version);
    }

    public boolean isSkipFencingIfSDActiveSupported(String version) {
        boolean result = false;
        if (version != null) {
            Boolean b = (Boolean) getConfigValuePreConverted(
                    ConfigurationValues.SkipFencingIfSDActiveSupported,
                    version
            );
            result = (b != null) && b;
        }
        return result;
    }

    public boolean isBootMenuSupported(String version) {
        return (Boolean) getConfigValuePreConverted(ConfigurationValues.BootMenuSupported, version);
    }

    public boolean isSpiceFileTransferToggleSupported(String version) {
        return (Boolean) getConfigValuePreConverted(ConfigurationValues.SpiceFileTransferToggleSupported, version);
    }

    public boolean isSpiceCopyPasteToggleSupported(String version) {
        return (Boolean) getConfigValuePreConverted(ConfigurationValues.SpiceCopyPasteToggleSupported, version);
    }

    public List<IStorageModel> getDataStorageModels() {
        ArrayList<IStorageModel> models = new ArrayList<IStorageModel>();
        models.addAll(getFileDataStorageModels());
        models.addAll(getBlockDataStorageModels());
        return models;
    }

    public List<IStorageModel> getFileDataStorageModels() {
        ArrayList<IStorageModel> models = new ArrayList<IStorageModel>();

        NfsStorageModel nfsDataModel = new NfsStorageModel();
        models.add(nfsDataModel);

        PosixStorageModel posixDataModel = new PosixStorageModel();
        models.add(posixDataModel);

        GlusterStorageModel GlusterDataModel = new GlusterStorageModel();
        models.add(GlusterDataModel);

        LocalStorageModel localDataModel = new LocalStorageModel();
        models.add(localDataModel);

        addTypeToStorageModels(StorageDomainType.Data, models);

        return models;
    }

    public List<IStorageModel> getBlockDataStorageModels() {
        ArrayList<IStorageModel> models = new ArrayList<IStorageModel>();

        IscsiStorageModel iscsiDataModel = new IscsiStorageModel();
        iscsiDataModel.setIsGrouppedByTarget(true);
        models.add(iscsiDataModel);

        FcpStorageModel fcpDataModel = new FcpStorageModel();
        models.add(fcpDataModel);

        addTypeToStorageModels(StorageDomainType.Data, models);

        return models;
    }

    public List<IStorageModel> getImportBlockDataStorageModels() {
        ArrayList<IStorageModel> models = new ArrayList<IStorageModel>();

        ImportIscsiStorageModel iscsiDataModel = new ImportIscsiStorageModel();
        models.add(iscsiDataModel);

        ImportFcpStorageModel fcpDataModel = new ImportFcpStorageModel();
        models.add(fcpDataModel);

        addTypeToStorageModels(StorageDomainType.Data, models);

        return models;
    }

    public List<IStorageModel> getIsoStorageModels() {
        ArrayList<IStorageModel> models = new ArrayList<IStorageModel>();

        NfsStorageModel nfsIsoModel = new NfsStorageModel();
        models.add(nfsIsoModel);

        PosixStorageModel posixIsoModel = new PosixStorageModel();
        models.add(posixIsoModel);

        LocalStorageModel localIsoModel = new LocalStorageModel();
        models.add(localIsoModel);

        addTypeToStorageModels(StorageDomainType.ISO, models);


        return models;
    }

    public List<IStorageModel> getExportStorageModels() {
        ArrayList<IStorageModel> models = new ArrayList<IStorageModel>();

        NfsStorageModel nfsExportModel = new NfsStorageModel();
        models.add(nfsExportModel);

        PosixStorageModel posixExportModel = new PosixStorageModel();
        models.add(posixExportModel);

        GlusterStorageModel glusterExportModel = new GlusterStorageModel();
        models.add(glusterExportModel);

        LocalStorageModel localExportStorageModel = new LocalStorageModel();
        models.add(localExportStorageModel);

        addTypeToStorageModels(StorageDomainType.ImportExport, models);

        return models;
    }

    private void addTypeToStorageModels(StorageDomainType storageDomainType, List<IStorageModel> models) {
        for (IStorageModel model : models) {
            model.setRole(storageDomainType);
        }
    }

    private static void convertAAAProfilesResult(AsyncQuery aQuery, final boolean passwordBasedOnly) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                List<String> results = new ArrayList<String>();
                for (ProfileEntry profileEntry : (Collection<ProfileEntry>) source) {
                    if (!passwordBasedOnly || profileEntry.getSupportsPasswordAuthenication()) {
                        results.add(profileEntry.getProfile());
                    }
                }
                return results;
            }
        };
    }

    public void getHostNumaTopologyByHostId(AsyncQuery asyncQuery, Guid hostId) {
        asyncQuery.converterCallback = new IAsyncConverter() {

            @Override
            public Object Convert(Object source, AsyncQuery asyncQuery) {
                if (source == null) {
                    return new ArrayList<VdsNumaNode>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVdsNumaNodesByVdsId,
                new IdQueryParameters(hostId),
                asyncQuery);
    }

    public void getVMsWithVNumaNodesByClusterId(AsyncQuery asyncQuery, Guid clusterId) {
        asyncQuery.converterCallback = new IAsyncConverter() {

            @Override
            public Object Convert(Object source, AsyncQuery asyncQuery) {
                if (source == null) {
                    return new ArrayList<VM>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetAllVmsWithNumaByVdsGroupId,
                new IdQueryParameters(clusterId),
                asyncQuery);
    }

    public ArrayList<NumaTuneMode> getNumaTuneModeList() {
        return new ArrayList<NumaTuneMode>(Arrays.asList(new NumaTuneMode[] {
                NumaTuneMode.STRICT,
                NumaTuneMode.PREFERRED,
                NumaTuneMode.INTERLEAVE
        }));
    }

    public void getEmulatedMachinesByClusterID(AsyncQuery aQuery, Guid clusterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<VDS> vdsList = Linq.<VDS> cast((List<IVdcQueryable>) source);
                    Set<String> emulatedMachineList = new HashSet<String>();
                    for (VDS host : vdsList) {
                        String hostSupportedMachines = host.getSupportedEmulatedMachines();
                        if(!StringHelper.isNullOrEmpty(hostSupportedMachines)) {
                            emulatedMachineList.addAll(Arrays.asList(hostSupportedMachines.split(","))); //$NON-NLS-1$
                        }
                    }
                    return emulatedMachineList;
                }

                return null;
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetHostsByClusterId, new IdQueryParameters(clusterId), aQuery);
    }

    public void getSupportedCpuList(AsyncQuery aQuery, String cpuName) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                if (source != null) {
                    ArrayList<ServerCpu> cpuList = Linq.<ServerCpu> cast((ArrayList<ServerCpu>) source);
                    return cpuList;
                }

                return null;
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetSupportedCpuList, new GetSupportedCpuListParameters(cpuName), aQuery);

    }

    public void getStorageDevices(AsyncQuery aQuery, Guid hostId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetGlusterStorageDevices,
                new IdQueryParameters(hostId),
                aQuery);
    }

    public void getClusterEditWarnings(AsyncQuery aQuery, Guid clusterId, VDSGroup cluster) {
        Frontend.getInstance().runQuery(VdcQueryType.GetClusterEditWarnings, new ClusterEditParameters(cluster), aQuery);
    }

    private static class AsIsAsyncConverter implements IAsyncConverter {
        @Override
        public Object Convert(Object source, AsyncQuery _asyncQuery) {
            return source;
        }
    }

    private static class ListAsyncConverter implements IAsyncConverter {
        @Override
        public Object Convert(Object source, AsyncQuery _asyncQuery) {
            return source != null ? source : new ArrayList();
        }
    }

    public void getUnusedBricksFromServer(AsyncQuery asyncQuery, Guid hostId) {
        asyncQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source;
            }
        };
        IdQueryParameters parameters = new IdQueryParameters(hostId);
        Frontend.getInstance().runQuery(VdcQueryType.GetUnusedGlusterBricks, parameters, asyncQuery);
    }

    public void getCinderVolumeTypesList(AsyncQuery aQuery, Guid storageDomainId) {
        aQuery.converterCallback = new IAsyncConverter<List<CinderVolumeType>>() {
            @Override
            public List<CinderVolumeType> Convert(Object source, AsyncQuery _asyncQuery) {
                return (List<CinderVolumeType>) source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetCinderVolumeTypesByStorageDomainId, new IdQueryParameters(storageDomainId), aQuery);
    }

    public void getClusterFeaturesByVersionAndCategory(AsyncQuery aQuery, Version version, ApplicationMode category) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (Set<AdditionalFeature>) source
                        : new HashSet<AdditionalFeature>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetClusterFeaturesByVersionAndCategory,
                new GetClusterFeaturesByVersionAndCategoryParameters(version, category),
                aQuery);
    }

    public void getClusterFeaturesByClusterId(AsyncQuery aQuery, Guid clusterId) {
        aQuery.converterCallback = new IAsyncConverter() {
            @Override
            public Object Convert(Object source, AsyncQuery _asyncQuery) {
                return source != null ? (Set<SupportedAdditionalClusterFeature>) source
                        : new HashSet<SupportedAdditionalClusterFeature>();
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetClusterFeaturesByClusterId,
                new IdQueryParameters(clusterId),
                aQuery);
    }

    private static class RepoImageToImageFileNameAsyncConverter implements IAsyncConverter {
        @Override
        public Object Convert(Object source, AsyncQuery _asyncQuery) {
            if (source != null) {
                ArrayList<RepoImage> repoList = (ArrayList<RepoImage>) source;
                ArrayList<String> fileNameList = new ArrayList<String>();
                for (RepoImage repoImage : repoList) {
                    if (desiredImage(repoImage)) {
                        fileNameList.add(transform(fileNameList, repoImage));
                    }
                }

                Collections.sort(fileNameList, String.CASE_INSENSITIVE_ORDER);
                return fileNameList;
            }
            return new ArrayList<String>();
        }

        protected String transform(ArrayList<String> fileNameList, RepoImage repoImage) {
            return repoImage.getRepoImageId();
        }

        protected boolean desiredImage(RepoImage repoImage) {
            return true;
        }
    }

    public void getVmTemplatesByBaseTemplateId(AsyncQuery asyncQuery, Guid baseTemplate) {
        asyncQuery.converterCallback = new IAsyncConverter() {

            @Override
            public Object Convert(Object source, AsyncQuery asyncQuery) {
                if (source == null) {
                    return new ArrayList<VmTemplate>();
                }
                return source;
            }
        };
        Frontend.getInstance().runQuery(VdcQueryType.GetVmTemplatesByBaseTemplateId,
                new GetVmTemplateParameters(baseTemplate),
                asyncQuery);
    }

    public ArrayList<LibvirtSecretUsageType> getLibvirtSecretUsageTypeList() {
        return new ArrayList<>(Arrays.asList(LibvirtSecretUsageType.values()));
    }

    public boolean isNetworkExclusivenessPermissiveValidation(String version) {
        return (Boolean) getConfigValuePreConverted(
                ConfigurationValues.NetworkExclusivenessPermissiveValidation,
                version);
    }
}
