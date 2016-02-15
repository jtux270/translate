package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmDAO;

/**
 * A test case for {@link GetVmsRunningOnOrMigratingToVdsQuery}. This test mocks away all the DAOs, and just tests the
 * flow of the query itself.
 */
public class GetVmsRunningOnOrMigratingToVdsQueryTest
        extends AbstractQueryTest<IdQueryParameters,
        GetVmsRunningOnOrMigratingToVdsQuery<IdQueryParameters>> {

    @Test
    public void testQueryExecution() {
        Guid vmGuid = Guid.newGuid();

        VM vm = new VM();
        vm.setId(vmGuid);

        List<VM> expected = Collections.singletonList(vm);
        VmDAO vmDAOMock = mock(VmDAO.class);
        when(vmDAOMock.getAllRunningOnOrMigratingToVds(vmGuid)).thenReturn(expected);
        when(getDbFacadeMockInstance().getVmDao()).thenReturn(vmDAOMock);
        // Set up the query parameters
        when(getQueryParameters().getId()).thenReturn(vmGuid);

        getQuery().executeQueryCommand();

        @SuppressWarnings("unchecked")
        List<VM> actual = (List<VM>) getQuery().getQueryReturnValue().getReturnValue();
        assertEquals("Wrong number of VMs", 1, actual.size());
    }
}
