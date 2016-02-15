package org.ovirt.engine.core.common.businessentities;

import java.util.HashMap;

public enum StorageType implements Identifiable {

    UNKNOWN(0, Subtype.NONE),
    NFS(1, Subtype.FILE),
    FCP(2, Subtype.BLOCK),
    ISCSI(3, Subtype.BLOCK),
    LOCALFS(4, Subtype.FILE),
    POSIXFS(6, Subtype.FILE),
    GLUSTERFS(7, Subtype.FILE),
    GLANCE(8, Subtype.FILE);

    private enum Subtype { NONE, FILE, BLOCK }

    private int value;
    private Subtype subtype;

    private static final HashMap<Integer, StorageType> mappings = new HashMap<Integer, StorageType>();

    static {
        for (StorageType storageType : values()) {
            mappings.put(storageType.getValue(), storageType);
        }
    }

    private StorageType(int value, Subtype subtype) {
        this.value = value;
        this.subtype = subtype;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    public Subtype getStorageSubtype() {
        return subtype;
    }

    public boolean isConcreteStorageType() {
        return subtype != Subtype.NONE;
    }

    public static StorageType forValue(int value) {
        return mappings.get(value);
    }

    public boolean isFileDomain() {
        return subtype == Subtype.FILE;
    }

    public boolean isBlockDomain() {
        return subtype == Subtype.BLOCK;
    }

    public boolean isLocal() {
        return this == LOCALFS;
    }
}
