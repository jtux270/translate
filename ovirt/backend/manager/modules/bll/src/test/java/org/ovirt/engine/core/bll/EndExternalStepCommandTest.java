package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.EndExternalStepParameters;
import org.ovirt.engine.core.common.job.Job;
import org.ovirt.engine.core.common.job.Step;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.JobDao;
import org.ovirt.engine.core.dao.StepDao;
import org.ovirt.engine.core.utils.log.Log;


@RunWith(MockitoJUnitRunner.class)
public class EndExternalStepCommandTest {

    private  EndExternalStepParameters parameters;
    private static final Guid jobId = Guid.newGuid();
    private static final Guid stepId = Guid.newGuid();
    private static final Guid nonExistingJobId = Guid.newGuid();
    private static final Guid nonExistingStepId = Guid.newGuid();
    private static final Guid nonExternalJobId = Guid.newGuid();
    private static final Guid nonExternalStepId = Guid.newGuid();
    private static final Guid nonExtistingJobstepId = Guid.newGuid();
    private static final Guid nonExternalJobstepId = Guid.newGuid();

    @Mock
    private JobDao jobDaoMock;
    @Mock
    private StepDao stepDaoMock;

    @Mock
    private EndExternalStepCommand<EndExternalStepParameters> commandMock;
    @Mock
    private Log log;

    @Before
    public void createParameters() {
        parameters = new EndExternalStepParameters(stepId, true);
    }

    private Job makeExternalTestJob(Guid jobId) {
        Job job = new Job();
        job.setId(jobId);
        job.setDescription("Sample Job");
        job.setExternal(true);
        return job;
    }

    private Step makeExternalTestStep(Guid id, Guid stepId) {
        Step step = new Step();
        step.setId(stepId);
        step.setJobId(id);
        step.setDescription("Sample Step");
        step.setExternal(true);
        return step;
    }

    private Job makeNonExternalTestJob(Guid jobId) {
        Job job = new Job();
        job.setId(jobId);
        job.setDescription("Sample Job");
        job.setExternal(false);
        return job;
    }

    private Step makeNonExternalTestStep(Guid id, Guid stepId) {
        Step step = new Step();
        step.setId(stepId);
        step.setJobId(id);
        step.setDescription("Sample Step");
        step.setExternal(false);
        return step;
    }

    private void setupMock() throws Exception {
        commandMock = spy(new EndExternalStepCommand<EndExternalStepParameters>(parameters));
        when(commandMock.getParameters()).thenReturn(parameters);
        doReturn(jobDaoMock).when(commandMock).getJobDao();
        doReturn(stepDaoMock).when(commandMock).getStepDao();
        when(jobDaoMock.get(jobId)).thenReturn(makeExternalTestJob(jobId));
        when(jobDaoMock.get(nonExternalJobId)).thenReturn(makeNonExternalTestJob(nonExternalJobId));
        when(jobDaoMock.get(nonExistingJobId)).thenReturn(null);
        when(stepDaoMock.get(stepId)).thenReturn(makeExternalTestStep(jobId, stepId));
        when(stepDaoMock.get(nonExternalStepId)).thenReturn(makeNonExternalTestStep(nonExternalJobId, nonExternalStepId));
        when(stepDaoMock.get(nonExtistingJobstepId)).thenReturn(makeExternalTestStep(nonExistingJobId, nonExtistingJobstepId));
        when(stepDaoMock.get(nonExternalJobstepId)).thenReturn(makeExternalTestStep(nonExternalJobId, nonExternalJobstepId));
        when(stepDaoMock.get(nonExistingStepId)).thenReturn(null);
    }

    @Test
    public void canDoActionOkSucceeds() throws Exception {
        setupMock();
        assertTrue(commandMock.canDoAction());
    }

    @Test
    public void canDoActionNonExistingJobFails() throws Exception {
        setupMock();
        parameters.setId(nonExistingStepId);
        assertTrue(! commandMock.canDoAction());
    }

    @Test
    public void canDoActionNonExternalJobFails() throws Exception {
        setupMock();
        parameters.setId(nonExternalStepId);
        assertTrue(! commandMock.canDoAction());
    }

    @Test
    public void canDoActionNonExistingStepFails() throws Exception {
        setupMock();
        parameters.setJobId(jobId);
        parameters.setId(nonExistingStepId);
        assertTrue(! commandMock.canDoAction());
    }

    @Test
    public void canDoActionNonExternalStepFails() throws Exception {
        setupMock();
        parameters.setJobId(jobId);
        parameters.setId(nonExternalStepId);
        assertTrue(! commandMock.canDoAction());
    }
}
