package org.ovirt.engine.core.bll.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCoordinator;
import org.ovirt.engine.core.bll.tasks.interfaces.SPMTask;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskParameters;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.businessentities.AsyncTask;
import org.ovirt.engine.core.common.businessentities.AsyncTaskResultEnum;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatusEnum;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.DateTime;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.collections.MultiValueMapUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.timer.SchedulerUtil;
import org.ovirt.engine.core.utils.timer.SchedulerUtilQuartzImpl;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

/**
 * AsyncTaskManager: Singleton, manages all tasks in the system.
 */
public final class AsyncTaskManager {
    private static final Log log = LogFactory.getLog(AsyncTaskManager.class);

    /** Map which consist all tasks that currently are monitored **/
    private ConcurrentMap<Guid, SPMTask> _tasks;

    /** Indication if _tasks has changed for logging process. **/
    private boolean logChangedMap = true;

    /** The period of time (in minutes) to hold the asynchronous tasks' statuses in the asynchronous tasks cache **/
    private final int _cacheTimeInMinutes;

    /**Map of tasks in DB per storage pool that exist after restart **/
    private ConcurrentMap<Guid, List<AsyncTask>> tasksInDbAfterRestart = null;

    /**
     * Map of tasks for commands that have been partially submitted to vdsm
     */
    private Map<Guid, AsyncTask> partiallyCompletedCommandTasks = new ConcurrentHashMap<>();

    private CountDownLatch irsBrokerLatch;

    private static volatile AsyncTaskManager taskManager;
    private static final Object LOCK = new Object();
    private CommandCoordinator coco;

    public static AsyncTaskManager getInstance() {
        return taskManager;
    }

    public static AsyncTaskManager getInstance(CommandCoordinator coco) {
        if (taskManager == null) {
            synchronized(LOCK) {
                if (taskManager == null) {
                    taskManager = new AsyncTaskManager(coco);
                }
            }
        }
        return taskManager;
    }

    private AsyncTaskManager(CommandCoordinator coco) {
        this.coco = coco;
        _tasks = new ConcurrentHashMap<Guid, SPMTask>();

        SchedulerUtil scheduler = SchedulerUtilQuartzImpl.getInstance();
        scheduler.scheduleAFixedDelayJob(this, "_timer_Elapsed", new Class[]{},
                new Object[]{}, Config.<Integer>getValue(ConfigValues.AsyncTaskPollingRate),
                Config.<Integer>getValue(ConfigValues.AsyncTaskPollingRate), TimeUnit.SECONDS);

        scheduler.scheduleAFixedDelayJob(this, "_cacheTimer_Elapsed", new Class[]{},
                new Object[]{}, Config.<Integer>getValue(ConfigValues.AsyncTaskStatusCacheRefreshRateInSeconds),
                Config.<Integer>getValue(ConfigValues.AsyncTaskStatusCacheRefreshRateInSeconds), TimeUnit.SECONDS);
        _cacheTimeInMinutes = Config.<Integer>getValue(ConfigValues.AsyncTaskStatusCachingTimeInMinutes);
    }

    public void initAsyncTaskManager() {
        tasksInDbAfterRestart = new ConcurrentHashMap();
        Map<Guid, List<AsyncTask>> rootCommandIdToTasksMap = groupTasksByRootCommandId(coco.getAllAsyncTasksFromDb());
        int numberOfCommandsWithEmptyVdsmId = 0;
        for (Entry<Guid, List<AsyncTask>> entry : rootCommandIdToTasksMap.entrySet()) {
            if (hasTasksWithoutVdsmId(rootCommandIdToTasksMap.get(entry.getKey()))) {
                log.infoFormat("Root Command {0} has tasks without vdsm id.", entry.getKey());
                numberOfCommandsWithEmptyVdsmId++;
            }
        }
        irsBrokerLatch = new CountDownLatch(numberOfCommandsWithEmptyVdsmId);
        for (Entry<Guid, List<AsyncTask>> entry : rootCommandIdToTasksMap.entrySet()) {
            if (hasTasksWithoutVdsmId(rootCommandIdToTasksMap.get(entry.getKey()))) {
                log.infoFormat("Root Command {0} has tasks without vdsm id.", entry.getKey());
                handleTasksOfCommandWithEmptyVdsmId(rootCommandIdToTasksMap.get(entry.getKey()));
            }
            for (AsyncTask task : entry.getValue()) {
                if (!hasEmptyVdsmId(task)) {
                    tasksInDbAfterRestart.putIfAbsent(task.getStoragePoolId(), new ArrayList<AsyncTask>());
                    tasksInDbAfterRestart.get(task.getStoragePoolId()).add(task);
                }
            }
        }
        try {
            irsBrokerLatch.await();
            log.info("Initialization of AsyncTaskManager completed successfully.");
        } catch (InterruptedException e) {
        }

    }

