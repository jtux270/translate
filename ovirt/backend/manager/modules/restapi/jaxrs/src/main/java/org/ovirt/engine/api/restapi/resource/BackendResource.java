package org.ovirt.engine.api.restapi.resource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.common.invocation.MetaData;
import org.ovirt.engine.api.common.util.QueryHelper;
import org.ovirt.engine.api.common.util.StatusUtils;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.CreationStatus;
import org.ovirt.engine.api.model.Job;
import org.ovirt.engine.api.model.Version;
import org.ovirt.engine.api.restapi.resource.exception.UrlParamException;
import org.ovirt.engine.api.restapi.resource.validation.Validator;
import org.ovirt.engine.api.restapi.util.ErrorMessageHelper;
import org.ovirt.engine.api.restapi.util.SessionHelper;
import org.ovirt.engine.api.utils.ExpectationHelper;
import org.ovirt.engine.api.utils.LinkHelper;
import org.ovirt.engine.core.common.action.LogoutUserParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.config.ConfigCommon;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.GetConfigurationValueParameters;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

public class BackendResource extends BaseBackendResource {
    private static final String FALSE = "false";
    private static final String TRUE = "true";
    protected static final int NO_LIMIT = -1;
    private static final String CORRELATION_ID = "Correlation-Id";
    private static final String ASYNC_CONSTRAINT = "async";
    public static final String FORCE_CONSTRAINT = "force";
    protected static final String MAX = "max";
    private static final String NON_BLOCKING_EXPECTATION = "202-accepted";
    protected static final Log LOG = LogFactory.getLog(BackendResource.class);
    public static final String POPULATE = "All-Content";
    public static final String JOB_ID_CONSTRAINT = "JobId";
    public static final String STEP_ID_CONSTRAINT = "StepId";

    private <T> T castQueryResultToEntity(Class<T> clz, VdcQueryReturnValue result,
                                          String constraint) throws BackendFailureException {
        T entity;

        if (List.class.isAssignableFrom(clz) && result.getReturnValue() instanceof List) {
            entity = clz.cast(result.getReturnValue());
        } else {
            List<T> list = asCollection(clz, result.getReturnValue());
            if (list == null || list.isEmpty()) {
                throw new EntityNotFoundException(constraint);
            }
            entity = clz.cast(list.get(0));
        }

        return entity;
    }

    @Deprecated
    protected <T> T getEntity(Class<T> clz, SearchType searchType, String constraint) {
        try {
            VdcQueryReturnValue result = runQuery(VdcQueryType.Search,
                    createSearchParameters(searchType, constraint));
            if (!result.getSucceeded()) {
                backendFailure(result.getExceptionString());
            }
            return castQueryResultToEntity(clz, result, constraint);
        } catch (Exception e) {
            return handleError(clz, e, false);
        }
    }


    protected VdcQueryReturnValue runQuery(VdcQueryType queryType, VdcQueryParametersBase queryParams) {
        queryParams.setFiltered(isFiltered());
        return backend.runQuery(queryType, sessionize(queryParams));
    }

    protected SearchParameters createSearchParameters(SearchType searchType, String constraint) {
        return new SearchParameters(constraint, searchType);
    }

    protected <T> T getEntity(Class<T> clz, VdcQueryType query, VdcQueryParametersBase queryParams, String identifier) {
        return getEntity(clz, query, queryParams, identifier, false);
    }

    protected <T> T getEntity(Class<T> clz,
                              VdcQueryType query,
                              VdcQueryParametersBase queryParams,
                              String identifier,
                              boolean notFoundAs404) {
        try {
            return doGetEntity(clz, query, queryParams, identifier);
        } catch (Exception e) {
            return handleError(clz, e, notFoundAs404);
        }
    }

    protected <T> T doGetEntity(Class<T> clz,
                                VdcQueryType query,
                                VdcQueryParametersBase queryParams,
                                String identifier) throws BackendFailureException {
        VdcQueryReturnValue result = runQuery(query, queryParams);
        if (!result.getSucceeded() || result.getReturnValue() == null) {
            if (result.getExceptionString() != null) {
                backendFailure(result.getExceptionString());
            } else {
                throw new EntityNotFoundException(identifier);
            }
        }
        return castQueryResultToEntity(clz, result, identifier);
    }

