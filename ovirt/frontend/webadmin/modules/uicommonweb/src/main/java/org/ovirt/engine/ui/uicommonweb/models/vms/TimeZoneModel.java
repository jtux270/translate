package org.ovirt.engine.ui.uicommonweb.models.vms;

import org.ovirt.engine.core.common.TimeZoneType;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class TimeZoneModel {
    private static final Map<TimeZoneType, Collection<TimeZoneModel>> cachedTimeZoneModels = new HashMap<TimeZoneType, Collection<TimeZoneModel>>();

    public static Collection<TimeZoneModel> getTimeZones(TimeZoneType timeZoneType) {
        return cachedTimeZoneModels.get(timeZoneType);
    }

    static {
        for (TimeZoneType timeZoneType : TimeZoneType.values()) {
            mapListModels(timeZoneType, timeZoneType.getTimeZoneList());
        }
    }

    private static void mapListModels(TimeZoneType timeZoneType, Map<String, String> timeZones) {
        List<TimeZoneModel> models = new ArrayList<TimeZoneModel>();
        models.add(new TimeZoneModel(null, timeZoneType)); // add empty field representing default engine TZ
        for (Map.Entry<String, String> entry : timeZones.entrySet()) {
            models.add(new TimeZoneModel(entry.getKey(), timeZoneType));
        }
        cachedTimeZoneModels.put(timeZoneType, models);
    }

    private final String timeZoneKey;
    private final TimeZoneType timeZoneType;

    public TimeZoneModel(String timeZoneKey, TimeZoneType timeZoneType) {
        this.timeZoneKey = timeZoneKey;
        this.timeZoneType = timeZoneType;

    }

    public String getTimeZoneKey() {
        return timeZoneKey;
    }

    public boolean isDefault() {
        return timeZoneKey == null;
    }

    public String getDisplayValue() {
        if (isDefault()) {
            String defaultTimeZoneKey = (String) AsyncDataProvider.getConfigValuePreConverted(timeZoneType.getDefaultTimeZoneConfigurationKey());
            // check if default timezone is correct
            if (!timeZoneType.getTimeZoneList().containsKey(defaultTimeZoneKey)) {
                // if not show GMT
                defaultTimeZoneKey = timeZoneType.getUltimateFallback();
            }
            return timeZoneType.getTimeZoneList().get(defaultTimeZoneKey);
        } else {
            return timeZoneType.getTimeZoneList().get(timeZoneKey);
        }
    }
}
