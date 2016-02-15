package org.ovirt.engine.api.restapi.resource;

import org.junit.Test;
import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.core.compat.Guid;

public class BackendSystemAssignedPermissionsResourceTest
        extends BackendEntityAssignedPermissionsResourceTest {

    public BackendSystemAssignedPermissionsResourceTest() {
        super(Guid.SYSTEM, BaseResource.class);
    }

    @Test
    @Override
    public void testAddPermission() throws Exception {
        super.testAddPermission();
    }
}

