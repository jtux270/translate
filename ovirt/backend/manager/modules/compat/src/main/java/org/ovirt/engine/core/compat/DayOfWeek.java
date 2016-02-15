package org.ovirt.engine.core.compat;

import java.util.HashMap;

public enum DayOfWeek {
    // Summary:
    // Indicates Sunday.
    Sunday(0),
    //
    // Summary:
    // Indicates Monday.
    Monday(1),
    //
    // Summary:
    // Indicates Tuesday.
    Tuesday(2),
    //
    // Summary:
    // Indicates Wednesday.
    Wednesday(3),
    //
    // Summary:
    // Indicates Thursday.
    Thursday(4),
    //
    // Summary:
    // Indicates Friday.
    Friday(5),
    //
    // Summary:
    // Indicates Saturday.
    Saturday(6);

    private int intValue;
    private static HashMap<Integer, DayOfWeek> mappings;

    private synchronized static HashMap<Integer, DayOfWeek> getMappings() {
        if (mappings == null) {
            mappings = new HashMap<Integer, DayOfWeek>();
        }
        return mappings;
    }

    private DayOfWeek(int value) {
        intValue = value;
        DayOfWeek.getMappings().put(value, this);
    }

    public int getValue() {
        return intValue;
    }

    public static DayOfWeek forValue(int value) {
        return getMappings().get(value);
    }

}
