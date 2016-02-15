package org.ovirt.engine.core.bll.lsm;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.ImagesHandler;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.tasks.SPMAsyncTaskHandler;
import org.ovirt.engine.core.bll.tasks.TaskHandlerCommand;
import org.ovirt.engine.core.common.action.LiveMigrateDiskParameters;
import org.ovirt.engine.core.common.action.LiveMigrateVmDisksParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveMigrateDisksTaskHandler implements SPMAsyncTaskHandler {

    private final TaskHandlerCommand<? extends LiveMigrateVmDisksParameters> enclosingCommand;
    private static final Logger log = LoggerFactory.getLogger(LiveMigrateDisksTaskHandler.class);

    public LiveMigrateDisksTaskHandler(TaskHandlerCommand<? extends LiveMigrateVmDisksParameters> enclosingCommand) {
        this.enclosingCommand = enclosingCommand;
    }

    @Override
    public void execute() {
        if (!enclosingCommand.getReturnValue().getSucceeded()) {
            throw new EngineException(EngineError.imageErr,
                "Auto-generated live snapshot for VM " + enclosingCommand.getParameters().getVmId() + " failed");
        }
        TransactionSupport.executeInScope(TransactionScopeOption.Suppress, new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                for (LiveMigrateDiskParameters parameters : enclosingCommand.getParameters().getParametersList()) {
                    CommandContext commandContext = ExecutionHandler.createInternalJobContext(enclosingCommand.cloneContextAndDetachFromParent());
                    ExecutionHandler.setAsyncJob(commandContext.getExecutionContext(), true);
                    parameters.setSessionId(enclosingCommand.getParameters().getSessionId());

                    VdcReturnValueBase vdcReturnValue =
                            Backend.getInstance().runInternalAction(VdcActionType.LiveMigrateDisk,
                                    parameters,
                                    commandContext);

                    if (!vdcReturnValue.getSucceeded()) {
                        ImagesHandler.updateAllDiskImageSnapshotsStatus(parameters.getImageGroupID(), ImageStatus.OK);
                    }

                    enclosingCommand.getReturnValue()
                            .getVdsmTaskIdList()
                            .addAll(vdcReturnValue.getInternalVdsmTaskIdList());

                    if (!parameters.getTaskGroupSuccess()) {
                        ExecutionHandler.endTaskJob(commandContext.getExecutionContext(), false);
                        log.error("Failed LiveMigrateDisk (Disk '{}' , VM '{}')",
                                parameters.getImageGroupID(),
                                parameters.getVmId());
                    }
                }

                enclosingCommand.getReturnValue().setSucceeded(true);
                return null;
            }
        });
    }

    @Override
    public void endSuccessfully() {
        enclosingCommand.getReturnValue().setEndActionTryAgain(false);
    }

    @Override
    public void endWithFailure() {
        enclosingCommand.getReturnValue().setSucceeded(true);
        enclosingCommand.getReturnValue().setEndActionTryAgain(false);
    }

    @Override
    public AsyncTaskType getTaskType() {
        // No implementation - handled by the command
        return null;
    }

    @Override
    public AsyncTaskType getRevertTaskType() {
        // No implementation - there is no live-merge
        return null;
    }

    @Override
    public void compensate() {
    }
}
