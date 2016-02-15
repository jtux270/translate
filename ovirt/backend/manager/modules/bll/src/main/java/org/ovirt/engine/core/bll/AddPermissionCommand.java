package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddGroupParameters;
import org.ovirt.engine.core.common.action.AddUserParameters;
import org.ovirt.engine.core.common.action.PermissionsOperationsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.RoleType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.aaa.DbGroup;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public class AddPermissionCommand<T extends PermissionsOperationsParameters> extends PermissionsCommandBase<T> {

    public AddPermissionCommand(T parameters) {
        this(parameters, null);
    }

    public AddPermissionCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }


    public AddPermissionCommand(Guid commandId) {
        super(commandId);
    }

    @Override
    protected boolean canDoAction() {
        Permissions perm = getParameters().getPermission();
        if (perm == null) {
            addCanDoActionMessage(VdcBllMessages.PERMISSION_ADD_FAILED_PERMISSION_NOT_SENT);
            return false;
        }

        Role role = getRoleDao().get(perm.getrole_id());
        Guid adElementId = perm.getad_element_id();

        if (role == null) {
            addCanDoActionMessage(VdcBllMessages.PERMISSION_ADD_FAILED_INVALID_ROLE_ID);
            return false;
        }

        if (perm.getObjectType() == null
                || getVdcObjectName() == null) {
            addCanDoActionMessage(VdcBllMessages.PERMISSION_ADD_FAILED_INVALID_OBJECT_ID);
            return false;
        }

        // if user and group not sent check user/group is in the db in order to
        // give permission
        if (getParameters().getUser() == null
                && getParameters().getGroup() == null
                && getDbUserDAO().get(adElementId) == null
                && getAdGroupDAO().get(adElementId) == null) {
            getReturnValue().getCanDoActionMessages().add(VdcBllMessages.USER_MUST_EXIST_IN_DB.toString());
            return false;
        }

        // only system super user can give permissions with admin roles
        if (!isSystemSuperUser() && role.getType() == RoleType.ADMIN) {
            addCanDoActionMessage(VdcBllMessages.PERMISSION_ADD_FAILED_ONLY_SYSTEM_SUPER_USER_CAN_GIVE_ADMIN_ROLES);
            return false;
        }

        // don't allow adding permissions to vms from pool externally
        if (!isInternalExecution() && perm.getObjectType() == VdcObjectType.VM) {
            VM vm = getVmDAO().get(perm.getObjectId());
            if (vm != null && vm.getVmPoolId() != null) {
                addCanDoActionMessage(VdcBllMessages.PERMISSION_ADD_FAILED_VM_IN_POOL);
                return false;
            }
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        // Get the parameters:
        T parameters = getParameters();

        // The user or group given in the parameters may haven't been added to the database yet, if this is the case
        // then they need to be added to the database now, before the permission:
        DbUser user = parameters.getUser();
        if (user != null) {
            Guid id = user.getId();
            String directory = user.getDomain();
            String externalId = user.getExternalId();
            DbUser existing = getDbUserDAO().getByIdOrExternalId(id, directory, externalId);
            if (existing != null) {
                user = existing;
            }
            else {
                user = addUser(user);
                if (user == null) {
                    setSucceeded(false);
                    return;
                }
            }
        }
        DbGroup group = parameters.getGroup();
        if (group != null) {
            Guid id = group.getId();
            String directory = group.getDomain();
            String externalId = group.getExternalId();
            DbGroup existing = getAdGroupDAO().getByIdOrExternalId(id, directory, externalId);
            if (existing != null) {
                group = existing;
            }
            else {
                group = addGroup(group);
                if (group == null) {
                    setSucceeded(false);
                    return;
                }
            }
        }

        // The identifier of the principal of the permission can come from the parameters directly or from the
        // user/group objects:
        Guid principalId;
        if (user != null) {
            principalId = user.getId();
        }
        else if (group != null) {
            principalId = group.getId();
        }
        else {
            principalId = parameters.getPermission().getad_element_id();
        }

        final Permissions paramPermission = parameters.getPermission();

        Permissions permission =
                getPermissionDAO().getForRoleAndAdElementAndObject(paramPermission.getrole_id(), principalId,
                        paramPermission.getObjectId());

        if (permission == null) {
            paramPermission.setId(Guid.newGuid());
            paramPermission.setad_element_id(principalId);

            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    getPermissionDAO().save(paramPermission);
                    getCompensationContext().snapshotNewEntity(paramPermission);
                    getCompensationContext().stateChanged();
                    return null;
                }
            });
            permission = paramPermission;
        }

        getReturnValue().setActionReturnValue(permission.getId());

        if (user != null) {
            updateAdminStatus(permission);
        }
        setSucceeded(true);
    }

    private void updateAdminStatus(Permissions perm) {
        // if the role of the permission is of type admin update the user
        // lastAdminCheckStatus to true
        Role role = getRoleDao().get(perm.getrole_id());
        if (role.getType() == RoleType.ADMIN) {
            MultiLevelAdministrationHandler.setIsAdminGUIFlag(perm.getad_element_id(), true);
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_ADD_PERMISSION : AuditLogType.USER_ADD_PERMISSION_FAILED;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        Permissions permission = getParameters().getPermission();
        List<PermissionSubject> permissionsSubject = new ArrayList<>();
        permissionsSubject.add(new PermissionSubject(permission.getObjectId(),
                permission.getObjectType(),
                getActionType().getActionGroup()));
        initUserAndGroupData();
        // if the user does not exist in the database we need to
        // check if the logged in user has permissions to add another
        // user from the directory service
        if ((getParameters().getUser() != null && dbUser == null)
                || (getParameters().getGroup() != null && dbGroup == null)) {
           permissionsSubject.add(new PermissionSubject(permission.getObjectId(),
                    permission.getObjectType(), ActionGroup.ADD_USERS_AND_GROUPS_FROM_DIRECTORY));
        }
        return permissionsSubject;
    }

    private DbUser addUser(DbUser dbUser) {
        // Try to add the user with the external id:
        if (dbUser.getDomain() != null && dbUser.getExternalId() != null) {
            AddUserParameters parameters = new AddUserParameters(dbUser);
            VdcReturnValueBase result = runInternalAction(VdcActionType.AddUser, parameters, cloneContextAndDetachFromParent());
            if (result.getSucceeded()) {
                Guid id = (Guid) result.getActionReturnValue();
                if (id != null) {
                    return getDbUserDAO().get(id);
                }
                return null;
            }
        }
        // There is no such user in the directory:
        return null;
    }

    private DbGroup addGroup(DbGroup groupToAdd) {
        // Try to add the user with the external id:
        if (groupToAdd.getDomain() != null && groupToAdd.getExternalId() != null) {
            AddGroupParameters parameters = new AddGroupParameters(groupToAdd);
            VdcReturnValueBase result = runInternalAction(VdcActionType.AddGroup, parameters, cloneContextAndDetachFromParent());
            if (result.getSucceeded()) {
                Guid id = (Guid) result.getActionReturnValue();
                if (id != null) {
                    return getAdGroupDAO().get(id);
                }
                return null;
            }
        }
        // There is no such group in the directory:
        return null;
    }

}
