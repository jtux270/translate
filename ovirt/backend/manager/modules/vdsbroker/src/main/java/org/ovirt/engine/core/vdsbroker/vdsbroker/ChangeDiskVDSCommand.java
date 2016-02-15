package org.ovirt.engine.core.vdsbroker.vdsbroker;

import org.ovirt.engine.core.common.vdscommands.ChangeDiskVDSCommandParameters;

public class ChangeDiskVDSCommand<P extends ChangeDiskVDSCommandParameters> extends VmReturnVdsBrokerCommand<P> {
    private String mIsoLocation = "";

    public ChangeDiskVDSCommand(P parameters) {
        super(parameters);
        mIsoLocation = parameters.getDiskPath();
    }

    @Override
    protected void executeVdsBrokerCommand() {
        mVmReturn = getBroker().changeDisk(mVmId.toString(), mIsoLocation);
        proceedProxyReturnValue();
        setReturnValue(VdsBrokerObjectsBuilder.buildVMDynamicData(mVmReturn.mVm).getStatus());
    }
}
