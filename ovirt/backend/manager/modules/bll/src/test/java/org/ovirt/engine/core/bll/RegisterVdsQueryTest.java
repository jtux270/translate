package org.ovirt.engine.core.bll;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.ovirt.engine.core.common.queries.RegisterVdsParameters;

/**
 * A test case for {@link RegisterVdsQuery}.
 */
public class RegisterVdsQueryTest extends AbstractUserQueryTest<RegisterVdsParameters, RegisterVdsQuery<RegisterVdsParameters>> {

    /**
     * A test for checking whether {@link RegisterVdsQuery#getStrippedVdsUniqueId()} method returns a valid VDS id which
     * contains only the valid characters. ^ is used as invalid character as defined in RegisterVdsQuery.
     */
    @Test
    public void testGetStrippedVdsUniqueIdWithUnacceptedChars() {
        String result = this.gerStrippedVdsUniqueId("Test_123");
        assertEquals("Vds id doesn't equal to the expected value", "Test_123", result);
    }

    /**
     * A test for checking whether {@link RegisterVdsQuery#getStrippedVdsUniqueId()} method returns a valid VDS id when
     * containing no invalid characters.
     */
    @Test
    public void testGetStrippedVdsUniqueIdOnlyAllowrdChars() {
        String result = this.gerStrippedVdsUniqueId("Test_123");
        assertEquals("Vds id doesn't equal to the expected value", "Test_123", result);
    }

    /**
     * A test for checking whether {@link RegisterVdsQuery#getStrippedVdsUniqueId()} method returns an empty VDS id when
     * containing only invalid characters.
     */
    @Test
    public void testGetStrippedVdsUniqueIdWithoutValidChars() {
        String result = this.gerStrippedVdsUniqueId("^%^");
        assertEquals("Vds id is not empty as expected", "", result);
    }

    private String gerStrippedVdsUniqueId(String vdsId) {
        RegisterVdsParameters paramsMock = getQueryParameters();
        when(paramsMock.getVdsUniqueId()).thenReturn(vdsId);
        return getQuery().getStrippedVdsUniqueId();
    }
}
