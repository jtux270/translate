package org.ovirt.engine.core.common.action;

import java.util.Date;

import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.compat.Guid;

public class ImagesActionsParametersBase extends StorageDomainParametersBase {
    private static final long serialVersionUID = -5791892465249711608L;

    private Guid imageId;
    private Guid destinationImageId;
    private String diskAlias;
    private String description;
    private Date oldLastModifiedValue;
    private Guid vmSnapshotId;
    private Guid imageGroupID;
    private boolean importEntity;
    private boolean leaveLocked;

    public ImagesActionsParametersBase() {
        imageId = Guid.Empty;
        destinationImageId = Guid.Empty;
        imageGroupID = Guid.Empty;
    }

    public ImagesActionsParametersBase(Guid imageId) {
        super(Guid.Empty);
        setEntityInfo(new EntityInfo(VdcObjectType.Disk, imageId));
        this.imageId = imageId;
        destinationImageId = Guid.Empty;
        imageGroupID = Guid.Empty;
    }

    public Guid getImageId() {
        return imageId;
    }

    /**
     * Needed in order to be able to deserialize this field.
     */
    protected void setImageId(Guid imageId) {
        this.imageId = imageId;
    }

    public Guid getDestinationImageId() {
        return destinationImageId;
    }

    public void setDestinationImageId(Guid value) {
        destinationImageId = value;
    }

    public String getDiskAlias() {
        return diskAlias;
    }

    public void setDiskAlias(String diskAlias) {
        this.diskAlias = diskAlias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        description = value;
    }

    public void setOldLastModifiedValue(Date oldLastModifiedValue) {
        this.oldLastModifiedValue = oldLastModifiedValue;
    }

    public Date getOldLastModifiedValue() {
        return oldLastModifiedValue;
    }

    public Guid getVmSnapshotId() {
        return vmSnapshotId;
    }

    public void setVmSnapshotId(Guid value) {
        vmSnapshotId = value;
    }

    public Guid getImageGroupID() {
        return imageGroupID;
    }

    public void setImageGroupID(Guid value) {
        imageGroupID = value;
    }

    public boolean isImportEntity() {
        return importEntity;
    }

    public void setImportEntity(boolean importEntity) {
        this.importEntity = importEntity;
    }

    public boolean isLeaveLocked() {
        return leaveLocked;
    }

    public void setLeaveLocked(boolean leaveLocked) {
        this.leaveLocked = leaveLocked;
    }
}
