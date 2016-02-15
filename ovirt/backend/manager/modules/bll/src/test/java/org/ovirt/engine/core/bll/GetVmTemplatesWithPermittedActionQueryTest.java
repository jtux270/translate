package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.queries.GetEntitiesWithPermittedActionParameters;
import org.ovirt.engine.core.dao.VmTemplateDAO;

/**
 * A test case for {@link GetDataCentersWithPermittedActionOnClusters}.
 * This test mocks away all the DAOs, and just tests the flow of the query itself.
 */
public class GetVmTemplatesWithPermittedActionQueryTest
        extends AbstractGetEntitiesWithPermittedActionParametersQueryTest
        <GetEntitiesWithPermittedActionParameters, GetVmTemplatesWithPermittedActionQuery<GetEntitiesWithPermittedActionParameters>> {

    @Test
    public void testQueryExecution() {
        // Set up the expected data
        VmTemplate expected = new VmTemplate();

        // Mock the DAO
        VmTemplateDAO vmTemplateDAOMock = mock(VmTemplateDAO.class);
        when(vmTemplateDAOMock.getTemplatesWithPermittedAction(getUser().getId(), getActionGroup())).thenReturn(Collections.singletonList(expected));
        when(getDbFacadeMockInstance().getVmTemplateDao()).thenReturn(vmTemplateDAOMock);

        getQuery().executeQueryCommand();

        @SuppressWarnings("unchecked")
        List<StoragePool> actual = (List<StoragePool>) getQuery().getQueryReturnValue().getReturnValue();
        assertEquals("Wrong number of VDS Groups", 1, actual.size());
        assertEquals("Wrong VDS Groups", expected, actual.get(0));
    }
}
