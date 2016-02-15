package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Ignore;
import org.junit.Test;
import org.ovirt.engine.api.model.Job;
import org.ovirt.engine.core.common.action.AddExternalJobParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendJobsResourceTest extends AbstractBackendCollectionResourceTest<Job, org.ovirt.engine.core.common.job.Job, BackendJobsResource> {

    public BackendJobsResourceTest() {
        super(new BackendJobsResource(), null, "");
    }


    @Override
    protected List<Job> getCollection() {
        return collection.list().getJobs();
    }

    @Test
    @Ignore
    @Override
    public void testQuery() throws Exception {
    }


    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        assertEquals("", query);

        setUpEntityQueryExpectations(VdcQueryType.GetAllJobs,
                                    VdcQueryParametersBase.class,
                                     new String[] { },
                                     new Object[] { },
                                     setUpJobs(),
                                     failure);

        control.replay();
    }

    @Override
    protected org.ovirt.engine.core.common.job.Job getEntity(int index) {
        org.ovirt.engine.core.common.job.Job job = new org.ovirt.engine.core.common.job.Job();
        job.setId(GUIDS[index]);
        job.setDescription(DESCRIPTIONS[index]);
        job.setActionType(VdcActionType.AddExternalJob);
        job.setStartTime(new Date());
        job.setExternal(true);
        job.setAutoCleared(true);
        return job;
    }

    @Override
    protected void verifyModel(Job model, int index) {
        assertEquals(GUIDS[index].toString(), model.getId());
        assertEquals(DESCRIPTIONS[index], model.getDescription());
        assertTrue(model.isExternal());
        verifyLinks(model);
    }

    @Test
    public void testAddJob() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpCreationExpectations(VdcActionType.AddExternalJob,
                                  AddExternalJobParameters.class,
                                  new String[] { "Description"},
                                  new Object[] { DESCRIPTIONS[0] },
                                  true,
                                  true,
                                  GUIDS[0],
                                  VdcQueryType.GetJobByJobId,
                                  IdQueryParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[0] },
                                  getEntity(0));
        Job model = new Job();
        model.setDescription(DESCRIPTIONS[0].toString());
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Job);
        verifyModel((Job) response.getEntity(), 0);
    }

    protected List<org.ovirt.engine.core.common.job.Job> setUpJobs() {
        List<org.ovirt.engine.core.common.job.Job> jobs = new ArrayList<org.ovirt.engine.core.common.job.Job>();
        for (int i = 0; i < NAMES.length; i++) {
            jobs.add(getEntity(i));
        }
        return jobs;
    }
}
