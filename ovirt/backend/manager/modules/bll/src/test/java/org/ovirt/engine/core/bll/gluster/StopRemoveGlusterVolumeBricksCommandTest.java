package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.validator.gluster.GlusterBrickValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRemoveBricksParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterAsyncTask;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeTaskStatusEntity;
import org.ovirt.engine.core.common.errors.VDSError;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeVDSParameters;
import org.ovirt.engine.core.compat.Guid;

@RunWith(MockitoJUnitRunner.class)
public class StopRemoveGlusterVolumeBricksCommandTest extends AbstractRemoveGlusterVolumeBricksCommandTest {
    private final Guid volumeWithRemoveBricksTaskCompleted = new Guid("b2cb2f73-fab3-4a42-93f0-d5e4c069a43e");

    /**
     * The command under test.
     */
    private StopRemoveGlusterVolumeBricksCommand cmd;

    private void prepareMocks(StopRemoveGlusterVolumeBricksCommand command) {
        doReturn(volumeDao).when(command).getGlusterVolumeDao();
        GlusterBrickValidator brickValidator = spy(command.getBrickValidator());
        doReturn(brickDao).when(brickValidator).getGlusterBrickDao();
        doReturn(brickValidator).when(command).getBrickValidator();
        doReturn(getVds(VDSStatus.Up)).when(command).getUpServer();
        doReturn(getVolumeWithRemoveBricksTask(volumeWithRemoveBricksTask)).when(volumeDao)
                .getById(volumeWithRemoveBricksTask);
        doReturn(getBricks(volumeWithoutRemoveBricksTask)).when(brickDao)
                .getGlusterVolumeBricksByTaskId(any(Guid.class));
        doReturn(getVolumeWithRemoveBricksTaskCompleted(volumeWithRemoveBricksTaskCompleted)).when(volumeDao)
                .getById(volumeWithRemoveBricksTaskCompleted);
        doReturn(getVolume(volumeWithoutAsyncTask)).when(volumeDao).getById(volumeWithoutAsyncTask);
        doReturn(getVolumeWithoutRemoveBricksTask(volumeWithoutRemoveBricksTask)).when(volumeDao)
                .getById(volumeWithoutRemoveBricksTask);
        doReturn(null).when(volumeDao).getById(null);
    }

    private Object getVolumeWithRemoveBricksTaskCompleted(Guid volumeId) {
        GlusterVolumeEntity volume = getVolumeWithRemoveBricksTask(volumeId);
        volume.getAsyncTask().setStatus(JobExecutionStatus.FINISHED);
        return volume;
    }

    @Override
    protected GlusterVolumeEntity getVolumeWithRemoveBricksTask(Guid volumeId) {
        GlusterVolumeEntity volume = getVolume(volumeId);
        GlusterAsyncTask asyncTask = new GlusterAsyncTask();
        asyncTask.setStatus(JobExecutionStatus.STARTED);
        asyncTask.setType(GlusterTaskType.REMOVE_BRICK);
        volume.setAsyncTask(asyncTask);
        return volume;
    }

    private void mockBackend(boolean succeeded, VdcBllErrors errorCode) {
        when(cmd.getBackend()).thenReturn(backend);
        when(backend.getResourceManager()).thenReturn(vdsBrokerFrontend);
        doNothing().when(cmd).endStepJobAborted(any(String.class));
        doNothing().when(cmd).releaseVolumeLock();

        VDSReturnValue vdsReturnValue = new VDSReturnValue();
        vdsReturnValue.setSucceeded(succeeded);
        if (!succeeded) {
            vdsReturnValue.setVdsError(new VDSError(errorCode, ""));
        } else {
            vdsReturnValue.setReturnValue(new GlusterVolumeTaskStatusEntity());
        }

        when(vdsBrokerFrontend.RunVdsCommand(eq(VDSCommandType.StopRemoveGlusterVolumeBricks),
                argThat(anyGlusterVolumeVDS()))).thenReturn(vdsReturnValue);
    }

    private ArgumentMatcher<VDSParametersBase> anyGlusterVolumeVDS() {
        return new ArgumentMatcher<VDSParametersBase>() {

            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof GlusterVolumeVDSParameters)) {
                    return false;
                }
                return true;
            }
        };
    }

    @Test
    public void testExecuteCommand() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithRemoveBricksTask,
                        getBricks(volumeWithRemoveBricksTask))));
        prepareMocks(cmd);
        mockBackend(true, null);
        assertTrue(cmd.canDoAction());
        cmd.executeCommand();

        verify(cmd, times(1)).endStepJobAborted(any(String.class));
        verify(cmd, times(1)).releaseVolumeLock();
        assertEquals(cmd.getAuditLogTypeValue(), AuditLogType.GLUSTER_VOLUME_REMOVE_BRICKS_STOP);
    }

    @Test
    public void executeCommandWhenFailed() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithRemoveBricksTask,
                        getBricks(volumeWithRemoveBricksTask))));
        prepareMocks(cmd);
        mockBackend(false, VdcBllErrors.GlusterVolumeRemoveBricksStopFailed);
        assertTrue(cmd.canDoAction());
        cmd.executeCommand();

        verify(cmd, never()).endStepJobAborted(any(String.class));
        verify(cmd, never()).releaseVolumeLock();
        assertEquals(cmd.getAuditLogTypeValue(), AuditLogType.GLUSTER_VOLUME_REMOVE_BRICKS_STOP_FAILED);
    }

    @Test
    public void canDoActionSucceedsOnVolumeWithRemoveBricksTask() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithRemoveBricksTask,
                        getBricks(volumeWithRemoveBricksTask))));

        prepareMocks(cmd);
        assertTrue(cmd.canDoAction());
    }

    // This happens in retain bricks scenario, where user can call stop remove brick after migrating the data
    @Test
    public void canDoActionSucceedsOnVolumeWithRemoveBricksTaskCompleted() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithRemoveBricksTaskCompleted,
                        getBricks(volumeWithRemoveBricksTaskCompleted))));
        prepareMocks(cmd);
        assertTrue(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsOnVolumeWithoutAsyncTask() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithoutAsyncTask,
                        getBricks(volumeWithoutAsyncTask))));
        prepareMocks(cmd);
        assertFalse(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsOnVolumeWithoutRemoveBricksTask() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithoutRemoveBricksTask,
                        getBricks(volumeWithoutRemoveBricksTask))));
        prepareMocks(cmd);
        assertFalse(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsOnNull() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(null,
                        getBricks(volumeWithRemoveBricksTaskCompleted))));
        prepareMocks(cmd);
        assertFalse(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsWithEmptyBricksList() {
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithoutRemoveBricksTask,
                        new ArrayList<GlusterBrickEntity>())));
        prepareMocks(cmd);
        assertFalse(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsWithInvalidBricks() {
        List<GlusterBrickEntity> paramBricks1 = getInvalidNoOfBricks(volumeWithRemoveBricksTask);
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithRemoveBricksTask,
                        paramBricks1)));
        prepareMocks(cmd);
        assertFalse(cmd.canDoAction());

        List<GlusterBrickEntity> paramBricks2 = getInvalidBricks(volumeWithRemoveBricksTask);
        cmd =
                spy(new StopRemoveGlusterVolumeBricksCommand(new GlusterVolumeRemoveBricksParameters(volumeWithRemoveBricksTask,
                        paramBricks2)));
        prepareMocks(cmd);
        assertFalse(cmd.canDoAction());
    }
}
