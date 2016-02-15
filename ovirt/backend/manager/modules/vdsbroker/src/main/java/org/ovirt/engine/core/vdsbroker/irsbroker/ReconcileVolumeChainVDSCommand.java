package org.ovirt.engine.core.vdsbroker.irsbroker;

import java.util.ArrayList;

import org.ovirt.engine.core.common.vdscommands.ReconcileVolumeChainVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public class ReconcileVolumeChainVDSCommand<P extends ReconcileVolumeChainVDSCommandParameters> extends IrsBrokerCommand<P> {
    protected VolumeListReturnForXmlRpc volumeListReturn;

    public ReconcileVolumeChainVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeIrsBrokerCommand() {
        volumeListReturn = getIrsProxy().reconcileVolumeChain(
                getParameters().getStoragePoolId().toString(),
                getParameters().getStorageDomainId().toString(),
                getParameters().getImageGroupId().toString(),
                getParameters().getImageId().toString());
        proceedProxyReturnValue();
        ArrayList<Guid> tempRetValue = new ArrayList<Guid>(volumeListReturn.getVolumeList().length);
        for (String id : volumeListReturn.getVolumeList()) {
            tempRetValue.add(new Guid(id));
        }
        setReturnValue(tempRetValue);
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return volumeListReturn;
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return volumeListReturn.mStatus;
    }
}
