package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.core.bll.aaa.SessionDataContainer;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.ThreadLocalParamsContainer;

/** A test case for the {@link QueriesCommandBase} class. */
public class QueriesCommandBaseTest {
    private static final Log log = LogFactory.getLog(QueriesCommandBaseTest.class);

    /* Getters and Setters tests */

    /** Test {@link QueriesCommandBase#isInternalExecution()} and {@link QueriesCommandBase#setInternalExecution(boolean) */
    @Test
    public void testIsInternalExecutionDefault() {
        ThereIsNoSuchQuery query = new ThereIsNoSuchQuery(mock(VdcQueryParametersBase.class));
        assertFalse("By default, a query should not be marked for internel execution", query.isInternalExecution());
    }

    @Test
    public void testIsInternalExecutionTrue() {
        ThereIsNoSuchQuery query = new ThereIsNoSuchQuery(mock(VdcQueryParametersBase.class));
        query.setInternalExecution(true);
        assertTrue("Query should be marked for internel execution", query.isInternalExecution());
    }

    @Test
    public void testIsInternalExecutionFalse() {
        ThereIsNoSuchQuery query = new ThereIsNoSuchQuery(mock(VdcQueryParametersBase.class));

        // Set as true, then override with false
        query.setInternalExecution(true);
        query.setInternalExecution(false);

        assertFalse("Query should not be marked for internel execution", query.isInternalExecution());
    }

    /** Test that an "oddly" typed query will be considered unknown */
    @Test
    public void testUnknownQuery() throws Exception {
        ThereIsNoSuchQuery query = new ThereIsNoSuchQuery(mock(VdcQueryParametersBase.class));
        assertEquals("Wrong type for 'ThereIsNoSuchQuery' ",
                VdcQueryType.Unknown,
                TestHelperQueriesCommandType.getQueryTypeFieldValue(query));
    }

    /** Tests Admin permission check */
    @Test
    public void testPermissionChecking() throws Exception {
        boolean[] booleans = { true, false };
        for (VdcQueryType queryType : VdcQueryType.values()) {
            for (boolean isFiltered : booleans) {
                for (boolean isUserAdmin : booleans) {
                    for (boolean isInternalExecution : booleans) {
                        boolean shouldBeAbleToRunQuery;
                        if (isFiltered) {
                            shouldBeAbleToRunQuery = !queryType.isAdmin();
                        } else {
                            shouldBeAbleToRunQuery = isInternalExecution || isUserAdmin;
                        }

                        log.debug("Running on query: " + toString());

                        String sessionId = getClass().getSimpleName();

                        // Mock parameters
                        VdcQueryParametersBase params = mock(VdcQueryParametersBase.class);
                        when(params.isFiltered()).thenReturn(isFiltered);
                        when(params.getSessionId()).thenReturn(sessionId);

                        Guid guid = mock(Guid.class);

                        // Set up the user id env.
                        DbUser user = mock(DbUser.class);
                        when(user.getId()).thenReturn(guid);
                        when(user.isAdmin()).thenReturn(isUserAdmin);
                        SessionDataContainer.getInstance().setUser(sessionId, user);

                        // Mock-Set the query as admin/user
                        ThereIsNoSuchQuery query = new ThereIsNoSuchQuery(params);
                        TestHelperQueriesCommandType.setQueryTypeFieldValue(query, queryType);

                        query.setInternalExecution(isInternalExecution);
                        query.executeCommand();
                        assertEquals("Running with type=" + queryType + " isUserAdmin=" + isUserAdmin + " isFiltered="
                                + isFiltered + " isInternalExecution=" + isInternalExecution + "\n " +
                                "Query should succeed is: ",
                                shouldBeAbleToRunQuery,
                                query.getQueryReturnValue().getSucceeded());

                        SessionDataContainer.getInstance().removeSessionOnLogout(sessionId);
                    }
                }
            }
        }
    }

    @Test
    public void testGetUserID() {
        DbUser user = mock(DbUser.class);
        when(user.getId()).thenReturn(Guid.EVERYONE);
        String session = UUID.randomUUID().toString();
        SessionDataContainer.getInstance().setUser(session, user);
        VdcQueryParametersBase params = new VdcQueryParametersBase(session);
        params.setRefresh(false);
        ThereIsNoSuchQuery query = new ThereIsNoSuchQuery(params);


        assertEquals("wrong guid", Guid.EVERYONE, query.getUserID());
    }

    @Test
    public void testGetUserIDWithNoUser() {
        ThereIsNoSuchQuery query = new ThereIsNoSuchQuery(new VdcQueryParametersBase());

        assertEquals("wrong guid", null, query.getUserID());
    }

    /* Test Utilities */

    @Before
    @After
    public void clearSession() {
        ThreadLocalParamsContainer.clean();
    }

    /** A stub class that will cause the {@link VdcQueryType#Unknown} to be used */
    private static class ThereIsNoSuchQuery extends QueriesCommandBase<VdcQueryParametersBase> {

        public ThereIsNoSuchQuery(VdcQueryParametersBase parameters) {
            super(parameters);
        }

        @Override
        protected void executeQueryCommand() {
            // Stub method, do nothing
        }
    }
}
