package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.BaseResource;

public abstract class AbstractBackendResourceTest<R extends BaseResource, Q /* extends IVdcQueryable */>
        extends AbstractBackendBaseTest {

    protected void initResource(AbstractBackendResource<R, Q> resource) {
        resource.setMappingLocator(mapperLocator);
        initBackendResource(resource);
    }

    protected abstract Q getEntity(int index);

    protected void verifyModel(R model, int index) {
        assertEquals(GUIDS[index].toString(), model.getId());
        assertEquals(NAMES[index], model.getName());
        assertEquals(DESCRIPTIONS[index], model.getDescription());
        verifyLinks(model);
    }
}
