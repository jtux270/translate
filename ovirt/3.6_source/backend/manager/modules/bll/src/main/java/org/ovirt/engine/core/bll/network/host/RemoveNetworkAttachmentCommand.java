package org.ovirt.engine.core.bll.network.host;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.VdsCommand;
import org.ovirt.engine.core.common.action.HostSetupNetworksParameters;
import org.ovirt.engine.core.common.action.RemoveNetworkAttachmentParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.validation.group.RemoveEntity;

@NonTransactiveCommandAttribute
public class RemoveNetworkAttachmentCommand<T extends RemoveNetworkAttachmentParameters> extends VdsCommand<T> {

    public RemoveNetworkAttachmentCommand(T parameters) {
        super(parameters);
        addValidationGroup(RemoveEntity.class);
    }

    @Override
    protected boolean canDoAction() {
        return true;
    }

    @Override
    protected void executeCommand() {
        HostSetupNetworksParameters params = new HostSetupNetworksParameters(getParameters().getVdsId());
        params.getRemovedNetworkAttachments().add(getParameters().getNetworkAttachmentId());
        VdcReturnValueBase returnValue = runInternalAction(VdcActionType.HostSetupNetworks, params);
        propagateFailure(returnValue);
        setSucceeded(returnValue.getSucceeded());
    }
}
