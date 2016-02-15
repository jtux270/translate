package org.ovirt.engine.core.vdsbroker.vdsbroker;

import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.HSMTaskGuidBaseVDSCommandParameters;

public class HSMClearTaskVDSCommand<P extends HSMTaskGuidBaseVDSCommandParameters> extends VdsBrokerCommand<P> {
    public HSMClearTaskVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        status = getBroker().clearTask(getParameters().getTaskId().toString());
        proceedProxyReturnValue();
    }

    @Override
    protected void proceedProxyReturnValue() {
        VdcBllErrors returnStatus = getReturnValueFromStatus(getReturnStatus());

        switch (returnStatus) {
        case UnknownTask:
            log.error(String.format("Trying to remove unknown task: %1$s", getParameters().getTaskId()));
            return;
        case TaskStateError:
            initializeVdsError(returnStatus);
            getVDSReturnValue().setSucceeded(false);
            return;
        }
        super.proceedProxyReturnValue();
    }
}
