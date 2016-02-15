package org.ovirt.engine.core.bll.network.vm;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.VmCommand;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReorderVmNicsCommand<T extends VmOperationParameterBase> extends VmCommand<T> {

    public ReorderVmNicsCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        VM vm = getVm();
        if (vm == null || vm.getStaticData() == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_EXIST);
            return false;
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        return true;
    }

    @Override
    protected void executeVmCommand() {
        reorderNics();
        setSucceeded(true);
    }

    private Map<Guid, VmDevice> getVmInterfaceDevices() {
        List<VmDevice> vmInterfaceDevicesList = getVmDeviceDao().getVmDeviceByVmIdAndType(getParameters().getVmId(), VmDeviceGeneralType.INTERFACE);
        Map<Guid, VmDevice> vmInterfaceDevices = new HashMap();
        for (VmDevice device : vmInterfaceDevicesList) {
            vmInterfaceDevices.put(device.getDeviceId(), device);
        }
        return vmInterfaceDevices;
    }

    private void reorderNics() {
        Map<Guid, VmDevice> vmInterfaceDevices = getVmInterfaceDevices();
        List<VmNic> nics = getVmNicDao().getAllForVm(getParameters().getVmId());
        List<VmNic> nicsToReorder = new ArrayList<VmNic>();
        List<String> macsToReorder = new ArrayList<String>();

        for (VmNic nic : nics) {
            VmDevice nicDevice = vmInterfaceDevices.get(nic.getId());
            // If there is not device, or the PCI address is empty
            if (nicDevice == null || StringUtils.isEmpty(nicDevice.getAddress())) {
                nicsToReorder.add(nic);
                // We know that all the NICs have a MAC address
                macsToReorder.add(nic.getMacAddress());
            }
        }

        // Sorting the NICs to reorder by name
        Collections.sort(nicsToReorder, new Comparator<VmNic>() {
            @Override
            public int compare(VmNic nic1, VmNic nic2) {
                return nic1.getName().compareTo(nic2.getName());
            }
        });

        // Sorting the MAC addresses to reorder
        Collections.sort(macsToReorder);
        for (int i = 0; i < nicsToReorder.size(); ++i) {
            VmNic nic = nicsToReorder.get(i);
            nic.setMacAddress(macsToReorder.get(i));
            getVmNicDao().update(nic);
        }
    }
}

