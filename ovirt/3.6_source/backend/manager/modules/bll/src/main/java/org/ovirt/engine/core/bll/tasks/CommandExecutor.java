package org.ovirt.engine.core.bll.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.CommandsFactory;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.bll.utils.BackendUtils;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.CommandAssociatedEntity;
import org.ovirt.engine.core.common.businessentities.CommandEntity;
import org.ovirt.engine.core.common.businessentities.SubjectEntity;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineFault;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.di.Injector;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.timer.SchedulerUtil;
import org.ovirt.engine.core.utils.timer.SchedulerUtilQuartzImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandExecutor {

    private static final ExecutorService executor = Executors.newFixedThreadPool(Config.<Integer>getValue(ConfigValues.CommandCoordinatorThreadPoolSize));
    private static final Logger log = LoggerFactory.getLogger(CommandExecutor.class);

    private static class CommandContainer {
        private int initialDelay;     // Total delay between callback executions
        private int remainingDelay;   // Remaining delay to next callback execution
        private CommandCallback callback;

        public CommandContainer(CommandCallback callback, int executionDelay) {
            this.callback = callback;
            this.initialDelay = executionDelay;
            this.remainingDelay = executionDelay;
        }
    }

    private final CommandCoordinatorImpl coco;
    private final Map<Guid, CommandContainer> cmdCallbackMap = new ConcurrentHashMap<>();
    private boolean cmdExecutorInitialized;
    private final int pollingRate = Config.<Integer>getValue(ConfigValues.AsyncCommandPollingLoopInSeconds);

    CommandExecutor(CommandCoordinatorImpl coco) {
        this.coco = coco;
        SchedulerUtil scheduler = Injector.get(SchedulerUtilQuartzImpl.class);
        scheduler.scheduleAFixedDelayJob(this, "invokeCallbackMethods", new Class[]{},
                new Object[]{}, pollingRate, pollingRate, TimeUnit.SECONDS);
    }

    @OnTimerMethodAnnotation("invokeCallbackMethods")
    public void invokeCallbackMethods() {
        initCommandExecutor();
        Iterator<Entry<Guid, CommandContainer>> iterator = cmdCallbackMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Guid, CommandContainer> entry = iterator.next();

            // Decrement counter; execute if it reaches 0
            if ((entry.getValue().remainingDelay -= pollingRate) > 0) {
                continue;
            }

            Guid cmdId = entry.getKey();
            CommandCallback callback = entry.getValue().callback;
            CommandStatus status = coco.getCommandStatus(cmdId);
            boolean errorInCallback = false;
            try {
                switch (status) {
                    case FAILED:
                        callback.onFailed(cmdId, coco.getChildCommandIds(cmdId));
                        break;
                    case SUCCEEDED:
                        callback.onSucceeded(cmdId, coco.getChildCommandIds(cmdId));
                        break;
                    case ACTIVE:
                        if (coco.getCommandEntity(cmdId).isExecuted()) {
                            callback.doPolling(cmdId, coco.getChildCommandIds(cmdId));
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                errorInCallback = true;
                handleError(ex, status, cmdId);
            } finally {
                if (CommandStatus.FAILED.equals(status) || (CommandStatus.SUCCEEDED.equals(status) && !errorInCallback)) {
                    coco.updateCallbackNotified(cmdId);
                    iterator.remove();
                    CommandEntity cmdEntity = coco.getCommandEntity(entry.getKey());
                    if (cmdEntity != null) {
                        // When a child finishes, its parent's callback should execute shortly thereafter
                        Guid rootCommandId = cmdEntity.getRootCommandId();
                        if (!Guid.isNullOrEmpty(rootCommandId) && cmdCallbackMap.containsKey(rootCommandId)) {
                            cmdCallbackMap.get(rootCommandId).initialDelay = pollingRate;
                            cmdCallbackMap.get(rootCommandId).remainingDelay = pollingRate;
                        }
                    }
                } else if (status != coco.getCommandStatus(cmdId)) {
                    entry.getValue().initialDelay = pollingRate;
                    entry.getValue().remainingDelay = pollingRate;
                } else {
                    int maxDelay = Config.<Integer>getValue(ConfigValues.AsyncCommandPollingRateInSeconds);
                    entry.getValue().initialDelay = Math.min(maxDelay, entry.getValue().initialDelay * 2);
                    entry.getValue().remainingDelay = entry.getValue().initialDelay;
                }
            }
        }
    }

    private void handleError(Exception ex, CommandStatus status, Guid cmdId) {
        log.error("Error invoking callback method '{}' for '{}' command '{}'",
                getCallbackMethod(status),
                status,
                cmdId);
        log.error("Exception", ex);
        if (!CommandStatus.FAILED.equals(status)) {
            coco.updateCommandStatus(cmdId, CommandStatus.FAILED);
        }
    }

    private String getCallbackMethod(CommandStatus status) {
        switch (status) {
            case FAILED:
            case FAILED_RESTARTED:
                return "onFailed";
            case SUCCEEDED:
                return "onSucceeded";
            case ACTIVE:
                return "doPolling";
            default:
                return "Unknown";
        }
    }

    private void initCommandExecutor() {
        if (!cmdExecutorInitialized) {
            for (CommandEntity cmdEntity : coco.getCommandsWithCallbackEnabled()) {
                if (!cmdEntity.isExecuted() &&
                        cmdEntity.getCommandStatus() != CommandStatus.FAILED &&
                        cmdEntity.getCommandStatus() != CommandStatus.FAILED_RESTARTED) {
                    coco.retrieveCommand(cmdEntity.getId()).setCommandStatus(CommandStatus.FAILED_RESTARTED);
                }
                if (!cmdEntity.isCallbackNotified()) {
                    addToCallbackMap(cmdEntity);
                }
            }
            cmdExecutorInitialized = true;
        }
    }

    public void addToCallbackMap(CommandEntity cmdEntity) {
        if (!cmdCallbackMap.containsKey(cmdEntity.getId())) {
            CommandBase<?> cmd = coco.retrieveCommand(cmdEntity.getId());
            if (cmd != null && cmd.getCallback() != null) {
                cmdCallbackMap.put(cmdEntity.getId(), new CommandContainer(cmd.getCallback(), pollingRate));
            }
        }
    }

    public Future<VdcReturnValueBase> executeAsyncCommand(final VdcActionType actionType,
                                                          final VdcActionParametersBase parameters,
                                                          final CommandContext cmdContext,
                                                          SubjectEntity... subjectEntities) {
        final CommandBase<?> command = CommandsFactory.createCommand(actionType, parameters, cmdContext);
        CommandCallback callBack = command.getCallback();
        command.persistCommand(command.getParameters().getParentCommand(), cmdContext, callBack != null);
        coco.persistCommandAssociatedEntities(buildCommandAssociatedEntities(command.getCommandId(), subjectEntities));
        if (callBack != null) {
            cmdCallbackMap.put(command.getCommandId(), new CommandContainer(callBack, pollingRate));
        }
        Future<VdcReturnValueBase> retVal;
        try {
            retVal = executor.submit(new Callable<VdcReturnValueBase>() {

                @Override
                public VdcReturnValueBase call() throws Exception {
                    return executeCommand(command, cmdContext);
                }
            });
        } catch(RejectedExecutionException ex) {
            command.setCommandStatus(CommandStatus.FAILED);
            log.error("Failed to submit command to executor service, command '{}' status has been set to FAILED",
                    command.getCommandId());
            retVal = new RejectedExecutionFuture();
        }
        return retVal;
    }

    private Collection<CommandAssociatedEntity> buildCommandAssociatedEntities(Guid cmdId, SubjectEntity... subjectEntities) {
        if (subjectEntities.length == 0) {
            return Collections.emptyList();
        }
        Set<SubjectEntity> entities = new HashSet<>(Arrays.asList(subjectEntities));
        List<CommandAssociatedEntity> results = new ArrayList<>(entities.size());
        for (SubjectEntity subjectEntity : entities) {
            results.add(new CommandAssociatedEntity(cmdId, subjectEntity.getEntityType(), subjectEntity.getEntityId()));
        }
        return results;
    }

    private VdcReturnValueBase executeCommand(final CommandBase<?> command, final CommandContext cmdContext) {
        CommandCallback callback = command.getCallback();
        VdcReturnValueBase result = BackendUtils.getBackendCommandObjectsHandler(log).runAction(command, cmdContext != null ?
                cmdContext.getExecutionContext() : null);
        updateCommand(command, result);
        if (callback != null) {
            callback.executed(result);
        }
        return result;
    }

    private void updateCommand(final CommandBase<?> command,
                               final VdcReturnValueBase result) {
        CommandEntity cmdEntity = coco.getCommandEntity(command.getCommandId());
        cmdEntity.setReturnValue(result);
        if (!result.getCanDoAction()) {
            cmdEntity.setCommandStatus(CommandStatus.FAILED);
        }
        coco.persistCommand(cmdEntity);
    }

    static class RejectedExecutionFuture implements Future<VdcReturnValueBase> {

        VdcReturnValueBase retValue;

        RejectedExecutionFuture() {
            retValue = new VdcReturnValueBase();
            retValue.setSucceeded(false);
            EngineFault fault = new EngineFault();
            fault.setError(EngineError.ResourceException);
            fault.setMessage(Backend.getInstance()
                    .getVdsErrorsTranslator()
                    .TranslateErrorTextSingle(fault.getError().toString()));
            retValue.setFault(fault);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public VdcReturnValueBase get() throws InterruptedException, ExecutionException {
            return retValue;
        }

        @Override
        public VdcReturnValueBase get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return retValue;
        }
    }

}
