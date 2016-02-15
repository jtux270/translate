package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.action.PermissionsOperationsParameters;
import org.ovirt.engine.core.common.businessentities.Permission;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.aaa.DbGroup;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.linq.Predicate;

public abstract class PermissionsCommandBase<T extends PermissionsOperationsParameters> extends CommandBase<T> {

    @Named
    @Inject
    private Predicate<Guid> isSystemSuperUserPredicate;

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected PermissionsCommandBase(Guid commandId) {
        super(commandId);
    }

    public PermissionsCommandBase(T parameters) {
        this(parameters, null);
    }

    public PermissionsCommandBase(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    protected DbUser dbUser;
    protected DbGroup dbGroup;

    /**
     * Get the object translated type (e.g Host , VM), on which the MLA operation has been executed on.
     *
     * @see VdcObjectType
     * @return Translated object type.
     */
    public String getVdcObjectType() {
        return getParameters().getPermission().getObjectType().getVdcObjectTranslation();
    }

    /**
     * Get the object name, which the MLA operation occurs on. If no entity found, returns null.
     *
     * @return Object name.
     */
    public String getVdcObjectName() {
        Permission perms = getParameters().getPermission();
        return getDbFacade().getEntityNameByIdAndType(perms.getObjectId(), perms.getObjectType());
    }

    public String getRoleName() {
        Role role = getRoleDao().get(getParameters().getPermission().getRoleId());
        return role == null ? null : role.getName();
    }

    public String getSubjectName() {
        // we may have to load user/group from db first.
        // it would be nice to handle this from command execution rather than
        // audit log messages
        initUserAndGroupData();
        return dbUser == null ? (dbGroup == null ? "" : dbGroup.getName()) : dbUser.getLoginName();
    }

    public String getNamespace() {
        initUserAndGroupData();
        return dbUser == null ? (dbGroup == null ? "" : dbGroup.getNamespace()) : dbUser.getNamespace();

    }

    public String getAuthz() {
        initUserAndGroupData();
        return dbUser == null ? (dbGroup == null ? "" : dbGroup.getDomain()) : dbUser.getDomain();

    }

    public void initUserAndGroupData() {
        if (dbUser == null) {
            dbUser = getDbUserDao().get(getParameters().getPermission().getAdElementId());
        }
        if (dbUser == null && dbGroup == null) {
            dbGroup = getAdGroupDao().get(getParameters().getPermission().getAdElementId());
        }
    }

    protected boolean isSystemSuperUser() {
        return isSystemSuperUserPredicate.eval(getCurrentUser().getId());
    }

    // TODO - this code is shared with addPermissionCommand - check if
    // addPermission can extend this command
    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<>();
        Permission permission = getParameters().getPermission();
        permissionList.add(new PermissionSubject(permission.getObjectId(),
                permission.getObjectType(),
                getActionType().getActionGroup()));
        return permissionList;
    }
}
