package org.ovirt.engine.core.vdsbroker.irsbroker;

import java.util.List;

import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.ExtendStorageDomainVDSCommandParameters;

public class ExtendStorageDomainVDSCommand<P extends ExtendStorageDomainVDSCommandParameters>
        extends IrsBrokerCommand<P> {
    public ExtendStorageDomainVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeIrsBrokerCommand() {
        String storageDomainId = getParameters().getStorageDomainId().toString();
        String storagePoolId = getParameters().getStoragePoolId().toString();
        List<String> deviceList = getParameters().getDeviceList();
        String[] deviceArray = deviceList.toArray(new String[deviceList.size()]);
        boolean isForce = getParameters().isForce();

        status = getParameters().isSupportForceExtendVG() ?
                getIrsProxy().extendStorageDomain(storageDomainId, storagePoolId, deviceArray, isForce) :
                getIrsProxy().extendStorageDomain(storageDomainId, storagePoolId, deviceArray);

        proceedProxyReturnValue();
    }

    @Override
    protected void proceedProxyReturnValue() {
        VdcBllErrors returnStatus = getReturnValueFromStatus(getReturnStatus());
        switch (returnStatus) {
        case DeviceNotFound:
            getVDSReturnValue().setSucceeded(false);
            break;
        default:
            super.proceedProxyReturnValue();
            initializeVdsError(returnStatus);
            break;
        }
    }
}
