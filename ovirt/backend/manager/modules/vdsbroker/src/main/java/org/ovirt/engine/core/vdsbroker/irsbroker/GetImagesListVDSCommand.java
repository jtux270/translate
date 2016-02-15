package org.ovirt.engine.core.vdsbroker.irsbroker;

import java.util.ArrayList;

import org.ovirt.engine.core.common.vdscommands.GetImagesListVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public class GetImagesListVDSCommand<P extends GetImagesListVDSCommandParameters>
        extends IrsBrokerCommand<P> {
    private ImagesListReturnForXmlRpc result;

    public GetImagesListVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeIrsBrokerCommand() {
        result = getIrsProxy().getImagesList(getParameters().getStorageDomainId().toString());
        proceedProxyReturnValue();
        ArrayList<Guid> tempRetValue = new ArrayList<Guid>(result.getImageList().length);
        for (String id : result.getImageList()) {
            tempRetValue.add(new Guid(id));
        }
        setReturnValue(tempRetValue);
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return result.mStatus;
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return result;
    }
}
