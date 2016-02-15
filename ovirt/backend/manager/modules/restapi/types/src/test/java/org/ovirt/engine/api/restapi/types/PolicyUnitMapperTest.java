package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.SchedulingPolicyUnit;
import org.ovirt.engine.api.restapi.utils.CustomPropertiesParser;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.scheduling.PolicyUnitType;

public class PolicyUnitMapperTest extends AbstractInvertibleMappingTest<SchedulingPolicyUnit, PolicyUnit, PolicyUnit> {

    public PolicyUnitMapperTest() {
        super(SchedulingPolicyUnit.class,
                PolicyUnit.class,
                PolicyUnit.class);
    }

    @Override
    protected void verify(SchedulingPolicyUnit model, SchedulingPolicyUnit transform) {
        assertNotNull(transform);
        assertEquals(model.getName(), transform.getName());
        assertEquals(model.getId(), transform.getId());
        assertEquals(model.getDescription(), transform.getDescription());
        assertEquals(model.getType(), transform.getType());
        assertEquals(model.isEnabled(), transform.isEnabled());
        assertNotNull(model.getPropertiesMetaData());
        assertEquals(CustomPropertiesParser.toMap(model.getPropertiesMetaData()),
                CustomPropertiesParser.toMap(transform.getPropertiesMetaData()));
    }

    @Override
    protected SchedulingPolicyUnit postPopulate(SchedulingPolicyUnit model) {
        model = super.postPopulate(model);
        model.setType(MappingTestHelper.shuffle(PolicyUnitType.class).name().toLowerCase());
        return model;
    }

}
