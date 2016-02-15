package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.action.RngDeviceParameters;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

public class AddRngDeviceCommand extends AbstractRngDeviceCommand<RngDeviceParameters> {

    public AddRngDeviceCommand(RngDeviceParameters parameters) {
        this(parameters, null);
    }

    public AddRngDeviceCommand(RngDeviceParameters parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        if (getTemplateType() != VmEntityType.INSTANCE_TYPE) {
            if (!validate(getVirtioRngValidator().canAddRngDevice(getVdsGroup(), getParameters().getRngDevice()))) {
                return false;
            }
        }

        if (!getRngDevices().isEmpty()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_RNG_ALREADY_EXISTS);
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        VmRngDevice rngDevice = getParameters().getRngDevice();
        if (rngDevice.getDeviceId() == null) {
            rngDevice.setDeviceId(Guid.newGuid());
        }
        getDbFacade().getVmDeviceDao().save(rngDevice);
        setActionReturnValue(rngDevice.getDeviceId());
        setSucceeded(true);
    }

}
