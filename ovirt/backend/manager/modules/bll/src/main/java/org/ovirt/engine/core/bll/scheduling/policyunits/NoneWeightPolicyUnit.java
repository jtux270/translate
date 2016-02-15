package org.ovirt.engine.core.bll.scheduling.policyunits;

import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;

public class NoneWeightPolicyUnit extends EvenDistributionWeightPolicyUnit {

    public NoneWeightPolicyUnit(PolicyUnit policyUnit) {
        super(policyUnit);
    }

    @Override
    public List<Pair<Guid, Integer>> score(List<VDS> hosts, VM vm, Map<String, String> parameters) {
        return super.score(hosts, vm, parameters);
    }

}
