package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmExitStatus;
import org.ovirt.engine.core.common.vdscommands.DestroyVmVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SetVmStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

/**
 * Restart VMs running on Vds, that was stopped using PM or kdump was detected
 * on it
 */
public class RestartVdsVmsOperation {
    private static final Log log = LogFactory.getLog(RestartVdsVmsOperation.class);

    /**
     * Context of command, that instantiates this class
     */
    private CommandContext commandContext;

    /**
     * VDS, that VMs was executed on
     */
    private VDS vds;

    /**
     * Creates instance with specified params
     */
    public RestartVdsVmsOperation(
            CommandContext commandContext,
            VDS vds
    ) {
        this.commandContext = commandContext;
        this.vds = vds;
    }

    /**
     * Destroys VM migration to another host
     * @param vm vm migrated to another host
     */
    protected void destroyVmOnDestination(VM vm) {
        if (vm.getStatus() == VMStatus.MigratingFrom) {
            try {
                if (vm.getMigratingToVds() != null) {
                    Backend.getInstance().getResourceManager().RunVdsCommand(
                            VDSCommandType.DestroyVm,
                            new DestroyVmVDSCommandParameters(
                                    new Guid(vm.getMigratingToVds().toString()),
                                    vm.getId(),
                                    true,
                                    false,
                                    0
                            )
                    );
                    log.infoFormat(
                            "Stopped migrating vm: {0} on vds: {1}",
                            vm.getName(),
                            vm.getMigratingToVds()
                    );
                }
            } catch (RuntimeException ex) {
                log.infoFormat(
                        "Could not stop migrating vm: {0} on vds: {1}, Error: {2}",
                        vm.getName(),
                        vm.getMigratingToVds(),
                        ex.getMessage()
                );
                // intentionally ignored
            }
        }
    }


    /**
     * Changes status of specified VMs to Down and starts HA VMs on another hosts
     *
     * @param vms list of VM to stopped/restarted
     */
    public void restartVms(List<VM> vms) {
        List<Guid> autoStartVmIdsToRerun = new ArrayList<>();
        // restart all running vms of a failed vds.
        for (VM vm : vms) {
            destroyVmOnDestination(vm);
            VDSReturnValue returnValue = Backend.getInstance().getResourceManager().RunVdsCommand(
                    VDSCommandType.SetVmStatus,
                    new SetVmStatusVDSCommandParameters(
                            vm.getId(),
                            VMStatus.Down,
                            VmExitStatus.Error
                    )
            );
            // Write that this VM was shut down by host reboot or manual fence
            if (returnValue != null && returnValue.getSucceeded()) {
                AuditLogDirector.log(
                        new AuditLogableBase(
                                vds.getId(),
                                vm.getId()
                        ),
                        AuditLogType.VM_WAS_SET_DOWN_DUE_TO_HOST_REBOOT_OR_MANUAL_FENCE
                );
            }
            Backend.getInstance().runInternalAction(
                    VdcActionType.ProcessDownVm,
                    new IdParameters(vm.getId()),
                    ExecutionHandler.createDefaultContextForTasks(commandContext)
            );

            // Handle highly available VMs
            if (vm.isAutoStartup()) {
                autoStartVmIdsToRerun.add(vm.getId());
            }
        }
        if (!autoStartVmIdsToRerun.isEmpty()) {
            AutoStartVmsRunner.getInstance().addVmsToRun(autoStartVmIdsToRerun);
        }
    }
}
