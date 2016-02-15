package org.ovirt.engine.core.bll.hostdev;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VmHostDevicesParameters;
import org.ovirt.engine.core.common.businessentities.HostDevice;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmHostDevice;
import org.ovirt.engine.core.common.errors.EngineMessage;

public class AddVmHostDevicesCommand extends AbstractVmHostDevicesCommand<VmHostDevicesParameters> {

    public AddVmHostDevicesCommand(VmHostDevicesParameters parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {

        Set<HostDevice> affectedHostDevices = getAffectedHostDevices();
        Map<String, VmHostDevice> existingDevices = getExistingVmHostDevicesByName();

        List<VmDevice> devicesToAdd = new ArrayList<>();
        List<VmDevice> devicesToUpdate = new ArrayList<>();

        for (HostDevice hostDevice : affectedHostDevices) {
            if (!existingDevices.containsKey(hostDevice.getDeviceName())) {
                VmHostDevice device = new VmHostDevice(getVmId(), hostDevice);

                // if the device was not explicitly intended by the user (only added due to the IOMMU group
                // we mark it as as placeholder
                boolean required = getPrimaryDeviceNames().contains(device.getDevice());
                device.setIommuPlaceholder(!required);

                devicesToAdd.add(device);
            } else {
                VmHostDevice device = new VmHostDevice(existingDevices.get(hostDevice.getDeviceName()));
                // if the device was previously only added as placeholder we update the flag
                // as it is now explicitly requested by the user
                if (getPrimaryDeviceNames().contains(device.getDevice()) && device.isIommuPlaceholder()) {
                    device.setIommuPlaceholder(false);

                    devicesToUpdate.add(device);
                }
            }
        }

        getVmDeviceDao().saveAllInBatch(devicesToAdd);
        getVmDeviceDao().updateAllInBatch(devicesToUpdate);

        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return AuditLogType.VM_ADD_HOST_DEVICES;
    }

    public List<String> getNamesAdded() {
        return new ArrayList<>(getPrimaryDeviceNames());
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__ADD);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__HOST_DEVICES);
    }
}

