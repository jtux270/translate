package org.ovirt.engine.core.bll.scheduling.policyunits;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.bll.scheduling.pending.PendingResourceManager;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.scheduling.PerHostMessages;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;

public class PinToHostPolicyUnit extends PolicyUnitImpl {

    public PinToHostPolicyUnit(PolicyUnit policyUnit,
            PendingResourceManager pendingResourceManager) {
        super(policyUnit, pendingResourceManager);
    }

    @Override
    public List<VDS> filter(List<VDS> hosts, VM vm, Map<String, String> parameters, PerHostMessages messages) {
        if (vm.getMigrationSupport() == MigrationSupport.PINNED_TO_HOST) {
            // host has been specified for pin to host.
            if(vm.getDedicatedVmForVdsList().size() > 0) {
                List<VDS> dedicatedHostsList = new LinkedList<>();
                for (VDS host : hosts) {
                    if (vm.getDedicatedVmForVdsList().contains(host.getId())) {
                        dedicatedHostsList.add(host);
                    }
                }
                return dedicatedHostsList;
            } else {
                // check pin to any (the VM should be down/ no migration allowed).
                if (vm.getRunOnVds() == null) {
                    return hosts;
                }
            }

            // if flow reaches here, the VM is pinned but there is no dedicated host.
            return Collections.emptyList();
        }

        return hosts;
    }
}
