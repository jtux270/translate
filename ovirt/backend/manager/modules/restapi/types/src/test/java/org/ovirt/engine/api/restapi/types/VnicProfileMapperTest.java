package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.VnicProfile;
import org.ovirt.engine.api.restapi.utils.CustomPropertiesParser;

public class VnicProfileMapperTest extends AbstractInvertibleMappingTest<VnicProfile,
        org.ovirt.engine.core.common.businessentities.network.VnicProfile,
        org.ovirt.engine.core.common.businessentities.network.VnicProfile> {

    public VnicProfileMapperTest() {
        super(VnicProfile.class,
                org.ovirt.engine.core.common.businessentities.network.VnicProfile.class,
                org.ovirt.engine.core.common.businessentities.network.VnicProfile.class);
    }

    @Override
    protected void verify(VnicProfile model, VnicProfile transform) {
        assertNotNull(transform);
        assertEquals(model.getId(), transform.getId());
        assertEquals(model.getName(), transform.getName());
        assertEquals(model.getDescription(), transform.getDescription());
        assertNotNull(transform.getNetwork());
        assertEquals(model.getNetwork().getId(), transform.getNetwork().getId());
        assertEquals(model.isPortMirroring(), transform.isPortMirroring());
        assertNotNull(transform.getCustomProperties());
        assertEquals(CustomPropertiesParser.parse(model.getCustomProperties().getCustomProperty()),
                CustomPropertiesParser.parse(transform.getCustomProperties().getCustomProperty()));
    }
}