    protected <T> List<T> getBackendCollection(Class<T> clz, VdcQueryType query, VdcQueryParametersBase queryParams) {
        try {
            VdcQueryReturnValue result = runQuery(query, queryParams);
            if (!result.getSucceeded()) {
                backendFailure(result.getExceptionString());
            }
            List<T> results = asCollection(clz, result.getReturnValue());
            if (results!=null && getMaxResults()!=NO_LIMIT && getMaxResults()<results.size()) {
                results = results.subList(0, getMaxResults());
            }
            return results;
        } catch (Exception e) {
            return handleError(e, false);
        }
    }

    protected int getMaxResults() throws MalformedNumberException {
        if (getUriInfo()!=null && QueryHelper.hasMatrixParam(getUriInfo(), MAX)) {
            HashMap<String, String> matrixConstraints = QueryHelper.getMatrixConstraints(getUriInfo(), MAX);
            String maxString = matrixConstraints.get(MAX);
            try {
                return Integer.valueOf(maxString);
            } catch (NumberFormatException e) {
                LOG.error("Max number of results is not a valid number: " + maxString);
                throw new MalformedNumberException("Max number of results is not a valid number: " + maxString);
            }
        } else {
            return NO_LIMIT;
        }
    }

    protected Response performAction(VdcActionType task, VdcActionParametersBase params, Action action) {
        return performAction(task, params, action, false);
    }

    protected Response performAction(VdcActionType task, VdcActionParametersBase params) {
        return performAction(task, params, (Action) null, false);
    }

    protected Response performAction(VdcActionType task, VdcActionParametersBase params, Action action, boolean getEntityWhenDone) {
        try {
            if (isAsync() || expectNonBlocking()) {
                getCurrent().get(MetaData.class).set("async", true);
                return performNonBlockingAction(task, params, action);
            } else {
                VdcReturnValueBase actionResult = doAction(task, params);
                if (action == null) {
                    action = new Action();
                }
                if (actionResult.getJobId() != null) {
                    setJobLink(action, actionResult);
                }
                action.setStatus(StatusUtils.create(CreationStatus.COMPLETE));
                if (getEntityWhenDone) {
                    setActionItem(action, getEntity());
                }
                return Response.ok().entity(action).build();
            }
        } catch (Exception e) {
            return handleError(Response.class, e, false);
        }
    }

    protected void setJobLink(final Action action, VdcReturnValueBase actionResult) {
        Job job = new Job();
        job.setId(actionResult.getJobId().toString());
        LinkHelper.addLinks(getUriInfo(), job, null, false);
        action.setJob(job);
    }

    private boolean getBooleanMatrixConstraint(String marixConsraint) {
        String value = QueryHelper.getMatrixConstraint(getUriInfo(), marixConsraint);
        if (value == null) {
            return false; // matrix param does not exist in URL, go with the default value, which is 'false'.
        } else {
            if (value.equalsIgnoreCase(TRUE) || value.isEmpty()) {
                return true;
            } else if (value.equalsIgnoreCase(FALSE)) {
                return false;
            } else {
                // param exists but has illegal value (not 'false' or 'true') - throw exception.
                throw new UrlParamException(this,
                        null,
                        "Illegal value '" + value
                                + "' for matrix param: '" + marixConsraint
                                + "'. Acceptable values are 'true' or 'false'");
            }
        }
    }

    protected boolean isAsync() {
        return getBooleanMatrixConstraint(ASYNC_CONSTRAINT);
    }

    /**
     * Checks if ;force matrix parameter specified in the URI
     */
    protected boolean isForce() {
        return getBooleanMatrixConstraint(FORCE_CONSTRAINT);
    }

    protected void badRequest(String message) {
        throw new WebFaultException(null, message, Response.Status.BAD_REQUEST);
    }

