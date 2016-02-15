package org.ovirt.engine.core.bll.scheduling.policyunits;

import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HostedEngineHAClusterWeightPolicyUnit extends PolicyUnitImpl {
    private static int DEFAULT_WEIGHT = 1;
    private static int MAXIMUM_HA_SCORE = 2400;

    public HostedEngineHAClusterWeightPolicyUnit(PolicyUnit policyUnit) {
        super(policyUnit);
    }

    void fillDefaultScores(List<VDS> hosts, List<Pair<Guid, Integer>> scores) {
        for (VDS host : hosts) {
            scores.add(new Pair<Guid, Integer>(host.getId(), DEFAULT_WEIGHT));
        }
    }

    @Override
    public List<Pair<Guid, Integer>> score(List<VDS> hosts, VM vm, Map<String, String> parameters) {
        List<Pair<Guid, Integer>> scores = new ArrayList<Pair<Guid, Integer>>();
        boolean isHostedEngine = vm.isHostedEngine();

        if (isHostedEngine) {
            // If the max HA score is higher than the max weight, then we normalize. Otherwise the ratio is 1, keeping the value as is
            float ratio = MAXIMUM_HA_SCORE > MaxSchedulerWeight ? ((float) MaxSchedulerWeight / MAXIMUM_HA_SCORE) : 1;
            for (VDS host : hosts) {
                scores.add(new Pair<Guid, Integer>(host.getId(), MaxSchedulerWeight - Math.round(host.getHighlyAvailableScore() * ratio)));
            }
        } else {
            fillDefaultScores(hosts, scores);
        }
        return scores;
    }

}
