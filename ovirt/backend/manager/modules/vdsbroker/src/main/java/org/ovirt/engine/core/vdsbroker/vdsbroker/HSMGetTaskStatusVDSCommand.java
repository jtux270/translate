package org.ovirt.engine.core.vdsbroker.vdsbroker;

import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.HSMTaskGuidBaseVDSCommandParameters;
import org.ovirt.engine.core.utils.log.Logged;
import org.ovirt.engine.core.utils.log.Logged.LogLevel;

@Logged(executionLevel = LogLevel.TRACE)
public class HSMGetTaskStatusVDSCommand<P extends HSMTaskGuidBaseVDSCommandParameters>
        extends HSMGetAllTasksStatusesVDSCommand<P> {
    private TaskStatusReturnForXmlRpc _result;

    public HSMGetTaskStatusVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        _result = getBroker().getTaskStatus(getParameters().getTaskId().toString());
        proceedProxyReturnValue();
        setReturnValue(parseTaskStatus(_result.taskStatus));
    }

    @Override
    protected void proceedProxyReturnValue() {
        VdcBllErrors returnStatus = getReturnValueFromStatus(getReturnStatus());
        switch (returnStatus) {
        case UnknownTask:
            // ignore this, the parser can handle the empty result.
            break;

        default:
            super.proceedProxyReturnValue();
            initializeVdsError(returnStatus);
            break;
        }
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return _result.mStatus;
    }

    @Override
    protected void updateReturnStatus(StatusForXmlRpc newReturnStatus) {
        _result.mStatus = newReturnStatus;
    }
}
