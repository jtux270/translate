package org.ovirt.engine.api.restapi.resource.gluster;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Cluster;
import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.model.GlusterBrick;
import org.ovirt.engine.api.model.GlusterBricks;
import org.ovirt.engine.api.model.GlusterVolume;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.gluster.GlusterBrickResource;
import org.ovirt.engine.api.resource.gluster.GlusterBricksResource;
import org.ovirt.engine.api.restapi.logging.Messages;
import org.ovirt.engine.api.restapi.resource.AbstractBackendCollectionResource;
import org.ovirt.engine.api.restapi.resource.BackendActionResource;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeBricksActionParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRemoveBricksParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeAdvancedDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeAdvancedDetailsParameters;
import org.ovirt.engine.core.compat.Guid;

public class BackendGlusterBricksResource
        extends AbstractBackendCollectionResource<GlusterBrick, GlusterBrickEntity>
        implements GlusterBricksResource {
    static final String[] SUB_COLLECTIONS = { "statistics" };

    private BackendGlusterVolumeResource parent;

    public BackendGlusterBricksResource() {
        super(GlusterBrick.class, GlusterBrickEntity.class);
    }

    public BackendGlusterBricksResource(BackendGlusterVolumeResource parent) {
        super(GlusterBrick.class, GlusterBrickEntity.class, SUB_COLLECTIONS);
        setParent(parent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public GlusterBricks list() {
        List<GlusterBrickEntity> bricks =
                getBackendCollection(VdcQueryType.GetGlusterVolumeBricks, new IdQueryParameters(asGuid(getVolumeId())));
        GlusterBricks bricksModel = mapCollection(bricks);
        return addActions(bricksModel);
    }

    private GlusterBricks mapCollection(List<GlusterBrickEntity> entities) {
        GlusterBricks collection = new GlusterBricks();
        for (GlusterBrickEntity entity : entities) {
            collection.getGlusterBricks().add(addLinks(populate(map(entity), entity), Cluster.class));
        }
        return collection;
    }

    @Override
    protected GlusterBrick addParents(GlusterBrick glusterBrick) {
        GlusterVolume volume = new GlusterVolume();
        parent.addParents(volume);
        glusterBrick.setGlusterVolume(volume);
        return glusterBrick;
    }

    private List<GlusterBrickEntity> mapBricks(Guid volumeId, GlusterBricks glusterBricks) {
        List<GlusterBrickEntity> bricks = new ArrayList<GlusterBrickEntity>();
        if (glusterBricks.getGlusterBricks().size() > 0) {
            for (GlusterBrick brick : glusterBricks.getGlusterBricks()) {
                GlusterBrickEntity brickEntity =
                        getMapper(GlusterBrick.class, GlusterBrickEntity.class).map(brick, null);
                brickEntity.setVolumeId(volumeId);
                bricks.add(brickEntity);
            }
        }
        return bricks;
    }

    @Override
    public Response add(GlusterBricks bricks) {
        for (GlusterBrick brick : bricks.getGlusterBricks()) {
            validateParameters(brick, "serverId", "brickDir");
        }

        List<GlusterBrickEntity> brickEntities = mapBricks(asGuid(getVolumeId()), bricks);
        int replicaCount = bricks.isSetReplicaCount() ? bricks.getReplicaCount() : 0;
        int stripeCount = bricks.isSetStripeCount() ? bricks.getStripeCount() : 0;

        return performCreationMultiple(VdcActionType.AddBricksToGlusterVolume,
                new GlusterVolumeBricksActionParameters(asGuid(getVolumeId()),
                        brickEntities,
                        replicaCount,
                        stripeCount,
                        isForce()),
                new QueryIdResolver<Guid>(VdcQueryType.GetGlusterBrickById, IdQueryParameters.class));
    }

    private String getVolumeId() {
        return parent.get().getId();
    }

    @SuppressWarnings("unchecked")
    protected GlusterBricks resolveCreatedList(VdcReturnValueBase result, EntityIdResolver<Guid> entityResolver) {
        try {
            GlusterBricks bricks = new GlusterBricks();
            for (Guid id : (List<Guid>) result.getActionReturnValue()) {
                GlusterBrickEntity created = entityResolver.resolve(id);
                bricks.getGlusterBricks().add(addLinks(populate(map(created), created)));
            }
            return bricks;
        } catch (Exception e) {
            // we tolerate a failure in the entity resolution
            // as the substantive action (entity creation) has
            // already succeeded
            e.printStackTrace();
            return null;
        }
    }

    protected Response performCreationMultiple(VdcActionType task,
            VdcActionParametersBase taskParams,
            EntityIdResolver<Guid> entityResolver) {
        VdcReturnValueBase createResult;
        try {
            createResult = doAction(task, taskParams);
        } catch (Exception e) {
            return handleError(e, false);
        }

        GlusterBricks model = resolveCreatedList(createResult, entityResolver);
        Response response = null;
        if (model == null) {
            response = Response.status(ACCEPTED_STATUS).build();
        } else {
            response =
                    Response.created(URI.create(getUriInfo().getPath())).entity(model).build();
        }
        return response;
    }

    @Override
    public Response remove(GlusterBricks bricks) {
        if (bricks.getGlusterBricks().size() > 0) {
            for (GlusterBrick brick : bricks.getGlusterBricks()) {
                validateParameters(brick, "id|name");
            }
        }

        int replicaCount = bricks.isSetReplicaCount() ? bricks.getReplicaCount() : 0;

        GlusterVolumeRemoveBricksParameters params = toParameters(bricks);
        params.setReplicaCount(replicaCount);

        GlusterVolumeEntity volume =
                getEntity(GlusterVolumeEntity.class,
                        VdcQueryType.GetGlusterVolumeById,
                        new IdQueryParameters(asGuid(getVolumeId())),
                        "");
        if (volume.getAsyncTask() != null && volume.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK
                && volume.getAsyncTask().getStatus() == JobExecutionStatus.FINISHED) {
            return performAction(VdcActionType.CommitRemoveGlusterVolumeBricks, params);
        } else {
            return performAction(VdcActionType.GlusterVolumeRemoveBricks, params);
        }
    }

    @Override
    public GlusterBrickResource getGlusterBrickSubResource(String brickId) {
        return inject(new BackendGlusterBrickResource(brickId, this));
    }

    public BackendGlusterVolumeResource getParent() {
        return parent;
    }

    public void setParent(BackendGlusterVolumeResource parent) {
        this.parent = parent;
    }

    @Override
    protected Response performRemove(String id) {
        GlusterBrick brick = new GlusterBrick();
        brick.setId(id);
        GlusterBricks bricks = new GlusterBricks();
        bricks.getGlusterBricks().add(brick);
        return remove(bricks);
    }

    @Override
    protected GlusterBrick doPopulate(GlusterBrick model, GlusterBrickEntity entity) {
        return populateAdvancedDetails(model, entity);
    }

    protected GlusterBrick populateAdvancedDetails(GlusterBrick model, GlusterBrickEntity entity) {

        GlusterVolumeEntity volumeEntity = getEntity(GlusterVolumeEntity.class,
                                                     VdcQueryType.GetGlusterVolumeById,
                                                     new IdQueryParameters(entity.getVolumeId()),
                                                     null,
                                                     true);
        GlusterVolumeAdvancedDetails detailsEntity = getEntity(GlusterVolumeAdvancedDetails.class,
                                                VdcQueryType.GetGlusterVolumeAdvancedDetails,
                                                new GlusterVolumeAdvancedDetailsParameters(volumeEntity.getClusterId(),
                                                                                           volumeEntity.getId(),
                                                                                           entity.getId(), true),
                                                null,
                                                true);

        model = getMapper(GlusterVolumeAdvancedDetails.class, GlusterBrick.class)
                                                        .map(detailsEntity, model);

        return model;

    }

    private GlusterVolumeRemoveBricksParameters toParameters(GlusterBricks bricks) {
        GlusterVolumeRemoveBricksParameters params = new GlusterVolumeRemoveBricksParameters();

        List<GlusterBrickEntity> entityBricks = new ArrayList<GlusterBrickEntity>();
        for (GlusterBrick brick : bricks.getGlusterBricks()) {
            GlusterBrickEntity entity = new GlusterBrickEntity();
            entity.setBrickDirectory(brick.getBrickDir());
            entity.setVolumeId(asGuid(getVolumeId()));
            if (brick.getName() != null) {
                String[] arr = brick.getName().split("\\:");
                if (arr.length > 1) {
                    entity.setServerName(arr[0]);
                    entity.setBrickDirectory(arr[1]);
                } else {
                    continue;
                }
            }
            if (brick.getId() != null) {
                entity.setId(asGuid(brick.getId()));
            }
            entityBricks.add(entity);
        }
        params.setVolumeId(asGuid(getVolumeId()));
        params.setBricks(entityBricks);
        params.setCommandType(VdcActionType.StartRemoveGlusterVolumeBricks);

        return params;
    }

    private void validateBrickNames(Action action) {
        List<GlusterBrick> bricks = action.getBricks().getGlusterBricks();
        for (GlusterBrick brick : bricks) {
            if (brick.getName() == null || brick.getName().equals("")) {
                Fault fault = new Fault();
                fault.setReason(localize(Messages.INCOMPLETE_PARAMS_REASON));
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(fault)
                        .build());
            }
        }
    }

    @Override
    public Response migrate(Action action) {
        validateParameters(action, "bricks");
        validateBrickNames(action);
        GlusterVolumeRemoveBricksParameters params = toParameters(action.getBricks());
        return performAction(VdcActionType.StartRemoveGlusterVolumeBricks, params, action, false);
    }

    @Override
    public Response stopMigrate(Action action) {
        validateParameters(action, "bricks");
        validateBrickNames(action);
        GlusterVolumeRemoveBricksParameters params = toParameters(action.getBricks());
        return performAction(VdcActionType.StopRemoveGlusterVolumeBricks, params, action, false);
    }

    @Override
    public Response activate(Action action) {
        validateParameters(action, "bricks");
        validateBrickNames(action);

        GlusterVolumeEntity volume =
                getEntity(GlusterVolumeEntity.class,
                        VdcQueryType.GetGlusterVolumeById,
                        new IdQueryParameters(asGuid(getVolumeId())),
                        "");

        if (volume.getAsyncTask() != null && volume.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK
                && volume.getAsyncTask().getStatus() == JobExecutionStatus.FINISHED) {
            return stopMigrate(action);
        } else {
            Fault fault = new Fault();
            fault.setReason(localize(Messages.CANNOT_ACTIVATE_UNLESS_MIGRATION_COMPLETED));
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                    .entity(fault)
                    .build());
        }
    }

    @Override
    public ActionResource getActionSubresource(String action, String id) {
        return inject(new BackendActionResource(action, id));
    }
}
