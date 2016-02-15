package org.ovirt.engine.core.bll.scheduling.policyunits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.hostdev.HostDeviceManager;
import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.bll.scheduling.pending.PendingResourceManager;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.scheduling.PerHostMessages;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.di.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters hosts based on the passthroughSupported flag when the VM requires hostdev passthrough
 */
public class HostDeviceFilterPolicyUnit extends PolicyUnitImpl {

    private static final Logger log = LoggerFactory.getLogger(HostDeviceFilterPolicyUnit.class);

    private HostDeviceManager hostDeviceManager;

    public HostDeviceFilterPolicyUnit(PolicyUnit policyUnit, PendingResourceManager pendingResourceManager) {
        super(policyUnit, pendingResourceManager);
        hostDeviceManager = Injector.get(HostDeviceManager.class);
    }

    @Override
    public List<VDS> filter(List<VDS> hosts, VM vm, Map<String, String> parameters, PerHostMessages messages) {

        // noop if VM does not require host devices
        if (!hostDeviceManager.checkVmNeedsDirectPassthrough(vm)) {
            return hosts;
        }

        List<VDS> list = new ArrayList<>();
        for (VDS host : hosts) {
            if (!host.isHostDevicePassthroughEnabled()) {
                messages.addMessage(host.getId(), EngineMessage.VAR__DETAIL__HOSTDEV_DISABLED.toString());
                log.debug("Host '{}' does not support host device passthrough", host.getName());
                continue;
            }
            if (!hostDeviceManager.checkVmHostDeviceAvailability(vm, host.getId())) {
                messages.addMessage(host.getId(), EngineMessage.VAR__DETAIL__HOST_DEVICE_UNAVAILABLE.toString());
                log.debug("Some of the devices on host '{}' are unavailable", host.getName());
                continue;
            }
            list.add(host);
        }

        return list;
    }
}
