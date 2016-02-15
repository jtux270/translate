package org.ovirt.engine.api.model;

import org.apache.commons.lang.StringUtils;

public enum WatchdogAction {
    NONE,
    RESET,
    POWEROFF,
    PAUSE,
    DUMP;
    public String value() {
        return this.name().toLowerCase();
    }

    public static WatchdogAction fromValue(String value) {
        try {
            return valueOf(StringUtils.upperCase(value));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
