package org.ovirt.engine.core.aaa;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ovirt.engine.api.extensions.Base;
import org.ovirt.engine.api.extensions.ExtKey;
import org.ovirt.engine.api.extensions.ExtMap;
import org.ovirt.engine.api.extensions.aaa.Authn;
import org.ovirt.engine.api.extensions.aaa.Authz;
import org.ovirt.engine.core.extensions.mgr.ExtensionProxy;

public class AuthzUtils {


    private static interface QueryResultHandler {
        public boolean handle(Collection<ExtMap> queryResults);
    }

    private static final Logger log = LoggerFactory.getLogger(AuthzUtils.class);

    private static final int QUERIES_RESULTS_LIMIT = 1000;
    private static final int PAGE_SIZE = 500;


    public static String getName(ExtensionProxy proxy) {
        return proxy.getContext().<String> get(Base.ContextKeys.INSTANCE_NAME);
    }

    public static boolean supportsPasswordAuthentication(ExtensionProxy proxy) {
        return (proxy.getContext().<Long> get(Authn.ContextKeys.CAPABILITIES, 0L) & Authn.Capabilities.AUTHENTICATE_PASSWORD) != 0;
    }

    public static ExtMap fetchPrincipalRecord(final ExtensionProxy extension, ExtMap authRecord) {
        return fetchPrincipalRecordImpl(extension, Authn.InvokeKeys.AUTH_RECORD, authRecord, true, true);
    }

    public static ExtMap fetchPrincipalRecord(final ExtensionProxy extension, String principal, boolean resolveGroups, boolean resolveGroupsRecursive) {
        return fetchPrincipalRecordImpl(extension, Authz.InvokeKeys.PRINCIPAL, principal, resolveGroups, resolveGroupsRecursive);
    }

    private static ExtMap fetchPrincipalRecordImpl(final ExtensionProxy extension, ExtKey key, Object value, boolean resolveGroups, boolean resolveGroupsRecursive) {
        ExtMap ret = null;
        ExtMap output = extension.invoke(new ExtMap().mput(
                Base.InvokeKeys.COMMAND,
                Authz.InvokeCommands.FETCH_PRINCIPAL_RECORD
                ).mput(
                        key,
                        value
                ).mput(
                        Authz.InvokeKeys.QUERY_FLAGS,
                        (
                            (resolveGroups ? Authz.QueryFlags.RESOLVE_GROUPS : 0) |
                            (resolveGroupsRecursive ? Authz.QueryFlags.RESOLVE_GROUPS_RECURSIVE : 0)
                        )
                ));
        if (output.<Integer> get(Authz.InvokeKeys.STATUS) == Authz.Status.SUCCESS) {
            ret = output.<ExtMap> get(Authz.InvokeKeys.PRINCIPAL_RECORD);
        }
        return ret;
    }

    public static Collection<ExtMap> queryPrincipalRecords(
            final ExtensionProxy extension,
            final String namespace,
            final ExtMap filter,
            boolean groupsResolving,
            boolean groupsResolvingRecursive
            ) {
        ExtMap inputMap = new ExtMap().mput(
                Authz.InvokeKeys.QUERY_ENTITY,
                Authz.QueryEntity.PRINCIPAL
                ).mput(
                        Authz.InvokeKeys.QUERY_FLAGS,
                        queryFlagValue(groupsResolving, groupsResolvingRecursive)
                ).mput(
                        Authz.InvokeKeys.QUERY_FILTER,
                        filter
                ).mput(
                        Authz.InvokeKeys.NAMESPACE,
                        namespace
                );
        return populatePrincipalRecords(
                extension,
                namespace,
                inputMap);

    }

    public static Collection<ExtMap> queryGroupRecords(
            final ExtensionProxy extension,
            final String namespace,
            final ExtMap filter,
            boolean groupsResolving,
            boolean groupsResolvingRecursive
            ) {
        ExtMap inputMap = new ExtMap().mput(
                Authz.InvokeKeys.QUERY_ENTITY,
                Authz.QueryEntity.GROUP
                ).mput(
                        Authz.InvokeKeys.QUERY_FLAGS,
                        queryFlagValue(groupsResolving, groupsResolvingRecursive)
                ).mput(
                        Authz.InvokeKeys.QUERY_FILTER,
                        filter
                ).mput(
                        Authz.InvokeKeys.NAMESPACE,
                        namespace
                );
        return populateGroups(
                extension,
                namespace,
                inputMap);

    }

