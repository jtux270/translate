package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.queries.MultilevelAdministrationsQueriesParameters;
import org.ovirt.engine.core.dao.RoleDAO;

/** A test case for the {@link GetAllRolesQuery} class. */
public class GetAllRolesQueryTest extends AbstractUserQueryTest<MultilevelAdministrationsQueriesParameters, GetAllRolesQuery<MultilevelAdministrationsQueriesParameters>> {

    @Test
    public void testExecuteQueryCommand() {
        // Prepare the result
        Role role = new Role();
        role.setname("test role");
        List<Role> result = Collections.singletonList(role);

        // Mock the DAO
        RoleDAO roleDAOMock = mock(RoleDAO.class);
        when(roleDAOMock.getAll()).thenReturn(result);
        when(getDbFacadeMockInstance().getRoleDao()).thenReturn(roleDAOMock);

        // Execute the query
        getQuery().executeQueryCommand();

        // Check the result
        assertEquals("Wrong roles returned", result, getQuery().getQueryReturnValue().getReturnValue());
    }
}
