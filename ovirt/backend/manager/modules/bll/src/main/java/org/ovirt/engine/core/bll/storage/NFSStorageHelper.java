package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.common.businessentities.StorageType;

public class NFSStorageHelper extends BaseFsStorageHelper {

    @Override
    protected StorageType getType() {
        return StorageType.NFS;
    }
}