    protected boolean expectNonBlocking() {
        Set<String> expectations = ExpectationHelper.getExpectations(httpHeaders);
        return expectations.contains(NON_BLOCKING_EXPECTATION);
    }
    protected Response performNonBlockingAction(VdcActionType task, VdcActionParametersBase params, Action action) {
        try {
            doNonBlockingAction(task, params);
            if (action!=null) {
                action.setStatus(StatusUtils.create(CreationStatus.IN_PROGRESS));
                return Response.status(Response.Status.ACCEPTED).entity(action).build();
            } else {
                return Response.status(Response.Status.ACCEPTED).build();
            }
        } catch (Exception e) {
            return handleError(Response.class, e, false);
        }
    }

    protected <T> T performAction(VdcActionType task, VdcActionParametersBase params, Class<T> resultType) {
        try {
            return resultType.cast(doAction(task, params).getActionReturnValue());
        } catch (Exception e) {
            return handleError(resultType, e, false);
        }
    }

    protected VdcReturnValueBase doAction(VdcActionType task,
                                          VdcActionParametersBase params) throws BackendFailureException {
        setJobOrStepId(params);
        setCorrelationId(params);
        VdcReturnValueBase result = backend.runAction(task, sessionize(params));
        if (result != null && !result.getCanDoAction()) {
            backendFailure(result.getCanDoActionMessages());
        } else if (result != null && !result.getSucceeded()) {
            backendFailure(result.getExecuteFailedMessages());
        }
        assert (result != null);
        return result;
    }

    protected void doNonBlockingAction(final VdcActionType task, final VdcActionParametersBase params) {
        setCorrelationId(params);
        setJobOrStepId(params);
        ThreadPoolUtil.execute(new Runnable() {
            SessionHelper sh = getSessionHelper();
            VdcActionParametersBase sp = sessionize(params);
            DbUser user = getCurrent().get(DbUser.class);

            @Override
            public void run() {
                try {
                    backend.runAction(task, sp);
                } finally {
                    if (user != null) {
                        backend.logoff(sh.sessionize(new LogoutUserParameters(user.getId())));
                    }
                    sh.clean();
                }
            }
        });
    }

    private void setJobOrStepId(VdcActionParametersBase params) {
        if (QueryHelper.hasMatrixParam(uriInfo, JOB_ID_CONSTRAINT)) {
            String value = QueryHelper.getMatrixConstraint(uriInfo, JOB_ID_CONSTRAINT);
            params.setJobId(asGuid(value));
        }

        if (QueryHelper.hasMatrixParam(uriInfo, STEP_ID_CONSTRAINT)) {
            String value = QueryHelper.getMatrixConstraint(uriInfo, STEP_ID_CONSTRAINT);
            params.setJobId(asGuid(value));
        }
    }
    private void setCorrelationId(VdcActionParametersBase params) {
        List<String> correlationIds = httpHeaders.getRequestHeader(CORRELATION_ID);
        if (correlationIds != null && correlationIds.size() > 0) {
            params.setCorrelationId(correlationIds.get(0));
        }
    }

    @SuppressWarnings("serial")
    protected <T> T getConfigurationValue(Class<T> clz, ConfigurationValues config, final Version version) {
        return getEntity(clz,
                         VdcQueryType.GetConfigurationValue,
                         new GetConfigurationValueParameters(config, asString(version)),
                         config.toString());
    }

    @SuppressWarnings("serial")
    protected <T> T getFenceConfigurationValue(Class<T> clz, ConfigurationValues config, final Version version) {
        return getEntity(clz,
                VdcQueryType.GetFenceConfigurationValue,
                new GetConfigurationValueParameters(config, asString(version)),
                config.toString());
    }

    protected <T> T getConfigurationValueDefault(Class<T> clz, ConfigurationValues config) {
        return getEntity(clz,
                VdcQueryType.GetConfigurationValue,
                new GetConfigurationValueParameters(config, ConfigCommon.defaultConfigurationVersion),
                config.toString());
    }

    private static final String VERSION_FORMAT = "{0}.{1}";

