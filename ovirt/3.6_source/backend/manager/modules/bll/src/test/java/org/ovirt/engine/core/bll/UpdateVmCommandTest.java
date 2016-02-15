package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.VmDao;
import org.ovirt.engine.core.dao.VmDeviceDao;
import org.ovirt.engine.core.utils.MockConfigRule;

/** A test case for the {@link UpdateVmCommand}. */
@RunWith(MockitoJUnitRunner.class)
public class UpdateVmCommandTest {

    private VM vm;
    private VmStatic vmStatic;
    private UpdateVmCommand<VmManagementParametersBase> command;
    private VDSGroup group;

    private static String vncKeyboardLayoutValues =
            "ar,da,de,de-ch,en-gb,en-us,es,et,fi,fo,fr,fr-be,fr-ca,fr-ch,hr,hu,is,it,ja,lt,lv,mk,nl,nl-be,no,pl,pt,pt-br,ru,sl,sv,th,tr";
    @Mock
    private VmDao vmDao;
    @Mock
    private VdsDao vdsDao;
    @Mock
    private DiskDao diskDao;
    @Mock
    private VmDeviceDao vmDeviceDao;

    @Mock
    OsRepository osRepository;

    @Mock
    DbFacade dbFacade;

    private static final Map<String, String> migrationMap = new HashMap<>();

