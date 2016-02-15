package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.AddVmParameters;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskImageBase;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.DiskImageDao;
import org.ovirt.engine.core.dao.SnapshotDao;
import org.ovirt.engine.core.dao.StorageDomainDao;
import org.ovirt.engine.core.dao.VdsGroupDao;
import org.ovirt.engine.core.dao.VmDao;
import org.ovirt.engine.core.dao.VmDeviceDao;
import org.ovirt.engine.core.dao.VmTemplateDao;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("serial")
public class AddVmCommandTest {

    private static final Guid STORAGE_DOMAIN_ID_1 = Guid.newGuid();
    private static final Guid STORAGE_DOMAIN_ID_2 = Guid.newGuid();
    protected static final int TOTAL_NUM_DOMAINS = 2;
    private static final int NUM_DISKS_STORAGE_DOMAIN_1 = 3;
    private static final int NUM_DISKS_STORAGE_DOMAIN_2 = 3;
    private static final int REQUIRED_DISK_SIZE_GB = 10;
    private static final int AVAILABLE_SPACE_GB = 11;
    private static final int USED_SPACE_GB = 4;
    private static int MAX_PCI_SLOTS = 26;
    private static final Guid STORAGE_POOL_ID = Guid.newGuid();
    private VmTemplate vmTemplate = null;
    private VDSGroup vdsGroup = null;
    private StoragePool storagePool = null;
    protected StorageDomainValidator storageDomainValidator;

    private static final Map<String, String> migrationMap = new HashMap<>();

    static {
        migrationMap.put("undefined", "true");
        migrationMap.put("x86_64", "true");
        migrationMap.put("ppc64", "false");
    }

    @Rule
    public MockConfigRule mcr = new MockConfigRule();

    @Mock
    StorageDomainDao sdDao;

    @Mock
    VmTemplateDao vmTemplateDao;

    @Mock
    VmDao vmDao;

    @Mock
    DiskImageDao diskImageDao;

    @Mock
    VdsGroupDao vdsGroupDao;

    @Mock
    BackendInternal backend;

    @Mock
    VDSBrokerFrontend vdsBrokerFrontend;

    @Mock
    SnapshotDao snapshotDao;

    @Mock
    OsRepository osRepository;

    @Mock
    VmDeviceDao deviceDao;

    @Mock
    DbFacade dbFacade;

    @Before
    public void InitTest() {
        mockOsRepository();
        SimpleDependecyInjector.getInstance().bind(DbFacade.class, dbFacade);
    }

    @Test
    public void create10GBVmWith11GbAvailableAndA5GbBuffer() throws Exception {
        VM vm = createVm();
        AddVmFromTemplateCommand<AddVmParameters> cmd = createVmFromTemplateCommand(vm);

        mockStorageDomainDaoGetForStoragePool();
        mockVdsGroupDaoReturnVdsGroup();
        mockVmTemplateDaoReturnVmTemplate();
        mockDiskImageDaoGetSnapshotById();
        mockVerifyAddVM(cmd);
        mockConfig();
        mockMaxPciSlots();

        mockOsRepository();
        mockOsRepositoryGraphics(0, Version.v3_3, new Pair<GraphicsType, DisplayType>(GraphicsType.SPICE, DisplayType.qxl));
        mockGraphicsDevices(vm.getId());

        mockStorageDomainDaoGetAllStoragesForPool(AVAILABLE_SPACE_GB);
        mockUninterestingMethods(cmd);
        mockGetAllSnapshots(cmd);
        doReturn(createStoragePool()).when(cmd).getStoragePool();
        assertFalse("If the disk is too big, canDoAction should fail", cmd.canDoAction());
        assertTrue("canDoAction failed for the wrong reason",
                cmd.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(EngineMessage.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN.toString()));
    }