    static String asString(Version version) {
        return version == null ? null : MessageFormat.format(VERSION_FORMAT, version.getMajor(), version.getMinor());
    }

    protected <E> Validator<E> getValidator(Class<E> validatedClass) {
        return getValidatorLocator().getValidator(validatedClass);
    }

    protected <E> void validateEnums(Class<E> validatedClass, E instance) {
        getValidator(validatedClass).validateEnums(instance);
    }

    /**
     * @return true if request header contains [All-Content='true']
     */
    protected boolean isPopulate() {
        List<String> populates = httpHeaders.getRequestHeader(POPULATE);
        if (populates != null && populates.size() > 0) {
            return Boolean.valueOf(populates.get(0)).booleanValue();
        } else {
            return false;
        }
    }

    /**
     * Runs implementation of the @GET annotated method of this resource (get() for single entity resources, and list()
     * for collection resources).
     *
     * @return The result of the @GET annotated method, an entity or list of entities.
     */
    protected Object getEntity() {
        try {
            Method m = resolveGet();
            if (m == null) {
                return null;
            }
            Object entity = m.invoke(this);
            return getEntityWithIdAndHref(entity);
        } catch (Exception e) {
            LOG.error("Getting resource after action failed.", e);
            return null;
        }
    }

    private Method resolveGet() throws NoSuchMethodException, SecurityException {
        Method methodSignature = findGetSignature(this.getClass());
        if (methodSignature == null) {
            return null;
        }
        Method methodImplementation =
                this.getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
        return methodImplementation;
    }

    private static Method findGetSignature(Class<?> clazz) {
        Class<?> currentAncestor = clazz;
        while (currentAncestor != null) {
            Class<?>[] interfaces = currentAncestor.getInterfaces();
            for (Class<?> ifc : interfaces) {
                Method m = find(ifc);
                if (m != null) {
                    return m;
                }
            }
            currentAncestor = currentAncestor.getSuperclass();
        }
        return null;
    }

    private static Method find(Class<?> ifc) {
        Class<?> currentAncestor = ifc;
        while (currentAncestor != null) {
            for (Method m : currentAncestor.getMethods()) {
                if (m.isAnnotationPresent(GET.class)) {
                    return m;
                }
            }
            currentAncestor = currentAncestor.getSuperclass();
        }
        return null;
    }

    protected Object getEntityWithIdAndHref(Object entity) throws InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        Object newEntity = entity.getClass().newInstance();
        setEntityValue(newEntity, "setId", getEntityValue(entity, "getId"));
        setEntityValue(newEntity, "setHref", getEntityValue(entity, "getHref"));
        return entity;
    }

    private void setEntityValue(Object entity, String methodName, Object value) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = entity.getClass().getMethod(methodName);
        method.invoke(entity, value);
    }

    private Object getEntityValue(Object entity, String methodName) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object nullObj = null;
        Method method = entity.getClass().getMethod(methodName);
        return method.invoke(entity, nullObj);
    }

    protected Response actionStatus(CreationStatus status, Action action, Object result) {
        setActionItem(action, result);
        action.setStatus(StatusUtils.create(status));
        return Response.ok().entity(action).build();
    }

    protected void setActionItem(Action action, Object result) {
        if (result == null) {
            return;
        }
        String name = result.getClass().getSimpleName().toLowerCase();
        for (Method m : action.getClass().getMethods()) {
            if (m.getName().startsWith("set") && m.getName().replace("set", "").toLowerCase().equals(name)) {
                try {
                    m.invoke(action, result);
                    break;
                } catch (Exception e) {
                    // should not happen
                    LOG.error("Resource to action asignment failure.", e);
                    break;
                }
            }
        }
    }

    protected void backendFailure(String msg) throws BackendFailureException {
        throw new BackendFailureException(localize(msg), ErrorMessageHelper.getErrorStatus(msg));
    }

    protected void backendFailure(List<String> messages) throws BackendFailureException {
        throw new BackendFailureException(localize(messages), ErrorMessageHelper.getErrorStatus(messages));
    }
}
