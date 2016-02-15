package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.ovirt.engine.core.common.TimeZoneType;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.queries.TimeZoneQueryParams;

public class GetDefaultTimeZoneQueryTest extends AbstractSysprepQueryTest<TimeZoneQueryParams, GetDefaultTimeZoneQuery<TimeZoneQueryParams>> {

    /** The default time zone for the test */
    private static final String DEFAULT_WINDOWS_TIME_ZONE = "Israel Standard Time";
    private static final String DEFAULT_GENERAL_TIME_ZONE = "Asia/Jerusalem";

    @Test
    public void testQueryDefaultWindowsTimeZone() {
        mcr.mockConfigValue(ConfigValues.DefaultWindowsTimeZone, DEFAULT_WINDOWS_TIME_ZONE);
        when(getQueryParameters().getTimeZoneType()).thenReturn(TimeZoneType.WINDOWS_TIMEZONE);
        getQuery().executeQueryCommand();

        @SuppressWarnings("unchecked")
        String result = (String) getQuery().getQueryReturnValue().getReturnValue();

        assertTrue("Wrong default time zone: " + result, result.equals(DEFAULT_WINDOWS_TIME_ZONE));
    }

    @Test
    public void testQueryDefaultGeneralTimeZone() {
        mcr.mockConfigValue(ConfigValues.DefaultGeneralTimeZone, DEFAULT_GENERAL_TIME_ZONE);
        when(getQueryParameters().getTimeZoneType()).thenReturn(TimeZoneType.GENERAL_TIMEZONE);
        getQuery().executeQueryCommand();

        @SuppressWarnings("unchecked")
        String result = (String) getQuery().getQueryReturnValue().getReturnValue();

        assertTrue("Wrong default time zone: " + result, result.equals(DEFAULT_GENERAL_TIME_ZONE));
    }
}
