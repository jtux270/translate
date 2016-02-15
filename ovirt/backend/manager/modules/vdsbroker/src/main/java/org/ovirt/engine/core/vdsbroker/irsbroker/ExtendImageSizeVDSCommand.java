package org.ovirt.engine.core.vdsbroker.irsbroker;

import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.vdscommands.ExtendImageSizeVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public class ExtendImageSizeVDSCommand<P extends ExtendImageSizeVDSCommandParameters> extends IrsBrokerCommand<P> {

    private OneUuidReturnForXmlRpc result;

    public ExtendImageSizeVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeIrsBrokerCommand() {
        result = getIrsProxy().extendVolumeSize(getParameters().getStoragePoolId().toString(),
                getParameters().getStorageDomainId().toString(),
                getParameters().getImageGroupId().toString(),
                getParameters().getImageId().toString(),
                String.valueOf(getParameters().getNewSize()));

        proceedProxyReturnValue();

        getVDSReturnValue().setCreationInfo(
                new AsyncTaskCreationInfo(Guid.createGuidFromString(result.mUuid),
                        AsyncTaskType.extendImageSize, getParameters().getStoragePoolId()));
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return result.mStatus;
    }
}
