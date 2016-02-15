package org.ovirt.engine.api.restapi.resource.gluster;

import static org.ovirt.engine.api.restapi.resource.gluster.BackendGlusterBricksResource.SUB_COLLECTIONS;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.GlusterBrick;
import org.ovirt.engine.api.resource.StatisticsResource;
import org.ovirt.engine.api.resource.gluster.GlusterBrickResource;
import org.ovirt.engine.api.restapi.logging.Messages;
import org.ovirt.engine.api.restapi.resource.AbstractBackendActionableResource;
import org.ovirt.engine.api.restapi.resource.BackendStatisticsResource;
import org.ovirt.engine.api.restapi.resource.BrickStatisticalQuery;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeAdvancedDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeAdvancedDetailsParameters;
import org.ovirt.engine.core.compat.Guid;

public class BackendGlusterBrickResource
        extends AbstractBackendActionableResource<GlusterBrick, GlusterBrickEntity>
        implements GlusterBrickResource {

    private BackendGlusterBricksResource parent;

    public BackendGlusterBrickResource(String brickId, BackendGlusterBricksResource parent) {
        this(brickId);
        setParent(parent);
    }

    public BackendGlusterBrickResource(String brickId) {
        super(brickId, GlusterBrick.class, GlusterBrickEntity.class, SUB_COLLECTIONS);
    }

    @Override
    public GlusterBrick get() {
        return performGet(VdcQueryType.GetGlusterBrickById, new IdQueryParameters(guid));
    }

    @Override
    protected GlusterBrick addParents(GlusterBrick model) {
        parent.addParents(model);
        return model;
    }

    protected String getClusterId() {
        return getParent().getParent().getParent().getParent().get().getId();
    }

    protected String getVolumeId() {
        return getParent().getParent().getId();
    }

    @Override
    public Response replace(Action action) {
        throw new WebFaultException(null,
                localize(Messages.GLUSTER_VOLUME_REPLACE_BRICK_NOT_SUPPORTED),
                Response.Status.SERVICE_UNAVAILABLE);

    }

    public BackendGlusterBricksResource getParent() {
        return parent;
    }

    public void setParent(BackendGlusterBricksResource parent) {
        this.parent = parent;
    }

    @Override
    protected GlusterBrick doPopulate(GlusterBrick model, GlusterBrickEntity entity) {
        return parent.populateAdvancedDetails(model, entity);
    }

    @Override
    @Path("statistics")
    public StatisticsResource getStatisticsResource() {
        EntityIdResolver<Guid> resolver = new QueryIdResolver<Guid>(VdcQueryType.GetGlusterBrickById, IdQueryParameters.class) {

            @Override
            public GlusterBrickEntity lookupEntity(Guid id) throws BackendFailureException {
                GlusterBrickEntity brickEntity = getEntity(GlusterBrickEntity.class,
                        VdcQueryType.GetGlusterBrickById,
                        new IdQueryParameters(id),
                        null,
                        true);
                GlusterVolumeEntity volumeEntity = getEntity(GlusterVolumeEntity.class,
                        VdcQueryType.GetGlusterVolumeById,
                        new IdQueryParameters(brickEntity.getVolumeId()),
                        null,
                        true);
                GlusterVolumeAdvancedDetails detailsEntity = getEntity(GlusterVolumeAdvancedDetails.class,
                        VdcQueryType.GetGlusterVolumeAdvancedDetails,
                        new GlusterVolumeAdvancedDetailsParameters(volumeEntity.getClusterId(),
                                                                   volumeEntity.getId(),
                                                                   brickEntity.getId(), true),
                        null,
                        true);
                brickEntity.setBrickDetails(detailsEntity.getBrickDetails().get(0));
                return brickEntity;
            }

        };
        BrickStatisticalQuery query = new BrickStatisticalQuery(resolver , newModel(id));
        return inject(new BackendStatisticsResource<GlusterBrick, GlusterBrickEntity>(GlusterBrickEntity.class, guid, query));
    }


}
