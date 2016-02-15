package org.ovirt.engine.ui.frontend;

import java.util.List;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.errors.VdcFault;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public interface IFrontendEventsHandler {
    Boolean isRaiseErrorModalPanel(VdcActionType action, VdcFault fault);

    Boolean isRaiseErrorModalPanel(VdcQueryType queryType);

    void runActionExecutionFailed(VdcActionType action, VdcFault fault);

    void runMultipleActionFailed(VdcActionType action, List<VdcReturnValueBase> returnValues);

    void runMultipleActionsFailed(List<VdcActionType> actions, List<VdcReturnValueBase> returnValues);

    void runQueryFailed(List<VdcQueryReturnValue> returnValue);

    void publicConnectionClosed(Exception ex);
}
