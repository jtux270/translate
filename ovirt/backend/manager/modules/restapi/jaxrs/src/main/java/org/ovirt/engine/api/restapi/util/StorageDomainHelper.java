package org.ovirt.engine.api.restapi.util;

import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;

public class StorageDomainHelper {


    public static StorageServerConnections getConnection(StorageType storageType, String address, String target, String userName, String password, Integer port) {
        return new StorageServerConnections(address,
                    null,
                    target,
                    password,
                    storageType,
                    userName,
                    port==null ? null : Integer.toString(port),
                    StorageServerConnections.DEFAULT_TPGT);
    }
}
