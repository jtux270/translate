package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.action.ActionGroupsToRoleParameter;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.RoleGroupMap;
import org.ovirt.engine.core.common.businessentities.RoleType;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

public class AttachActionGroupsToRoleCommandTest extends AbstractRolesCommandTestBase {

    @Override
    protected ActionGroupsToRoleParameter generateParameters() {
        Guid roleId = Guid.newGuid();
        ArrayList<ActionGroup> groups =
                new ArrayList<ActionGroup>(Arrays.asList(ActionGroup.DELETE_HOST, ActionGroup.CONFIGURE_ENGINE));
        return new ActionGroupsToRoleParameter(roleId, groups);
    }

    @Override
    protected AttachActionGroupsToRoleCommand<? extends ActionGroupsToRoleParameter> generateCommand() {
        return new AttachActionGroupsToRoleCommand<ActionGroupsToRoleParameter>(getParams());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected AttachActionGroupsToRoleCommand<? extends ActionGroupsToRoleParameter> getCommand() {
        return (AttachActionGroupsToRoleCommand<? extends ActionGroupsToRoleParameter>) super.getCommand();
    }

    @Override
    protected ActionGroupsToRoleParameter getParams() {
        return (ActionGroupsToRoleParameter) super.getParams();
    }

    /* canDoAction related tests */

    @Test
    public void testCheckGroupsCanBeAttachedAlreadyExists() {
        RoleGroupMap map = new RoleGroupMap(getParams().getActionGroups().get(0), getParams().getRoleId());
        mockGetAllForRole(Collections.singletonList(map));

        List<String> messages = new ArrayList<String>(1);
        assertTrue("canDoAction should fail", getCommand().checkIfGroupsCanBeAttached(messages));
        assertEquals("wrong messages",
                VdcBllMessages.ERROR_CANNOT_ATTACH_ACTION_GROUP_TO_ROLE_ATTACHED.toString(),
                messages.get(0));
    }

    @Test
    public void testCheckGroupsCanBeAttachedAdminIssues() {
        getRole().setType(RoleType.USER);
        RoleGroupMap map = new RoleGroupMap(ActionGroup.DELETE_STORAGE_POOL, getParams().getRoleId());
        mockGetAllForRole(Collections.singletonList(map));

        List<String> messages = new ArrayList<String>(1);
        assertTrue("canDoAction should fail", getCommand().checkIfGroupsCanBeAttached(messages));
        assertEquals("wrong messages",
                VdcBllMessages.CANNOT_ADD_ACTION_GROUPS_TO_ROLE_TYPE.toString(),
                messages.get(0));
    }

    @Test
    public void testCheckGroupsCanBeAttachedSuccess() {
        getRole().setType(RoleType.ADMIN);
        RoleGroupMap map = new RoleGroupMap(ActionGroup.DELETE_STORAGE_POOL, getParams().getRoleId());
        mockGetAllForRole(Collections.singletonList(map));

        List<String> messages = new ArrayList<String>();
        assertFalse("canDoAction should succeed", getCommand().checkIfGroupsCanBeAttached(messages));
        assertTrue("no messages sould have been added", messages.isEmpty());
    }

    private void mockGetAllForRole(List<RoleGroupMap> groups) {
        when(getRoleGroupMapDAOMock().getAllForRole(getParams().getRoleId())).thenReturn(groups);
    }

    /* execute related tests */

    /** A flow test that makes sure all the action groups are set correctly if the role is not updated */
    @Test
    public void testExecuteCommandNoUpdate() {
        getRole().setAllowsViewingChildren(true);
        getCommand().executeCommand();
        verifyRoleSaving(false);
    }

    /** A flow test that makes sure all the action groups are set correctly if the role is updated*/
    @Test
    public void testExecuteCommandWithUpdate() {
        getRole().setAllowsViewingChildren(false);
        getCommand().executeCommand();
        verifyRoleSaving(true);
    }

    /** A flow test that makes sure all the action groups are set correctly if the role isn't updated because the added permission doesn't allow viewing children */
    @Test
    public void testExecuteCommandNoUpdateNonInheritableRole() {
        getRole().setAllowsViewingChildren(false);
        getParams().setActionGroups(new ArrayList<ActionGroup>(Collections.singletonList(ActionGroup.CREATE_VM)));
        getCommand().executeCommand();
        verifyRoleSaving(false);
    }

    private void verifyRoleSaving(boolean roleStatusChanged) {
        for (ActionGroup group : getParams().getActionGroups()) {
            verify(getRoleGroupMapDAOMock()).save(new RoleGroupMap(group, getParams().getRoleId()));
        }

        if (roleStatusChanged) {
            verify(getRoleDAOMock()).update(getRole());
        }
        verifyNoMoreInteractions(getRoleGroupMapDAOMock());
    }
}
