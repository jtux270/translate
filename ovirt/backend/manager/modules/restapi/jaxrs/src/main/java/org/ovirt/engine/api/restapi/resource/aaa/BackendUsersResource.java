package org.ovirt.engine.api.restapi.resource.aaa;

import static org.ovirt.engine.api.utils.ReflectionHelper.assignChildModel;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.api.common.util.QueryHelper;
import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.User;
import org.ovirt.engine.api.model.Users;
import org.ovirt.engine.api.resource.aaa.UserResource;
import org.ovirt.engine.api.resource.aaa.UsersResource;
import org.ovirt.engine.api.restapi.resource.AbstractBackendCollectionResource;
import org.ovirt.engine.api.restapi.resource.ResourceConstants;
import org.ovirt.engine.api.restapi.resource.SingleEntityResource;
import org.ovirt.engine.api.restapi.utils.aaa.AuthzUtils;
import org.ovirt.engine.api.restapi.utils.DirectoryEntryIdUtils;
import org.ovirt.engine.core.aaa.DirectoryUser;
import org.ovirt.engine.core.common.action.AddUserParameters;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.DirectoryIdQueryParameters;
import org.ovirt.engine.core.common.queries.GetDirectoryUserByPrincipalParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

/**
 * This resource corresponds to users that have been looked up in some directory
 * accessible to the engine and then added to the engine database. Users can
 * be added and removed from the collection, and this will add or remove them
 * from the database (not from the underlying directory).
 */
