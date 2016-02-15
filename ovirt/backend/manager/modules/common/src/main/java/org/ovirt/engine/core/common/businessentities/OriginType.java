package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;
import java.util.Map;

public enum OriginType {
    RHEV(0),
    VMWARE(1),
    XEN(2),
    OVIRT(3),
    // VMs that externally run on the host (not created by the engine)
    EXTERNAL(4),
    // VMs that were created by the hosted engine setup
    HOSTED_ENGINE(5);

    private int intValue;
    private static Map<Integer, OriginType> mappings;

    static {
        mappings = new HashMap<Integer, OriginType>();
        for (OriginType error : values()) {
            mappings.put(error.getValue(), error);
        }
    }

    private OriginType(int value) {
        intValue = value;
    }

    public int getValue() {
        return intValue;
    }

    public static OriginType forValue(int value) {
        return mappings.get(value);
    }
}
