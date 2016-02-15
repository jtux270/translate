package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;

public enum RaidType {
    NONE("-1"),
    RAID0("0"),
    RAID6("6"),
    RAID10("10");

    private String value;
    private static final HashMap<String, RaidType> mappings = new HashMap<String, RaidType>();

    static {
        for (RaidType raidType : values()) {
            mappings.put(raidType.getValue(), raidType);
        }
    }

    private RaidType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RaidType forValue(String value) {
        return mappings.get(value);
    }

}
