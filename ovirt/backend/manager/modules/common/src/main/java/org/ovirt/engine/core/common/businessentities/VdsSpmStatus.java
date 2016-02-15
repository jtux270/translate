package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;

public enum VdsSpmStatus {
    None(0),
    Contending(1),
    SPM(2);

    private int intValue;
    private static final HashMap<Integer, VdsSpmStatus> mappings = new HashMap<Integer, VdsSpmStatus>();

    static {
        for (VdsSpmStatus vdsSpmStatus : values()) {
            mappings.put(vdsSpmStatus.getValue(), vdsSpmStatus);
        }
    }

    private VdsSpmStatus(int value) {
        intValue = value;
    }

    public int getValue() {
        return intValue;
    }

    public static VdsSpmStatus forValue(int value) {
        return mappings.get(value);
    }
}
