package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.DetachUserFromVmFromPoolParameters;
import org.ovirt.engine.core.common.action.PermissionsOperationsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Permission;
import org.ovirt.engine.core.common.businessentities.RoleType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;

public class RemovePermissionCommand<T extends PermissionsOperationsParameters> extends PermissionsCommandBase<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected RemovePermissionCommand(Guid commandId) {
        super(commandId);
    }

    public RemovePermissionCommand(T parameters) {
        this(parameters, null);
    }

    public RemovePermissionCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__REMOVE);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__PERMISSION);
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = true;
        Permission p = getPermissionDao().get(getParameters().getPermission().getId());
        if (MultiLevelAdministrationHandler.isLastSuperUserPermission(p.getRoleId())) {
            getReturnValue().getCanDoActionMessages()
                    .add(EngineMessage.ERROR_CANNOT_REMOVE_LAST_SUPER_USER_ROLE.toString());
            returnValue = false;
        } else if (p.getRoleType().equals(RoleType.ADMIN) && !isSystemSuperUser()) {
            addCanDoActionMessage(EngineMessage.PERMISSION_REMOVE_FAILED_ONLY_SYSTEM_SUPER_USER_CAN_REMOVE_ADMIN_ROLES);
            returnValue = false;
        }
        return returnValue;
    }

    @Override
    protected void executeCommand() {
        Permission perms = getParameters().getPermission();
        Guid userId = perms.getAdElementId();

        // if removing engine user permission from vm,
        // check if vm is from pool and detach it
        if (perms.getObjectType().equals(VdcObjectType.VM)
                && perms.getRoleId().equals(PredefinedRoles.ENGINE_USER.getId())) {
            VM vm = getVmDao().get(perms.getObjectId());
            if (vm != null && vm.getVmPoolId() != null) {
                runInternalActionWithTasksContext(VdcActionType.DetachUserFromVmFromPool,
                        new DetachUserFromVmFromPoolParameters(vm.getVmPoolId(), userId, vm.getId(), true));
            }
        }

        getPermissionDao().remove(perms.getId());
        getDbFacade().updateLastAdminCheckStatus(userId);
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_REMOVE_PERMISSION : AuditLogType.USER_REMOVE_PERMISSION_FAILED;
    }
}
