package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.AffinityGroup;
import org.ovirt.engine.api.model.AffinityGroups;
import org.ovirt.engine.api.resource.AffinityGroupResource;
import org.ovirt.engine.api.resource.AffinityGroupsResource;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.scheduling.parameters.AffinityGroupCRUDParameters;
import org.ovirt.engine.core.compat.Guid;

public class BackendAffinityGroupsResource
        extends AbstractBackendCollectionResource<AffinityGroup, org.ovirt.engine.core.common.scheduling.AffinityGroup>
        implements AffinityGroupsResource {

    static final String[] SUB_COLLECTIONS = { "vms" };

    private String clusterId;

    public BackendAffinityGroupsResource(String clusterId) {
        super(AffinityGroup.class, org.ovirt.engine.core.common.scheduling.AffinityGroup.class, SUB_COLLECTIONS);
        this.clusterId = clusterId;
    }

    @Override
    public AffinityGroups list() {
        List<org.ovirt.engine.core.common.scheduling.AffinityGroup> entities =
                getBackendCollection(VdcQueryType.GetAffinityGroupsByClusterId,
                        new IdQueryParameters(asGuid(clusterId)));
        return mapCollection(entities);
    }

    private AffinityGroups mapCollection(List<org.ovirt.engine.core.common.scheduling.AffinityGroup> entities) {
        AffinityGroups collection = new AffinityGroups();
        for (org.ovirt.engine.core.common.scheduling.AffinityGroup entity : entities) {
            collection.getAffinityGroups().add(addLinks(populate(map(entity), entity)));
        }
        return collection;
    }

    @Override
    public Response add(AffinityGroup affinityGroup) {
        org.ovirt.engine.core.common.scheduling.AffinityGroup backendEntity =
                getMapper(AffinityGroup.class, org.ovirt.engine.core.common.scheduling.AffinityGroup.class).map(affinityGroup,
                        null);
        backendEntity.setClusterId(asGuid(clusterId));

        return performCreate(VdcActionType.AddAffinityGroup,
                new AffinityGroupCRUDParameters(null, backendEntity),
                new QueryIdResolver<Guid>(VdcQueryType.GetAffinityGroupById, IdQueryParameters.class),
                true);
    }

    @Override
    protected Response performRemove(String id) {
        AffinityGroupCRUDParameters params = new AffinityGroupCRUDParameters();
        params.setAffinityGroupId(asGuid(id));
        return performAction(VdcActionType.RemoveAffinityGroup, params);
    }

    @Override
    protected AffinityGroup doPopulate(AffinityGroup model, org.ovirt.engine.core.common.scheduling.AffinityGroup entity) {
        return model;
    }

    @Override
    @SingleEntityResource
    public AffinityGroupResource getAffinityGroupResource(String id) {
        return inject(new BackendAffinityGroupResource(id));
    }
}
