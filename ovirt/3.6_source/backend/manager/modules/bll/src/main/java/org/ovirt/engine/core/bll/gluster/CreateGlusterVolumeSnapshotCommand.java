package org.ovirt.engine.core.bll.gluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.utils.ClusterUtils;
import org.ovirt.engine.core.bll.utils.GlusterUtil;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.gluster.CreateGlusterVolumeSnapshotParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeGeoRepSessionParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.gluster.GeoRepSessionStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSession;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterSnapshotStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeSnapshotEntity;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.CreateGlusterVolumeSnapshotVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.lock.EngineLock;
import org.ovirt.engine.core.utils.lock.LockManagerFactory;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public class CreateGlusterVolumeSnapshotCommand extends GlusterSnapshotCommandBase<CreateGlusterVolumeSnapshotParameters> {

    private GlusterVolumeSnapshotEntity snapshot;
    private boolean force;
    private List<GlusterGeoRepSession> georepSessions;
    private List<GlusterGeoRepSession> enginePausedSessions;

    public CreateGlusterVolumeSnapshotCommand(CreateGlusterVolumeSnapshotParameters params) {
        super(params);
        this.snapshot = params.getSnapshot();
        this.force = params.getForce();
        this.enginePausedSessions = new ArrayList<>();

        if (this.snapshot != null) {
            setVdsGroupId(this.snapshot.getClusterId());
            setGlusterVolumeId(snapshot.getVolumeId());
            this.georepSessions = getDbFacade().getGlusterGeoRepDao().getGeoRepSessions(getGlusterVolumeId());
        }
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__CREATE);
        addCustomValue(GlusterConstants.VOLUME_SNAPSHOT_NAME, getParameters().getSnapshot().getSnapshotName());
        super.setActionMessageParameters();
    }

    private boolean pauseAndCreateSnapshotForGeoRepSessions() {
        if (georepSessions != null && georepSessions.size() > 0) {
            for (GlusterGeoRepSession session : georepSessions) {
                final GlusterVolumeEntity slaveVolume =
                        getDbFacade().getGlusterVolumeDao().getById(session.getSlaveVolumeId());

                if (slaveVolume == null) {
                    // Continue to other geo-rep sessions and pause them for snapshot purpose
                    continue;
                }

                VDS slaveUpServer = ClusterUtils.getInstance().getRandomUpServer(slaveVolume.getClusterId());
                if (slaveUpServer == null) {
                    handleVdsError(AuditLogType.GLUSTER_VOLUME_SNAPSHOT_CREATE_FAILED,
                            "No up server found in slave cluster of geo-rep session");
                    setSucceeded(false);
                    return false;
                }

                // Pause the geo-rep session if required
                if (!(session.getStatus() == GeoRepSessionStatus.CREATED
                        || session.getStatus() == GeoRepSessionStatus.PAUSED
                        || session.getStatus() == GeoRepSessionStatus.STOPPED)) {
                    VdcReturnValueBase sessionPauseRetVal = null;
                    try (EngineLock lock = acquireEngineLock(slaveVolume.getId(), LockingGroup.GLUSTER_SNAPSHOT)) {
                        sessionPauseRetVal =
                                runInternalAction(VdcActionType.PauseGlusterVolumeGeoRepSession,
                                        new GlusterVolumeGeoRepSessionParameters(getGlusterVolumeId(),
                                                session.getId()));
                    }
                    if (sessionPauseRetVal != null && !sessionPauseRetVal.getSucceeded()) {
                        handleVdsErrors(AuditLogType.GLUSTER_VOLUME_GEO_REP_PAUSE_FAILED,
                                sessionPauseRetVal.getExecuteFailedMessages());
                        setSucceeded(false);
                        return false;
                    }
                    session.setStatus(GeoRepSessionStatus.PAUSED);
                    enginePausedSessions.add(session);
                }

                // Create snapshot for slave volume
                VDSReturnValue snapCreationRetVal =
                        runVdsCommand(VDSCommandType.CreateGlusterVolumeSnapshot,
                                new CreateGlusterVolumeSnapshotVDSParameters(slaveUpServer.getId(),
                                        session.getSlaveVolumeName(),
                                        snapshot.getSnapshotName(),
                                        snapshot.getDescription(),
                                        force));
                if (!snapCreationRetVal.getSucceeded()) {
                    handleVdsError(AuditLogType.GLUSTER_VOLUME_SNAPSHOT_CREATE_FAILED,
                            snapCreationRetVal.getVdsError().getMessage());
                            setSucceeded(false);
                            return false;
                } else {
                    // Persist the snapshot details
                    GlusterVolumeSnapshotEntity slaveVolumeSnapshot =
                            (GlusterVolumeSnapshotEntity) snapCreationRetVal.getReturnValue();
                    slaveVolumeSnapshot.setClusterId(slaveVolume.getClusterId());
                    slaveVolumeSnapshot.setVolumeId(slaveVolume.getId());
                    slaveVolumeSnapshot.setDescription(snapshot.getDescription());
                    slaveVolumeSnapshot.setStatus(GlusterSnapshotStatus.DEACTIVATED);
                    getDbFacade().getGlusterVolumeSnapshotDao().save(slaveVolumeSnapshot);

                    // check if the snapshot soft limit reached now for the volume and alert
                    getGlusterUtil().alertVolumeSnapshotLimitsReached(slaveVolume);
                }
            }
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        GlusterVolumeEntity volume = getGlusterVolume();

        // Pause geo-rep sessions and create snapshot for slave volumes
        Boolean tranRetVal = TransactionSupport.executeInNewTransaction(new TransactionMethod<Boolean>() {
            @Override
            public Boolean runInTransaction() {
                return pauseAndCreateSnapshotForGeoRepSessions();
            }
        });

        if (!tranRetVal) {
            return;
        }

        // Create snapshot for the master volume
        VDSReturnValue retVal =
                runVdsCommand(VDSCommandType.CreateGlusterVolumeSnapshot,
                        new CreateGlusterVolumeSnapshotVDSParameters(upServer.getId(),
                                volume.getName(),
                                snapshot.getSnapshotName(),
                                snapshot.getDescription(),
                                force));

        setSucceeded(retVal.getSucceeded());

        if (!retVal.getSucceeded()) {
            handleVdsError(AuditLogType.GLUSTER_VOLUME_SNAPSHOT_CREATE_FAILED, retVal.getVdsError().getMessage());
        } else {
            GlusterVolumeSnapshotEntity createdSnapshot = (GlusterVolumeSnapshotEntity) retVal.getReturnValue();
            createdSnapshot.setClusterId(snapshot.getClusterId());
            createdSnapshot.setVolumeId(snapshot.getVolumeId());
            createdSnapshot.setDescription(snapshot.getDescription());
            createdSnapshot.setStatus(GlusterSnapshotStatus.DEACTIVATED);
            getDbFacade().getGlusterVolumeSnapshotDao().save(createdSnapshot);
            addCustomValue(GlusterConstants.VOLUME_SNAPSHOT_NAME, createdSnapshot.getSnapshotName());
            // check if the snapshot soft limit reached now for the volume and alert
            getGlusterUtil().alertVolumeSnapshotLimitsReached(getGlusterVolume());
        }

        // Resume the snapshot paused sessions by engine
        for (GlusterGeoRepSession session : enginePausedSessions) {
            if (session.getStatus() == GeoRepSessionStatus.PAUSED) {
                try (EngineLock lock = acquireGeoRepSessionLock(session.getId())) {
                    VdcReturnValueBase sessionResumeRetVal =
                            runInternalAction(VdcActionType.ResumeGeoRepSession,
                                    new GlusterVolumeGeoRepSessionParameters(volume.getId(), session.getId()));
                    if (!sessionResumeRetVal.getSucceeded()) {
                        handleVdsErrors(AuditLogType.GLUSTER_VOLUME_GEO_REP_RESUME_FAILED,
                                sessionResumeRetVal.getExecuteFailedMessages());
                        setSucceeded(false);
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        GlusterVolumeEntity volume = getGlusterVolume();
        if (volume.getStatus() == GlusterStatus.DOWN) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_GLUSTER_VOLUME_IS_DOWN);
        }

        if (volume.getAsyncTask() != null
                && (volume.getAsyncTask().getType() == GlusterTaskType.REBALANCE
                || volume.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK)
                && volume.getAsyncTask().getStatus() == JobExecutionStatus.STARTED) {
            addCanDoActionMessageVariable("asyncTask", volume.getAsyncTask().getType().name().toLowerCase());
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VOLUME_ASYNC_OPERATION_IN_PROGRESS);
        }

        if (!GlusterUtil.getInstance().isVolumeThinlyProvisioned(volume)) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_GLUSTER_VOLUME_IS_NOT_THINLY_PROVISIONED);
        }

        if (getDbFacade().getGlusterVolumeSnapshotDao().getByName(getGlusterVolumeId(), snapshot.getSnapshotName()) != null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_SNAPSHOT_ALREADY_EXISTS);
        }

        List<GlusterBrickEntity> bricks = volume.getBricks();
        for (GlusterBrickEntity brick : bricks) {
            if (!brick.isOnline()) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_ONE_OR_MORE_BRICKS_ARE_DOWN);
            }
        }

        for (GlusterGeoRepSession session : georepSessions) {
            if (session.getSlaveNodeUuid() == null || session.getSlaveVolumeId() == null) {
                // Slave cluster is not maintained by engine, so cannot pause geo-rep session and create snapshot for
                // the volume
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_REMOTE_CLUSTER_NOT_MAINTAINED_BY_ENGINE);
            }
        }

        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_VOLUME_SNAPSHOT_CREATED;
        } else {
            return errorType == null ? AuditLogType.GLUSTER_VOLUME_SNAPSHOT_CREATE_FAILED : errorType;
        }
    }

    protected EngineLock acquireGeoRepSessionLock(Guid id) {
        EngineLock lock = new EngineLock(Collections.singletonMap(id.toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.GLUSTER_GEOREP,
                        EngineMessage.ACTION_TYPE_FAILED_GEOREP_SESSION_LOCKED)), null);
        LockManagerFactory.getLockManager().acquireLockWait(lock);
        return lock;
    }
}
