package org.ovirt.engine.core.bll.network.host;

import org.ovirt.engine.core.bll.VdsCommand;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;

public class CommitNetworkChangesCommand<T extends VdsActionParameters> extends VdsCommand<T> {
    public CommitNetworkChangesCommand(T param) {
        this(param, null);
    }

    public CommitNetworkChangesCommand(T param, CommandContext commandContext) {
        super(param, commandContext);
    }


    @Override
    protected void executeCommand() {
        VDSReturnValue retVal =
                runVdsCommand(VDSCommandType.SetSafeNetworkConfig,
                        new VdsIdVDSCommandParametersBase(getParameters().getVdsId()));

        getDbFacade().getVdsDynamicDao().updateNetConfigDirty(getParameters().getVdsId(), false);
        setSucceeded(retVal.getSucceeded());
    }

    @Override
    protected boolean canDoAction() {
        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_COMMINT_NETWORK_CHANGES
                : AuditLogType.NETWORK_COMMINT_NETWORK_CHANGES_FAILED;
    }
}
