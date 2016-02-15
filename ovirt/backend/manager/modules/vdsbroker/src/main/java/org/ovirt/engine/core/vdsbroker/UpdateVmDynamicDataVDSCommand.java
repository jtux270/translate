package org.ovirt.engine.core.vdsbroker;

import org.ovirt.engine.core.common.vdscommands.UpdateVmDynamicDataVDSCommandParameters;

public class UpdateVmDynamicDataVDSCommand<P extends UpdateVmDynamicDataVDSCommandParameters> extends VdsIdVDSCommandBase<P> {
    public UpdateVmDynamicDataVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsIdCommand() {
        if (_vdsManager != null) {
            _vdsManager.updateVmDynamic(getParameters().getVmDynamic());
        } else {
            getVDSReturnValue().setSucceeded(false);
        }
    }
}
