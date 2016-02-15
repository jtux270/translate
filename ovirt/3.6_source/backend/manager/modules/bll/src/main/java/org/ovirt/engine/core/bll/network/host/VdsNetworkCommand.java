package org.ovirt.engine.core.bll.network.host;

import org.ovirt.engine.core.bll.VdsCommand;
import org.ovirt.engine.core.common.action.AttachNetworkToVdsParameters;

public abstract class VdsNetworkCommand<T extends AttachNetworkToVdsParameters> extends VdsCommand<T> {
    public VdsNetworkCommand(T parameters) {
        super(parameters);
    }

    public String getNetworkName() {
        return getParameters().getNetwork().getName();
    }
}
