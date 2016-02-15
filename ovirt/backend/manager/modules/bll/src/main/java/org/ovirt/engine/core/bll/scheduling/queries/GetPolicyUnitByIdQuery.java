package org.ovirt.engine.core.bll.scheduling.queries;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.bll.scheduling.SchedulingManager;
import org.ovirt.engine.core.common.queries.IdQueryParameters;

public class GetPolicyUnitByIdQuery extends QueriesCommandBase<IdQueryParameters> {
    public GetPolicyUnitByIdQuery(IdQueryParameters parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        PolicyUnitImpl value = SchedulingManager.getInstance()
                .getPolicyUnitsMap()
                .get(getParameters().getId());
        if (value != null) {
            getQueryReturnValue().setReturnValue(value.getPolicyUnit());
        }
    }
}
