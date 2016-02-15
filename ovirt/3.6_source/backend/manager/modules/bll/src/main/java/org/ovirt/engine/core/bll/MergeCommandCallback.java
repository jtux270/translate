package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.common.action.MergeParameters;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmJob;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeCommandCallback extends CommandCallback {
    private static final Logger log = LoggerFactory.getLogger(MergeCommandCallback.class);

    @Override
    public void doPolling(Guid cmdId, List<Guid> childCmdIds) {
        // If the VM Job exists, the command is still active
        boolean isRunning = false;
        MergeCommand<MergeParameters> command = getCommand(cmdId);
        VMStatus vmStatus = DbFacade.getInstance().getVmDynamicDao().get(command.getParameters().getVmId()).getStatus();
        List<VmJob> vmJobs = DbFacade.getInstance().getVmJobDao().getAllForVmDisk(
                command.getParameters().getVmId(),
                command.getParameters().getImageGroupId());
        for (VmJob vmJob : vmJobs) {
            if (vmJob.getId().equals(command.getParameters().getVmJobId())) {
                if (vmStatus == VMStatus.Down) {
                    DbFacade.getInstance().getVmJobDao().remove(vmJob.getId());
                    log.info("VM '{}' is down, Merge command '{}' removed",
                            command.getParameters().getVmId(), vmJob.getId());
                } else {
                    log.info("Waiting on merge command to complete");
                    isRunning = true;
                }
                break;
            }
        }

        if (!isRunning) {
            // It finished; a command will be called later to determine the status.
            command.setSucceeded(true);
            command.setCommandStatus(CommandStatus.SUCCEEDED);
            command.persistCommand(command.getParameters().getParentCommand(), true);
            log.info("Merge command has completed for images '{}'..'{}'",
                    command.getParameters().getBaseImage().getImageId(),
                    command.getParameters().getTopImage().getImageId());
        }
    }

    private MergeCommand<MergeParameters> getCommand(Guid cmdId) {
        return CommandCoordinatorUtil.retrieveCommand(cmdId);
    }
}
