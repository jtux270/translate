package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.Guid;

import java.util.HashMap;
import java.util.Map;

public class ExtendVmDiskSizeVDSCommandParameters extends VdsAndVmIDVDSParametersBase {

    private long newSize;
    private Guid storagePoolId;
    private Guid storageDomainId;
    private Guid imageGroupId;
    private Guid imageId;

    public ExtendVmDiskSizeVDSCommandParameters(Guid vdsId,
                                                Guid vmId,
                                                Guid storagePoolId,
                                                Guid storageDomainId,
                                                Guid imageId,
                                                Guid imageGroupId,
                                                long newSize) {
        super(vdsId, vmId);
        this.storagePoolId = storagePoolId;
        this.storageDomainId = storageDomainId;
        this.imageId = imageId;
        this.imageGroupId = imageGroupId;
        this.newSize = newSize;
    }

    public ExtendVmDiskSizeVDSCommandParameters() {
    }

    public long getNewSize() {
        return newSize;
    }

    public void setNewSize(long newSize) {
        this.newSize = newSize;
    }

    public Guid getStoragePoolId() {
        return storagePoolId;
    }

    public void setStoragePoolId(Guid storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public Guid getStorageDomainId() {
        return storageDomainId;
    }

    public void setStorageDomainId(Guid storageDomainId) {
        this.storageDomainId = storageDomainId;
    }

    public Guid getImageGroupId() {
        return imageGroupId;
    }

    public void setImageGroupId(Guid imageGroupId) {
        this.imageGroupId = imageGroupId;
    }

    public Guid getImageId() {
        return imageId;
    }

    public void setImageId(Guid imageId) {
        this.imageId = imageId;
    }

    public Map<String, String> getDriveSpecs() {
        Map<String, String> driveSpecs = new HashMap<String, String>();
        driveSpecs.put("poolID", getStoragePoolId().toString());
        driveSpecs.put("domainID", getStorageDomainId().toString());
        driveSpecs.put("imageID", getImageGroupId().toString());
        driveSpecs.put("volumeID", getImageId().toString());
        return driveSpecs;
    }
}
