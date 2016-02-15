package org.ovirt.engine.api.restapi.resource.gluster;

import java.util.HashMap;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.common.util.QueryHelper;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.GlusterVolume;
import org.ovirt.engine.api.model.GlusterVolumeProfileDetails;
import org.ovirt.engine.api.model.Option;
import org.ovirt.engine.api.resource.StatisticsResource;
import org.ovirt.engine.api.resource.gluster.GlusterBricksResource;
import org.ovirt.engine.api.resource.gluster.GlusterVolumeResource;
import org.ovirt.engine.api.restapi.logging.Messages;
import org.ovirt.engine.api.restapi.resource.AbstractBackendActionableResource;
import org.ovirt.engine.api.restapi.resource.BackendStatisticsResource;
import org.ovirt.engine.api.restapi.resource.VolumeStatisticalQuery;
import org.ovirt.engine.api.utils.LinkHelper;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeActionParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeOptionParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRebalanceParameters;
import org.ovirt.engine.core.common.action.gluster.ResetGlusterVolumeOptionsParameters;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeProfileInfo;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.gluster.GlusterVolumeProfileParameters;
import org.ovirt.engine.core.compat.Guid;

/**
 * Implementation of the "glustervolumes/{id}" resource
 */
public class BackendGlusterVolumeResource
        extends AbstractBackendActionableResource<GlusterVolume, GlusterVolumeEntity>
        implements GlusterVolumeResource {
    private static final String NFS_CONSTRAINT_PARAMETER = "nfsStatistics";
    private BackendGlusterVolumesResource parent;

    public BackendGlusterVolumeResource(String volumeId, BackendGlusterVolumesResource parent) {
        this(volumeId);
        setParent(parent);
    }

    public BackendGlusterVolumeResource(String volumeId) {
        super(volumeId, GlusterVolume.class, GlusterVolumeEntity.class, BackendGlusterVolumesResource.SUB_COLLECTIONS);
    }

    @Override
    protected GlusterVolume addParents(GlusterVolume model) {
        model.setId(id);
        parent.addParents(model);
        return model;
    }

    @Override
    public GlusterVolume get() {
        return performGet(VdcQueryType.GetGlusterVolumeById, new IdQueryParameters(guid));
    }

    @Override
    public Response start(Action action) {
        return doAction(VdcActionType.StartGlusterVolume, new GlusterVolumeActionParameters(guid,
                action.isSetForce() ? action.isForce() : false), action);
    }

    @Override
    public Response stop(Action action) {
        return doAction(VdcActionType.StopGlusterVolume, new GlusterVolumeActionParameters(guid,
                action.isSetForce() ? action.isForce() : false), action);
    }

    @Override
    public Response rebalance(Action action) {
        boolean fixLayoutOnly = (action.isSetFixLayout() ? action.isFixLayout() : false);
        boolean force = (action.isSetForce() ? action.isForce() : false);
        return doAction(VdcActionType.StartRebalanceGlusterVolume,
                new GlusterVolumeRebalanceParameters(guid,
                        fixLayoutOnly,
                        force), action);
    }

    @Override
    public Response stopRebalance(Action action) {
        return doAction(VdcActionType.StopRebalanceGlusterVolume,
                new GlusterVolumeRebalanceParameters(guid), action);
    }

    @Override
    public Response setOption(Action action) {
        Option option = action.getOption();
        validateParameters(option, "name", "value");

        return doAction(VdcActionType.SetGlusterVolumeOption,
                new GlusterVolumeOptionParameters(new GlusterVolumeOptionEntity(guid,
                        option.getName(),
                        option.getValue())),
                action);
    }

    @Override
    public Response resetOption(Action action) {
        Option option = action.getOption();
        validateParameters(option, "name");
        return resetOption(action, option.getName(), option.getValue(), action.isSetForce() ? action.isForce() : false);
    }

    @Override
    public Response resetAllOptions(Action action) {
        return resetOption(action, null, null, action.isSetForce() ? action.isForce() : false);
    }

    private Response resetOption(Action action, String optionName, String optionValue, boolean force) {
        return doAction(VdcActionType.ResetGlusterVolumeOptions,
                new ResetGlusterVolumeOptionsParameters(guid,
                        new GlusterVolumeOptionEntity(guid, optionName, optionValue),
                        force), action);
    }

    @Override
    public Response startProfile(Action action) {
        return doAction(VdcActionType.StartGlusterVolumeProfile, new GlusterVolumeParameters(guid), action);
    }


    @Override
    public Response stopProfile(Action action) {
        return doAction(VdcActionType.StopGlusterVolumeProfile, new GlusterVolumeParameters(guid), action);
    }

    @Override
    public GlusterBricksResource getGlusterBrickSubResource() {
        return inject(new BackendGlusterBricksResource(this));
    }

    public BackendGlusterVolumesResource getParent() {
        return parent;
    }

    public void setParent(BackendGlusterVolumesResource parent) {
        this.parent = parent;
    }

    @Override
    protected GlusterVolume doPopulate(GlusterVolume model, GlusterVolumeEntity entity) {
        return model;
    }

    public String getId() {
        return this.id;
    }

    @Override
    @Path("statistics")
    public StatisticsResource getStatisticsResource() {

        EntityIdResolver<Guid> resolver =
                new QueryIdResolver<Guid>(VdcQueryType.GetGlusterVolumeById, IdQueryParameters.class);
        VolumeStatisticalQuery query = new VolumeStatisticalQuery(resolver, newModel(id));
        return inject(new BackendStatisticsResource<GlusterVolume, GlusterVolumeEntity>(entityType,
                guid,
                query));
    }

    @Override
    public GlusterVolumeProfileDetails getProfileStatistics() {
        boolean nfsStats = isNfsStatistics();
        VdcQueryReturnValue result = runQuery(VdcQueryType.GetGlusterVolumeProfileInfo,
                new GlusterVolumeProfileParameters(Guid.createGuidFromString(parent.getParent().get().getId()), guid, nfsStats));
        if (result != null
                && result.getSucceeded()
                && result.getReturnValue() != null) {
            return LinkHelper.addLinks(uriInfo, getMapper(GlusterVolumeProfileInfo.class,
                    GlusterVolumeProfileDetails.class)
            .map((GlusterVolumeProfileInfo)result.getReturnValue(), null));
        } else {
            //throw exception
            throw new WebFaultException(null, localize(Messages.BACKEND_FAILED), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Boolean isNfsStatistics() {
        if (getUriInfo() != null && QueryHelper.hasMatrixParam(getUriInfo(), NFS_CONSTRAINT_PARAMETER)) {
            HashMap<String, String> matrixConstraints = QueryHelper.getMatrixConstraints(getUriInfo(), NFS_CONSTRAINT_PARAMETER);
            String maxString = matrixConstraints.get(NFS_CONSTRAINT_PARAMETER);
                return Boolean.valueOf(maxString);
        } else {
            return false;
        }
    }
}
