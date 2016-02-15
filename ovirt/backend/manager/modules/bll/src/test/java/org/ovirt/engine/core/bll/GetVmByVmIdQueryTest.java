package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmDAO;

/**
 * A test case for {@link GetVmByVmIdQuery}.
 * It does not test database implementation, but rather tests that the right delegations to the DAO occur.
 */
public class GetVmByVmIdQueryTest extends AbstractUserQueryTest<IdQueryParameters, GetVmByVmIdQuery<IdQueryParameters>> {
    @Test
    public void testExecuteQuery() {
        Guid vmID = Guid.newGuid();
        VM expectedResult = new VM();
        expectedResult.setId(vmID);

        IdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getId()).thenReturn(vmID);

        VmDAO vmDAOMock = mock(VmDAO.class);
        when(vmDAOMock.get(vmID, getUser().getId(), paramsMock.isFiltered())).thenReturn(expectedResult);
        when(getQuery().getDbFacade().getVmDao()).thenReturn(vmDAOMock);

        doNothing().when(getQuery()).updateVMDetails(expectedResult);
        getQuery().executeQueryCommand();

        VM result = (VM) getQuery().getQueryReturnValue().getReturnValue();

        assertEquals("Wrong VM returned", expectedResult, result);
    }
}
