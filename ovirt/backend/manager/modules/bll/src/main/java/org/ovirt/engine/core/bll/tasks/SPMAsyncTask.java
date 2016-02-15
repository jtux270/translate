package org.ovirt.engine.core.bll.tasks;

import static org.ovirt.engine.core.common.config.ConfigValues.UnknownTaskPrePollingLapse;

import java.util.Map;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCoordinator;
import org.ovirt.engine.core.bll.tasks.interfaces.SPMTask;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskParameters;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatusEnum;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class SPMAsyncTask implements SPMTask {

    protected final CommandCoordinator coco;

    private boolean zombieTask;

    public SPMAsyncTask(CommandCoordinator coco, AsyncTaskParameters parameters) {
        this.coco = coco;
        setParameters(parameters);
        setState(AsyncTaskState.Initializing);
    }

    private AsyncTaskParameters privateParameters;

    private Map<Guid, VdcObjectType> entitiesMap;

    public Map<Guid, VdcObjectType> getEntitiesMap() {
        return entitiesMap;
    }

    public void setEntitiesMap(Map<Guid, VdcObjectType> entitiesMap) {
        this.entitiesMap = entitiesMap;
    }

    public AsyncTaskParameters getParameters() {
        return privateParameters;
    }

    public void setParameters(AsyncTaskParameters value) {
        privateParameters = value;
    }

    public Guid getVdsmTaskId() {
        return getParameters().getVdsmTaskId();
    }

    public Guid getStoragePoolID() {
        return getParameters().getStoragePoolID();
    }

    private AsyncTaskState privateState = AsyncTaskState.forValue(0);

    public AsyncTaskState getState() {
        return privateState;
    }

    public void setState(AsyncTaskState value) {
        privateState = value;
    }

    public boolean getShouldPoll() {
        AsyncTaskState state = getState();
        return (state == AsyncTaskState.Polling || state == AsyncTaskState.Ended || state == AsyncTaskState.ClearFailed)
                && getLastTaskStatus().getStatus() != AsyncTaskStatusEnum.unknown
                && (getParameters().getEntityInfo() == null ? isTaskOverPrePollingLapse() : true);
    }

    private AsyncTaskStatus _lastTaskStatus = new AsyncTaskStatus(AsyncTaskStatusEnum.init);

    @Override
    public AsyncTaskStatus getLastTaskStatus() {
        return _lastTaskStatus;
    }

    /**
     * Set the _lastTaskStatus with taskStatus.
     *
     * @param taskStatus
     *            - task status to set.
     */
    @Override
    public void setLastTaskStatus(AsyncTaskStatus taskStatus) {
        _lastTaskStatus = taskStatus;
    }

    /**
     * Update task last access date ,only for not active task.
     */
    @Override
    public void setLastStatusAccessTime() {
        // Change access date to now , when task is not active.
        if (getState() == AsyncTaskState.Ended
                || getState() == AsyncTaskState.AttemptingEndAction
                || getState() == AsyncTaskState.ClearFailed
                || getState() == AsyncTaskState.Cleared) {
            _lastAccessToStatusSinceEnd = System.currentTimeMillis();
        }
    }

    // Indicates time in milliseconds when task status recently changed.
    protected long _lastAccessToStatusSinceEnd = System.currentTimeMillis();

    @Override
    public long getLastAccessToStatusSinceEnd() {
        return _lastAccessToStatusSinceEnd;
    }

    @Override
    public Guid getCommandId() {
        return getParameters().getDbAsyncTask().getRootCommandId();
    }

    @Override
    public void startPollingTask() {
        AsyncTaskState state = getState();
        if (state != AsyncTaskState.AttemptingEndAction
                && state != AsyncTaskState.Cleared
                && state != AsyncTaskState.ClearFailed) {
            log.infoFormat("BaseAsyncTask::startPollingTask: Starting to poll task '{0}'.", getVdsmTaskId());
            concreteStartPollingTask();
        }
    }

    /**
     * Use this to hold unknown tasks from polling, to overcome bz673695 without a complete re-haul to the
     * AsyncTaskManager and CommandBase.
     * @TODO remove this and re-factor {@link org.ovirt.engine.core.bll.tasks.AsyncTaskManager}
     * @return true when the time passed after creating the task is bigger than
     *         <code>ConfigValues.UnknownTaskPrePollingLapse</code>
     * @see org.ovirt.engine.core.bll.tasks.AsyncTaskManager
     * @see org.ovirt.engine.core.bll.CommandBase
     * @since 3.0
     */
    boolean isTaskOverPrePollingLapse() {
        AsyncTaskParameters parameters = getParameters();
        long taskStartTime = parameters.getDbAsyncTask().getStartTime().getTime();
        Integer prePollingPeriod = Config.<Integer> getValue(UnknownTaskPrePollingLapse);
        boolean idlePeriodPassed =
                System.currentTimeMillis() - taskStartTime > prePollingPeriod;

        log.infoFormat("task id {0} {1}. Pre-polling period is {2} millis. ",
                parameters.getVdsmTaskId(),
                idlePeriodPassed ? "has passed pre-polling period time and should be polled"
                        : "is in pre-polling  period and should not be polled", prePollingPeriod);
        return idlePeriodPassed;
    }

    @Override
    public void concreteStartPollingTask() {
        setState(AsyncTaskState.Polling);
    }

    /**
     * For each task set its updated status retrieved from VDSM.
     *
     * @param returnTaskStatus
     *            - Task status returned from VDSM.
     */
    @SuppressWarnings("incomplete-switch")
    public void updateTask(AsyncTaskStatus returnTaskStatus) {
        try {
            switch (getState()) {
            case Polling:
                // Get the returned task
                returnTaskStatus = checkTaskExist(returnTaskStatus);
                if (returnTaskStatus.getStatus() != getLastTaskStatus().getStatus()) {
                    addLogStatusTask(returnTaskStatus);
                }
                setLastTaskStatus(returnTaskStatus);

                if (!getLastTaskStatus().getTaskIsRunning()) {
                    handleEndedTask();
                }
                break;

            case Ended:
                handleEndedTask();
                break;

            // Try to clear task which failed to be cleared before SPM and DB
            case ClearFailed:
                clearAsyncTask();
                break;
            }
        }

        catch (RuntimeException e) {
            log.error(
                    String.format(
                            "BaseAsyncTask::PollAndUpdateTask: Handling task '%1$s' (State: %2$s, Parent Command: %3$s, Parameters Type: %4$s) threw an exception",
                            getVdsmTaskId(),
                            getState(),
                            (getParameters().getDbAsyncTask()
                                    .getActionType()),
                            getParameters()
                                    .getClass().getName()),
                    e);
        }
    }

    /**
     * Handle ended task operation. Change task state to Ended ,Cleared or
     * Cleared Failed , and log appropriate message.
     */
    private void handleEndedTask() {
        // If task state is different from Ended change it to Ended and set the
        // last access time to now.
        if (getState() != AsyncTaskState.Ended) {
            setState(AsyncTaskState.Ended);
            setLastStatusAccessTime();
        }

        // Fail zombie task and task that belongs to a partially submitted command
        if (isZombieTask() || isPartiallyCompletedCommandTask()) {
            getParameters().getDbAsyncTask().getTaskParameters().setTaskGroupSuccess(false);
            ExecutionHandler.endTaskStep(privateParameters.getDbAsyncTask().getStepId(), JobExecutionStatus.FAILED);
            onTaskEndFailure();
        }

        if (hasTaskEndedSuccessfully()) {
            ExecutionHandler.endTaskStep(privateParameters.getDbAsyncTask().getStepId(), JobExecutionStatus.FINISHED);
            onTaskEndSuccess();
        }

        else if (hasTaskEndedInFailure()) {
            ExecutionHandler.endTaskStep(privateParameters.getDbAsyncTask().getStepId(), JobExecutionStatus.FAILED);
            onTaskEndFailure();
        }

        else if (!doesTaskExist()) {
            ExecutionHandler.endTaskStep(privateParameters.getDbAsyncTask().getStepId(), JobExecutionStatus.UNKNOWN);
            onTaskDoesNotExist();
        }
    }

    protected void removeTaskFromDB() {
        try {
            if (coco.removeByVdsmTaskId(getVdsmTaskId()) != 0) {
                log.infoFormat("BaseAsyncTask::removeTaskFromDB: Removed task {0} from DataBase", getVdsmTaskId());
            }
        }

        catch (RuntimeException e) {
            log.error(String.format(
                    "BaseAsyncTask::removeTaskFromDB: Removing task %1$s from DataBase threw an exception.",
                    getVdsmTaskId()), e);
        }
    }

    private boolean hasTaskEndedSuccessfully() {
        return getLastTaskStatus().getTaskEndedSuccessfully();
    }

    private boolean hasTaskEndedInFailure() {
        return !getLastTaskStatus().getTaskIsRunning() && !getLastTaskStatus().getTaskEndedSuccessfully();
    }

    private boolean doesTaskExist() {
        return getLastTaskStatus().getStatus() != AsyncTaskStatusEnum.unknown;
    }

    protected void onTaskEndSuccess() {
        logEndTaskSuccess();
        clearAsyncTask();
    }

    protected void logEndTaskSuccess() {
        log.infoFormat(
                "BaseAsyncTask::onTaskEndSuccess: Task '{0}' (Parent Command {1}, Parameters Type {2}) ended successfully.",
                getVdsmTaskId(),
                (getParameters().getDbAsyncTask().getActionType()),
                getParameters()
                        .getClass().getName());
    }

    protected void onTaskEndFailure() {
        logEndTaskFailure();
        clearAsyncTask();
    }

    protected void logEndTaskFailure() {
        log.errorFormat(
                "BaseAsyncTask::logEndTaskFailure: Task '{0}' (Parent Command {1}, Parameters Type {2}) ended with failure:"
                        + "\r\n" + "-- Result: '{3}'" + "\r\n" + "-- Message: '{4}'," + "\r\n" + "-- Exception: '{5}'",
                getVdsmTaskId(),
                (getParameters().getDbAsyncTask().getActionType()),
                getParameters()
                        .getClass().getName(),
                getLastTaskStatus().getResult(),
                (getLastTaskStatus().getMessage() == null ? "[null]" : getLastTaskStatus().getMessage()),
                (getLastTaskStatus()
                        .getException() == null ? "[null]" : getLastTaskStatus().getException().getMessage()));
    }

    protected void onTaskDoesNotExist() {
        logTaskDoesntExist();
        clearAsyncTask();
    }

    protected void logTaskDoesntExist() {
        log.errorFormat(
                "BaseAsyncTask::logTaskDoesntExist: Task '{0}' (Parent Command {1}, Parameters Type {2}) does not exist.",
                getVdsmTaskId(),
                (getParameters().getDbAsyncTask().getActionType()),
                getParameters()
                        .getClass().getName());
    }

    /**
     * Print log message, Checks if the cachedStatusTask is null, (indicating the task was not found in the SPM).
     * If so returns {@link AsyncTaskStatusEnum#unknown} status, otherwise returns the status as given.<br>
     * <br>
     * @param cachedStatusTask The status from the SPM, or <code>null</code> is the task wasn't found in the SPM.
     * @return - Updated status task
     */
    protected AsyncTaskStatus checkTaskExist(AsyncTaskStatus cachedStatusTask) {
        AsyncTaskStatus returnedStatusTask = null;

        // If the cachedStatusTask is null ,that means the task has not been found in the SPM.
        if (cachedStatusTask == null) {
            // Set to running in order to continue polling the task in case SPM hasn't loaded the tasks yet..
            returnedStatusTask = new AsyncTaskStatus(AsyncTaskStatusEnum.unknown);

            log.errorFormat("SPMAsyncTask::PollTask: Task '{0}' (Parent Command {1}, Parameters Type {2}) " +
                        "was not found in VDSM, will change its status to unknown.",
                        getVdsmTaskId(), (getParameters().getDbAsyncTask().getActionType()),
                        getParameters().getClass().getName());
        } else {
            returnedStatusTask = cachedStatusTask;
        }
        return returnedStatusTask;
    }

    /**
     * Prints a log message of the task status,
     *
     * @param cachedStatusTask
     *            - Status got from VDSM
     */
    protected void addLogStatusTask(AsyncTaskStatus cachedStatusTask) {

        String formatString = "SPMAsyncTask::PollTask: Polling task '{0}' (Parent Command {1}, Parameters Type {2}) "
                + "returned status '{3}'{4}.";

        // If task doesn't exist (unknown) or has ended with failure (aborting)
        // , log warn.
        if (cachedStatusTask.getTaskIsInUnusualState()) {
            log.warnFormat(
                    formatString,
                    getVdsmTaskId(),
                    (getParameters().getDbAsyncTask()
                            .getActionType()),
                    getParameters().getClass().getName(),
                    cachedStatusTask.getStatus(),
                    ((cachedStatusTask.getStatus() == AsyncTaskStatusEnum.finished) ? (String
                            .format(", result '%1$s'",
                                    cachedStatusTask.getResult())) : ("")));
        }

        else {
            log.infoFormat(
                    formatString,
                    getVdsmTaskId(),
                    (getParameters().getDbAsyncTask()
                            .getActionType()),
                    getParameters().getClass().getName(),
                    cachedStatusTask.getStatus(),
                    ((cachedStatusTask.getStatus() == AsyncTaskStatusEnum.finished) ? (String
                            .format(", result '%1$s'",
                                    cachedStatusTask.getResult())) : ("")));
        }
    }

    public void stopTask() {
        stopTask(false);
    }

    public void stopTask(boolean forceFinish) {
        if (getState() != AsyncTaskState.AttemptingEndAction && getState() != AsyncTaskState.Cleared
                && getState() != AsyncTaskState.ClearFailed && !getLastTaskStatus().getTaskIsInUnusualState()) {
            try {
                log.infoFormat(
                        "SPMAsyncTask::StopTask: Attempting to stop task '{0}' (Parent Command {1}, Parameters Type {2}).",
                        getVdsmTaskId(),
                        (getParameters().getDbAsyncTask().getActionType()),
                        getParameters().getClass().getName());

                coco.stopTask(getStoragePoolID(), getVdsmTaskId());
            } catch (RuntimeException e) {
                log.error(
                        String.format("SPMAsyncTask::StopTask: Stopping task '%1$s' threw an exception.", getVdsmTaskId()),
                        e);
            } finally {
                if (forceFinish) {
                    //Force finish flag allows to force the task completion, regardless of the result from call to SPMStopTask
                    setState(AsyncTaskState.Ended);
                    setLastTaskStatus(new AsyncTaskStatus(AsyncTaskStatusEnum.finished));
                } else {
                    setState(AsyncTaskState.Polling);
                }
            }
        }
    }

    public void clearAsyncTask() {
        // if we are calling updateTask on a task which has not been submitted,
        // to vdsm there is no need to clear the task. The task is just deleted
        // from the database
        if (Guid.Empty.equals(getVdsmTaskId())) {
            removeTaskFromDB();
            return;
        }
        clearAsyncTask(false);
    }

    public void clearAsyncTask(boolean forceDelete) {
        VDSReturnValue vdsReturnValue = null;

        try {
            log.infoFormat("SPMAsyncTask::ClearAsyncTask: Attempting to clear task '{0}'", getVdsmTaskId());
            vdsReturnValue = coco.clearTask(getStoragePoolID(), getVdsmTaskId());
        }

        catch (RuntimeException e) {
            log.error(String.format("SPMAsyncTask::ClearAsyncTask: Clearing task '%1$s' threw an exception.",
                    getVdsmTaskId()), e);
        }

        boolean shouldGracefullyDeleteTask = false;
        if (!isTaskStateError(vdsReturnValue)) {
            if (vdsReturnValue == null || !vdsReturnValue.getSucceeded()) {
                setState(AsyncTaskState.ClearFailed);
                onTaskCleanFailure();
            } else {
                setState(AsyncTaskState.Cleared);
                shouldGracefullyDeleteTask =  true;
            }
        }
        //A task should be removed from DB if forceDelete is set to true, or if it was cleared successfully.
        if (shouldGracefullyDeleteTask || forceDelete) {
            removeTaskFromDB();
        }
    }

    /**
     * Function return true if we got error 410 - which is SPM initializing and
     * we did not clear the task
     *
     * @param vdsReturnValue
     * @return
     */
    private boolean isTaskStateError(VDSReturnValue vdsReturnValue) {
        if (vdsReturnValue != null && vdsReturnValue.getVdsError() != null
                && vdsReturnValue.getVdsError().getCode() == VdcBllErrors.TaskStateError) {
            log.infoFormat(
                    "SPMAsyncTask::ClearAsyncTask: At time of attempt to clear task '{0}' the response code was {1} and message was {2}. Task will not be cleaned",
                    getVdsmTaskId(),
                    vdsReturnValue.getVdsError().getCode(),
                    vdsReturnValue.getVdsError().getMessage());
            return true;
        }
        return false;
    }

    protected void onTaskCleanFailure() {
        logTaskCleanFailure();
    }

    protected void logTaskCleanFailure() {
        log.errorFormat("SPMAsyncTask::ClearAsyncTask: Clearing task '{0}' failed.", getVdsmTaskId());
    }

    private static final Log log = LogFactory.getLog(SPMAsyncTask.class);

    private boolean partiallyCompletedCommandTask = false;

    public boolean isPartiallyCompletedCommandTask() {
        return partiallyCompletedCommandTask;
    }

    public void setPartiallyCompletedCommandTask(boolean val) {
        this.partiallyCompletedCommandTask = val;
    }

    public boolean isZombieTask() {
        return zombieTask;
    }

    public void setZombieTask(boolean zombieTask) {
        this.zombieTask = zombieTask;
    }
}
