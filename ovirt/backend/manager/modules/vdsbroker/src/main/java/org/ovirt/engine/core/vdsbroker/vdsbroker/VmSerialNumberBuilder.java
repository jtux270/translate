package org.ovirt.engine.core.vdsbroker.vdsbroker;

import org.ovirt.engine.core.common.businessentities.SerialNumberPolicy;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;

import java.util.Map;

public class VmSerialNumberBuilder {

    private VM vm;
    private VDSGroup vdsGroup;
    private Map<String, Object> createInfo;

    public VmSerialNumberBuilder(VM vm, VDSGroup vdsGroup, Map<String, Object> createInfo) {
        this.vm = vm;
        this.vdsGroup = vdsGroup;
        this.createInfo = createInfo;
    }

    public void buildVmSerialNumber() {
        if (vm.getSerialNumberPolicy() != null) {
            setSerialNumber(getSerialNumberFromPolicy(vm.getSerialNumberPolicy(), vm.getCustomSerialNumber()));
        } else {
            buildVmSerialNumberFromCluster();
        }
    }

    private void buildVmSerialNumberFromCluster() {
        if (vdsGroup.getSerialNumberPolicy() != null) {
            setSerialNumber(getSerialNumberFromPolicy(vdsGroup.getSerialNumberPolicy(), vdsGroup.getCustomSerialNumber()));
        } else {
            buildVmSerialNumberFromConfig();
        }
    }

    private void buildVmSerialNumberFromConfig() {
        final SerialNumberPolicy policy = Config.getValue(ConfigValues.DefaultSerialNumberPolicy);
        final String customSerialNumber = Config.getValue(ConfigValues.DefaultCustomSerialNumber);
        setSerialNumber(getSerialNumberFromPolicy(policy, customSerialNumber));
    }

    private void setSerialNumber(String serialNumber) {
        if (serialNumber != null) {
            createInfo.put(VdsProperties.SERIAL_NUMBER, serialNumber);
        }
    }

    private String getSerialNumberFromPolicy(SerialNumberPolicy serialNumberPolicy, String customSerialNumber) {
        switch (serialNumberPolicy) {
            default:
            case HOST_ID:
                return null;
            case VM_ID:
                return vm.getId().toString();
            case CUSTOM:
                return customSerialNumber;
        }
    }
}
