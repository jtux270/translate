package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.AffinityGroup;
import org.ovirt.engine.api.resource.AffinityGroupResource;
import org.ovirt.engine.api.resource.AffinityGroupVmsResource;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.scheduling.parameters.AffinityGroupCRUDParameters;
import org.ovirt.engine.core.compat.Guid;

public class BackendAffinityGroupResource
        extends AbstractBackendSubResource<AffinityGroup, org.ovirt.engine.core.common.scheduling.AffinityGroup>
        implements AffinityGroupResource {
    static final String[] SUB_COLLECTIONS = { "vms" };

    public BackendAffinityGroupResource(String id) {
        super(id, AffinityGroup.class,
                org.ovirt.engine.core.common.scheduling.AffinityGroup.class, SUB_COLLECTIONS);
    }

    @Override
    public AffinityGroup get() {
        return performGet(VdcQueryType.GetAffinityGroupById, new IdQueryParameters(guid));
    }

    @Override
    public AffinityGroup update(final AffinityGroup incoming) {
        return performUpdate(incoming,
                new QueryIdResolver<Guid>(VdcQueryType.GetAffinityGroupById, IdQueryParameters.class),
                VdcActionType.EditAffinityGroup,
                new ParametersProvider<AffinityGroup, org.ovirt.engine.core.common.scheduling.AffinityGroup>() {
                    @Override
                    public VdcActionParametersBase getParameters(AffinityGroup model,
                            org.ovirt.engine.core.common.scheduling.AffinityGroup entity) {
                        return new AffinityGroupCRUDParameters(guid, map(incoming, entity));
                    }
                });
    }

    @Override
    protected AffinityGroup doPopulate(AffinityGroup model, org.ovirt.engine.core.common.scheduling.AffinityGroup entity) {
        return model;
    }

    @Override
    public AffinityGroupVmsResource getAffinityGroupVmsSubResource() {
        return inject(new BackendAffinityGroupVmsResource(guid));
    }

}
