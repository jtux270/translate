package org.ovirt.engine.core.vdsbroker;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkStatistics;
import org.ovirt.engine.core.common.vdscommands.SetVmStatusVDSCommandParameters;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class SetVmStatusVDSCommand<P extends SetVmStatusVDSCommandParameters> extends VDSCommandBase<P> {
    private static final Log log = LogFactory.getLog(SetVmStatusVDSCommand.class);

    public SetVmStatusVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVDSCommand() {
        SetVmStatusVDSCommandParameters parameters = getParameters();
        final VMStatus status = parameters.getStatus();

        if (status == null) {
            log.warnFormat("got request to change the status of VM whose id is {0} to null,  ignoring", parameters.getVmId());
            return;
        }

        VmDynamic vmDynamic = DbFacade.getInstance().getVmDynamicDao().get(parameters.getVmId());
        vmDynamic.setStatus(status);
        if (status.isNotRunning()) {
            ResourceManager.getInstance().RemoveAsyncRunningVm(parameters.getVmId());
            VmStatistics vmStatistics = DbFacade.getInstance().getVmStatisticsDao().get(parameters.getVmId());
            VM vm = new VM(null, vmDynamic, vmStatistics);
            ResourceManager.getInstance().InternalSetVmStatus(vm, status, parameters.getExitStatus());
            DbFacade.getInstance().getVmStatisticsDao().update(vm.getStatisticsData());
            List<VmNetworkInterface> interfaces = vm.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                for (VmNetworkInterface ifc : interfaces) {
                    VmNetworkStatistics stats = ifc.getStatistics();
                    DbFacade.getInstance().getVmNetworkStatisticsDao().update(stats);
                }
            }

        } else if (status == VMStatus.Unknown) {
            ResourceManager.getInstance().RemoveAsyncRunningVm(parameters.getVmId());
        }
        DbFacade.getInstance().getVmDynamicDao().update(vmDynamic);
    }
}
