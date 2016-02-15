package org.ovirt.engine.core.vdsbroker.irsbroker;

import org.ovirt.engine.core.common.vdscommands.IrsBaseVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPMGetAllTasksInfoVDSCommand<P extends IrsBaseVDSCommandParameters> extends IrsBrokerCommand<P> {
    private static final Logger log = LoggerFactory.getLogger(SPMGetAllTasksInfoVDSCommand.class);

    public SPMGetAllTasksInfoVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeIrsBrokerCommand() {
        log.info(
                "-- executeIrsBrokerCommand: Attempting on storage pool '{}'",
                getParameters().getStoragePoolId());

        setReturnValue(ResourceManager
                .getInstance()
                .runVdsCommand(VDSCommandType.HSMGetAllTasksInfo,
                        new VdsIdVDSCommandParametersBase(getCurrentIrsProxyData().getCurrentVdsId()))
                .getReturnValue());
    }
}