public class BackendUsersResource
        extends AbstractBackendCollectionResource<User, DbUser>
        implements UsersResource {

    static final String[] SUB_COLLECTIONS = { "permissions", "roles", "tags" };

    private static final String USERS_SEARCH_PATTERN = "usrname != \"\"";
    private static final String AND_SEARCH_PATTERN = " and ";

    private BackendDomainResource parent;

    public BackendUsersResource() {
        super(User.class, DbUser.class, SUB_COLLECTIONS);
    }

    public BackendUsersResource(String id, BackendDomainResource parent) {
        super(User.class, DbUser.class, SUB_COLLECTIONS);
        this.parent = parent;
    }

    public BackendUsersResource(Class<User> class1, Class<DbUser> class2, String[] subCollections) {
        super(class1, class2, subCollections);
    }

    /**
     * This method calculates the search pattern that will be used to perform
     * the search of database users during the execution of the {@code list}
     * operation.
     */
    private String getSearchPattern() {
        String user_defined_pattern = QueryHelper.getConstraint(getUriInfo(), "",  modelType);
        return user_defined_pattern.equals("Users : ") ?
               user_defined_pattern + USERS_SEARCH_PATTERN
               :
               user_defined_pattern + AND_SEARCH_PATTERN + USERS_SEARCH_PATTERN;
    }

    protected String getAuthzProviderName(User user, Collection<String> authzProvidersNames) {
        if (user.isSetDomain() && user.getDomain().isSetName()) {
            return user.getDomain().getName();
        }
        else if (user.isSetDomain() && user.getDomain().isSetId()) {
            for (String domain : authzProvidersNames) {
                Guid domainId = asGuid(domain.getBytes(Charset.forName("UTF-8")), true);
                if (domainId.toString().equals(user.getDomain().getId())) {
                   return domain;
                }
            }
            throw new WebFaultException(null, "Domain: '" + user.getDomain().getId().toString() + "' does not exist.", Response.Status.BAD_REQUEST);
        }
        return AuthzUtils.getAuthzNameFromEntityName(user.getUserName(), authzProvidersNames);
    }

    /**
     * This method calculates the search pattern used to search for the
     * directory user that will be added to the database when performing the
     * {@code add} operation.
     *
     * @param username the name of the user that will be searched in the
     *     directory
     * @param domain the name of the directory where the search will be
     *     performed
     */
    private String getDirectoryUserSearchPattern(String username, String namespace, String domain) {
        String constraint = QueryHelper.getConstraint(getUriInfo(), DbUser.class, false);
        final StringBuilder sb = new StringBuilder(128);

        sb.append(MessageFormat.format(ResourceConstants.AAA_PRINCIPALS_SEARCH_TEMPLATE,
                  parent!=null?
                        parent.getDirectory().getName()
                        :
                        domain,
                  namespace != null ? namespace : ""));

        sb.append(StringUtils.isEmpty(constraint) ?
                "username=" + username
                        :
                        constraint);

        return sb.toString();
    }

    @Override
    public Response performRemove(String id) {
        return performAction(VdcActionType.RemoveUser, new IdParameters(asGuid(id)));
    }

    protected Users mapDbUserCollection(List<DbUser> entities) {
        Users collection = new Users();
        for (DbUser entity : entities) {
            User user = map(entity);
            user = populate(user, entity);
            user = addLinks(user, BaseResource.class);
            collection.getUsers().add(user);
        }
        return collection;
    }

    @Override
    protected User addParents(User user) {
        if(parent!=null){
            assignChildModel(user, User.class).setId(parent.get().getId());
        }
        return user;
    }

    @Override
    protected User doPopulate(User model, DbUser entity) {
        return model;
    }

    @SingleEntityResource
    public UserResource getUserSubResource(String id) {
        return inject(new BackendUserResource(id, this));
    }

    public Users list() {
        if (isFiltered()) {
            return mapDbUserCollection(getBackendCollection(VdcQueryType.GetAllDbUsers, new VdcQueryParametersBase()));
        }
        else {
          return mapDbUserCollection(getBackendCollection(SearchType.DBUser, getSearchPattern()));
        }
    }

    @Override
    public Response add(User user) {
        validateParameters(user, "userName");
        List<String> authzProvidersNames = getBackendCollection(
                String.class,
                VdcQueryType.GetDomainList,
                new VdcQueryParametersBase());
        if (AuthzUtils.getAuthzNameFromEntityName(user.getUserName(), authzProvidersNames) == null) {// user-name may contain the domain (e.g: oliel@xxx.yyy)
            validateParameters(user, "domain.id|name");
        }
        String domain = getAuthzProviderName(user, authzProvidersNames);
        DirectoryUser directoryUser = findDirectoryUser(domain, user);
        if (directoryUser == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("No such user: " + user.getUserName() + " in domain " + domain)
                    .build();
        }
        AddUserParameters parameters = new AddUserParameters(new DbUser(directoryUser));
        QueryIdResolver<Guid> resolver = new QueryIdResolver<>(VdcQueryType.GetDbUserByUserId, IdQueryParameters.class);
        return performCreate(VdcActionType.AddUser, parameters, resolver, BaseResource.class);
    }

    /**
     * Find the directory user that corresponds to the given model.
     *
     * @param directoryName
     *            the name of the directory where to perform the search
     * @param groupModel
     *            the group model
     * @return the requested directory group or {@code null} if no such group exists
     */
    private DirectoryUser findDirectoryUser(String directoryName, User user) {
        DirectoryUser result = null;
        String namespace = user.getNamespace();
        if (user.isSetDomainEntryId()) {
            result = getUserById(directoryName, namespace, user.getDomainEntryId());
        } else if (user.isSetId()) {
            result = getUserById(directoryName, namespace, user.getId());
        } else  if (user.isSetPrincipal()) {
            result = getEntity(DirectoryUser.class, VdcQueryType.GetDirectoryUserByPrincipal, new GetDirectoryUserByPrincipalParameters(directoryName, user.getPrincipal()), user.getPrincipal());
        } else if (user.isSetUserName()) {
                result = getEntity(
                        DirectoryUser.class,
                        SearchType.DirectoryUser,
                        getDirectoryUserSearchPattern(
                            AuthzUtils.getEntityNameWithoutAuthz(user.getUserName(), directoryName),
                                user.getNamespace(),
                                directoryName)
                        );
            }
        return result;
    }

    private DirectoryUser getUserById(String directoryName, String namespace, String userId) {
        DirectoryUser result;
        userId = DirectoryEntryIdUtils.decode(userId);
        result = getEntity(
                DirectoryUser.class,
                VdcQueryType.GetDirectoryUserById,
                new DirectoryIdQueryParameters(directoryName, namespace, userId),
                userId,
                true);
        return result;
    }
}
