package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Job;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.JobResource;
import org.ovirt.engine.api.resource.StepsResource;
import org.ovirt.engine.api.restapi.types.JobMapper;
import org.ovirt.engine.core.common.action.EndExternalJobParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendJobResource extends AbstractBackendActionableResource<Job, org.ovirt.engine.core.common.job.Job> implements JobResource{

    public BackendJobResource(String id) {
        super(id, Job.class, org.ovirt.engine.core.common.job.Job.class);
    }

    @Override
    public ActionResource getActionSubresource(@PathParam("action") String action, @PathParam("oid") String oid) {
        return inject(new BackendActionResource(action, oid));
    }

    @Override
    public Response end(Action action) {
        validateParameters(action, "status");
        return doAction(VdcActionType.EndExternalJob,
                new EndExternalJobParameters(guid, JobMapper.map(action.getStatus(), null), action.isSetForce() ? action.isForce() : false), action);
    }

    @Override
    public Response clear(Action action) {
        VdcActionParametersBase params = new VdcActionParametersBase();
        params.setJobId(guid);
        return doAction(VdcActionType.ClearExternalJob, params, action);
    }

    @Override
    public Job get() {
        IdQueryParameters params =  new IdQueryParameters(guid);
        return performGet(VdcQueryType.GetJobByJobId, params);
     }

    @Override
    public StepsResource getStepsResource() {
       return inject(new BackendStepsResource(guid));
    }

    @Override
    protected Job doPopulate(Job model, org.ovirt.engine.core.common.job.Job entity) {
        return model;
    }

}
