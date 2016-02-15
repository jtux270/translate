package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.QuotaDAO;

/**
 * A test for the {@link GetQuotaStorageByQuotaIdQuery} class.
 * This is a flow test that uses mocking to verify the correct DAOs are called.
 */
public class GetQuotaStorageByQuotaIdQueryTest
        extends AbstractQueryTest<IdQueryParameters, GetQuotaStorageByQuotaIdQuery<IdQueryParameters>> {

    @Test
    public void testExecuteQueryCommand() {
        // Mock the parameters
        Guid quotaId = Guid.newGuid();
        when(params.getId()).thenReturn(quotaId);

        // Create the return value
        QuotaStorage group = new QuotaStorage();
        group.setQuotaId(quotaId);

        // Mock the DAO
        QuotaDAO quotaDAO = mock(QuotaDAO.class);
        when(quotaDAO.getQuotaStorageByQuotaGuidWithGeneralDefault(quotaId)).thenReturn(Collections.singletonList(group));
        when(getDbFacadeMockInstance().getQuotaDao()).thenReturn(quotaDAO);

        // Execute the query
        getQuery().executeQueryCommand();

        // Assert the result
        @SuppressWarnings("unchecked")
        List<QuotaStorage> results = (List<QuotaStorage>) getQuery().getQueryReturnValue().getReturnValue();
        assertEquals("Wrong number of results returned", 1, results.size());
        assertEquals("Wrong results returned", group, results.get(0));
    }
}
