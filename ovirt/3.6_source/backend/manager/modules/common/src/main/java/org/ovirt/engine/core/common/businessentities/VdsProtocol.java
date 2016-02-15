package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;
import java.util.Map;

/**
 * Determines which protocol is used during connection to VDSM.
 *
 */
public enum VdsProtocol {
    XML(0),
    STOMP(1),
    AMQP(2);

    private static final Map<Integer, VdsProtocol> MAPPING = new HashMap<Integer, VdsProtocol>();
    static {
        for (VdsProtocol protocol : VdsProtocol.values()) {
            MAPPING.put(protocol.value, protocol);
        }
    }

    private Integer value;

    private VdsProtocol(Integer value) {
        this.value = value;
    }

    public String value() {
        return name().toLowerCase();
    }

    public Integer getValue() {
        return this.value;
    }

    public static VdsProtocol fromValue(Integer value) {
        return MAPPING.get(value);
    }

    public static VdsProtocol fromValue(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
