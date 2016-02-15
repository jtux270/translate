package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.resource.CreationResource;
import org.ovirt.engine.api.resource.StorageDomainContentDiskResource;

public class BackendExportDomainDiskResource
        extends AbstractBackendSubResource<Disk, org.ovirt.engine.core.common.businessentities.Disk>
        implements StorageDomainContentDiskResource {

    private final BackendExportDomainDisksResource parent;
    private final String diskId;

    public BackendExportDomainDiskResource(
            String diskId,
            BackendExportDomainDisksResource parent) {
        super(diskId, Disk.class, org.ovirt.engine.core.common.businessentities.Disk.class);
        this.parent = parent;
        this.diskId = diskId;
    }

    @Override
    protected Disk doPopulate(Disk model, org.ovirt.engine.core.common.businessentities.Disk entity) {
        return model;
    }

    @Override
    public Disk get() {
        org.ovirt.engine.core.common.businessentities.Disk disk = parent.getDisk(asGuid(diskId));
        if (disk == null) {
            return notFound();
        }
        return map(disk);
    }

    @Override
    public CreationResource getCreationSubresource(String ids) {
        return inject(new BackendCreationResource(ids));
    }

    public BackendExportDomainDisksResource getParent() {
        return parent;
    }

}
