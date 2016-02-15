package org.ovirt.engine.ui.uicommonweb.validation;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class HostWithProtocolAndPortAddressValidationTest {

    @Test
    public void onlyHostname() {
        assertTrue(new TestableHostWithProtocolAndPortAddressValidation().validate("someHostname").getSuccess()); //$NON-NLS-1$
    }

    @Test
    public void hostnameWithProtocol() {
        assertTrue(new TestableHostWithProtocolAndPortAddressValidation().validate("Xasd://someHostname").getSuccess()); //$NON-NLS-1$
    }

    @Test
    public void hostnameShortPort() {
        assertTrue(new TestableHostWithProtocolAndPortAddressValidation().validate("someHostname:1").getSuccess()); //$NON-NLS-1$
    }

    @Test
    public void hostnameNormalPort() {
        assertTrue(new TestableHostWithProtocolAndPortAddressValidation().validate("someHostname:4040").getSuccess()); //$NON-NLS-1$
    }

    @Test
    public void hostnameLongPort() {
        assertTrue(new TestableHostWithProtocolAndPortAddressValidation().validate("someHostname:65535").getSuccess()); //$NON-NLS-1$
    }

    @Test
    public void fullCorrect() {
        assertTrue(new TestableHostWithProtocolAndPortAddressValidation().validate("someProtocol://someHostname:4040").getSuccess()); //$NON-NLS-1$
    }

    @Test
    public void hostnameTooLongPort() {
        assertFalse(new TestableHostWithProtocolAndPortAddressValidation().validate("someHostname:655359").getSuccess()); //$NON-NLS-1$
    }

    @Test
    public void incorrect() {
        assertFalse(new TestableHostWithProtocolAndPortAddressValidation().validate("someHostname:").getSuccess()); //$NON-NLS-1$
        assertFalse(new TestableHostWithProtocolAndPortAddressValidation().validate("://someHostname").getSuccess()); //$NON-NLS-1$
        assertFalse(new TestableHostWithProtocolAndPortAddressValidation().validate("so m eHostname").getSuccess()); //$NON-NLS-1$
        assertFalse(new TestableHostWithProtocolAndPortAddressValidation().validate("asd:/someHostname").getSuccess()); //$NON-NLS-1$
        assertFalse(new TestableHostWithProtocolAndPortAddressValidation().validate("asd:someHostname").getSuccess()); //$NON-NLS-1$
        assertFalse(new TestableHostWithProtocolAndPortAddressValidation().validate("someHostname:abc").getSuccess()); //$NON-NLS-1$
    }

    class TestableHostWithProtocolAndPortAddressValidation extends HostWithProtocolAndPortAddressValidation {
        @Override
        protected String composeMessage() {
            return ""; //$NON-NLS-1$
        }
    }
}