    private void mockGraphicsDevices(Guid vmId) {
        VmDevice graphicsDevice = new GraphicsDevice(VmDeviceType.SPICE);
        graphicsDevice.setDeviceId(Guid.Empty);
        graphicsDevice.setVmId(vmId);

        when(deviceDao.getVmDeviceByVmIdAndType(vmId, VmDeviceGeneralType.GRAPHICS)).thenReturn(Arrays.asList(graphicsDevice));
        doReturn(deviceDao).when(dbFacade).getVmDeviceDao();
    }

    private void mockOsRepositoryGraphics(int osId, Version ver, Pair<GraphicsType, DisplayType> supportedGraphicsAndDisplay) {
        HashMap<Version, List<Pair<GraphicsType, DisplayType>>> value = new HashMap<Version, List<Pair<GraphicsType, DisplayType>>>();
        value.put(ver, Collections.singletonList(supportedGraphicsAndDisplay));

        HashMap<Integer, Map<Version, List<Pair<GraphicsType, DisplayType>>>> g = new HashMap<>();
        g.put(osId, value);
        when(osRepository.getGraphicsAndDisplays()).thenReturn(g);
    }

    protected void mockOsRepository() {
        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);
        VmHandler.init();
        when(osRepository.isWindows(0)).thenReturn(true);
        when(osRepository.isCpuSupported(anyInt(), any(Version.class), anyString())).thenReturn(true);
    }

    @Test
    public void canAddVm() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 20;
        final int sizeRequired = 5;
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired);
        cmd.postConstruct();
        doReturn(true).when(cmd).validateCustomProperties(any(VmStatic.class), any(ArrayList.class));
        doReturn(true).when(cmd).validateSpaceRequirements();
        assertTrue("vm could not be added", cmd.canAddVm(reasons, Arrays.asList(createStorageDomain(domainSizeGB))));
    }

    @Test
    public void canAddCloneVmFromSnapshotSnapshotDoesNotExist() {
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        final Guid sourceSnapshotId = Guid.newGuid();
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                setupCanAddVmFromSnapshotTests(domainSizeGB, sizeRequired, sourceSnapshotId);
        cmd.getVm().setName("vm1");
        mockNonInterestingMethodsForCloneVmFromSnapshot(cmd);
        assertFalse("Clone vm should have failed due to non existing snapshot id", cmd.canDoAction());
        ArrayList<String> reasons = cmd.getReturnValue().getCanDoActionMessages();
        assertTrue("Clone vm should have failed due to non existing snapshot id",
                reasons.contains(EngineMessage.ACTION_TYPE_FAILED_VM_SNAPSHOT_DOES_NOT_EXIST.toString()));
    }

    @Test
    public void canAddCloneVmFromSnapshotNoConfiguration() {
        final int domainSizeGB = 15;
        final int sizeRequired = 4;
        final Guid sourceSnapshotId = Guid.newGuid();
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                setupCanAddVmFromSnapshotTests(domainSizeGB, sizeRequired, sourceSnapshotId);
        cmd.getVm().setName("vm1");
        mockNonInterestingMethodsForCloneVmFromSnapshot(cmd);
        SnapshotsValidator sv = spy(new SnapshotsValidator());
        doReturn(ValidationResult.VALID).when(sv).vmNotDuringSnapshot(any(Guid.class));
        doReturn(sv).when(cmd).createSnapshotsValidator();
        when(snapshotDao.get(sourceSnapshotId)).thenReturn(new Snapshot());
        assertFalse("Clone vm should have failed due to non existing vm configuration", cmd.canDoAction());
        ArrayList<String>  reasons = cmd.getReturnValue().getCanDoActionMessages();
        assertTrue("Clone vm should have failed due to no configuration id",
                reasons.contains(EngineMessage.ACTION_TYPE_FAILED_VM_SNAPSHOT_HAS_NO_CONFIGURATION.toString()));

    }

    @Test
    public void canAddVmWithVirtioScsiControllerNotSupportedOs() {
        VM vm = createVm();
        AddVmFromTemplateCommand<AddVmParameters> cmd = createVmFromTemplateCommand(vm);
        VDSGroup vdsGroup = createVdsGroup();

        mockStorageDomainDaoGetForStoragePool();
        mockVmTemplateDaoReturnVmTemplate();
        mockDiskImageDaoGetSnapshotById();
        mockVerifyAddVM(cmd);
        mockConfig();
        mockMaxPciSlots();
        mockStorageDomainDaoGetAllStoragesForPool(20);
        mockUninterestingMethods(cmd);
        mockDisplayTypes(vm.getOs(), vdsGroup.getCompatibilityVersion());
        mockGraphicsDevices(vm.getId());
        doReturn(true).when(cmd).checkCpuSockets();

        doReturn(vdsGroup).when(cmd).getVdsGroup();
        doReturn(createStoragePool()).when(cmd).getStoragePool();
        cmd.getParameters().setVirtioScsiEnabled(true);
        when(osRepository.getArchitectureFromOS(any(Integer.class))).thenReturn(ArchitectureType.x86_64);
        when(osRepository.getDiskInterfaces(any(Integer.class), any(Version.class))).thenReturn(
                new ArrayList<>(Arrays.asList("VirtIO")));
        mockGetAllSnapshots(cmd);
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_OS_TYPE_DOES_NOT_SUPPORT_VIRTIO_SCSI);
    }

    @Test
    public void isVirtioScsiEnabledDefaultedToTrue() {
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(0, 0);
        doReturn(createVdsGroup()).when(cmd).getVdsGroup();
        when(osRepository.getDiskInterfaces(any(Integer.class), any(Version.class))).thenReturn(
                new ArrayList<>(Arrays.asList("VirtIO_SCSI")));
        assertTrue("isVirtioScsiEnabled hasn't been defaulted to true on cluster >= 3.3.", cmd.isVirtioScsiEnabled());
    }

    @Test
    public void validateSpaceAndThreshold() {
        AddVmCommand<AddVmParameters> command = setupCanAddVmTests(0, 0);
        doReturn(ValidationResult.VALID).when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(ValidationResult.VALID).when(storageDomainValidator).hasSpaceForNewDisks(anyList());
        doReturn(storageDomainValidator).when(command).createStorageDomainValidator(any(StorageDomain.class));
        assertTrue(command.validateSpaceRequirements());
        verify(storageDomainValidator, times(TOTAL_NUM_DOMAINS)).hasSpaceForNewDisks(anyList());
        verify(storageDomainValidator, never()).hasSpaceForClonedDisks(anyList());
    }

    @Test
    public void validateSpaceNotEnough() throws Exception {
        AddVmCommand<AddVmParameters> command = setupCanAddVmTests(0, 0);
        doReturn(ValidationResult.VALID).when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN)).
                when(storageDomainValidator).hasSpaceForNewDisks(anyList());
        doReturn(storageDomainValidator).when(command).createStorageDomainValidator(any(StorageDomain.class));
        assertFalse(command.validateSpaceRequirements());
        verify(storageDomainValidator).hasSpaceForNewDisks(anyList());
        verify(storageDomainValidator, never()).hasSpaceForClonedDisks(anyList());
    }

    @Test
    public void validateSpaceNotWithinThreshold() throws Exception {
        AddVmCommand<AddVmParameters> command = setupCanAddVmTests(0, 0);
        doReturn((new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN))).
               when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(storageDomainValidator).when(command).createStorageDomainValidator(any(StorageDomain.class));
        assertFalse(command.validateSpaceRequirements());
    }

    @Test
    public void testUnsupportedCpus() {
        // prepare a command to pass canDo action
        VM vm = createVm();
        vm.setVmOs(OsRepository.DEFAULT_X86_OS);
        VDSGroup vdsGroup = createVdsGroup();

        AddVmFromTemplateCommand<AddVmParameters> cmd = createVmFromTemplateCommand(vm);

        mockStorageDomainDaoGetForStoragePool();
        mockVmTemplateDaoReturnVmTemplate();
        mockDiskImageDaoGetSnapshotById();
        mockVerifyAddVM(cmd);
        mockConfig();
        mockMaxPciSlots();
        mockStorageDomainDaoGetAllStoragesForPool(20);
        mockDisplayTypes(vm.getOs(), vdsGroup.getCompatibilityVersion());
        mockUninterestingMethods(cmd);
        mockGetAllSnapshots(cmd);
        when(osRepository.getArchitectureFromOS(0)).thenReturn(ArchitectureType.x86_64);
        doReturn(createStoragePool()).when(cmd).getStoragePool();

        // prepare the mock values
        HashMap<Pair<Integer, Version>, Set<String>> unsupported = new HashMap<>();
        HashSet<String> value = new HashSet<>();
        value.add(null);
        unsupported.put(new Pair<>(vm.getVmOsId(), vdsGroup.getCompatibilityVersion()), value);

        when(osRepository.isCpuSupported(vm.getVmOsId(), vdsGroup.getCompatibilityVersion(), null)).thenReturn(false);
        when(osRepository.getUnsupportedCpus()).thenReturn(unsupported);
        when(cmd.isCpuSupported(vm)).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                AddVmFromTemplateCommand<AddVmParameters> self =
                        (AddVmFromTemplateCommand<AddVmParameters>) invocationOnMock.getMock();
                self.getReturnValue().getCanDoActionMessages().add(
                        EngineMessage.CPU_TYPE_UNSUPPORTED_FOR_THE_GUEST_OS.name());
                return false;
            }

        });

        CanDoActionTestUtils.runAndAssertCanDoActionFailure(
                cmd,
                EngineMessage.CPU_TYPE_UNSUPPORTED_FOR_THE_GUEST_OS);
    }


    private void mockDisplayTypes(int osId, Version clusterVersion) {
        Map<Integer, Map<Version, List<Pair<GraphicsType, DisplayType>>>> displayTypeMap = new HashMap<>();
        displayTypeMap.put(osId, new HashMap<Version, List<Pair<GraphicsType, DisplayType>>>());
        displayTypeMap.get(osId).put(null, Arrays.asList(new Pair<>(GraphicsType.SPICE, DisplayType.qxl)));
        when(osRepository.getGraphicsAndDisplays()).thenReturn(displayTypeMap);
    }

    protected void mockNonInterestingMethodsForCloneVmFromSnapshot(AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd) {
        mockUninterestingMethods(cmd);
        doReturn(true).when(cmd).checkCpuSockets();
        doReturn(null).when(cmd).getVmFromConfiguration();
    }

    private void mockMaxPciSlots() {
        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);
        doReturn(MAX_PCI_SLOTS).when(osRepository).getMaxPciDevices(anyInt(), any(Version.class));
    }

    protected AddVmFromTemplateCommand<AddVmParameters> createVmFromTemplateCommand(VM vm) {
        AddVmParameters param = new AddVmParameters();
        param.setVm(vm);
        AddVmFromTemplateCommand<AddVmParameters> concrete =
                new AddVmFromTemplateCommand<AddVmParameters>(param) {
                    @Override
                    protected void initTemplateDisks() {
                        // Stub for testing
                    }

                    @Override
                    protected void initStoragePoolId() {
                        // Stub for testing
                    }

                    @Override
                    public VmTemplate getVmTemplate() {
                        return createVmTemplate();
                    }
                };
        AddVmFromTemplateCommand<AddVmParameters> result = spy(concrete);
        doReturn(true).when(result).checkNumberOfMonitors();
        doReturn(createVmTemplate()).when(result).getVmTemplate();
        doReturn(true).when(result).validateCustomProperties(any(VmStatic.class), any(ArrayList.class));
        mockDaos(result);
        mockBackend(result);
        initCommandMethods(result);
        result.postConstruct();
        return result;
    }

    private AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> createVmFromSnapshotCommand(VM vm,
            Guid sourceSnapshotId) {
        AddVmFromSnapshotParameters param = new AddVmFromSnapshotParameters();
        param.setVm(vm);
        param.setSourceSnapshotId(sourceSnapshotId);
        param.setStorageDomainId(Guid.newGuid());
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd =
                new AddVmFromSnapshotCommand<AddVmFromSnapshotParameters>(param) {
                    @Override
                    protected void initTemplateDisks() {
                        // Stub for testing
                    }

                    @Override
                    protected void initStoragePoolId() {
                        // Stub for testing
                    }

                    @Override
                    public VmTemplate getVmTemplate() {
                        return createVmTemplate();
                    }
                };
        cmd = spy(cmd);
        doReturn(vm).when(cmd).getVm();
        mockDaos(cmd);
        doReturn(snapshotDao).when(cmd).getSnapshotDao();
        mockBackend(cmd);
        return cmd;
    }

    protected AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> setupCanAddVmFromSnapshotTests(final int domainSizeGB,
            final int sizeRequired,
            Guid sourceSnapshotId) {
        VM vm = initializeMock(domainSizeGB, sizeRequired);
        initializeVmDaoMock(vm);
        AddVmFromSnapshotCommand<AddVmFromSnapshotParameters> cmd = createVmFromSnapshotCommand(vm, sourceSnapshotId);
        initCommandMethods(cmd);
        return cmd;
    }

    private void initializeVmDaoMock(VM vm) {
        when(vmDao.get(Matchers.<Guid>any(Guid.class))).thenReturn(vm);
    }

    private AddVmCommand<AddVmParameters> setupCanAddVmTests(final int domainSizeGB,
            final int sizeRequired) {
        VM vm = initializeMock(domainSizeGB, sizeRequired);
        AddVmCommand<AddVmParameters> cmd = createCommand(vm);
        initCommandMethods(cmd);
        doReturn(createVmTemplate()).when(cmd).getVmTemplate();
        doReturn(createStoragePool()).when(cmd).getStoragePool();
        return cmd;
    }

    private static <T extends AddVmParameters> void initCommandMethods(AddVmCommand<T> cmd) {
        doReturn(Guid.newGuid()).when(cmd).getStoragePoolId();
        doReturn(true).when(cmd).canAddVm(anyListOf(String.class), anyString(), any(Guid.class), anyInt());
        doReturn(STORAGE_POOL_ID).when(cmd).getStoragePoolId();
        doReturn(true).when(cmd).isCpuSupported(any(VM.class));
    }

    private VM initializeMock(final int domainSizeGB, final int sizeRequired) {
        mockVmTemplateDaoReturnVmTemplate();
        mockDiskImageDaoGetSnapshotById();
        mockStorageDomainDaoGetForStoragePool(domainSizeGB);
        mockStorageDomainDaoGet(domainSizeGB);
        mockConfig();
        VM vm = createVm();
        return vm;
    }

    private void mockBackend(AddVmCommand<?> cmd) {
        when(backend.getResourceManager()).thenReturn(vdsBrokerFrontend);
        doReturn(backend).when(cmd).getBackend();
    }

    private void mockDaos(AddVmCommand<?> cmd) {
        doReturn(vmDao).when(cmd).getVmDao();
        doReturn(sdDao).when(cmd).getStorageDomainDao();
        doReturn(vmTemplateDao).when(cmd).getVmTemplateDao();
        doReturn(vdsGroupDao).when(cmd).getVdsGroupDao();
        doReturn(deviceDao).when(cmd).getVmDeviceDao();
    }

    private void mockStorageDomainDaoGetForStoragePool(int domainSpaceGB) {
        when(sdDao.getForStoragePool(Matchers.<Guid> any(Guid.class), Matchers.<Guid> any(Guid.class))).thenReturn(createStorageDomain(domainSpaceGB));
    }

    private void mockStorageDomainDaoGet(final int domainSpaceGB) {
        doAnswer(new Answer<StorageDomain>() {

            @Override
            public StorageDomain answer(InvocationOnMock invocation) throws Throwable {
                StorageDomain result = createStorageDomain(domainSpaceGB);
                result.setId((Guid) invocation.getArguments()[0]);
                return result;
            }

        }).when(sdDao).get(any(Guid.class));
    }

    private void mockStorageDomainDaoGetAllStoragesForPool(int domainSpaceGB) {
        when(sdDao.getAllForStoragePool(any(Guid.class))).thenReturn(Arrays.asList(createStorageDomain(domainSpaceGB)));
    }

    private void mockStorageDomainDaoGetForStoragePool() {
        mockStorageDomainDaoGetForStoragePool(AVAILABLE_SPACE_GB);
    }

    private void mockVmTemplateDaoReturnVmTemplate() {
        when(vmTemplateDao.get(Matchers.<Guid> any(Guid.class))).thenReturn(createVmTemplate());
    }

    private void mockVdsGroupDaoReturnVdsGroup() {
        when(vdsGroupDao.get(Matchers.<Guid>any(Guid.class))).thenReturn(createVdsGroup());
    }

    private VmTemplate createVmTemplate() {
        if (vmTemplate == null) {
            vmTemplate = new VmTemplate();
            vmTemplate.setStoragePoolId(STORAGE_POOL_ID);
            DiskImage image = createDiskImageTemplate();
            vmTemplate.getDiskTemplateMap().put(image.getImageId(), image);
            HashMap<Guid, DiskImage> diskImageMap = new HashMap<Guid, DiskImage>();
            DiskImage diskImage = createDiskImage(REQUIRED_DISK_SIZE_GB);
            diskImageMap.put(diskImage.getId(), diskImage);
            vmTemplate.setDiskImageMap(diskImageMap);
        }
        return vmTemplate;
    }

    private VDSGroup createVdsGroup() {
        if (vdsGroup == null) {
            vdsGroup = new VDSGroup();
            vdsGroup.setVdsGroupId(Guid.newGuid());
            vdsGroup.setCompatibilityVersion(Version.v3_3);
            vdsGroup.setCpuName("Intel Conroe Family");
            vdsGroup.setArchitecture(ArchitectureType.x86_64);
        }

        return vdsGroup;
    }

    private StoragePool createStoragePool() {
        if (storagePool == null) {
            storagePool = new StoragePool();
            storagePool.setId(STORAGE_POOL_ID);
            storagePool.setStatus(StoragePoolStatus.Up);
        }
        return storagePool;
    }


    private static DiskImage createDiskImageTemplate() {
        DiskImage i = new DiskImage();
        i.setSizeInGigabytes(USED_SPACE_GB + AVAILABLE_SPACE_GB);
        i.setActualSizeInBytes(REQUIRED_DISK_SIZE_GB * 1024L * 1024L * 1024L);
        i.setImageId(Guid.newGuid());
        i.setStorageIds(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID_1)));
        return i;
    }

    private void mockDiskImageDaoGetSnapshotById() {
        when(diskImageDao.getSnapshotById(Matchers.<Guid> any(Guid.class))).thenReturn(createDiskImage(REQUIRED_DISK_SIZE_GB));
    }

    private static DiskImage createDiskImage(int size) {
        DiskImage diskImage = new DiskImage();
        diskImage.setSizeInGigabytes(size);
        diskImage.setActualSize(size);
        diskImage.setId(Guid.newGuid());
        diskImage.setImageId(Guid.newGuid());
        diskImage.setStorageIds(new ArrayList<Guid>(Arrays.asList(STORAGE_DOMAIN_ID_1)));
        return diskImage;
    }

    protected StorageDomain createStorageDomain(int availableSpace) {
        StorageDomain sd = new StorageDomain();
        sd.setStorageDomainType(StorageDomainType.Master);
        sd.setStatus(StorageDomainStatus.Active);
        sd.setAvailableDiskSize(availableSpace);
        sd.setUsedDiskSize(USED_SPACE_GB);
        sd.setId(STORAGE_DOMAIN_ID_1);
        return sd;
    }

    private static void mockVerifyAddVM(AddVmCommand<?> cmd) {
        doReturn(true).when(cmd).verifyAddVM(anyListOf(String.class), anyInt());
    }

    private void mockConfig() {
        mcr.mockConfigValue(ConfigValues.PredefinedVMProperties, Version.v3_0, "");
        mcr.mockConfigValue(ConfigValues.UserDefinedVMProperties, Version.v3_0, "");
        mcr.mockConfigValue(ConfigValues.InitStorageSparseSizeInGB, 1);
        mcr.mockConfigValue(ConfigValues.VirtIoScsiEnabled, Version.v3_3, true);
        mcr.mockConfigValue(ConfigValues.ValidNumOfMonitors, Arrays.asList("1,2,4".split(",")));
        mcr.mockConfigValue(ConfigValues.IsMigrationSupported, Version.v3_3, migrationMap);
        mcr.mockConfigValue(ConfigValues.MaxIoThreadsPerVm, 127);
    }

    protected static VM createVm() {
        VM vm = new VM();
        VmDynamic dynamic = new VmDynamic();
        VmStatic stat = new VmStatic();
        stat.setVmtGuid(Guid.newGuid());
        stat.setName("testVm");
        stat.setPriority(1);
        vm.setStaticData(stat);
        vm.setDynamicData(dynamic);
        vm.setSingleQxlPci(false);
        return vm;
    }

    private AddVmCommand<AddVmParameters> createCommand(VM vm) {
        AddVmParameters param = new AddVmParameters(vm);
        AddVmCommand<AddVmParameters> cmd = new AddVmCommand<AddVmParameters>(param) {
            @Override
            protected void initTemplateDisks() {
                // Stub for testing
            }

            @Override
            protected void initStoragePoolId() {
                // stub for testing
            }

            @Override
            public VmTemplate getVmTemplate() {
                return createVmTemplate();
            }
        };
        cmd = spy(cmd);
        mockDaos(cmd);
        mockBackend(cmd);
        doReturn(new VDSGroup()).when(cmd).getVdsGroup();
        generateStorageToDisksMap(cmd);
        initDestSDs(cmd);
        storageDomainValidator = mock(StorageDomainValidator.class);
        doReturn(ValidationResult.VALID).when(storageDomainValidator).isDomainWithinThresholds();
        doReturn(storageDomainValidator).when(cmd).createStorageDomainValidator(any(StorageDomain.class));
        return cmd;
    }

     protected void generateStorageToDisksMap(AddVmCommand<? extends AddVmParameters> command) {
        command.storageToDisksMap = new HashMap<Guid, List<DiskImage>>();
        command.storageToDisksMap.put(STORAGE_DOMAIN_ID_1, generateDisksList(NUM_DISKS_STORAGE_DOMAIN_1));
        command.storageToDisksMap.put(STORAGE_DOMAIN_ID_2, generateDisksList(NUM_DISKS_STORAGE_DOMAIN_2));
    }

    private static List<DiskImage> generateDisksList(int size) {
        List<DiskImage> disksList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            DiskImage diskImage = createDiskImage(REQUIRED_DISK_SIZE_GB);
            disksList.add(diskImage);
        }
        return disksList;
    }

    protected void initDestSDs(AddVmCommand<? extends AddVmParameters> command) {
        StorageDomain sd1 = new StorageDomain();
        StorageDomain sd2 = new StorageDomain();
        sd1.setId(STORAGE_DOMAIN_ID_1);
        sd2.setId(STORAGE_DOMAIN_ID_2);
        command.destStorages.put(STORAGE_DOMAIN_ID_1, sd1);
        command.destStorages.put(STORAGE_DOMAIN_ID_2, sd2);
    }

    protected List<DiskImage> createDiskSnapshot(Guid diskId, int numOfImages) {
        List<DiskImage> disksList = new ArrayList<>();
        for (int i = 0; i < numOfImages; ++i) {
            DiskImage diskImage = new DiskImage();
            diskImage.setActive(false);
            diskImage.setId(diskId);
            diskImage.setImageId(Guid.newGuid());
            diskImage.setParentId(Guid.newGuid());
            diskImage.setImageStatus(ImageStatus.OK);
            disksList.add(diskImage);
        }
        return disksList;
    }

    private void mockGetAllSnapshots(AddVmFromTemplateCommand<AddVmParameters> command) {
        doAnswer(new Answer<List<DiskImage>>() {
            @Override
            public List<DiskImage> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                DiskImage arg = (DiskImage) args[0];
                List<DiskImage> list  = createDiskSnapshot(arg.getId(), 3);
                return list;
            }
        }).when(command).getAllImageSnapshots(any(DiskImage.class));
    }



    private <T extends AddVmParameters> void mockUninterestingMethods(AddVmCommand<T> spy) {
        doReturn(true).when(spy).isVmNameValidLength(Matchers.<VM> any(VM.class));
        doReturn(false).when(spy).isVmWithSameNameExists(anyString(), any(Guid.class));
        doReturn(STORAGE_POOL_ID).when(spy).getStoragePoolId();
        doReturn(createVmTemplate()).when(spy).getVmTemplate();
        doReturn(createVdsGroup()).when(spy).getVdsGroup();
        doReturn(true).when(spy).areParametersLegal(anyListOf(String.class));
        doReturn(Collections.<VmNetworkInterface> emptyList()).when(spy).getVmInterfaces();
        doReturn(Collections.<DiskImageBase> emptyList()).when(spy).getVmDisks();
        doReturn(false).when(spy).isVirtioScsiControllerAttached(any(Guid.class));
        spy.setVmTemplateId(Guid.newGuid());
    }

    @Test
    public void testBeanValidations() {
        assertTrue(createCommand(initializeMock(1, 1)).validateInputs());
    }

    @Test
    public void testPatternBasedNameFails() {
        AddVmCommand<AddVmParameters> cmd = createCommand(initializeMock(1, 1));
        cmd.getParameters().getVm().setName("aa-??bb");
        assertFalse("Pattern-based name should not be supported for VM", cmd.validateInputs());
    }

    @Test
    public void refuseBalloonOnPPC() {
        ArrayList<String> reasons = new ArrayList<String>();
        final int domainSizeGB = 20;
        final int sizeRequired = 5;
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired);
        doReturn(true).when(cmd).validateCustomProperties(any(VmStatic.class), any(ArrayList.class));
        doReturn(true).when(cmd).validateSpaceRequirements();

        cmd.getParameters().setBalloonEnabled(true);
        cmd.getParameters().getVm().setClusterArch(ArchitectureType.ppc64);
        VDSGroup cluster = new VDSGroup();
        cluster.setArchitecture(ArchitectureType.ppc64);
        cluster.setCompatibilityVersion(Version.getLast());
        doReturn(cluster).when(cmd).getVdsGroup();
        doReturn(true).when(cmd).buildAndCheckDestStorageDomains();
        when(osRepository.isBalloonEnabled(cmd.getParameters().getVm().getVmOsId(), cluster.getCompatibilityVersion())).thenReturn(false);
        assertFalse(cmd.canDoAction());
        assertTrue(cmd.getReturnValue()
                .getCanDoActionMessages()
                .contains(EngineMessage.BALLOON_REQUESTED_ON_NOT_SUPPORTED_ARCH.toString()));
    }

    @Test
    public void testStoragePoolDoesntExist() {
        final int domainSizeGB = 20;
        final int sizeRequired = 5;
        AddVmCommand<AddVmParameters> cmd = setupCanAddVmTests(domainSizeGB, sizeRequired);

        doReturn(null).when(cmd).getStoragePool();

        assertFalse(cmd.canDoAction());
        assertTrue(cmd.getReturnValue()
                .getCanDoActionMessages()
                .contains(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_EXIST.toString()));
    }
}
