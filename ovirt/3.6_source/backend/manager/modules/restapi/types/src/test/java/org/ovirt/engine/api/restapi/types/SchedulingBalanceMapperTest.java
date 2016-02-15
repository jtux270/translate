package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.Balance;
import org.ovirt.engine.api.model.SchedulingPolicyUnit;
import org.ovirt.engine.core.common.scheduling.ClusterPolicy;
import org.ovirt.engine.core.compat.Guid;

public class SchedulingBalanceMapperTest extends AbstractInvertibleMappingTest<Balance, ClusterPolicy, ClusterPolicy> {

    public SchedulingBalanceMapperTest() {
        super(Balance.class,
                ClusterPolicy.class,
                ClusterPolicy.class);
    }

    private final static String ID = Guid.newGuid().toString();

    @Override
    protected Balance postPopulate(Balance model) {
        model.setId(ID);
        SchedulingPolicyUnit schedulingPolicyUnit = new SchedulingPolicyUnit();
        schedulingPolicyUnit.setId(ID);
        model.setSchedulingPolicyUnit(schedulingPolicyUnit);
        return model;
    }

    @Override
    protected Balance getModel(Balance Balance) {
        Balance = new Balance();
        Balance.setId(ID);
        return Balance;
    }

    @Override
    protected void verify(Balance model, Balance transform) {
        assertNotNull(transform);

        assertEquals(model.getId(), transform.getId());
        assertEquals(model.getSchedulingPolicyUnit().getId(), transform.getSchedulingPolicyUnit().getId());
    }

}
