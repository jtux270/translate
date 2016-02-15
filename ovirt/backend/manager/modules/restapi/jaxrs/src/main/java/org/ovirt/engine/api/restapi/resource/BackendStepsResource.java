package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.NotImplementedException;
import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.Job;
import org.ovirt.engine.api.model.Step;
import org.ovirt.engine.api.model.StepEnum;
import org.ovirt.engine.api.model.Steps;
import org.ovirt.engine.api.resource.StepResource;
import org.ovirt.engine.api.resource.StepsResource;
import org.ovirt.engine.api.restapi.types.StepMapper;
import org.ovirt.engine.core.common.action.AddExternalStepParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.GetStepsByJobIdQueryParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendStepsResource extends AbstractBackendCollectionResource<Step, org.ovirt.engine.core.common.job.Step> implements StepsResource {

    private Guid jobId;

    public BackendStepsResource(Guid jobId) {
        super(Step.class, org.ovirt.engine.core.common.job.Step.class);
        this.jobId = jobId;
    }

    @Override
    public Steps list() {
        GetStepsByJobIdQueryParameters params = new GetStepsByJobIdQueryParameters(jobId);
        List<org.ovirt.engine.core.common.job.Step> steps = getBackendCollection(VdcQueryType.GetStepsByJobId, params);
        return mapCollection(steps);
    }

    @Override
    public Response add(Step step) {
        validateParameters(step, "type", "description");
        validateEnums(Step.class, step);
        String id;
        if (step.isSetParentStep()) {
            validateParameters(step, "parentStep.id");
            id = step.getParentStep().getId();
        }
        else {
            id = jobId.toString();
        }

        return performCreate(VdcActionType.AddExternalStep,
                new AddExternalStepParameters(asGuid(id), step.getDescription(), StepMapper.map(StepEnum.fromValue(step.getType()))),
                new QueryIdResolver<Guid>(VdcQueryType.GetStepByStepId, IdQueryParameters.class));
    }

    @Override
    @SingleEntityResource
    public StepResource getStepSubResource(@PathParam("id") String id) {
        return inject(new BackendStepResource(id, this));
    }

    @Override
    protected Response performRemove(String id) {
        throw new NotImplementedException();
    }

    @Override
    protected Step doPopulate(Step model, org.ovirt.engine.core.common.job.Step entity) {
        return model;
    }

    protected Steps mapCollection(List<org.ovirt.engine.core.common.job.Step> entities) {
        Steps collection = new Steps();
        for (org.ovirt.engine.core.common.job.Step entity : entities) {
            collection.getSteps().add(injectParent(addLinks(map(entity))));
        }
        return collection;
    }

    public Step injectParent(Step step) {
        if (step.getParentStep() !=null) {
            step.getParentStep()
            .setHref(step.getHref()
                    .replace(step.getId(), step.getParentStep().getId()));
        }
        return step;
    }

    @Override
    protected Step addParents(Step model) {
        model.setJob(new Job());
        model.getJob().setId(jobId.toString());
        return model;
    }

    @Override
    protected Step addLinks(Step model,
            Class<? extends BaseResource> suggestedParent,
            String... subCollectionMembersToExclude) {
        Step linked = super.addLinks(model, suggestedParent, subCollectionMembersToExclude);
        return injectParent(linked);
    }
}
