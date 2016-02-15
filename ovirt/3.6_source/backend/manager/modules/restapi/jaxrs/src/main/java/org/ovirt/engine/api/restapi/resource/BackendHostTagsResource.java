package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import org.ovirt.engine.api.model.Host;
import org.ovirt.engine.api.resource.AssignedTagResource;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.queries.GetTagsByVdsIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendHostTagsResource extends AbstractBackendAssignedTagsResource {
    public BackendHostTagsResource(String parentId) {
        super(Host.class, parentId, VdcActionType.AttachVdsToTag);
    }

    public List<Tags> getCollection() {
        return getBackendCollection(VdcQueryType.GetTagsByVdsId, new GetTagsByVdsIdParameters(parentId));
    }

    @Override
    public AssignedTagResource getAssignedTagSubResource(String id) {
        return inject(new BackendHostTagResource(asGuid(parentId), id));
    }
}
