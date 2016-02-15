package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;
import java.util.Map;

public enum StorageFormatType {

    V1("0"),
    V2("2"),
    V3("3");

    private final String value;
    private final static Map<String, StorageFormatType> mappings = new HashMap<String, StorageFormatType>();
    static {
        for (StorageFormatType storageDomainFormat : values()) {
            mappings.put(storageDomainFormat.getValue(), storageDomainFormat);
        }
    }

    private StorageFormatType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StorageFormatType forValue(String value) {
        return mappings.get(value);
    }

}
