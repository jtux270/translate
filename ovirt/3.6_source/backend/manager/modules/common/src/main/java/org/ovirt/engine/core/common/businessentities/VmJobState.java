package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;

public enum VmJobState implements Identifiable {
    UNKNOWN(0),
    NORMAL(1);

    private final int jobState;
    private static final HashMap<Integer, VmJobState> mappings = new HashMap<Integer, VmJobState>();

    static {
        for (VmJobState component : values()) {
            mappings.put(component.getValue(), component);
        }
    }

    public static VmJobState getByName(String name) {
        if (name == null || name.length() == 0) {
            return null;
        } else {
            for (VmJobState vmJobState : VmJobState.values()) {
                if (vmJobState.name().equalsIgnoreCase(name)) {
                    return vmJobState;
                }
            }
        }
        return null;
    }

    public static VmJobState forValue(int value) {
        return mappings.get(value);
    }

    private VmJobState(int jobState) {
        this.jobState = jobState;
    }

    @Override
    public int getValue() {
        return jobState;
    }
}
