package org.ovirt.engine.core.vdsbroker.irsbroker;

import org.ovirt.engine.core.common.vdscommands.StoragePoolDomainAndGroupIdBaseVDSCommandParameters;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public abstract class IrsCreateCommand<P extends StoragePoolDomainAndGroupIdBaseVDSCommandParameters>
        extends IrsBrokerCommand<P> {
    protected String mCreatedImageId = "";
    protected OneUuidReturnForXmlRpc uuidReturn;

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return uuidReturn.mStatus;
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return uuidReturn;
    }

    public IrsCreateCommand(P parameters) {
        super(parameters);
    }
}
