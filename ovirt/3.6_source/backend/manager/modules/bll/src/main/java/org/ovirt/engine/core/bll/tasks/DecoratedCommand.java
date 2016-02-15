package org.ovirt.engine.core.bll.tasks;

import org.ovirt.engine.core.bll.tasks.interfaces.Command;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;

public class DecoratedCommand<T extends VdcActionParametersBase> implements Command<T> {

    private Command<T> innerCommand;

    public DecoratedCommand(Command<T> innerCommand) {
        this.innerCommand = innerCommand;
    }

    @Override
    public VdcReturnValueBase endAction() {
        return innerCommand.endAction();
    }

    @Override
    public T getParameters() {
        return innerCommand.getParameters();
    }
}