    public static Collection<ExtMap> populatePrincipalRecords(
            final ExtensionProxy extension,
            final String namespace,
            final ExtMap input) {
        final Collection<ExtMap> principalRecords = new ArrayList<>();
        queryImpl(extension, namespace, input, new QueryResultHandler() {

            @Override
            public boolean handle(Collection<ExtMap> queryResults) {
                boolean result = true;
                for (ExtMap queryResult : queryResults) {
                    if (principalRecords.size() < QUERIES_RESULTS_LIMIT) {
                        principalRecords.add(queryResult);
                    } else {
                        result = false;
                        break;
                    }
                }
                return result;
            }
        });
        return principalRecords;
    }

    public static Collection<ExtMap> populateGroups(final ExtensionProxy extension, final String namespace,
            final ExtMap input) {
        final Collection<ExtMap> groups = new ArrayList<>();
        queryImpl(extension, namespace, input, new QueryResultHandler() {
            @Override
            public boolean handle(Collection<ExtMap> queryResults) {

                boolean result = true;
                for (ExtMap queryResult : queryResults) {
                    if (groups.size() < QUERIES_RESULTS_LIMIT) {
                        groups.add(queryResult);
                    } else {
                        result = false;
                    }
                }
                return result;
            }

        });
        return groups;
    }

    private static void queryImpl(
            final ExtensionProxy extension,
            final String namespace,
            final ExtMap input,
            final QueryResultHandler handler
            ) {
        Object opaque = extension.invoke(
                new ExtMap().mput(
                        Base.InvokeKeys.COMMAND,
                        Authz.InvokeCommands.QUERY_OPEN
                        ).mput(
                                Authz.InvokeKeys.NAMESPACE,
                                namespace
                        ).mput(
                                input
                        )
                ).get(Authz.InvokeKeys.QUERY_OPAQUE);
        Collection<ExtMap> result = null;
        try {
            do {
                result = extension.invoke(new ExtMap().mput(
                        Base.InvokeKeys.COMMAND,
                        Authz.InvokeCommands.QUERY_EXECUTE
                        ).mput(
                                Authz.InvokeKeys.QUERY_OPAQUE,
                                opaque
                        ).mput(
                                Authz.InvokeKeys.PAGE_SIZE,
                                PAGE_SIZE
                        )
                        ).get(Authz.InvokeKeys.QUERY_RESULT);
            } while (result != null && handler.handle(result));
        } finally {
            extension.invoke(new ExtMap().mput(
                    Base.InvokeKeys.COMMAND,
                    Authz.InvokeCommands.QUERY_CLOSE
                    ).mput(
                            Authz.InvokeKeys.QUERY_OPAQUE,
                            opaque
                    )
                    );
        }
    }

    public static Collection<ExtMap> findPrincipalsByIds(
            final ExtensionProxy extension,
            final String namespace,
            final Collection<String> ids,
            final boolean groupsResolving,
            final boolean groupsResolvingRecursive
            ) {
        Collection<ExtMap> results = new ArrayList<>();
        for (Collection<String> batch : SearchQueryParsingUtils.getIdsBatches(extension.getContext(), ids)) {
            results.addAll(
                    queryPrincipalRecords(
                            extension,
                            namespace,
                            SearchQueryParsingUtils.generateQueryMap(
                                    batch,
                                    Authz.QueryEntity.PRINCIPAL
                                    ),
                            groupsResolving,
                            groupsResolvingRecursive
                    )
                    );
        }
        return results;
    }

    public static Collection<ExtMap> findGroupRecordsByIds(
            final ExtensionProxy extension,
            final String namespace,
            final Collection<String> ids,
            final boolean groupsResolving,
            final boolean groupsResolvingRecursive
            ) {
        Collection<ExtMap> results = new ArrayList<>();
        for (Collection<String> batch : SearchQueryParsingUtils.getIdsBatches(extension.getContext(), ids)) {
            results.addAll(
                    queryGroupRecords(
                            extension,
                            namespace,
                            SearchQueryParsingUtils.generateQueryMap(
                                    batch,
                                    Authz.QueryEntity.GROUP
                                    ),
                            groupsResolving,
                            groupsResolvingRecursive
                    )
                    );
        }
        return results;
    }

    private static int queryFlagValue(boolean resolveGroups, boolean resolveGroupsRecursive) {
        int result = 0;
        if (resolveGroups) {
            result |= Authz.QueryFlags.RESOLVE_GROUPS;
        }
        if (resolveGroupsRecursive) {
            result |= Authz.QueryFlags.RESOLVE_GROUPS_RECURSIVE | Authz.QueryFlags.RESOLVE_GROUPS;
        }
        return result;

    }

}
