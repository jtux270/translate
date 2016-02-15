package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;
import java.util.Map;

public enum ScsiGenericIO {
    FILTERED(1),
    UNFILTERED(2);

    private int intValue;
    private static Map<Integer, ScsiGenericIO> mappings;

    static {
        mappings = new HashMap<Integer, ScsiGenericIO>();
        for (ScsiGenericIO error : values()) {
            mappings.put(error.getValue(), error);
        }
    }

    private ScsiGenericIO(int value) {
        intValue = value;
    }

    public int getValue() {
        return intValue;
    }

    public static ScsiGenericIO forValue(int value) {
        return mappings.get(value);
    }
}
