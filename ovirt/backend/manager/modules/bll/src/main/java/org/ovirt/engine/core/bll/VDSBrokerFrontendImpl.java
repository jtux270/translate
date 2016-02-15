package org.ovirt.engine.core.bll;

import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.IVdsAsyncCommand;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.interfaces.FutureVDSCall;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.vdscommands.FutureVDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsAndVmIDVDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.ResourceManager;

public class VDSBrokerFrontendImpl implements VDSBrokerFrontend {

    private Map<Guid, IVdsAsyncCommand> _asyncRunningCommands = new HashMap<Guid, IVdsAsyncCommand>();

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ovirt.engine.core.bll.VDSBrokerFrontend#RunVdsCommand(org.ovirt.engine.core
     * .common.vdscommands.VDSCommandType,
     * org.ovirt.engine.core.common.vdscommands.VDSParametersBase)
     */
    @Override
    public VDSReturnValue RunVdsCommand(VDSCommandType commandType, VDSParametersBase parameters) {
        return VdsHandler.handleVdsResult(getResourceManager().runVdsCommand(commandType, parameters));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ovirt.engine.core.bll.VDSBrokerFrontend#RunAsyncVdsCommand(com.redhat.
     * engine.common.vdscommands.VDSCommandType,
     * org.ovirt.engine.core.common.vdscommands.VdsAndVmIDVDSParametersBase,
     * org.ovirt.engine.core.common.businessentities.IVdsAsyncCommand)
     */
    @Override
    public VDSReturnValue RunAsyncVdsCommand(VDSCommandType commandType, VdsAndVmIDVDSParametersBase parameters,
                                             IVdsAsyncCommand command) {
        VDSReturnValue result = RunVdsCommand(commandType, parameters);
        if (result.getSucceeded()) {
            // Add async command to cached commands
            IVdsAsyncCommand prevCommand = _asyncRunningCommands.put(parameters.getVmId(), command);
            if (prevCommand != null && !prevCommand.equals(command)) {
                prevCommand.reportCompleted();
            }
        } else {
            throw new VdcBLLException(result.getVdsError().getCode(), result.getExceptionString());
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ovirt.engine.core.bll.VDSBrokerFrontend#GetAsyncCommandForVm(com.redhat
     * .engine.compat.Guid)
     */
    @Override
    public IVdsAsyncCommand GetAsyncCommandForVm(Guid vmId) {
        IVdsAsyncCommand result = null;
        result = _asyncRunningCommands.get(vmId);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ovirt.engine.core.bll.VDSBrokerFrontend#RemoveAsyncRunningCommand(com.
     * redhat.engine.compat.Guid)
     */
    @Override
    public IVdsAsyncCommand RemoveAsyncRunningCommand(Guid vmId) {
        return _asyncRunningCommands.remove(vmId);
    }

    @Override
    public FutureVDSCall<VDSReturnValue> runFutureVdsCommand(FutureVDSCommandType commandType,
            VdsIdVDSCommandParametersBase parameters) {
        return getResourceManager().runFutureVdsCommand(commandType, parameters);
    }

    private ResourceManager getResourceManager() {
        return ResourceManager.getInstance();
    }
}
