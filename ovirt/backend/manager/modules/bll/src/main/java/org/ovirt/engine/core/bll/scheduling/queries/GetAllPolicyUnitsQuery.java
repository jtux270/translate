package org.ovirt.engine.core.bll.scheduling.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.bll.scheduling.SchedulingManager;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.compat.Guid;

public class GetAllPolicyUnitsQuery extends QueriesCommandBase<VdcQueryParametersBase> {
    public GetAllPolicyUnitsQuery(VdcQueryParametersBase parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        Map<Guid, PolicyUnitImpl> map = SchedulingManager.getInstance().getPolicyUnitsMap();
        List<PolicyUnit> retList = new ArrayList<PolicyUnit>();
        for (PolicyUnitImpl policyUnitImpl : map.values()) {
            retList.add(policyUnitImpl.getPolicyUnit());
        }
        getQueryReturnValue().setReturnValue(retList);
    }
}
