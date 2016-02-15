package org.ovirt.engine.core.bll.gluster;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.gluster.tasks.GlusterTaskUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRebalanceParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeTaskStatusEntity;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeVDSParameters;

/**
 * BLL command to Stop the active Rebalancing on a Gluster volume.
 */

@NonTransactiveCommandAttribute
public class StopRebalanceGlusterVolumeCommand extends GlusterAsyncCommandBase<GlusterVolumeRebalanceParameters> {

    public StopRebalanceGlusterVolumeCommand(GlusterVolumeRebalanceParameters params) {
        super(params);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REBALANCE_STOP);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_VOLUME);
        super.setActionMessageParameters();
    }

    @Override
    protected boolean canDoAction() {
        GlusterVolumeEntity glusterVolume = getGlusterVolume();
        if (!super.canDoAction()) {
            return false;
        }

        if (!(getGlusterTaskUtils().isTaskOfType(glusterVolume, GlusterTaskType.REBALANCE))
                || !(getGlusterTaskUtils().isTaskStatus(glusterVolume, JobExecutionStatus.STARTED))) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_VOLUME_REBALANCE_NOT_STARTED);
        }
        return true;
    }

    @Override
    protected StepEnum getStepType() {
        return StepEnum.REBALANCING_VOLUME;
    }

    @Override
    protected void executeCommand() {
        VDSReturnValue vdsReturnaValue =
                runVdsCommand(VDSCommandType.StopRebalanceGlusterVolume,
                        new GlusterVolumeVDSParameters(upServer.getId(),
                                getGlusterVolumeName()));
        if (!vdsReturnaValue.getSucceeded()) {
            handleVdsError(AuditLogType.GLUSTER_VOLUME_REBALANCE_STOP_FAILED, vdsReturnaValue.getVdsError()
                    .getMessage());
            setSucceeded(false);
            return;
        }

        GlusterVolumeTaskStatusEntity rebalanceStatusEntity =
                (GlusterVolumeTaskStatusEntity) vdsReturnaValue.getReturnValue();
        JobExecutionStatus stepStatus = rebalanceStatusEntity.getStatusSummary().getStatus();
        if (stepStatus != null) {
            endStepJob(stepStatus,
                    getStepMessageMap(stepStatus,
                            GlusterTaskUtils.getInstance().getSummaryMessage(rebalanceStatusEntity.getStatusSummary())),
                    GlusterTaskUtils.getInstance().isTaskSuccess(stepStatus));

        } else {
            endStepJob(JobExecutionStatus.ABORTED, getStepMessageMap(JobExecutionStatus.ABORTED, null), false);
        }
        releaseVolumeLock();
        setSucceeded(vdsReturnaValue.getSucceeded());
        getReturnValue().setActionReturnValue(rebalanceStatusEntity);

    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_VOLUME_REBALANCE_STOP;
        } else {
            return errorType == null ? AuditLogType.GLUSTER_VOLUME_REBALANCE_STOP_FAILED : errorType;
        }
    }
}
