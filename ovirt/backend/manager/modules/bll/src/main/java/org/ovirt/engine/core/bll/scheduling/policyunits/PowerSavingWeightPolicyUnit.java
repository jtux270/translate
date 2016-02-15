package org.ovirt.engine.core.bll.scheduling.policyunits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class PowerSavingWeightPolicyUnit extends EvenDistributionWeightPolicyUnit {

    public PowerSavingWeightPolicyUnit(PolicyUnit policyUnit) {
        super(policyUnit);
    }

    @Override
    public List<Pair<Guid, Integer>> score(List<VDS> hosts, VM vm, Map<String, String> parameters) {
        VDSGroup vdsGroup = null;
        List<Pair<Guid, Integer>> scores = new ArrayList<Pair<Guid, Integer>>();
        for (VDS vds : hosts) {
            int score = MaxSchedulerWeight - 1;
            if (vds.getVmCount() > 0) {
                if (vdsGroup == null) {
                    vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(hosts.get(0).getVdsGroupId());
                }
                score -=
                        calcEvenDistributionScore(vds, vm, vdsGroup != null ? vdsGroup.getCountThreadsAsCores() : false);
            }
            scores.add(new Pair<Guid, Integer>(vds.getId(), score));
        }
        return scores;
    }
}
