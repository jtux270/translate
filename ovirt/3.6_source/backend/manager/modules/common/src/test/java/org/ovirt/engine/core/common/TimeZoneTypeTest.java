package org.ovirt.engine.core.common;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.WindowsJavaTimezoneMapping;

public class TimeZoneTypeTest {

    private static final TimeZoneType windows = TimeZoneType.WINDOWS_TIMEZONE;
    private static final TimeZoneType general = TimeZoneType.GENERAL_TIMEZONE;

    @Test
    public void testWindowsTimeZones() {
        Map<String, String> map = windows.getTimeZoneList();
        assertNotNull(map);
        assertEquals(map.get("GMT Standard Time"), "(GMT) GMT Standard Time");
    }

    @Test
    public void testGeneralTimeZones() {
        Map<String, String> map = general.getTimeZoneList();
        assertNotNull(map);
        assertFalse(map.get("Asia/Riyadh78") != null);
        assertEquals(map.get("Asia/Jerusalem"), "(GMT+02:00) Israel Standard Time");
    }

    @Test
    public void testDefaultConfigValues() {
        assertEquals(windows.getDefaultTimeZoneConfigKey(), ConfigValues.DefaultWindowsTimeZone);
        assertEquals(general.getDefaultTimeZoneConfigKey(), ConfigValues.DefaultGeneralTimeZone);
    }

    @Test
    public void testDefaultConfigurationValues() {
        assertEquals(windows.getDefaultTimeZoneConfigurationKey(), ConfigurationValues.DefaultWindowsTimeZone);
        assertEquals(general.getDefaultTimeZoneConfigurationKey(), ConfigurationValues.DefaultGeneralTimeZone);
    }

    @Test
    public void testUltimateFallbackValues() {
        assertEquals(windows.getUltimateFallback(), "GMT Standard Time");
        assertEquals(general.getUltimateFallback(), "Etc/GMT");
    }

    @Test
    public void testWindowsTimeZonesKeys() {
        final Set<String> windowsTimeZoneKeys = WindowsJavaTimezoneMapping.windowsToJava.keySet();
        for (String timeZoneKey : TimeZoneType.WINDOWS_TIMEZONE.getTimeZoneList().keySet()) {
            assertThat(windowsTimeZoneKeys, hasItem(timeZoneKey));
        }
    }
}
