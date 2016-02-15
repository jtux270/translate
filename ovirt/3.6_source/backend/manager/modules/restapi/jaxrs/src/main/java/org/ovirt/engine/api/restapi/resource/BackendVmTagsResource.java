package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.resource.AssignedTagResource;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.queries.GetTagsByVmIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendVmTagsResource extends AbstractBackendAssignedTagsResource {
    public BackendVmTagsResource(String parentId) {
        super(VM.class, parentId, VdcActionType.AttachVmsToTag);
    }

    public List<Tags> getCollection() {
        return getBackendCollection(VdcQueryType.GetTagsByVmId, new GetTagsByVmIdParameters(parentId));
    }

    @Override
    public AssignedTagResource getAssignedTagSubResource(String id) {
        return inject(new BackendVmTagResource(asGuid(parentId), id));
    }
}
