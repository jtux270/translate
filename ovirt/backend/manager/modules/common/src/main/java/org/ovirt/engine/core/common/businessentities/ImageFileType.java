package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;
import java.util.Map;

public enum ImageFileType {
    Unknown(0),
    ISO(1),
    Floppy(2),
    Disk(3),
    All(4);

    private int intValue;
    private static Map<Integer, ImageFileType> mappings;

    static {
        mappings = new HashMap<Integer, ImageFileType>();
        for (ImageFileType error : values()) {
            mappings.put(error.getValue(), error);
        }
    }

    private ImageFileType(int value) {
        intValue = value;
    }

    public int getValue() {
        return intValue;
    }

    public static ImageFileType forValue(int value) {
        return mappings.get(value);
    }
}
