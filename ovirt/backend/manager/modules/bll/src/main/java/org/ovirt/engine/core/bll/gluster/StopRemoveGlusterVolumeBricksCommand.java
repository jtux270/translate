package org.ovirt.engine.core.bll.gluster;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.gluster.tasks.GlusterTaskUtils;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRemoveBricksParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeTaskStatusEntity;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeRemoveBricksVDSParameters;

/**
 * BLL command to stop remove brick asynchronous task started on a gluster volume
 */

@NonTransactiveCommandAttribute
public class StopRemoveGlusterVolumeBricksCommand extends GlusterAsyncCommandBase<GlusterVolumeRemoveBricksParameters> {

    public StopRemoveGlusterVolumeBricksCommand(GlusterVolumeRemoveBricksParameters params) {
        super(params);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REMOVE_BRICKS_STOP);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_BRICK);
        super.setActionMessageParameters();
    }

    @Override
    protected boolean canDoAction() {
        GlusterVolumeEntity volume = getGlusterVolume();

        if (!super.canDoAction()) {
            return false;
        }

        if (!validate(getBrickValidator().canStopOrCommitRemoveBrick(volume, getParameters().getBricks()))) {
            return false;
        }

        if (!(getGlusterTaskUtils().isTaskOfType(volume, GlusterTaskType.REMOVE_BRICK))
                || !(getGlusterTaskUtils().isTaskStatus(volume, JobExecutionStatus.STARTED) || getGlusterTaskUtils().isTaskStatus(volume,
                        JobExecutionStatus.FINISHED))) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_VOLUME_REMOVE_BRICKS_NOT_STARTED);
        }

        return true;
    }

    @Override
    protected StepEnum getStepType() {
        return StepEnum.REMOVING_BRICKS;
    }

    @Override
    protected void executeCommand() {
        GlusterVolumeEntity volume = getGlusterVolume();
        VDSReturnValue returnValue =
                runVdsCommand(VDSCommandType.StopRemoveGlusterVolumeBricks,
                        new GlusterVolumeRemoveBricksVDSParameters(getUpServer().getId(),
                                volume.getName(),
                                getParameters().getBricks()));

        setSucceeded(returnValue.getSucceeded());
        if (!getSucceeded()) {
            handleVdsError(AuditLogType.GLUSTER_VOLUME_REMOVE_BRICKS_FAILED, returnValue.getVdsError().getMessage());
            return;
        }

        GlusterVolumeTaskStatusEntity rebalanceStatusEntity =
                (GlusterVolumeTaskStatusEntity) returnValue.getReturnValue();
        endStepJobAborted(GlusterTaskUtils.getInstance().getSummaryMessage(rebalanceStatusEntity.getStatusSummary()));
        releaseVolumeLock();
        getReturnValue().setActionReturnValue(rebalanceStatusEntity);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_VOLUME_REMOVE_BRICKS_STOP;
        } else {
            return AuditLogType.GLUSTER_VOLUME_REMOVE_BRICKS_STOP_FAILED;
        }
    }

    @Override
    public BackendInternal getBackend() {
        return super.getBackend();
    }
}
