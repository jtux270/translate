package org.ovirt.engine.core.bll;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.action.RngDeviceParameters;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.errors.VdcBllMessages;

public class RemoveRngDeviceCommand extends AbstractRngDeviceCommand<RngDeviceParameters> {

    RemoveRngDeviceCommand(RngDeviceParameters parameters) {
        this(parameters, null);
    }

    RemoveRngDeviceCommand(RngDeviceParameters parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        if (getRngDevices().isEmpty()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_RNG_NOT_FOUND);
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        List<VmDevice> rngDevices = getRngDevices();
        Set<VmDeviceId> idsToRemove = new HashSet<>();

        for (VmDevice dev : rngDevices) {
            idsToRemove.add(dev.getId());
        }

        getDbFacade().getVmDeviceDao().removeAll(idsToRemove);
        setSucceeded(true);
    }
}
