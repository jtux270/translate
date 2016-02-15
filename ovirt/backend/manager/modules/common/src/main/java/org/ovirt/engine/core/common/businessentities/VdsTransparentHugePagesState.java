package org.ovirt.engine.core.common.businessentities;

import java.util.EnumSet;
import java.util.HashMap;

public enum VdsTransparentHugePagesState {

    Never(0),
    MAdvise(1),
    Always(2);

    private int intValue;
    private static final HashMap<Integer, VdsTransparentHugePagesState> mappings =
            new HashMap<Integer, VdsTransparentHugePagesState>();

    static {
        for (VdsTransparentHugePagesState status : EnumSet.allOf(VdsTransparentHugePagesState.class)) {
            mappings.put(status.getValue(), status);
        }
    }

    private VdsTransparentHugePagesState(int value) {
        intValue = value;
    }

    public int getValue() {
        return intValue;
    }

    public static VdsTransparentHugePagesState forValue(int value) {
        return mappings.get(value);
    }
}
