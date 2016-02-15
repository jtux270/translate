package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

public class ParallelMultipleActionsRunner extends MultipleActionsRunner {

    public ParallelMultipleActionsRunner(VdcActionType actionType,
            List<VdcActionParametersBase> parameters,
            CommandContext commandContext, boolean isInternal) {
        super(actionType, parameters, commandContext, isInternal);
    }

    @Override
    protected void invokeCommands() {
        runCommands();
    }

    @Override
    protected void runCommands() {
        for (final CommandBase<?> command : getCommands()) {
            if (command.getReturnValue().getCanDoAction()) {
                ThreadPoolUtil.execute(new Runnable() {

                    @Override
                    public void run() {
                        executeValidatedCommand(command);
                    }
                });
            }
        }
    }
}
