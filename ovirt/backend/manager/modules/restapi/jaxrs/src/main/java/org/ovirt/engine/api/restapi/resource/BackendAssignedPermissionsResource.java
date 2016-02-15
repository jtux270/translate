package org.ovirt.engine.api.restapi.resource;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.Group;
import org.ovirt.engine.api.model.Permission;
import org.ovirt.engine.api.model.User;
import org.ovirt.engine.api.resource.AssignedPermissionsResource;
import org.ovirt.engine.api.resource.PermissionResource;
import org.ovirt.engine.api.restapi.types.GroupMapper;
import org.ovirt.engine.api.restapi.types.UserMapper;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.PermissionsOperationsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.aaa.DbGroup;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.queries.GetPermissionsForObjectParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendAssignedPermissionsResource
        extends AbstractBackendCollectionResource<Permission, Permissions>
        implements AssignedPermissionsResource {

    private Guid targetId;
    private VdcQueryType queryType;
    private VdcQueryParametersBase queryParams;
    private Class<? extends BaseResource> suggestedParentType;
    private VdcObjectType objectType;

    public BackendAssignedPermissionsResource(Guid targetId,
                                              VdcQueryType queryType,
                                              VdcQueryParametersBase queryParams,
                                              Class<? extends BaseResource> suggestedParentType) {
        this(targetId, queryType, queryParams, suggestedParentType, null);
    }

    public BackendAssignedPermissionsResource(Guid targetId,
                                              VdcQueryType queryType,
                                              VdcQueryParametersBase queryParams,
                                              Class<? extends BaseResource> suggestedParentType,
                                              VdcObjectType objectType) {
        super(Permission.class, Permissions.class);
        this.targetId = targetId;
        this.queryType = queryType;
        this.queryParams = queryParams;
        this.suggestedParentType = suggestedParentType;
        this.objectType = objectType;
    }

    @Override
    public org.ovirt.engine.api.model.Permissions list() {
        Set<Permissions> permissions = new TreeSet<Permissions>(new PermissionsComparator());
        List<Permissions> directPermissions = getBackendCollection(queryType, queryParams);
        permissions.addAll(directPermissions);
        if (queryType.equals(VdcQueryType.GetPermissionsForObject)) {
            permissions.addAll(getInheritedPermissions());
        }
        return mapCollection(permissions);
    }

    private List<Permissions> getInheritedPermissions() {
        ((GetPermissionsForObjectParameters)queryParams).setVdcObjectType(objectType);
        ((GetPermissionsForObjectParameters)queryParams).setDirectOnly(false);
        List<Permissions> inheritedPermissions = getBackendCollection(queryType, queryParams);
        for (Permissions entity : inheritedPermissions) {
            if (objectType != null) {
                entity.setObjectType(objectType);
                entity.setObjectId(targetId);
            }
        }
        return inheritedPermissions;
    }

    static class PermissionsComparator implements Comparator<Permissions>, Serializable {
        @Override
        public int compare(Permissions o1, Permissions o2) {
            String id1 = o1.getId().toString();
            String id2 = o2.getId().toString();
            return id1.compareTo(id2);
        }
    }

    @Override
    public Response add(Permission permission) {
        validateParameters(permission,
                           isPrincipalSubCollection()
                           ? new String[] {"role.id", "dataCenter|cluster|host|storageDomain|vm|vmpool|template.id"}
                           : new String[] {"role.id", "user|group.id"});
        PermissionsOperationsParameters parameters = getParameters(permission);
        QueryIdResolver<Guid> resolver = new QueryIdResolver<>(VdcQueryType.GetPermissionById, IdQueryParameters.class);
        return performCreate(VdcActionType.AddPermission, parameters, resolver);
    }

    @Override
    public Response performRemove(String id) {
        return performAction(VdcActionType.RemovePermission, new PermissionsOperationsParameters(getPermissions(id)));
    }

    @Override
    @SingleEntityResource
    public PermissionResource getPermissionSubResource(String id) {
        return inject(new BackendPermissionResource(id, this, suggestedParentType));
    }

    protected org.ovirt.engine.api.model.Permissions mapCollection(Set<Permissions> entities) {
        org.ovirt.engine.api.model.Permissions collection = new org.ovirt.engine.api.model.Permissions();
        for (Permissions entity : entities) {
             castEveryonePermissionsToUser(entity);
             Permission permission = map(entity, getUserById(entity.getad_element_id()));
             collection.getPermissions().add(addLinks(permission, permission.getUser() != null ? suggestedParentType : Group.class));
        }
        return collection;
    }

    private void castEveryonePermissionsToUser(Permissions entity) {
        if (entity.getad_element_id() != null &&
            entity.getad_element_id().equals(Guid.EVERYONE) &&
            queryType.equals(VdcQueryType.GetPermissionsByAdElementId)) {
            entity.setad_element_id(this.targetId);
        }
    }

    public DbUser getUserById(Guid userId) {
        IdQueryParameters queryParameters = new IdQueryParameters(userId);
        VdcQueryReturnValue userQueryResponse = runQuery(VdcQueryType.GetDbUserByUserId, queryParameters);

        DbUser returnValue = null;
        if (userQueryResponse != null && userQueryResponse.getSucceeded()) {
            returnValue = userQueryResponse.getReturnValue();
        }

        return returnValue;
    }

    public Map<Guid, DbUser> getUsers() {
        HashMap<Guid, DbUser> users = new HashMap<Guid, DbUser>();
        for (DbUser user : lookupUsers()) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private List<DbUser> lookupUsers() {
        VdcQueryParametersBase queryParams = new VdcQueryParametersBase();
        queryParams.setFiltered(isFiltered());
        return getBackendCollection(DbUser.class, VdcQueryType.GetAllDbUsers, queryParams);
    }

    /**
     * injects user/group base on permission owner type
     * @param entity the permission to map
     * @param user the permission owner
     * @return permission
     */
    public Permission map(Permissions entity, DbUser user) {
        Permission template = new Permission();
        if (entity.getad_element_id() != null) {
            if (isUser(user)) {
                template.setUser(new User());
                template.getUser().setId(entity.getad_element_id().toString());
            } else {
                template.setGroup(new Group());
                template.getGroup().setId(entity.getad_element_id().toString());
            }
        }
        return map(entity, template);
    }

    //REVISIT: fix once BE can distinguish between the user and group
    private static boolean isUser(DbUser user) {
        return user != null && !user.isGroup();
    }

    /**
     * Find the user or group that the permissions applies to.
     *
     * @param permission the incoming permission model
     * @return the user or group that the permission applies to
     */
    private Object getPrincipal(Permission permission) {
        if (isUserSubCollection()) {
            DbUser dbUser = new DbUser();
            dbUser.setId(targetId);
            return dbUser;
        }
        if (isGroupSubCollection()) {
            DbGroup dbGroup = new DbGroup();
            dbGroup.setId(targetId);
            return dbGroup;
        }
        if (permission.isSetUser()) {
            User user = permission.getUser();
            DbUser dbUser = UserMapper.map(user, null);
            if (dbUser.getDomain() == null) {
                dbUser.setDomain(getCurrent().get(DbUser.class).getDomain());
            }
            return dbUser;
        }
        if (permission.isSetGroup()) {
            Group group = permission.getGroup();
            DbGroup dbGroup = GroupMapper.map(group, null);
            if (dbGroup.getDomain() == null) {
                dbGroup.setDomain(getCurrent().get(DbUser.class).getDomain());
            }
            return dbGroup;
        }
        return null;
    }

    /**
     * Create the parameters for the permissions operation.
     *
     * @param model the incoming permission
     * @return the parameters for the operation
     */
    private PermissionsOperationsParameters getParameters(Permission model) {
        Permissions entity = map(model, null);
        if (!isPrincipalSubCollection()) {
            entity.setObjectId(targetId);
            entity.setObjectType(objectType);
        }
        PermissionsOperationsParameters parameters = new PermissionsOperationsParameters();
        parameters.setPermission(entity);
        Object principal = getPrincipal(model);
        if (principal instanceof DbUser) {
            DbUser user = (DbUser) principal;
            entity.setad_element_id(user.getId());
            parameters.setUser(user);
        }
        if (principal instanceof DbGroup) {
            DbGroup group = (DbGroup) principal;
            entity.setad_element_id(group.getId());
            parameters.setGroup(group);
        }
        return parameters;
    }

    @Override
    public Permission addParents(Permission permission) {
        // REVISIT for entity-level permissions we need an isUser
        // flag on the permissions entity in order to distinguish
        // between the user and group cases
        if (isGroupSubCollection() && permission.isSetUser() && permission.getUser().isSetId()) {
            permission.setGroup(new Group());
            permission.getGroup().setId(permission.getUser().getId());
            permission.setUser(null);
        }
        return permission;
    }

    protected boolean isPrincipalSubCollection() {
        return isUserSubCollection() || isGroupSubCollection();
    }

    protected boolean isUserSubCollection() {
        return User.class.equals(suggestedParentType);
    }

    protected boolean isGroupSubCollection() {
        return Group.class.equals(suggestedParentType);
    }

    protected Permissions getPermissions(String id) {
        return getEntity(Permissions.class,
                         VdcQueryType.GetPermissionById,
                new IdQueryParameters(asGuid(id)),
                         id);
    }

    @Override
    protected Permission doPopulate(Permission model, Permissions entity) {
        return model;
    }
}