    static {
        migrationMap.put("undefined", "true");
        migrationMap.put("x86_64", "true");
        migrationMap.put("ppc64", "false");
    }

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.MaxVmNameLengthWindows, 15),
            mockConfig(ConfigValues.MaxVmNameLengthNonWindows, 64),
            mockConfig(ConfigValues.SupportedClusterLevels,
                    new HashSet<Version>(Arrays.asList(Version.v3_0, Version.v3_1))),
            mockConfig(ConfigValues.VMMinMemorySizeInMB, 256),
            mockConfig(ConfigValues.VM32BitMaxMemorySizeInMB, 20480),
            mockConfig(ConfigValues.PredefinedVMProperties, "3.1", ""),
            mockConfig(ConfigValues.UserDefinedVMProperties, "3.1", ""),
            mockConfig(ConfigValues.PredefinedVMProperties, "3.0", ""),
            mockConfig(ConfigValues.UserDefinedVMProperties, "3.0", ""),
            mockConfig(ConfigValues.VmPriorityMaxValue, 100),
            mockConfig(ConfigValues.MaxNumOfVmCpus, "3.0", 16),
            mockConfig(ConfigValues.MaxNumOfVmSockets, "3.0", 16),
            mockConfig(ConfigValues.MaxNumOfCpuPerSocket, "3.0", 16),
            mockConfig(ConfigValues.MaxNumOfVmCpus, "3.3", 16),
            mockConfig(ConfigValues.MaxNumOfVmSockets, "3.3", 16),
            mockConfig(ConfigValues.MaxNumOfCpuPerSocket, "3.3", 16),
            mockConfig(ConfigValues.VirtIoScsiEnabled, Version.v3_3.toString(), true),
            mockConfig(ConfigValues.VncKeyboardLayoutValidValues, Arrays.asList(vncKeyboardLayoutValues.split(","))),
            mockConfig(ConfigValues.ValidNumOfMonitors, Arrays.asList("1,2,4".split(","))),
            mockConfig(ConfigValues.IsMigrationSupported, Version.v3_0.toString(), migrationMap),
            mockConfig(ConfigValues.IsMigrationSupported, Version.v3_3.toString(), migrationMap),
            mockConfig(ConfigValues.MaxIoThreadsPerVm, 127)
            );

    @Before
    public void setUp() {
        final int osId = 0;
        final Version version = Version.v3_0;

        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);
        SimpleDependecyInjector.getInstance().bind(DbFacade.class, dbFacade);

        when(osRepository.getMinimumRam(osId, version)).thenReturn(0);
        when(osRepository.getMinimumRam(osId, null)).thenReturn(0);
        when(osRepository.getMaximumRam(osId, version)).thenReturn(256);
        when(osRepository.getMaximumRam(osId, null)).thenReturn(256);
        when(osRepository.isWindows(osId)).thenReturn(false);
        when(osRepository.getArchitectureFromOS(osId)).thenReturn(ArchitectureType.x86_64);
        when(osRepository.isCpuSupported(anyInt(), any(Version.class), anyString())).thenReturn(true);

        Map<Integer, Map<Version, List<Pair<GraphicsType, DisplayType>>>> displayTypeMap = new HashMap<>();
        displayTypeMap.put(osId, new HashMap<Version, List<Pair<GraphicsType, DisplayType>>>());
        displayTypeMap.get(osId).put(version, Arrays.asList(new Pair<>(GraphicsType.SPICE, DisplayType.qxl)));
        when(osRepository.getGraphicsAndDisplays()).thenReturn(displayTypeMap);

        VmHandler.init();
        vm = new VM();
        vmStatic = new VmStatic();
        group = new VDSGroup();
        group.setCpuName("Intel Conroe Family");
        group.setId(Guid.newGuid());
        group.setCompatibilityVersion(version);
        group.setArchitecture(ArchitectureType.x86_64);

        vm.setVdsGroupId(group.getId());
        vm.setClusterArch(ArchitectureType.x86_64);
        vmStatic.setVdsGroupId(group.getId());
        vmStatic.setName("my_vm");

        VmManagementParametersBase params = new VmManagementParametersBase();
        params.setCommandType(VdcActionType.UpdateVm);
        params.setVmStaticData(vmStatic);

        command = spy(new UpdateVmCommand<VmManagementParametersBase>(params) {
            @Override
            public VDSGroup getVdsGroup() {
                return group;
            }
        });
        doReturn(vm).when(command).getVm();

        doReturn(false).when(command).isVirtioScsiEnabledForVm(any(Guid.class));
        doReturn(true).when(command).isBalloonEnabled();
        doReturn(true).when(osRepository).isBalloonEnabled(vm.getVmOsId(), group.getCompatibilityVersion());
        doReturn(true).when(command).isCpuSupported(vm);
    }

    @Test
    public void testBeanValidations() {
        assertTrue(command.validateInputs());
    }

    @Test
    public void testPatternBasedNameFails() {
        vmStatic.setName("aa-??bb");
        assertFalse("Pattern-based name should not be supported for VM", command.validateInputs());
    }

    @Test
    public void testLongName() {
        vmStatic.setName("this_should_be_very_long_vm_name_so_it will_fail_can_do_action_validation");
        assertFalse("canDoAction should fail for too long vm name.", command.canDoAction());
        assertCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_NAME_LENGTH_IS_TOO_LONG);
    }

    @Test
    public void testValidName() {
        prepareVmToPassCanDoAction();
        mockVmValidator();

        boolean c = command.canDoAction();
        assertTrue("canDoAction should have passed.", c);
    }

    @Test
    public void testChangeToExistingName() {
        prepareVmToPassCanDoAction();
        mockSameNameQuery(true);

        assertFalse("canDoAction should have failed with vm name already in use.", command.canDoAction());
        assertCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_NAME_ALREADY_USED);
    }

    @Test
    public void testNameNotChanged() {
        prepareVmToPassCanDoAction();
        vm.setName("vm1");
        mockSameNameQuery(true);
        mockVmValidator();

        assertTrue("canDoAction should have passed.", command.canDoAction());
    }

    @Test
    public void testDedicatedHostNotExistOrNotSameCluster() {
        prepareVmToPassCanDoAction();

        // this will cause null to return when getting vds from vdsDao
        doReturn(vdsDao).when(command).getVdsDao();
        doReturn(false).when(command).isDedicatedVdsExistOnSameCluster(any(VmBase.class), any(ArrayList.class));

        vmStatic.setDedicatedVmForVdsList(Guid.newGuid());

        assertFalse("canDoAction should have failed with invalid dedicated host.", command.canDoAction());
    }

    @Test
    public void testValidDedicatedHost() {
        prepareVmToPassCanDoAction();
        mockVmValidator();

        VDS vds = new VDS();
        vds.setVdsGroupId(group.getId());
        doReturn(vdsDao).when(command).getVdsDao();
        when(vdsDao.get(any(Guid.class))).thenReturn(vds);
        doReturn(true).when(command).isDedicatedVdsExistOnSameCluster(any(VmBase.class), any(ArrayList.class));
        vmStatic.setDedicatedVmForVdsList(Guid.newGuid());

        assertTrue("canDoAction should have passed.", command.canDoAction());
    }

    @Test
    public void testInvalidNumberOfMonitors() {
        prepareVmToPassCanDoAction();
        vmStatic.setNumOfMonitors(99);

        assertFalse("canDoAction should have failed with invalid number of monitors.", command.canDoAction());
        assertCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_NUM_OF_MONITORS);
    }

    private void mockGraphicsDevice() {
        VmDevice graphicsDevice = new GraphicsDevice(VmDeviceType.SPICE);
        graphicsDevice.setDeviceId(Guid.Empty);
        graphicsDevice.setVmId(vm.getId());

        mockVmDevice(graphicsDevice);
    }

    private void mockVmDevice(VmDevice vmDevice) {
        when(vmDeviceDao.getVmDeviceByVmIdAndType(vm.getId(), vmDevice.getType())).thenReturn(Arrays.asList(vmDevice));
        doReturn(vmDeviceDao).when(dbFacade).getVmDeviceDao();
    }

    @Test
    public void testUpdateFieldsQuotaEnforcementType() {
        vm.setQuotaEnforcementType(QuotaEnforcementTypeEnum.DISABLED);
        vmStatic.setQuotaEnforcementType(QuotaEnforcementTypeEnum.SOFT_ENFORCEMENT);

        assertTrue("Quota enforcement type should be updatable", command.areUpdatedFieldsLegal());
    }

    @Test
    public void testUpdateFieldsQutoaDefault() {
        vm.setIsQuotaDefault(true);
        vmStatic.setQuotaDefault(false);

        assertTrue("Quota default should be updatable", command.areUpdatedFieldsLegal());
    }

    @Test
    public void testChangeClusterForbidden() {
        prepareVmToPassCanDoAction();
        vmStatic.setVdsGroupId(Guid.newGuid());

        assertFalse("canDoAction should have failed with can't change cluster.", command.canDoAction());
        assertCanDoActionMessage(EngineMessage.VM_CANNOT_UPDATE_CLUSTER);
    }

    @Test
    public void testCannotDisableVirtioScsi() {
        prepareVmToPassCanDoAction();
        command.getParameters().setVirtioScsiEnabled(false);

        Disk disk = new DiskImage();
        disk.setDiskInterface(DiskInterface.VirtIO_SCSI);
        disk.setPlugged(true);

        mockDiskDaoGetAllForVm(Collections.singletonList(disk), true);
        mockVmValidator();

        CanDoActionTestUtils.runAndAssertCanDoActionFailure(command,
                EngineMessage.CANNOT_DISABLE_VIRTIO_SCSI_PLUGGED_DISKS);
    }

    @Test
    public void testCanEditARunningVM() {
        prepareVmToPassCanDoAction();
        vm.setStatus(VMStatus.Up);
        mockDiskDaoGetAllForVm(Collections.<Disk>emptyList(), true);
        mockVmValidator();

        doReturn(vmDeviceDao).when(command).getVmDeviceDao();
        doReturn(true).when(command).areUpdatedFieldsLegal();

        CanDoActionTestUtils.runAndAssertCanDoActionSuccess(command);
    }

    @Test
    public void testUnsupportedCpus() {
        prepareVmToPassCanDoAction();

        // prepare the mock values
        HashMap<Pair<Integer, Version>, Set<String>> unsupported = new HashMap<>();
        HashSet<String> value = new HashSet<>();
        value.add(null);
        unsupported.put(new Pair<>(0, Version.v3_0), value);

        when(osRepository.isCpuSupported(0, Version.v3_0, null)).thenReturn(false);
        when(osRepository.getUnsupportedCpus()).thenReturn(unsupported);
        when(command.isCpuSupported(vm)).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                UpdateVmCommand<VmManagementParametersBase> self =
                        (UpdateVmCommand<VmManagementParametersBase>) invocationOnMock.getMock();
                self.getReturnValue().getCanDoActionMessages().add(
                        EngineMessage.CPU_TYPE_UNSUPPORTED_FOR_THE_GUEST_OS.name());
                return false;
            }

        });

        CanDoActionTestUtils.runAndAssertCanDoActionFailure(
                command,
                EngineMessage.CPU_TYPE_UNSUPPORTED_FOR_THE_GUEST_OS);
    }

    public void testCannotUpdateOSNotSupportVirtioScsi() {
        prepareVmToPassCanDoAction();
        group.setCompatibilityVersion(Version.v3_3);

        when(command.isVirtioScsiEnabledForVm(any(Guid.class))).thenReturn(true);
        when(osRepository.getDiskInterfaces(any(Integer.class), any(Version.class))).thenReturn(
                new ArrayList<>(Arrays.asList("VirtIO")));

        CanDoActionTestUtils.runAndAssertCanDoActionFailure(command,
                EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_OS_TYPE_DOES_NOT_SUPPORT_VIRTIO_SCSI);
    }

    @Test
    public void testMigratoinCanBeSetWhenVMUsesScsiReservation() {
        prepareVmToPassCanDoAction();
        vm.setMigrationSupport(MigrationSupport.MIGRATABLE);
        VmDevice device = createVmDevice();
        device.setUsingScsiReservation(false);

        mockVmDevice(device);
        mockVmValidator();

        CanDoActionTestUtils.runAndAssertCanDoActionSuccess(command);
    }

    private void mockVmValidator() {
        VmValidator vmValidator = spy(new VmValidator(vm));
        doReturn(vmValidator).when(command).createVmValidator(vm);
        doReturn(dbFacade).when(vmValidator).getDbFacade();
        doReturn(diskDao).when(vmValidator).getDiskDao();
    }

    private VmDevice createVmDevice() {
        return new VmDevice(new VmDeviceId(Guid.Empty, vm.getId()),
                VmDeviceGeneralType.DISK,
                "device",
                "address",
                1,
                new HashMap<String, Object>(),
                true,
                true,
                true,
                "alias",
                new HashMap<String, String>(),
                Guid.newGuid(),
                "logical",
                true);
    }

    private void prepareVmToPassCanDoAction() {
        vmStatic.setName("vm1");
        vmStatic.setMemSizeMb(256);
        vmStatic.setSingleQxlPci(false);
        mockVmDaoGetVm();
        mockSameNameQuery(false);
        mockValidateCustomProperties();
        mockValidatePciAndIdeLimit();
        doReturn(true).when(command).setAndValidateCpuProfile();
        mockGraphicsDevice();
    }

    private void assertCanDoActionMessage(EngineMessage msg) {
        assertTrue("canDoAction failed for the wrong reason",
                command.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(msg.name()));
    }

    private void mockDiskDaoGetAllForVm(List<Disk> disks, boolean onlyPluggedDisks) {
        doReturn(diskDao).when(command).getDiskDao();
        doReturn(disks).when(diskDao).getAllForVm(vm.getId(), onlyPluggedDisks);
    }

    private void mockVmDaoGetVm() {
        doReturn(vmDao).when(command).getVmDao();
        when(vmDao.get(any(Guid.class))).thenReturn(vm);
    }

    private void mockValidateCustomProperties() {
        doReturn(true).when(command).validateCustomProperties(any(VmStatic.class), any(ArrayList.class));
    }

    private void mockValidatePciAndIdeLimit() {
        doReturn(true).when(command).isValidPciAndIdeLimit(any(VM.class));
    }

    private void mockSameNameQuery(boolean result) {
        doReturn(result).when(command).isVmWithSameNameExists(anyString(), any(Guid.class));
    }
}
