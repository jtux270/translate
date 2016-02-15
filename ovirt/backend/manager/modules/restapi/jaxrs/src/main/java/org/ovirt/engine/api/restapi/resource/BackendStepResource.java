package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Step;
import org.ovirt.engine.api.resource.StatisticsResource;
import org.ovirt.engine.api.resource.StepResource;
import org.ovirt.engine.core.common.action.EndExternalStepParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRemoveBricksQueriesParameters;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeTaskStatusEntity;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeQueriesParameters;
import org.ovirt.engine.core.compat.Guid;

public class BackendStepResource extends AbstractBackendActionableResource<org.ovirt.engine.api.model.Step, org.ovirt.engine.core.common.job.Step> implements StepResource{

    private BackendStepsResource parent;

    public BackendStepsResource getParent() {
        return parent;
    }

    public void setParent(BackendStepsResource parent) {
        this.parent = parent;
    }

    public BackendStepResource(String id, BackendStepsResource parent) {
        super(id, org.ovirt.engine.api.model.Step.class, org.ovirt.engine.core.common.job.Step.class);
        this.setParent(parent);
    }

    @Override
    protected Step doPopulate(Step model, org.ovirt.engine.core.common.job.Step entity) {
        return model;
    }

    @Override
    protected Step addParents(Step model) {
        parent.addParents(model);
        return model;
    }

    @Override
    public Response end(Action action) {
        validateParameters(action, "succeeded");
        return doAction(VdcActionType.EndExternalStep,
                new EndExternalStepParameters(guid, action.isSucceeded()), action);
    }

    @Override
    public Step get() {
        return parent.injectParent(getStepById(guid));
    }

    private Step getStepById(Guid id) {
        IdQueryParameters params =  new IdQueryParameters(id);
        Step step = performGet(VdcQueryType.GetStepByStepId, params);
        return step;
    }

    @Override
    public StatisticsResource getStatisticsResource() {
        StepStatisticalQuery query = new StepStatisticalQuery(new TaskQueryResolver<Guid>(), newModel(id));
        return inject(new BackendStatisticsResource<Step, GlusterVolumeTaskStatusEntity>(GlusterVolumeTaskStatusEntity.class, guid, query));
    }

    public class TaskQueryResolver<T> implements IResolver<Guid, GlusterVolumeTaskStatusEntity> {

        public TaskQueryResolver() {
        }

        @Override
        public GlusterVolumeTaskStatusEntity resolve(Guid id) throws BackendFailureException {
            org.ovirt.engine.core.common.job.Step stepEntity =  getEntity(org.ovirt.engine.core.common.job.Step.class,
                    VdcQueryType.GetStepByStepId,
                    new IdQueryParameters(id),
                    null,
                    true);
            if (stepEntity.getExternalSystem() == null) {
                return null;
            }
            Guid glusterTaskId = stepEntity.getExternalSystem().getId();
            if (glusterTaskId == null) {
                return null;
            }
            GlusterVolumeEntity volume = getEntity(GlusterVolumeEntity.class,
                    VdcQueryType.GetGlusterVolumeByTaskId,
                    new IdQueryParameters(glusterTaskId),
                    null,
                    true);
            if (volume == null) {
                return null;
            }

            switch (stepEntity.getStepType()) {
            case REBALANCING_VOLUME:
                GlusterVolumeTaskStatusEntity rebalanceStatusEntity = getEntity(GlusterVolumeTaskStatusEntity.class,
                        VdcQueryType.GetGlusterVolumeRebalanceStatus,
                        new GlusterVolumeQueriesParameters(volume.getClusterId(), volume.getId()),
                        null,
                        true);
                return rebalanceStatusEntity;
            case REMOVING_BRICKS:
                List<GlusterBrickEntity> bricks = new ArrayList<>();
                for (GlusterBrickEntity brick: volume.getBricks()) {
                    if (brick.getAsyncTask()!=null && brick.getAsyncTask().getTaskId()!=null) {
                        bricks.add(brick);
                    }
                }
                GlusterVolumeTaskStatusEntity removeBricksStatusEntity = getEntity(GlusterVolumeTaskStatusEntity.class,
                        VdcQueryType.GetGlusterVolumeRemoveBricksStatus,
                        new GlusterVolumeRemoveBricksQueriesParameters(volume.getClusterId(), volume.getId(), bricks),
                        null,
                        true);
                return removeBricksStatusEntity;
            default:
                break;

            }
            return null;
        }
    }
}