    @OnTimerMethodAnnotation("_timer_Elapsed")
    public synchronized void _timer_Elapsed() {
        if (thereAreTasksToPoll()) {
            pollAndUpdateAsyncTasks();

            if (thereAreTasksToPoll() && logChangedMap) {
                log.infoFormat("Finished polling Tasks, will poll again in {0} seconds.",
                        Config.<Integer>getValue(ConfigValues.AsyncTaskPollingRate));

                // Set indication to false for not logging the same message next
                // time.
                logChangedMap = false;
            }

            // check for zombie tasks
            if (_tasks.size() > 0) {
                cleanZombieTasks();
            }
        }
    }

    @OnTimerMethodAnnotation("_cacheTimer_Elapsed")
    public void _cacheTimer_Elapsed() {
        removeClearedAndOldTasks();
    }

    /**
     * Check if task should be cached or not. Task should not be cached , only
     * if the task has been rolled back (Cleared) or failed to be rolled back
     * (ClearedFailed), and been in that status for several minutes
     * (_cacheTimeInMinutes).
     *
     * @param task - Asynchronous task we check to cache or not.
     * @return - true for uncached object , and false when the object should be
     * cached.
     */
    public synchronized boolean cachingOver(SPMTask task) {
        // Get time in milliseconds that the task should be cached
        long SubtractMinutesAsMills = TimeUnit.MINUTES
                .toMillis(_cacheTimeInMinutes);

        // check if task has been rolled back (Cleared) or failed to be rolled
        // back (ClearedFailed)
        // for SubtractMinutesAsMills of minutes.
        return (task.getState() == AsyncTaskState.Cleared || task.getState() == AsyncTaskState.ClearFailed)
                && task.getLastAccessToStatusSinceEnd() < (System
                        .currentTimeMillis() - SubtractMinutesAsMills);
    }

    public synchronized boolean hasTasksByStoragePoolId(Guid storagePoolID) {
        boolean retVal = false;
        if (_tasks != null) {
            for (SPMTask task : _tasks.values()) {
                if (task.getStoragePoolID().equals(storagePoolID)) {
                    retVal = true;
                    break;
                }
            }
        }
        return retVal;
    }

