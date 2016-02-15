package org.ovirt.engine.core.notifier.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class NotificationPropertiesTest {

    private static NotificationProperties prop = null;

    @BeforeClass
    static public void beforeClass() throws UnsupportedEncodingException {
        NotificationProperties.release();
        NotificationProperties.setDefaults(
                URLDecoder.decode(ClassLoader.getSystemResource("conf/notifier-prop-test.conf").getPath(), "UTF-8"),
                ""
        );
        prop = NotificationProperties.getInstance();
        assertNotNull(prop);
    }

    @Test
    public void testProperties() {
        assertEquals(60, prop.getLong(NotificationProperties.INTERVAL_IN_SECONDS));
    }
}

