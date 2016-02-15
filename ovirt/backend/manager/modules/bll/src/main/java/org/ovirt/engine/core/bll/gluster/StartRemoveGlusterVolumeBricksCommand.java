package org.ovirt.engine.core.bll.gluster;

import java.util.List;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.validator.gluster.GlusterBrickValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRemoveBricksParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterAsyncTask;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.common.validation.group.gluster.RemoveBrick;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeRemoveBricksVDSParameters;

/**
 * BLL command to Start Remove Bricks from Gluster volume. Before removing the brick, it will migrate the contents in
 * the brick(s) being removed in an async task. Use has to call commit to remove the brick(s) after the completion of
 * the migration task.
 */
@NonTransactiveCommandAttribute
public class StartRemoveGlusterVolumeBricksCommand extends GlusterAsyncCommandBase<GlusterVolumeRemoveBricksParameters> {

    public StartRemoveGlusterVolumeBricksCommand(GlusterVolumeRemoveBricksParameters params) {
        super(params);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Command);
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(RemoveBrick.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REMOVE_BRICKS_START);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_BRICK);
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        GlusterBrickValidator brickValidator = new GlusterBrickValidator();
        return validate(brickValidator.canRemoveBrick(getParameters().getBricks(),
                getGlusterVolume(),
                getParameters().getReplicaCount(), false));
    }

    @Override
    protected void executeCommand() {
        int replicaCount =
                getGlusterVolume().getVolumeType().isReplicatedType() ? getParameters().getReplicaCount() : 0;

        startSubStep();
        VDSReturnValue returnValue =
                runVdsCommand(
                        VDSCommandType.StartRemoveGlusterVolumeBricks,
                        new GlusterVolumeRemoveBricksVDSParameters(upServer.getId(),
                                getGlusterVolumeName(), getParameters().getBricks(), replicaCount, false));
        setSucceeded(returnValue.getSucceeded());
        if (!getSucceeded()) {
            handleVdsError(AuditLogType.START_REMOVING_GLUSTER_VOLUME_BRICKS_FAILED, returnValue.getVdsError()
                    .getMessage());
            return;
        }
        GlusterAsyncTask glusterTask = (GlusterAsyncTask) returnValue.getReturnValue();
        handleTaskReturn(glusterTask);
        updateBricksWithTaskID(glusterTask);
        getReturnValue().setActionReturnValue(returnValue.getReturnValue());
    }

    protected void updateBricksWithTaskID(GlusterAsyncTask asyncTask) {
        for (GlusterBrickEntity brickEntity : getParameters().getBricks()) {
            brickEntity.getAsyncTask().setTaskId(asyncTask.getTaskId());
        }

        getGlusterBrickDao().updateBrickTasksInBatch(getParameters().getBricks());

        getGlusterVolumeDao().updateVolumeTask(getGlusterVolumeId(), asyncTask.getTaskId());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.START_REMOVING_GLUSTER_VOLUME_BRICKS;
        } else {
            return errorType == null ? AuditLogType.START_REMOVING_GLUSTER_VOLUME_BRICKS_FAILED : errorType;
        }
    }

    @Override
    protected StepEnum getStepType() {
        return StepEnum.REMOVING_BRICKS;
    }

    @Override
    protected void freeLock() {
        // We have to keep the lock acquired on gluster volume if start remove brick was success, otherwsie we should
        // release all the locks.
        if (!getSucceeded()) {
            super.freeLock();
        }
    }
}
