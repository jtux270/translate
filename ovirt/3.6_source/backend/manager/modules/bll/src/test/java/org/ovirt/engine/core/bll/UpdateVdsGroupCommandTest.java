package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.network.cluster.DefaultManagementNetworkFinder;
import org.ovirt.engine.core.bll.network.cluster.UpdateClusterNetworkClusterValidator;
import org.ovirt.engine.core.common.action.ManagementNetworkOnClusterOperationParameters;
import org.ovirt.engine.core.common.businessentities.AdditionalFeature;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.SupportedAdditionalClusterFeature;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.ClusterFeatureDao;
import org.ovirt.engine.core.dao.StoragePoolDao;
import org.ovirt.engine.core.dao.SupportedHostFeatureDao;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.VdsGroupDao;
import org.ovirt.engine.core.dao.VmDao;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
public class UpdateVdsGroupCommandTest {

    private static final Version VERSION_1_0 = new Version(1, 0);
    private static final Version VERSION_1_1 = new Version(1, 1);
    private static final Version VERSION_1_2 = new Version(1, 2);
    private static final Guid DC_ID1 = Guid.newGuid();
    private static final Guid DC_ID2 = Guid.newGuid();
    private static final Guid DEFAULT_VDS_GROUP_ID = new Guid("99408929-82CF-4DC7-A532-9D998063FA95");
    private static final Guid DEFAULT_FEATURE_ID = new Guid("99408929-82CF-4DC7-A532-9D998063FA96");
    private static final Guid TEST_MANAGEMENT_NETWORK_ID = Guid.newGuid();

