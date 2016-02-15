package org.ovirt.engine.core.bll.aaa;

import org.ovirt.engine.core.bll.MultiLevelAdministrationHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.action.PermissionsOperationsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

public class RemoveGroupCommand<T extends IdParameters> extends AdGroupsHandlingCommandBase<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected RemoveGroupCommand(Guid commandId) {
        super(commandId);
    }

    public RemoveGroupCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        // Get the identifier of the group from the parameters:
        Guid id = getParameters().getId();

        // Remove the permissions of the group:
        // TODO: This should be done without invoking the command to avoid the overhead.
        for (Permissions permission : getPermissionDAO().getAllDirectPermissionsForAdElement(id)) {
            PermissionsOperationsParameters param = new PermissionsOperationsParameters(permission);
            param.setSessionId(getParameters().getSessionId());
            runInternalActionWithTasksContext(VdcActionType.RemovePermission, param);
        }

        // Remove the group itself:
        getAdGroupDAO().remove(id);

        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded()? AuditLogType.USER_REMOVE_AD_GROUP : AuditLogType.USER_REMOVE_AD_GROUP_FAILED;
    }

    @Override
    protected boolean canDoAction() {
        // Get the identifier of the group from the parameters:
        Guid id = getParameters().getId();

        // Check that the group being removed isn't the last remaining group
        // of super users:
        if (isLastSuperUserGroup(id)) {
            addCanDoActionMessage(VdcBllMessages.ERROR_CANNOT_REMOVE_LAST_SUPER_USER_ROLE);
            return false;
        }

        // Check that the group being removed isn't the everyone group:
        if (MultiLevelAdministrationHandler.EVERYONE_OBJECT_ID.equals(id)) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_CANNOT_REMOVE_BUILTIN_GROUP_EVERYONE);
            return false;
        }

        return true;
    }

    protected boolean isLastSuperUserGroup(Guid groupId) {
        return MultiLevelAdministrationHandler.isLastSuperUserGroup(groupId);
    }
}