    public synchronized boolean hasTasksForEntityIdAndAction(Guid id, VdcActionType type) {
        if (_tasks != null) {
            for (SPMTask task : _tasks.values()) {
                if (isCurrentTaskLookedFor(id, task)
                        && type.equals(task.getParameters().getDbAsyncTask().getActionType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void handleTasksOfCommandWithEmptyVdsmId(final List<AsyncTask> tasks) {
        ThreadPoolUtil.execute(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                TransactionSupport.executeInNewTransaction(new TransactionMethod<Object>() {
                    @Override
                    public Object runInTransaction() {
                        try {
                            boolean isPartiallySubmittedCommand = isPartiallySubmittedCommand(tasks);
                            for (AsyncTask task : tasks) {
                                handleTaskOfCommandWithEmptyVdsmId(isPartiallySubmittedCommand, task);
                            }
                            return null;
                        } finally {
                            irsBrokerLatch.countDown();
                        }
                    }
                });
            }
        });
    }

    /**
     * A command is considered partially submitted to the vdsm if some of its
     * place holders have vdsm id and some have empty vdsm id, indicating that
     * the server was restarted during command execution.
     * @param tasks
     * @return
     */
    private boolean isPartiallySubmittedCommand(List<AsyncTask> tasks) {
        for (AsyncTask task : tasks) {
            // if one of the tasks has a vdsm id, the command was partially
            // submitted to vdsm
            if (!hasEmptyVdsmId(task)) {
                return true;
            }
        }
        return false;
    }

    /**
     * We group the tasks by the root command id, so that we can identify the
     * commands that were partially submitted to vdsm
     * @param tasksInDB
     * @return
     */
    private Map<Guid, List<AsyncTask>> groupTasksByRootCommandId(List<AsyncTask> tasksInDB) {
        Map<Guid, List<AsyncTask>> rootCommandIdToCommandsMap = new HashMap<>();
        for (AsyncTask task : tasksInDB) {
            MultiValueMapUtils.addToMap(task.getRootCommandId(), task, rootCommandIdToCommandsMap);
        }
        return rootCommandIdToCommandsMap;
    }

    /**
     * Determines if any of the tasks has an empty vdsm id.
     * @param tasks
     * @return
     */
    private boolean hasTasksWithoutVdsmId(List<AsyncTask> tasks) {
        for (AsyncTask task : tasks) {
            if (hasEmptyVdsmId(task)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEmptyVdsmId(AsyncTask task) {
        return Guid.Empty.equals(task.getVdsmTaskId());
    }

    /**
     * If a command is partially submitted to vdsm, the empty place holders
     * can be removed from the database and we poll for the completion of tasks
     * that were already submitted. Once the tasks that were submitted finish
     * execution, they are marked to be failed by adding them to the
     * partiallyCompletedCommandTasks list.
     * If none of the tasks were submitted to vdsm, the empty place holders
     * are deleted from the database and we endAction on the command with
     * failure
     * @param isPartiallySubmittedCommand
     * @param task
     */
    private void handleTaskOfCommandWithEmptyVdsmId(
            final boolean isPartiallySubmittedCommand,
            final AsyncTask task) {
        if (isPartiallySubmittedCommand) {
            if (hasEmptyVdsmId(task)) {
                removeTaskFromDbByTaskId(task.getTaskId());
                return;
            }
            partiallyCompletedCommandTasks.put(task.getVdsmTaskId(), task);
            return;
        }
        TransactionSupport.executeInScope(TransactionScopeOption.Required,
                new TransactionMethod<Void>() {
                    @Override
                    public Void runInTransaction() {
                        logAndFailTaskOfCommandWithEmptyVdsmId(task,
                                "Engine was restarted before all tasks of the command could be submitted to vdsm.");
                        return null;
                    }
                });
    }

    public void logAndFailTaskOfCommandWithEmptyVdsmId(Guid asyncTaskId, String message) {
        AsyncTask task = coco.getAsyncTaskFromDb(asyncTaskId);
        if (task != null) {
            logAndFailTaskOfCommandWithEmptyVdsmId(task, message);
        }
    }

    public void logAndFailTaskOfCommandWithEmptyVdsmId(final AsyncTask task, String message) {
        log.infoFormat(
                "Failing task with empty vdsm id AsyncTaskType {0} : Task '{1}' Parent Command {2}",
                task.getTaskType(),
                task.getTaskId(),
                (task.getActionType()));
        task.getTaskParameters().setTaskGroupSuccess(false);
        if (task.getActionType() == VdcActionType.Unknown) {
            removeTaskFromDbByTaskId(task.getTaskId());
            log.infoFormat(
                    "Not calling endAction for task with out vdsm id and AsyncTaskType {0} : Task '{1}' Parent Command {2}",
                    task.getTaskType(),
                    task.getTaskId(),
                    (task.getActionType()));
            return;
        }
        log.infoFormat("Calling updateTask for task with out vdsm id and AsyncTaskType {0} : Task '{1}' Parent Command {2} Parameters class {3}", task.getTaskType(),
                    task.getTaskId(),
                    (task.getActionType()));
        AsyncTaskCreationInfo creationInfo = new AsyncTaskCreationInfo(Guid.Empty, task.getTaskType(), task.getStoragePoolId());
        SPMTask spmTask = coco.construct(creationInfo, task);
        AsyncTaskStatus failureStatus = new AsyncTaskStatus();
        failureStatus.setStatus(AsyncTaskStatusEnum.finished);
        failureStatus.setResult(AsyncTaskResultEnum.failure);
        failureStatus.setMessage(message);
        spmTask.setState(AsyncTaskState.Ended);
        spmTask.setLastTaskStatus(failureStatus);
        spmTask.updateTask(failureStatus);
    }

    private static VdcActionType getEndActionType(AsyncTask dbAsyncTask) {
        VdcActionType commandType = dbAsyncTask.getActionParameters().getCommandType();
        if (!VdcActionType.Unknown.equals(commandType)) {
            return commandType;
        }
        return dbAsyncTask.getActionType();
    }

    public static void removeTaskFromDbByTaskId(Guid taskId) {
        try {
            if (CommandCoordinatorUtil.callRemoveTaskFromDbByTaskId(taskId) != 0) {
                log.infoFormat("Removed task {0} from DataBase", taskId);
            }
        } catch (RuntimeException e) {
            log.error(String.format(
                    "Removing task %1$s from DataBase threw an exception.",
                    taskId), e);
        }
    }

    private boolean isCurrentTaskLookedFor(Guid id, SPMTask task) {
        return (task instanceof CommandAsyncTask) && task.getParameters().getEntityInfo() != null &&
                id.equals(task.getParameters().getEntityInfo().getId())
                && (task.getState() != AsyncTaskState.Cleared)
                && (task.getState() != AsyncTaskState.ClearFailed);
    }

    private void cleanZombieTasks() {
        long maxTime = DateTime.getNow()
                .addMinutes((-1) * Config.<Integer>getValue(ConfigValues.AsyncTaskZombieTaskLifeInMinutes)).getTime();
        for (SPMTask task : _tasks.values()) {

            if (task.getParameters().getDbAsyncTask().getStartTime().getTime() < maxTime) {
                AuditLogableBase logable = new AuditLogableBase();
                logable.addCustomValue("CommandName", task.getParameters().getDbAsyncTask().getActionType().toString());
                logable.addCustomValue("Date", task.getParameters().getDbAsyncTask().getStartTime().toString());

                // if task is not finish and not unknown then it's in running
                // status
                if (task.getLastTaskStatus().getStatus() != AsyncTaskStatusEnum.finished
                        && task.getLastTaskStatus().getStatus() != AsyncTaskStatusEnum.unknown) {
                    // mark it as a zombie task, Will result in failure of the command
                    task.setZombieTask(true);
                    AuditLogDirector.log(logable, AuditLogType.TASK_STOPPING_ASYNC_TASK);

                    log.infoFormat("Cleaning zombie tasks: Stopping async task {0} that started at {1}",
                            task.getParameters().getDbAsyncTask().getActionType(), task
                                    .getParameters().getDbAsyncTask().getStartTime());

                    task.stopTask(true);
                } else {
                    AuditLogDirector.log(logable, AuditLogType.TASK_CLEARING_ASYNC_TASK);

                    log.infoFormat("Cleaning zombie tasks: Clearing async task {0} that started at {1}",
                            task.getParameters().getDbAsyncTask().getActionType(), task
                                    .getParameters().getDbAsyncTask().getStartTime());

                    task.clearAsyncTask(true);
                }
            }
        }
    }

    private int numberOfTasksToPoll() {
        int retValue = 0;
        for (SPMTask task : _tasks.values()) {
            if (task.getShouldPoll()) {
                retValue++;
            }
        }

        return retValue;
    }

    private boolean thereAreTasksToPoll() {
        for (SPMTask task : _tasks.values()) {
            if (task.getShouldPoll()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetch all tasks statuses from each storagePoolId , and update the _tasks
     * map with the updated statuses.
     */
    private void pollAndUpdateAsyncTasks() {
        if (logChangedMap) {
            log.infoFormat("Polling and updating Async Tasks: {0} tasks, {1} tasks to poll now",
                           _tasks.size(), numberOfTasksToPoll());
        }

        // Fetch Set of pool id's
        Set<Guid> poolsOfActiveTasks = getPoolIdsTasks();

        // Get all tasks from all the SPMs.
        Map<Guid, Map<Guid, AsyncTaskStatus>> poolsAllTasksMap = getSPMsTasksStatuses(poolsOfActiveTasks);

        // For each task that found on each pool id
        updateTaskStatuses(poolsAllTasksMap);
    }

    /**
     * Update task status based on asyncTaskMap.
     *
     * @param asyncTaskMap - Task statuses Map fetched from VDSM.
     */
    private void updateTaskStatuses(
                                    Map<Guid, Map<Guid, AsyncTaskStatus>> poolsAllTasksMap) {
        for (SPMTask task : _tasks.values()) {
            if (task.getShouldPoll()) {
                Map<Guid, AsyncTaskStatus> asyncTasksForPoolMap = poolsAllTasksMap
                        .get(task.getStoragePoolID());

                // If the storage pool id exists
                if (asyncTasksForPoolMap != null) {
                    AsyncTaskStatus cachedAsyncTaskStatus = asyncTasksForPoolMap
                            .get(task.getVdsmTaskId());

                    // task found in VDSM.
                    task.updateTask(cachedAsyncTaskStatus);
                }
            }
        }

    }

    /**
     * Call VDSCommand for each pool id fetched from poolsOfActiveTasks , and
     * Initialize a map with each storage pool Id task statuses.
     *
     * @param poolsOfActiveTasks - Set of all the active tasks fetched from
     * _tasks.
     * @return poolsAsyncTaskMap - Map which contains tasks for each storage
     * pool id.
     */
    @SuppressWarnings("unchecked")
    private Map<Guid, Map<Guid, AsyncTaskStatus>> getSPMsTasksStatuses(Set<Guid> poolsOfActiveTasks) {
        Map<Guid, Map<Guid, AsyncTaskStatus>> poolsAsyncTaskMap = new HashMap<Guid, Map<Guid, AsyncTaskStatus>>();

        // For each pool Id (SPM) ,add its tasks to the map.
        for (Guid storagePoolID : poolsOfActiveTasks) {
            try {
                Map<Guid, AsyncTaskStatus> map = coco.getAllTasksStatuses(storagePoolID);
                if (map != null) {
                    poolsAsyncTaskMap.put(storagePoolID, map);
                }
            } catch (RuntimeException e) {
                if ((e instanceof VdcBLLException)
                        && (((VdcBLLException) e).getErrorCode() == VdcBllErrors.VDS_NETWORK_ERROR)) {
                    log.debugFormat("Get SPM task statuses: Calling Command {1}{2}, "
                            + "with storagePoolId {3}) threw an exception.",
                            VDSCommandType.SPMGetAllTasksStatuses, "VDSCommand", storagePoolID);
                } else {
                    log.debugFormat("Get SPM task statuses: Calling Command {1}{2}, "
                            + "with storagePoolId {3}) threw an exception.",
                            VDSCommandType.SPMGetAllTasksStatuses, "VDSCommand", storagePoolID, e);
                }
            }
        }

        return poolsAsyncTaskMap;
    }

    /**
     * Get a Set of all the storage pool id's of tasks that should pool.
     *
     * @see SPMAsyncTask#getShouldPoll()
     * @return - Set of active tasks.
     */
    private Set<Guid> getPoolIdsTasks() {
        Set<Guid> poolsOfActiveTasks = new HashSet<Guid>();

        for (SPMTask task : _tasks.values()) {
            if (task.getShouldPoll()) {
                poolsOfActiveTasks.add(task.getStoragePoolID());
            }
        }
        return poolsOfActiveTasks;
    }

    /**
     * get list of pools that have only cleared and old tasks (which don't exist
     * anymore in the manager):
     *
     * @return
     */
    synchronized private void removeClearedAndOldTasks() {
        Set<Guid> poolsOfActiveTasks = new HashSet<Guid>();
        Set<Guid> poolsOfClearedAndOldTasks = new HashSet<Guid>();
        ConcurrentMap<Guid, SPMTask> activeTaskMap = new ConcurrentHashMap<>();
        for (SPMTask task : _tasks.values()) {
            if (!cachingOver(task)) {
                activeTaskMap.put(task.getVdsmTaskId(), task);
                poolsOfActiveTasks.add(task.getStoragePoolID());
            } else {
                poolsOfClearedAndOldTasks.add(task.getStoragePoolID());
            }
        }

        // Check if _tasks need to be updated with less tasks (activated tasks).
        if (poolsOfClearedAndOldTasks.size() > 0) {
            setNewMap(activeTaskMap);
            poolsOfClearedAndOldTasks.removeAll(poolsOfActiveTasks);
        }
        for (Guid storagePoolID : poolsOfClearedAndOldTasks) {
            log.infoFormat("Cleared all tasks of pool {0}.",
                    storagePoolID);
        }
    }

    public synchronized void lockAndAddTaskToManager(SPMTask task) {
        addTaskToManager(task);
    }

    private void addTaskToManager(SPMTask task) {
        if (task == null) {
            log.error("Cannot add a null task.");
        } else {
            if (!_tasks.containsKey(task.getVdsmTaskId())) {
                log.infoFormat(
                        "Adding task '{0}' (Parent Command {1}, Parameters Type {2}), {3}.",
                        task.getVdsmTaskId(),
                        (task.getParameters().getDbAsyncTask().getActionType()),
                        task.getParameters().getClass().getName(),
                        (task.getShouldPoll() ? "polling started."
                                : "polling hasn't started yet."));

                // Set the indication to true for logging _tasks status on next
                // quartz execution.
                addTaskToMap(task.getVdsmTaskId(), task);
            } else {
                SPMTask existingTask = _tasks.get(task.getVdsmTaskId());
                if (existingTask.getParameters().getDbAsyncTask().getActionType() == VdcActionType.Unknown
                        && task.getParameters().getDbAsyncTask().getActionType() != VdcActionType.Unknown) {
                    log.infoFormat(
                            "Task '{0}' already exists with action type 'Unknown', now overriding it with action type '{1}'",
                            task.getVdsmTaskId(),
                            task.getParameters().getDbAsyncTask().getActionType());

                    // Set the indication to true for logging _tasks status on
                    // next quartz execution.
                    addTaskToMap(task.getVdsmTaskId(), task);
                }
            }
        }
    }

    /**
     * Adds new task to _tasks map , and set the log status to true. We set the
     * indication to true for logging _tasks status on next quartz execution.
     *
     * @param guid - Key of the map.
     * @param asyncTask - Value of the map.
     */
    private void addTaskToMap(Guid guid, SPMTask asyncTask) {
        _tasks.put(guid, asyncTask);
        logChangedMap = true;
    }

    /**
     * We set the indication to true when _tasks map changes for logging _tasks
     * status on next quartz execution.
     *
     * @param asyncTaskMap - Map to copy to _tasks map.
     */
    private void setNewMap(ConcurrentMap<Guid, SPMTask> asyncTaskMap) {
        // If not the same set _tasks to be as asyncTaskMap.
        _tasks = asyncTaskMap;

        // Set the indication to true for logging.
        logChangedMap = true;

        // Log tasks to poll now.
        log.infoFormat("Setting new tasks map. The map contains now {0} tasks", _tasks.size());
    }

    public SPMTask createTask(AsyncTaskType taskType, AsyncTaskParameters taskParameters) {
        return coco.construct(taskType, taskParameters, false);
    }

    public synchronized void startPollingTask(Guid vdsmTaskId) {
        if (_tasks.containsKey(vdsmTaskId)) {
            _tasks.get(vdsmTaskId).startPollingTask();
        }
    }

    public synchronized ArrayList<AsyncTaskStatus> pollTasks(ArrayList<Guid> vdsmTaskIdList) {
        ArrayList<AsyncTaskStatus> returnValue = new ArrayList<AsyncTaskStatus>();

        if (vdsmTaskIdList != null && vdsmTaskIdList.size() > 0) {
            for (Guid vdsmTaskId : vdsmTaskIdList) {
                if (_tasks.containsKey(vdsmTaskId)) {
                    // task is still running or is still in the cache:
                    _tasks.get(vdsmTaskId).setLastStatusAccessTime();
                    returnValue.add(_tasks.get(vdsmTaskId).getLastTaskStatus());
                } else { // task doesn't exist in the manager (shouldn't happen) ->
                // assume it has been ended successfully.
                    log.warnFormat(
                            "Polling tasks. Task ID '{0}' doesn't exist in the manager -> assuming 'finished'.",
                            vdsmTaskId);

                    AsyncTaskStatus tempVar = new AsyncTaskStatus();
                    tempVar.setStatus(AsyncTaskStatusEnum.finished);
                    tempVar.setResult(AsyncTaskResultEnum.success);
                    returnValue.add(tempVar);
                }
            }
        }

        return returnValue;
    }

    /**
     * Retrieves from the specified storage pool the tasks that exist on it and
     * adds them to the manager.
     *
     * @param sp the storage pool to retrieve running tasks from
     */
    public void addStoragePoolExistingTasks(StoragePool sp) {
        List<AsyncTaskCreationInfo> currPoolTasks = null;
        try {
            currPoolTasks = coco.getAllTasksInfo(sp.getId());
        } catch (RuntimeException e) {
            log.error(
                    String.format(
                            "Getting existing tasks on Storage Pool %1$s failed.",
                            sp.getName()),
                    e);
        }

        if (currPoolTasks != null && currPoolTasks.size() > 0) {
            synchronized (this) {
                final List<SPMTask> newlyAddedTasks = new ArrayList<SPMTask>();

                for (AsyncTaskCreationInfo creationInfo : currPoolTasks) {
                    creationInfo.setStoragePoolID(sp.getId());
                    if (!_tasks.containsKey(creationInfo.getVdsmTaskId())) {
                        try {
                            SPMTask task;
                            if (partiallyCompletedCommandTasks.containsKey(creationInfo.getVdsmTaskId())) {
                                AsyncTask asyncTaskInDb = partiallyCompletedCommandTasks.get(creationInfo.getVdsmTaskId());
                                task = coco.construct(creationInfo, asyncTaskInDb);
                                if (task.getEntitiesMap() == null) {
                                    task.setEntitiesMap(new HashMap<Guid, VdcObjectType>());
                                }
                                partiallyCompletedCommandTasks.remove(task.getVdsmTaskId());
                                // mark it as a task of a partially completed command
                                // Will result in failure of the command
                                task.setPartiallyCompletedCommandTask(true);
                            } else {
                                task = coco.construct(creationInfo);
                            }
                            addTaskToManager(task);
                            newlyAddedTasks.add(task);
                        } catch (Exception e) {
                            log.errorFormat("Failed to load task of type {0} with id {1}, due to: {2}.",
                                       creationInfo.getTaskType(), creationInfo.getVdsmTaskId(),
                                       ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                }

                TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                    @Override
                    public Void runInTransaction() {
                        for (SPMTask task : newlyAddedTasks) {
                            AsyncTaskUtils.addOrUpdateTaskInDB(task);
                        }
                        return null;
                    }
                });

                for (SPMTask task : newlyAddedTasks) {
                    startPollingTask(task.getVdsmTaskId());
                }

                log.infoFormat(
                        "Discovered {0} tasks on Storage Pool '{1}', {2} added to manager.",
                        currPoolTasks.size(),
                        sp.getName(),
                        newlyAddedTasks.size());
            }
        } else {
            log.infoFormat("Discovered no tasks on Storage Pool {0}",
                    sp.getName());
        }


        List<AsyncTask> tasksInDForStoragePool = tasksInDbAfterRestart.get(sp.getId());
        if (tasksInDForStoragePool != null) {
            for (AsyncTask task : tasksInDForStoragePool) {
                if (!_tasks.containsKey(task.getVdsmTaskId())) {
                    coco.removeByVdsmTaskId(task.getVdsmTaskId());
                }
            }
        }

        //Either the tasks were only in DB - so they were removed from db, or they are polled -
        //in any case no need to hold them in the map that represents the tasksInDbAfterRestart
        tasksInDbAfterRestart.remove(sp.getId());

    }

    /**
     * Stops all tasks, and set them to polling state, for clearing them up
     * later.
     *
     * @param vdsmTaskList - List of tasks to stop.
     */
    public synchronized void cancelTasks(List<Guid> vdsmTaskList) {
        for (Guid vdsmTaskId : vdsmTaskList) {
            cancelTask(vdsmTaskId);
        }
    }

    public synchronized void cancelTask(Guid vdsmTaskId) {
        if (_tasks.containsKey(vdsmTaskId)) {
            log.infoFormat("Attempting to cancel task '{0}'.", vdsmTaskId);
            _tasks.get(vdsmTaskId).stopTask();
            _tasks.get(vdsmTaskId).concreteStartPollingTask();
        }
    }

    public synchronized boolean entityHasTasks(Guid id) {
        for (SPMTask task : _tasks.values()) {
            if (isCurrentTaskLookedFor(id, task)) {
                return true;
            }
        }
        return false;
    }

    public Collection<Guid> getUserIdsForVdsmTaskIds(List<Guid> vdsmTaskIds) {
        Set<Guid> users = new TreeSet<>();
        for (Guid id : vdsmTaskIds) {
            if (_tasks.containsKey(id)) {
                users.add(_tasks.get(id).getParameters().getDbAsyncTask().getUserId());
            }
        }
        return users;
    }

}
