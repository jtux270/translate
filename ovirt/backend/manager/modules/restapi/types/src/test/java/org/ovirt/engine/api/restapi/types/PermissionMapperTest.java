package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.Permission;
import org.ovirt.engine.core.common.businessentities.Permissions;

public class PermissionMapperTest extends AbstractInvertibleMappingTest<Permission, Permissions, Permissions> {

    public PermissionMapperTest() {
        super(Permission.class, Permissions.class, Permissions.class);
    }

    @Override
    protected void verify(Permission model, Permission transform) {
        assertNotNull(transform);
        assertTrue(transform.isSetId());
        assertEquals(model.getId(), transform.getId());
        assertTrue(transform.isSetRole());
        assertEquals(model.getRole().getId(), transform.getRole().getId());
        assertTrue(transform.isSetDataCenter());
        assertEquals(model.getDataCenter().getId(), transform.getDataCenter().getId());
    }

}

