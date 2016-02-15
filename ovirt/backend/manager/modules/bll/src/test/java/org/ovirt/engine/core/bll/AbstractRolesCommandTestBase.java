package org.ovirt.engine.core.bll;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.RolesParameterBase;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.RoleDAO;
import org.ovirt.engine.core.dao.RoleGroupMapDAO;

/** An abstract base class for tests for roles commands */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractRolesCommandTestBase {
    private RolesParameterBase params;
    private RolesCommandBase<? extends RolesParameterBase> command;
    private Role role;

    @Mock
    private RoleDAO roleDAOMock;

    @Mock
    private RoleGroupMapDAO roleGroupMapDAOMock;

    @Before
    public void setUp() {
        params = generateParameters();
        command = spy(generateCommand());
        role = new Role();
        role.setId(params.getRoleId());

        doReturn(roleDAOMock).when(command).getRoleDao();
        when(roleDAOMock.get(params.getRoleId())).thenReturn(role);
        doReturn(roleGroupMapDAOMock).when(command).getRoleGroupMapDAO();
    }

    protected RolesParameterBase generateParameters() {
        return new RolesParameterBase(Guid.newGuid());
    }

    abstract protected RolesCommandBase<? extends RolesParameterBase> generateCommand();

    protected RolesParameterBase getParams() {
        return params;
    }

    protected RolesCommandBase<? extends RolesParameterBase> getCommand() {
        return command;
    }

    protected Role getRole() {
        return role;
    }

    protected RoleDAO getRoleDAOMock() {
        return roleDAOMock;
    }

    protected RoleGroupMapDAO getRoleGroupMapDAOMock() {
        return roleGroupMapDAOMock;
    }
}
