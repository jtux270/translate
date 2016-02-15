package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.compat.Guid;

public class ImportRepoImageParameters extends ImagesActionsParametersBase {

    private static final long serialVersionUID = 8168949491104775480L;

    private String sourceRepoImageId;

    private DiskImage diskImage;

    private Guid sourceStorageDomainId;
    private Guid clusterId;
    private boolean importAsTemplate;

    public String getSourceRepoImageId() {
        return sourceRepoImageId;
    }

    public void setSourceRepoImageId(String sourceRepoImageId) {
        this.sourceRepoImageId = sourceRepoImageId;
    }

    public void setImportAsTemplate(boolean importAsTemplate) {
        this.importAsTemplate = importAsTemplate;
    }

    public boolean getImportAsTemplate() {
        return importAsTemplate;
    }

    public Guid getSourceStorageDomainId() {
        return sourceStorageDomainId;
    }

    public void setSourceStorageDomainId(Guid sourceStorageDomainId) {
        this.sourceStorageDomainId = sourceStorageDomainId;
    }

    public Guid getClusterId() {
        return clusterId;
    }

    public void setClusterId(Guid clusterId) {
        this.clusterId = clusterId;
    }

    public DiskImage getDiskImage() {
        return diskImage;
    }

    public void setDiskImage(DiskImage diskImage) {
        this.diskImage = diskImage;
    }

}
