package org.ovirt.engine.core.bll;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.ovirt.engine.core.common.action.WatchdogParameters;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.VmWatchdogAction;
import org.ovirt.engine.core.common.businessentities.VmWatchdogType;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.dao.VmDeviceDAO;

public class UpdateWatchdogCommandTest {

    private VmWatchdogType vmWatchdogType = VmWatchdogType.i6300esb;

    private static final Set<VmWatchdogType> WATCHDOG_MODELS = new HashSet<VmWatchdogType>(
            Arrays.asList(VmWatchdogType.i6300esb));

    @Test
    public void getSpecParams() {
        WatchdogParameters params = new WatchdogParameters();
        params.setAction(VmWatchdogAction.RESET);
        params.setModel(vmWatchdogType);
        UpdateWatchdogCommand command = new UpdateWatchdogCommand(params);
        HashMap<String, Object> specParams = command.getSpecParams();
        Assert.assertNotNull(specParams);
        Assert.assertEquals("i6300esb", specParams.get("model"));
        Assert.assertEquals("reset", specParams.get("action"));
    }

    @Test
    public void testCanDoActionNoVM() {
        WatchdogParameters params = new WatchdogParameters();
        params.setId(new Guid("a09f57b1-5739-4352-bf88-a6f834ed46db"));
        params.setAction(VmWatchdogAction.PAUSE);
        params.setModel(vmWatchdogType);
        final VmDAO vmDaoMock = mock(VmDAO.class);
        when(vmDaoMock.get(new Guid("a09f57b1-5739-4352-bf88-a6f834ed46db"))).thenReturn(null);
        UpdateWatchdogCommand command = new UpdateWatchdogCommand(params) {
            @Override
            public VmDAO getVmDAO() {
                return vmDaoMock;
            }
        };

        Assert.assertFalse(command.canDoAction());
    }

    @Test
    public void testCanDoAction() {
        WatchdogParameters params = new WatchdogParameters();
        Guid vmGuid = new Guid("a09f57b1-5739-4352-bf88-a6f834ed46db");
        params.setId(vmGuid);
        params.setAction(VmWatchdogAction.PAUSE);
        params.setModel(vmWatchdogType);
        final VmDAO vmDaoMock = mock(VmDAO.class);
        when(vmDaoMock.get(vmGuid)).thenReturn(new VM());
        final VmDeviceDAO deviceDAO = mock(VmDeviceDAO.class);
        when(deviceDAO.getVmDeviceByVmIdAndType(vmGuid, VmDeviceGeneralType.WATCHDOG)).thenReturn(Collections.singletonList(new VmDevice()));
        UpdateWatchdogCommand command = new UpdateWatchdogCommand(params) {
            @Override
            public VmDAO getVmDAO() {
                return vmDaoMock;
            }

            @Override
            protected VmDeviceDAO getVmDeviceDao() {
                return deviceDAO;
            }
        };

        OsRepository osRepository = mock(OsRepository.class);
        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);
        when(osRepository.getVmWatchdogTypes(any(Integer.class), any(Version.class))).thenReturn(WATCHDOG_MODELS);
        VmWatchdog vmWatchdog = spy(new VmWatchdog());
        when(vmWatchdog.getModel()).thenReturn(vmWatchdogType);

        Assert.assertTrue(command.canDoAction());
    }

}
