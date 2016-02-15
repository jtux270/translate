package org.ovirt.engine.core.bll.gluster.tasks;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.gluster.GlusterTasksSyncJob;
import org.ovirt.engine.core.bll.job.ExecutionContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.job.JobRepository;
import org.ovirt.engine.core.bll.job.JobRepositoryFactory;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterAsyncTask;
import org.ovirt.engine.core.common.utils.SizeConverter;
import org.ovirt.engine.core.common.utils.SizeConverter.SizeUnit;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterTaskSupport;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeTaskStatusDetail;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.gluster.GlusterFeatureSupported;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.job.Step;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.gluster.GlusterAuditLogUtil;
import org.ovirt.engine.core.dal.job.ExecutionMessageDirector;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;
import org.ovirt.engine.core.utils.lock.EngineLock;
import org.ovirt.engine.core.utils.lock.LockManager;
import org.ovirt.engine.core.utils.lock.LockManagerFactory;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class GlusterTaskUtils {
    private static GlusterTaskUtils instance;

    private static final GlusterAuditLogUtil logUtil = GlusterAuditLogUtil.getInstance();
    private static final String REBALANCE_IN_PROGRESS = "IN PROGRESS";
    private static final String REMOVE_BRICK_FAILED = "MIGRATION FAILED";
    private static final String REMOVE_BRICK_IN_PROGRESS = "MIGRATION IN PROGRESS";
    private static final String REMOVE_BRICK_FINISHED = "MIGRATION COMPLETE";
    private static final Map<GlusterTaskType, String> taskTypeStrMap = new HashMap<>();
    private static final Map<GlusterTaskType, AuditLogType> taskTypeAuditMsg = new HashMap<>();
    static {
        taskTypeStrMap.put(GlusterTaskType.REBALANCE, "Rebalance");
        taskTypeStrMap.put(GlusterTaskType.REMOVE_BRICK, "Data Migration");
        taskTypeAuditMsg.put(GlusterTaskType.REBALANCE, AuditLogType.GLUSTER_VOLUME_REBALANCE_FINISHED);
        taskTypeAuditMsg.put(GlusterTaskType.REMOVE_BRICK, AuditLogType.GLUSTER_VOLUME_MIGRATE_BRICK_DATA_FINISHED);
    }

    private static final Log log = LogFactory.getLog(GlusterTasksSyncJob.class);

    private GlusterTaskUtils() {
    }

    public static GlusterTaskUtils getInstance() {
        if (instance == null) {
            instance = new GlusterTaskUtils();
        }

        return instance;
    }

    public boolean isTaskOfType(GlusterTaskSupport supportObj, GlusterTaskType type) {
        if (supportObj.getAsyncTask() != null && supportObj.getAsyncTask().getType() == type) {
            return true;
        }

        return false;
    }

    public boolean isTaskStatus(GlusterTaskSupport supportObj, JobExecutionStatus status) {
        if (supportObj.getAsyncTask() != null && supportObj.getAsyncTask().getStatus() == status) {
            return true;
        }

        return false;
    }

    /**
     * Releases the lock held on the cluster having given id and locking group {@link LockingGroup#GLUSTER}
     *
     * @param clusterId
     *            ID of the cluster on which the lock is to be released
     */
    public void releaseLock(Guid clusterId) {
        getLockManager().releaseLock(getEngineLock(clusterId));
    }

    /**
     * Returns an {@link EngineLock} instance that represents a lock on a cluster with given id and the locking group
     * {@link LockingGroup#GLUSTER}
     *
     * @param clusterId
     * @return
     */
    private EngineLock getEngineLock(Guid clusterId) {
        return new EngineLock(Collections.singletonMap(clusterId.toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.GLUSTER,
                        VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED)), null);
    }

    public void releaseVolumeLock(Guid taskId) {
        // get volume associated with task
        GlusterVolumeEntity vol = getVolumeDao().getVolumeByGlusterTask(taskId);

        if (vol != null) {
            // release lock on volume
            releaseLock(vol.getId());

        } else {
            log.debugFormat("Did not find a volume associated with task {0}", taskId);
        }
    }

    public void endStepJob(Step step) {
        getJobRepository().updateStep(step);
        ExecutionContext finalContext = ExecutionHandler.createFinalizingContext(step.getId());
        ExecutionHandler.endTaskJob(finalContext, isTaskSuccess(step.getStatus()));
    }

    public boolean isTaskSuccess(JobExecutionStatus status) {
        switch (status) {
        case ABORTED:
        case FAILED:
            return false;
        case FINISHED:
            return true;
        default:
            return false;
        }
    }

    public boolean hasTaskCompleted(GlusterAsyncTask task) {
        // Remove brick task is marked completed only if committed or aborted.
        if (JobExecutionStatus.ABORTED == task.getStatus() ||
                (JobExecutionStatus.FINISHED == task.getStatus() && task.getType() != GlusterTaskType.REMOVE_BRICK)
                || JobExecutionStatus.FAILED == task.getStatus()) {
            return true;
        }
        return false;
    }

    public String getTaskMessage(VDSGroup cluster, StepEnum stepType, GlusterAsyncTask task) {
        if (task == null) {
            return null;
        }
        Map<String, String> values = getMessageMap(cluster, task);

        return ExecutionMessageDirector.resolveStepMessage(stepType, values);
    }

    public Map<String, String> getMessageMap(VDSGroup cluster, GlusterAsyncTask task) {
        Map<String, String> values = new HashMap<String, String>();
        values.put(GlusterConstants.CLUSTER, cluster.getName());
        GlusterTaskParameters params = task.getTaskParameters();
        values.put(GlusterConstants.VOLUME, params != null ? params.getVolumeName() : "");
        String jobStatus = getJobStatusInfo(task);
        values.put(GlusterConstants.JOB_STATUS, jobStatus);
        values.put(GlusterConstants.JOB_INFO, task.getMessage());
        return values;
    }

    private String getJobStatusInfo(GlusterAsyncTask task) {
        String jobStatus = task.getStatus().toString();
        if (task.getType() == GlusterTaskType.REMOVE_BRICK) {
            switch (task.getStatus()) {
            case FINISHED:
                jobStatus = REMOVE_BRICK_FINISHED;
                break;
            case STARTED:
                jobStatus = REMOVE_BRICK_IN_PROGRESS;
                break;
            case FAILED:
                jobStatus = REMOVE_BRICK_FAILED;
                break;
            default:
                break;
            }
        }
        if (task.getType() == GlusterTaskType.REBALANCE) {
            switch (task.getStatus()) {
            case STARTED:
                jobStatus = REBALANCE_IN_PROGRESS;
                break;
            default:
                break;
            }
        }
        return jobStatus;
    }

    public void updateSteps(VDSGroup cluster, GlusterAsyncTask task, List<Step> steps) {
        // update status in step table
        for (Step step : steps) {
            if (step.getEndTime() != null) {
                // we have already processed the task
                continue;
            }
            JobExecutionStatus oldStatus = step.getStatus();
            step.setDescription(getTaskMessage(cluster, step.getStepType(), task));
            step.setStatus(task.getStatus());
            logEventMessage(task, oldStatus, cluster);
            if (hasTaskCompleted(task)) {
                step.markStepEnded(task.getStatus());
                endStepJob(step);
                releaseVolumeLock(task.getTaskId());
            } else {
                getJobRepository().updateStep(step);
            }
        }
    }

    public void logEventMessage(GlusterAsyncTask task, JobExecutionStatus oldStatus, VDSGroup cluster) {
        GlusterVolumeEntity volume = getVolumeDao().getVolumeByGlusterTask(task.getTaskId());
        if ( volume == null){
            if(task.getTaskParameters() != null) {
                String volName = task.getTaskParameters().getVolumeName();
                volume = getVolumeDao().getByName(cluster.getId(), volName);
            }
            else {
                return;
            }
        }
        if (JobExecutionStatus.ABORTED == task.getStatus() || JobExecutionStatus.FINISHED == task.getStatus() || JobExecutionStatus.FAILED == task.getStatus()){
            if(oldStatus != task.getStatus()){
                logMessage(cluster.getId(), volume , taskTypeStrMap.get(task.getType()), task.getStatus().name().toLowerCase(), taskTypeAuditMsg.get(task.getType()));
            }
        }
    }

    public boolean supportsGlusterAsyncTasksFeature(VDSGroup cluster) {
        return cluster.supportsGlusterService()
                && GlusterFeatureSupported.glusterAsyncTasks(cluster.getcompatibility_version());
    }

    public GlusterVolumeDao getVolumeDao() {
        return DbFacade.getInstance().getGlusterVolumeDao();
    }

    public JobRepository getJobRepository() {
        return JobRepositoryFactory.getJobRepository();
    }

    public LockManager getLockManager() {
        return LockManagerFactory.getLockManager();
    }
    @SuppressWarnings("serial")
    private void logMessage(Guid clusterId, GlusterVolumeEntity volume, final String action, final String status, AuditLogType logType) {
        logUtil.logAuditMessage(clusterId, volume, null, logType, new HashMap<String, String>(){
            {
                put("action", action);
                put("status", status);
            }});
    }

    public String getSummaryMessage(GlusterVolumeTaskStatusDetail statusSummary) {
        NumberFormat formatSize = NumberFormat.getInstance();
        formatSize.setMaximumFractionDigits(2);
        formatSize.setMinimumFractionDigits(2);
        Pair<SizeConverter.SizeUnit, Double> sizeMoved =
                SizeConverter.autoConvert(statusSummary.getTotalSizeMoved(), SizeUnit.BYTES);
        StringBuilder builder = new StringBuilder();
        builder.append("Files [scanned: ")
                .append(statusSummary.getFilesScanned())
                .append(", moved: ")
                .append(statusSummary.getFilesMoved())
                .append(", failed: ")
                .append(statusSummary.getFilesFailed())
                .append(", Total size moved: ")
                .append(new StringBuilder(formatSize.format(sizeMoved.getSecond().doubleValue()))
                        .append(" ")
                        .append(sizeMoved.getFirst().toString()));
        return builder.toString();
    }
}
