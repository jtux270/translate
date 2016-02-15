package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterHookManageParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookEntity;
import org.ovirt.engine.core.common.errors.VDSError;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterHookVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VdsDAO;


@RunWith(MockitoJUnitRunner.class)
public class RemoveGlusterHookCommandTest extends GlusterHookCommandTest<RemoveGlusterHookCommand> {
    /**
     * The command under test.
     */
    RemoveGlusterHookCommand cmd;

    @Mock
    private VdsDAO vdsDao;

    private void setUpMocksForRemove() {
        setUpMocksForRemove(true);
    }

    private void setUpMocksForRemove(boolean hookFound) {
        setUpMocksForRemove(hookFound, getHookEntity(), VDSStatus.Up);
    }

    private void setUpMocksForRemove(boolean hookFound, GlusterHookEntity hook, VDSStatus status) {
        setupMocks(cmd, hookFound, hook);
        doReturn(vdsDao).when(cmd).getVdsDAO();
        when(vdsDao.getAllForVdsGroup(any(Guid.class))).thenReturn(getServers(status));
    }

    private List<VDS> getServers(VDSStatus status) {
        List<VDS> servers = new ArrayList<VDS>();
        servers.add(getServer(GUIDS[0], "gfs1", CLUSTER_ID, status));
        servers.add(getServer(GUIDS[1], "gfs2", CLUSTER_ID, status));
        servers.add(getServer(GUIDS[2], "gfs3", CLUSTER_ID, status));
        servers.add(getServer(GUIDS[3], "gfs4", CLUSTER_ID, status));
        return servers;
    }

    private void mockBackend(boolean succeeded, VdcBllErrors errorCode) {
        when(cmd.getBackend()).thenReturn(backend);
        when(backend.getResourceManager()).thenReturn(vdsBrokerFrontend);

        VDSReturnValue vdsReturnValue = new VDSReturnValue();
        vdsReturnValue.setSucceeded(succeeded);
        if (!succeeded) {
            vdsReturnValue.setVdsError(new VDSError(errorCode, ""));
        }
        when(vdsBrokerFrontend.RunVdsCommand(eq(VDSCommandType.RemoveGlusterHook), argThat(anyHookVDS()))).thenReturn(vdsReturnValue);
     }

    private ArgumentMatcher<VDSParametersBase> anyHookVDS() {
        return new ArgumentMatcher<VDSParametersBase>() {

            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof GlusterHookVDSParameters)) {
                    return false;
                }
                return true;
            }
        };
    }

    @Test
    public void executeCommand() {
        cmd = spy(new RemoveGlusterHookCommand(new GlusterHookManageParameters(HOOK_ID)));
        setUpMocksForRemove();
        mockBackend(true, null);
        cmd.executeCommand();
        verify(hooksDao, times(1)).remove(any(Guid.class));
        assertEquals(cmd.getAuditLogTypeValue(), AuditLogType.GLUSTER_HOOK_REMOVED);
    }


    @Test
    public void executeCommandWhenFailed() {
        cmd = spy(new RemoveGlusterHookCommand(new GlusterHookManageParameters(HOOK_ID)));
        setUpMocksForRemove();
        mockBackend(false, VdcBllErrors.GlusterHookRemoveFailed);
        cmd.executeCommand();
        verify(hooksDao, never()).remove(any(Guid.class));
        assertEquals(cmd.getAuditLogTypeValue(), AuditLogType.GLUSTER_HOOK_REMOVE_FAILED);
    }

    @Test
    public void canDoActionSucceeds() {
        cmd = spy(new RemoveGlusterHookCommand(new GlusterHookManageParameters(HOOK_ID)));
        setUpMocksForRemove();
        assertTrue(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsOnNullHookId() {
        cmd = spy(new RemoveGlusterHookCommand(new GlusterHookManageParameters(null)));
        setUpMocksForRemove();
        assertFalse(cmd.canDoAction());
        assertTrue(cmd.getReturnValue().getCanDoActionMessages().contains(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_HOOK_ID_IS_REQUIRED.toString()));
    }

    @Test
    public void canDoActionFailsOnNoHook() {
        cmd = spy(new RemoveGlusterHookCommand(new GlusterHookManageParameters(HOOK_ID)));
        setUpMocksForRemove(false);
        assertFalse(cmd.canDoAction());
        assertTrue(cmd.getReturnValue().getCanDoActionMessages().contains(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_HOOK_DOES_NOT_EXIST.toString()));
    }

    @Test
    public void canDoActionFailsOnServerNotUp() {
        cmd = spy(new RemoveGlusterHookCommand(new GlusterHookManageParameters(HOOK_ID)));
        setUpMocksForRemove(true, getHookEntity(), VDSStatus.Down);
        assertFalse(cmd.canDoAction());
        assertTrue(cmd.getReturnValue().getCanDoActionMessages().contains(VdcBllMessages.ACTION_TYPE_FAILED_SERVER_STATUS_NOT_UP.toString()));
    }

}
