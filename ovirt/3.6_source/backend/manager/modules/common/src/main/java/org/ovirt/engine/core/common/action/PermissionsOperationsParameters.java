package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.common.businessentities.Permission;
import org.ovirt.engine.core.common.businessentities.aaa.DbGroup;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;

public class PermissionsOperationsParameters extends VdcActionParametersBase {
    private static final long serialVersionUID = 8854712438369127152L;

    private DbUser user;
    private DbGroup group;
    private Permission permission;

    public PermissionsOperationsParameters() {
    }

    public PermissionsOperationsParameters(Permission permission) {
        setPermission(permission);
    }

    public PermissionsOperationsParameters(Permission permission, DbUser user) {
        this.permission = permission;
        this.user = user;
    }

    public PermissionsOperationsParameters(Permission permission, DbGroup group) {
        this.permission = permission;
        this.group = group;
    }

    public DbUser getUser() {
        return user;
    }

    public void setUser(DbUser value) {
        user = value;
    }

    public DbGroup getGroup() {
        return group;
    }

    public void setGroup(DbGroup value) {
        group = value;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission value) {
        permission = value;
    }
}