    private static final Map<String, String> migrationMap = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("undefined", "true");
                put("x86_64", "true");
                put("ppc64", "false");
            }});

    private static final Set<Version> versions = Collections.unmodifiableSet(
            new HashSet<Version>() {{
                add(VERSION_1_0);
                add(VERSION_1_1);
                add(VERSION_1_2);
            }});

    @Rule
    public MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.IsMigrationSupported, VERSION_1_0.getValue(), migrationMap),
            mockConfig(ConfigValues.IsMigrationSupported, VERSION_1_1.getValue(), migrationMap),
            mockConfig(ConfigValues.IsMigrationSupported, VERSION_1_2.getValue(), migrationMap)
            );

    @Mock
    DbFacade dbFacadeMock;

    @Mock
    private VdsGroupDao vdsGroupDao;
    @Mock
    private VdsDao vdsDao;
    @Mock
    private StoragePoolDao storagePoolDao;
    @Mock
    private GlusterVolumeDao glusterVolumeDao;
    @Mock
    private VmDao vmDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private ClusterFeatureDao clusterFeatureDao;
    @Mock
    private SupportedHostFeatureDao hostFeatureDao;
    @Mock
    private DefaultManagementNetworkFinder defaultManagementNetworkFinder;
    @Mock
    private UpdateClusterNetworkClusterValidator networkClusterValidator;

    @Mock
    private Network mockManagementNetwork = createManagementNetwork();
    private Guid managementNetworkId;

    private Network createManagementNetwork() {
        final Network network = new Network();
        network.setId(TEST_MANAGEMENT_NETWORK_ID);
        return network;
    }

    private UpdateVdsGroupCommand<ManagementNetworkOnClusterOperationParameters> cmd;

    @Test
    public void nameInUse() {
        createSimpleCommand();
        createCommandWithDifferentName();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_DO_ACTION_NAME_IN_USE);
    }

    @Test
    public void invalidVdsGroup() {
        createSimpleCommand();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(null);
        canDoActionFailedWithReason(EngineMessage.VDS_CLUSTER_IS_NOT_VALID);
    }

    @Test
    public void legalArchitectureChange() {
        createCommandWithDefaultVdsGroup();
        cpuExists();
        architectureIsUpdatable();
        assertTrue(cmd.canDoAction());
    }

    @Test
    public void illegalArchitectureChange() {
        createCommandWithDefaultVdsGroup();
        clusterHasVMs();
        cpuExists();
        architectureIsNotUpdatable();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_UPDATE_CPU_ARCHITECTURE_ILLEGAL);
    }

    @Test
    public void illegalCpuUpdate() {
        createCommandWithDifferentCpuName();
        cpuIsNotUpdatable();
        cpuManufacturersMatch();
        cpuExists();
        clusterHasVMs();
        clusterHasVds();
        architectureIsUpdatable();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CPU_IS_NOT_UPDATABLE);
    }

    @Test
    public void legalCpuUpdate() {
        createCommandWithDifferentCpuName();
        cpuExists();
        architectureIsUpdatable();
        assertTrue(cmd.canDoAction());
    }

    private void cpuIsNotUpdatable() {
        doReturn(false).when(cmd).isCpuUpdatable(any(VDSGroup.class));
    }

    @Test
    public void invalidCpuSelection() {
        createCommandWithDefaultVdsGroup();
        canDoActionFailedWithReason(EngineMessage.ACTION_TYPE_FAILED_CPU_NOT_FOUND);
    }

    @Test
    public void illegalCpuChange() {
        createCommandWithDefaultVdsGroup();
        cpuExists();
        cpuManufacturersDontMatch();
        clusterHasVds();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_UPDATE_CPU_ILLEGAL);
    }

    @Test
    public void invalidVersion() {
        createCommandWithInvalidVersion();
        setupCpu();
        canDoActionFailedWithReason(EngineMessage.ACTION_TYPE_FAILED_GIVEN_VERSION_NOT_SUPPORTED);
    }

    @Test
    public void versionDecreaseWithHost() {
        createCommandWithOlderVersion(true, false);
        setupCpu();
        VdsExist();
        canDoActionFailedWithReason(EngineMessage.ACTION_TYPE_FAILED_CANNOT_DECREASE_COMPATIBILITY_VERSION);
    }

    @Test
    public void versionDecreaseNoHostsOrNetwork() {
        createCommandWithOlderVersion(true, false);
        setupCpu();
        StoragePoolDao storagePoolDao2 = Mockito.mock(StoragePoolDao.class);
        when(storagePoolDao2.get(any(Guid.class))).thenReturn(createStoragePoolLocalFS());
        doReturn(storagePoolDao2).when(cmd).getStoragePoolDao();
        assertTrue(cmd.canDoAction());
    }

    @Test
    public void versionDecreaseLowerVersionThanDC() {
        createCommandWithOlderVersion(true, false);
        StoragePoolDao storagePoolDao2 = Mockito.mock(StoragePoolDao.class);
        when(storagePoolDao2.get(any(Guid.class))).thenReturn(createStoragePoolLocalFSOldVersion());
        doReturn(storagePoolDao2).when(cmd).getStoragePoolDao();
        doReturn(storagePoolDao2).when(dbFacadeMock).getStoragePoolDao();
        setupCpu();
        canDoActionFailedWithReason(EngineMessage.ACTION_TYPE_FAILED_CANNOT_DECREASE_COMPATIBILITY_VERSION_UNDER_DC);
    }

    @Test
    public void updateWithLowerVersionThanHosts() {
        createCommandWithDefaultVdsGroup();
        setupCpu();
        VdsExistWithHigherVersion();
        architectureIsUpdatable();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_UPDATE_COMPATIBILITY_VERSION_WITH_LOWER_HOSTS);
    }

    @Test
    public void updateWithCpuLowerThanHost() {
        createCommandWithDefaultVdsGroup();
        setupCpu();
        clusterHasVds();
        cpuFlagsMissing();
        architectureIsUpdatable();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_UPDATE_CPU_WITH_LOWER_HOSTS);
    }

    @Test
    public void updateStoragePool() {
        createCommandWithDifferentPool();
        setupCpu();
        clusterHasVds();
        cpuFlagsNotMissing();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_CHANGE_STORAGE_POOL);
    }

    @Test
    public void clusterAlreadyInLocalFs() {
        prepareManagementNetworkMocks();

        createCommandWithDefaultVdsGroup();
        oldGroupIsDetachedDefault();
        storagePoolIsLocalFS();
        setupCpu();
        allQueriesForVms();
        storagePoolAlreadyHasCluster();
        architectureIsUpdatable();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_ADD_MORE_THEN_ONE_HOST_TO_LOCAL_STORAGE);
    }

    @Test
    public void clusterMovesToDcWithNoDefaultManagementNetwork() {
        noNewDefaultManagementNetworkFound();

        createCommandWithDefaultVdsGroup();
        oldGroupIsDetachedDefault();
        setupCpu();
        canDoActionFailedWithReason(EngineMessage.ACTION_TYPE_FAILED_DEFAULT_MANAGEMENT_NETWORK_NOT_FOUND);
    }

    @Test
    public void detachedClusterMovesToDcWithNonExistentManagementNetwork() {
        managementNetworkNotFoundById();

        createCommandWithDefaultVdsGroup();
        oldGroupIsDetachedDefault();
        setupCpu();

        canDoActionFailedWithReason(EngineMessage.NETWORK_NOT_EXISTS);
    }

    @Test
    public void detachedClusterMovesToDcWithExistingManagementNetwork() {

        prepareManagementNetworkMocks();

        mcr.mockConfigValue(ConfigValues.AutoRegistrationDefaultVdsGroupID, Guid.Empty);
        createCommandWithDefaultVdsGroup();
        oldGroupIsDetachedDefault();
        setupCpu();
        storagePoolIsLocalFS();

        assertTrue(cmd.canDoAction());
    }

    private void managementNetworkNotFoundById() {
        managementNetworkId = TEST_MANAGEMENT_NETWORK_ID;
        when(networkDao.get(TEST_MANAGEMENT_NETWORK_ID)).thenReturn(null);
    }

    @Test
    public void invalidDefaultManagementNetworkAttachement() {
        newDefaultManagementNetworkFound();
        final EngineMessage expected = EngineMessage.Unassigned;
        when(networkClusterValidator.managementNetworkChange()).thenReturn(new ValidationResult(expected));

        createCommandWithDefaultVdsGroup();
        oldGroupIsDetachedDefault();
        setupCpu();
        canDoActionFailedWithReason(expected);
    }

    private void setupCpu() {
        cpuExists();
        cpuManufacturersMatch();
    }

    @Test
    public void defaultClusterInLocalFs() {
        prepareManagementNetworkMocks();

        mcr.mockConfigValue(ConfigValues.AutoRegistrationDefaultVdsGroupID, DEFAULT_VDS_GROUP_ID);
        createCommandWithDefaultVdsGroup();
        oldGroupIsDetachedDefault();
        storagePoolIsLocalFS();
        setupCpu();
        allQueriesForVms();
        architectureIsUpdatable();
        canDoActionFailedWithReason(EngineMessage.DEFAULT_CLUSTER_CANNOT_BE_ON_LOCALFS);
    }

    private void prepareManagementNetworkMocks() {
        newDefaultManagementNetworkFound();
        when(networkClusterValidator.managementNetworkChange()).thenReturn(ValidationResult.VALID);
    }

    private void newDefaultManagementNetworkFound() {
        managementNetworkId = null;
        when(defaultManagementNetworkFinder.findDefaultManagementNetwork(DC_ID1)).
                thenReturn(mockManagementNetwork);
    }

    private void noNewDefaultManagementNetworkFound() {
        managementNetworkId = null;
        when(defaultManagementNetworkFinder.findDefaultManagementNetwork(DC_ID1)).
                thenReturn(null);
    }

    @Test
    public void vdsGroupWithNoCpu() {
        createCommandWithNoCpuName();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createVdsGroupWithNoCpuName());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createVdsGroupWithNoCpuName());
        when(glusterVolumeDao.getByClusterId(any(Guid.class))).thenReturn(new ArrayList<GlusterVolumeEntity>());
        allQueriesForVms();
        assertTrue(cmd.canDoAction());
    }

    @Test
    public void vdsGroupWithNoServiceEnabled() {
        createCommandWithNoService();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createVdsGroupWithNoCpuName());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createVdsGroupWithNoCpuName());
        cpuExists();
        allQueriesForVms();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_AT_LEAST_ONE_SERVICE_MUST_BE_ENABLED);
    }

    @Test
    public void vdsGroupWithVirtGlusterServicesNotAllowed() {
        createCommandWithVirtGlusterEnabled();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createVdsGroupWithNoCpuName());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createVdsGroupWithNoCpuName());
        mcr.mockConfigValue(ConfigValues.AllowClusterWithVirtGlusterEnabled, Boolean.FALSE);
        mcr.mockConfigValue(ConfigValues.GlusterSupport, VERSION_1_1, Boolean.TRUE);
        cpuExists();
        allQueriesForVms();
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_ENABLING_BOTH_VIRT_AND_GLUSTER_SERVICES_NOT_ALLOWED);
    }

    @Test
    public void vdspysGroupWithVirtGlusterNotSupported() {
        createCommandWithGlusterEnabled();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createVdsGroupWithNoCpuName());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createVdsGroupWithNoCpuName());
        mcr.mockConfigValue(ConfigValues.AllowClusterWithVirtGlusterEnabled, Boolean.FALSE);
        mcr.mockConfigValue(ConfigValues.GlusterSupport, VERSION_1_1, Boolean.FALSE);
        cpuExists();
        allQueriesForVms();
        canDoActionFailedWithReason(EngineMessage.GLUSTER_NOT_SUPPORTED);
    }

    @Test
    public void disableVirtWhenVmsExist() {
        createCommandWithGlusterEnabled();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createDefaultVdsGroup());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createDefaultVdsGroup());
        mcr.mockConfigValue(ConfigValues.GlusterSupport, VERSION_1_1, Boolean.TRUE);
        cpuExists();
        cpuFlagsNotMissing();
        clusterHasVds();
        clusterHasVMs();

        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_DISABLE_VIRT_WHEN_CLUSTER_CONTAINS_VMS);
    }

    @Test
    public void disableGlusterWhenVolumesExist() {
        createCommandWithVirtEnabled();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createVdsGroupWithNoCpuName());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createVdsGroupWithNoCpuName());
        cpuExists();
        cpuFlagsNotMissing();
        allQueriesForVms();
        clusterHasGlusterVolumes();

        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_DISABLE_GLUSTER_WHEN_CLUSTER_CONTAINS_VOLUMES);
    }

    @Test
    public void enableNewAddtionalFeatureWhenHostDoesnotSupport() {
        createCommandWithAddtionalFeature();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createVdsGroupWithNoCpuName());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createVdsGroupWithNoCpuName());
        cpuExists();
        cpuFlagsNotMissing();
        allQueriesForVms();
        clusterHasVds();
        when(clusterFeatureDao.getSupportedFeaturesByClusterId(any(Guid.class))).thenReturn(Collections.EMPTY_SET);
        when(hostFeatureDao.getSupportedHostFeaturesByHostId(any(Guid.class))).thenReturn(Collections.EMPTY_SET);
        canDoActionFailedWithReason(EngineMessage.VDS_GROUP_CANNOT_UPDATE_SUPPORTED_FEATURES_WITH_LOWER_HOSTS);
    }

    @Test
    public void enableNewAddtionalFeatureWhenHostSupports() {
        createCommandWithAddtionalFeature();
        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createVdsGroupWithNoCpuName());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createVdsGroupWithNoCpuName());
        cpuExists();
        cpuFlagsNotMissing();
        allQueriesForVms();
        clusterHasVds();
        when(clusterFeatureDao.getSupportedFeaturesByClusterId(any(Guid.class))).thenReturn(Collections.EMPTY_SET);
        when(hostFeatureDao.getSupportedHostFeaturesByHostId(any(Guid.class))).thenReturn(new HashSet(Arrays.asList("TEST_FEATURE")));
        assertTrue(cmd.canDoAction());
    }

    private void createSimpleCommand() {
        createCommand(createNewVdsGroup());
    }

    private void createCommandWithOlderVersion(boolean supportsVirtService, boolean supportsGlusterService) {
        createCommand(createVdsGroupWithOlderVersion(true, false));

    }

    private void createCommandWithInvalidVersion() {
        createCommand(createVdsGroupWithBadVersion());
    }

    private void createCommandWithDifferentPool() {
        createCommand(createVdsGroupWithDifferentPool());
    }

    private void createCommandWithDefaultVdsGroup() {
        createCommand(createDefaultVdsGroup());
    }

    private void createCommandWithDifferentCpuName() {
        createCommand(createDefaultVdsGroupWithDifferentCpuName());
    }

    private void createCommandWithNoCpuName() {
        createCommand(createVdsGroupWithNoCpuName());
    }

    private void createCommandWithNoService() {
        createCommand(createVdsGroupWith(false, false));
    }

    private void createCommandWithVirtEnabled() {
        createCommand(createVdsGroupWith(true, false));
    }

    private void createCommandWithAddtionalFeature() {
        createCommand(createVdsGroupWithAddtionalFeature());
    }

    private void createCommandWithGlusterEnabled() {
        createCommand(createVdsGroupWith(false, true));
    }

    private void createCommandWithVirtGlusterEnabled() {
        createCommand(createVdsGroupWith(true, true));
    }

    private void createCommand(final VDSGroup group) {
        setValidCpuVersionMap();
        final ManagementNetworkOnClusterOperationParameters param;
        if (managementNetworkId == null) {
            param = new ManagementNetworkOnClusterOperationParameters(group);
        } else {
            param = new ManagementNetworkOnClusterOperationParameters(group, managementNetworkId);
        }
        cmd = spy(new UpdateVdsGroupCommand<>(param));

        doReturn(0).when(cmd).compareCpuLevels(any(VDSGroup.class));

        doReturn(dbFacadeMock).when(cmd).getDbFacade();
        doReturn(vdsGroupDao).when(cmd).getVdsGroupDao();
        doReturn(vdsGroupDao).when(dbFacadeMock).getVdsGroupDao();
        doReturn(vdsDao).when(cmd).getVdsDao();
        doReturn(storagePoolDao).when(cmd).getStoragePoolDao();
        doReturn(storagePoolDao).when(dbFacadeMock).getStoragePoolDao();
        doReturn(glusterVolumeDao).when(cmd).getGlusterVolumeDao();
        doReturn(vmDao).when(cmd).getVmDao();
        doReturn(networkDao).when(cmd).getNetworkDao();
        doReturn(defaultManagementNetworkFinder).when(cmd).getDefaultManagementNetworkFinder();
        doReturn(clusterFeatureDao).when(cmd).getClusterFeatureDao();
        doReturn(hostFeatureDao).when(cmd).getHostFeatureDao();
        doReturn(networkClusterValidator).when(cmd).createManagementNetworkClusterValidator();
        doReturn(true).when(cmd).validateClusterPolicy();

        if (StringUtils.isEmpty(group.getCpuName())) {
            doReturn(ArchitectureType.undefined).when(cmd).getArchitecture();
        } else {
            doReturn(ArchitectureType.x86_64).when(cmd).getArchitecture();
        }

        when(vdsGroupDao.get(any(Guid.class))).thenReturn(createDefaultVdsGroup());
        when(vdsGroupDao.getByName(anyString())).thenReturn(createDefaultVdsGroup());
        List<VDSGroup> vdsGroupList = new ArrayList<>();
        vdsGroupList.add(createDefaultVdsGroup());
        when(vdsGroupDao.getByName(anyString(), anyBoolean())).thenReturn(vdsGroupList);
    }

    private void createCommandWithDifferentName() {
        createCommand(createVdsGroupWithDifferentName());
    }

    private static VDSGroup createVdsGroupWithDifferentName() {
        VDSGroup group = new VDSGroup();
        group.setName("BadName");
        group.setCompatibilityVersion(VERSION_1_1);
        return group;
    }

    private static VDSGroup createNewVdsGroup() {
        VDSGroup group = new VDSGroup();
        group.setCompatibilityVersion(VERSION_1_1);
        group.setName("Default");
        return group;
    }

    private static VDSGroup createDefaultVdsGroup() {
        VDSGroup group = new VDSGroup();
        group.setName("Default");
        group.setId(DEFAULT_VDS_GROUP_ID);
        group.setCpuName("Intel Conroe");
        group.setCompatibilityVersion(VERSION_1_1);
        group.setStoragePoolId(DC_ID1);
        group.setArchitecture(ArchitectureType.x86_64);
        return group;
    }

    private static VDSGroup createDefaultVdsGroupWithDifferentCpuName() {
        VDSGroup group = createDefaultVdsGroup();

        group.setCpuName("Another CPU name");

        return group;
    }

    private static VDSGroup createVdsGroupWithNoCpuName() {
        VDSGroup group = new VDSGroup();
        group.setName("Default");
        group.setId(DEFAULT_VDS_GROUP_ID);
        group.setCompatibilityVersion(VERSION_1_1);
        group.setStoragePoolId(DC_ID1);
        group.setArchitecture(ArchitectureType.undefined);
        return group;
    }

    private static VDSGroup createDetachedDefaultVdsGroup() {
        VDSGroup group = createDefaultVdsGroup();
        group.setStoragePoolId(null);
        return group;
    }

    private static VDSGroup createVdsGroupWithOlderVersion(boolean supportsVirtService, boolean supportsGlusterService) {
        VDSGroup group = createNewVdsGroup();
        group.setCompatibilityVersion(VERSION_1_0);
        group.setStoragePoolId(DC_ID1);
        group.setVirtService(supportsVirtService);
        group.setGlusterService(supportsGlusterService);
        return group;
    }

    private static VDSGroup createVdsGroupWithBadVersion() {
        VDSGroup group = createNewVdsGroup();
        group.setCompatibilityVersion(new Version(5, 0));
        return group;
    }

    private static VDSGroup createVdsGroupWithDifferentPool() {
        VDSGroup group = createNewVdsGroup();
        group.setStoragePoolId(DC_ID2);
        return group;
    }

    private static VDSGroup createVdsGroupWith(boolean virtService, boolean glusterService) {
        VDSGroup group = createDefaultVdsGroup();
        group.setVirtService(virtService);
        group.setGlusterService(glusterService);
        group.setCompatibilityVersion(VERSION_1_1);
        return group;
    }

    private static VDSGroup createVdsGroupWithAddtionalFeature() {
        VDSGroup group = createDefaultVdsGroup();
        group.setCompatibilityVersion(VERSION_1_1);
        Set<SupportedAdditionalClusterFeature> addtionalFeaturesSupported = new HashSet<>();
        AdditionalFeature feature =
                new AdditionalFeature(DEFAULT_FEATURE_ID,
                        "TEST_FEATURE",
                        VERSION_1_1,
                        "Test Feature",
                        ApplicationMode.AllModes);
        addtionalFeaturesSupported.add(new SupportedAdditionalClusterFeature(group.getId(), true, feature));
        group.setAddtionalFeaturesSupported(addtionalFeaturesSupported);
        return group;
    }

    private static StoragePool createStoragePoolLocalFSOldVersion() {
        StoragePool pool = new StoragePool();
        pool.setIsLocal(true);
        pool.setCompatibilityVersion(VERSION_1_2);
        return pool;
    }

    private static StoragePool createStoragePoolLocalFS() {
        StoragePool pool = new StoragePool();
        pool.setIsLocal(true);
        return pool;
    }

    private void storagePoolIsLocalFS() {
        when(storagePoolDao.get(DC_ID1)).thenReturn(createStoragePoolLocalFS());
    }

    private void oldGroupIsDetachedDefault() {
        when(vdsGroupDao.get(DEFAULT_VDS_GROUP_ID)).thenReturn(createDetachedDefaultVdsGroup());
    }

    private void storagePoolAlreadyHasCluster() {
        VDSGroup group = new VDSGroup();
        List<VDSGroup> groupList = new ArrayList<VDSGroup>();
        groupList.add(group);
        when(vdsGroupDao.getAllForStoragePool(any(Guid.class))).thenReturn(groupList);
    }

    private void VdsExist() {
        VDS vds = new VDS();
        vds.setStatus(VDSStatus.Up);
        List<VDS> vdsList = new ArrayList<VDS>();
        vdsList.add(vds);
        when(vdsDao.getAllForVdsGroup(any(Guid.class))).thenReturn(vdsList);
    }

    private void VdsExistWithHigherVersion() {
        VDS vds = new VDS();
        vds.setStatus(VDSStatus.Up);
        vds.setVdsGroupCompatibilityVersion(VERSION_1_2);
        List<VDS> vdsList = new ArrayList<VDS>();
        vdsList.add(vds);
        when(vdsDao.getAllForVdsGroup(any(Guid.class))).thenReturn(vdsList);
    }

    private void allQueriesForVms() {
        when(vmDao.getAllForVdsGroup(any(Guid.class))).thenReturn(Collections.<VM> emptyList());
    }

    private void clusterHasVds() {
        VDS vds = new VDS();
        vds.setStatus(VDSStatus.Up);
        vds.setSupportedClusterLevels(VERSION_1_1.toString());
        List<VDS> vdsList = new ArrayList<VDS>();
        vdsList.add(vds);
        when(vdsDao.getAllForVdsGroup(any(Guid.class))).thenReturn(vdsList);
    }

    private void clusterHasGlusterVolumes() {
        List<GlusterVolumeEntity> volumes = new ArrayList<GlusterVolumeEntity>();
        volumes.add(new GlusterVolumeEntity());
        when(glusterVolumeDao.getByClusterId(any(Guid.class))).thenReturn(volumes);
    }

    private void clusterHasVMs() {
        VM vm = new VM();
        vm.setVdsGroupId(DEFAULT_VDS_GROUP_ID);
        List<VM> vmList = new ArrayList<VM>();
        vmList.add(vm);

        when(vmDao.getAllForVdsGroup(any(Guid.class))).thenReturn(vmList);
    }

    private void cpuFlagsMissing() {
        List<String> strings = new ArrayList<String>();
        strings.add("foo");
        doReturn(strings).when(cmd).missingServerCpuFlags(any(VDS.class));
    }

    private void cpuFlagsNotMissing() {
        doReturn(null).when(cmd).missingServerCpuFlags(any(VDS.class));
    }

    private void cpuManufacturersDontMatch() {
        doReturn(false).when(cmd).checkIfCpusSameManufacture(any(VDSGroup.class));
    }

    private void cpuManufacturersMatch() {
        doReturn(true).when(cmd).checkIfCpusSameManufacture(any(VDSGroup.class));
    }

    private void cpuExists() {
        doReturn(true).when(cmd).checkIfCpusExist();
    }

    private void architectureIsUpdatable() {
        doReturn(true).when(cmd).isArchitectureUpdatable();
    }

    private void architectureIsNotUpdatable() {
        doReturn(false).when(cmd).isArchitectureUpdatable();
    }

    private void setValidCpuVersionMap() {
        mcr.mockConfigValue(ConfigValues.SupportedClusterLevels, versions);
    }

    private void canDoActionFailedWithReason(final EngineMessage message) {
        assertFalse(cmd.canDoAction());
        assertTrue(cmd.getReturnValue().getCanDoActionMessages().contains(message.toString()));
    }
}
