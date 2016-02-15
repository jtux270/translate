package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;
import java.util.Map;

public enum PropagateErrors {
    Off(0),
    On(1);

    private int intValue;
    private static Map<Integer, PropagateErrors> mappings;

    static {
        mappings = new HashMap<Integer, PropagateErrors>();
        for (PropagateErrors error : values()) {
            mappings.put(error.getValue(), error);
        }
    }

    private PropagateErrors(int value) {
        intValue = value;
    }

    public int getValue() {
        return intValue;
    }

    public static PropagateErrors forValue(int value) {
        return mappings.get(value);
    }
}
