package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.Disks;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageType;
import org.ovirt.engine.api.resource.DisksResource;
import org.ovirt.engine.api.resource.MovableCopyableDiskResource;
import org.ovirt.engine.api.restapi.logging.Messages;
import org.ovirt.engine.api.restapi.resource.utils.DiskResourceUtils;
import org.ovirt.engine.api.restapi.types.DiskMapper;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendDisksResource extends AbstractBackendCollectionResource<Disk, org.ovirt.engine.core.common.businessentities.storage.Disk> implements DisksResource{

    static final String[] SUB_COLLECTIONS = { "permissions", "statistics" };
    public BackendDisksResource() {
        super(Disk.class, org.ovirt.engine.core.common.businessentities.storage.Disk.class, SUB_COLLECTIONS);
    }

    @Override
    public Response add(Disk disk) {
        validateDiskForCreation(disk);
        AddDiskParameters params = new AddDiskParameters();
        Guid storageDomainId = getStorageDomainId(disk);
        params.setStorageDomainId(storageDomainId);
        if (storageDomainId != null) {
            updateStorageTypeForDisk(disk, storageDomainId);
        }
        params.setDiskInfo(getMapper(Disk.class, org.ovirt.engine.core.common.businessentities.storage.Disk.class).map(disk, null));
        if (disk.isSetLunStorage() && disk.getLunStorage().isSetHost()) {
            params.setVdsId(getHostId(disk.getLunStorage().getHost()));
        }
        return performCreate(VdcActionType.AddDisk, params,
                new QueryIdResolver<Guid>(VdcQueryType.GetDiskByDiskId, IdQueryParameters.class));
    }

    private void updateStorageTypeForDisk(Disk disk, Guid storageDomainId) {
        org.ovirt.engine.core.common.businessentities.StorageDomain storageDomain = getStorageDomainById(storageDomainId);
        if (storageDomain != null) {
            disk.setStorageType(DiskMapper.map(storageDomain.getStorageDomainType()).value());
        }
    }

    private Guid getStorageDomainId(Disk disk) {
        if (disk.isSetStorageDomains() && disk.getStorageDomains().isSetStorageDomains()
                && disk.getStorageDomains().getStorageDomains().get(0).isSetId()) {
            return asGuid(disk.getStorageDomains().getStorageDomains().get(0).getId());
        } else if (disk.isSetStorageDomains() && disk.getStorageDomains().getStorageDomains().get(0).isSetName()) {
            Guid storageDomainId = getStorageDomainIdByName(disk.getStorageDomains().getStorageDomains().get(0).getName());
            if (storageDomainId == null) {
                notFound(StorageDomain.class);
            } else {
                return storageDomainId;
            }
        }
        return null;
    }

    private org.ovirt.engine.core.common.businessentities.StorageDomain getStorageDomainById(Guid id) {
        return getEntity(org.ovirt.engine.core.common.businessentities.StorageDomain.class, VdcQueryType.GetStorageDomainById, new IdQueryParameters(id), id.toString());
    }

    protected void validateDiskForCreation(Disk disk) {
        validateParameters(disk, 2, "interface");
        if (DiskResourceUtils.isLunDisk(disk)) {
            validateParameters(disk.getLunStorage(), 3, "type"); // when creating a LUN disk, user must specify type.
            StorageType storageType = StorageType.fromValue(disk.getLunStorage().getType());
            if (storageType != null && storageType == StorageType.ISCSI) {
                validateParameters(disk.getLunStorage().getLogicalUnits().get(0), 3, "address", "target", "port", "id");
            }
        } else if (disk.isSetLunStorage() && disk.getLunStorage().getLogicalUnits().isEmpty()) {
            // TODO: Implement nested entity existence validation infra for validateParameters()
            throw new WebFaultException(null,
                                        localize(Messages.INCOMPLETE_PARAMS_REASON),
                                        localize(Messages.INCOMPLETE_PARAMS_DETAIL_TEMPLATE, "LogicalUnit", "", "add"),
                                        Response.Status.BAD_REQUEST);
        } else {
            validateParameters(disk, 2, "provisionedSize|size", "format"); // Non lun disks require size and format
        }
        validateEnums(Disk.class, disk);
    }

    private Guid getStorageDomainIdByName(String storageDomainName) {
        List<org.ovirt.engine.core.common.businessentities.StorageDomain> storageDomains =
                getBackendCollection(org.ovirt.engine.core.common.businessentities.StorageDomain.class,
                        VdcQueryType.GetAllStorageDomains,
                        new VdcQueryParametersBase());
        for (org.ovirt.engine.core.common.businessentities.StorageDomain storageDomain : storageDomains) {
            if (storageDomain.getStorageName().equals(storageDomainName)) {
                return storageDomain.getId();
            }
        }
        return null;
    }

    @Override
    public Disks list() {
        if (isFiltered()) {
            return mapCollection(getBackendCollection(VdcQueryType.GetAllDisks, new VdcQueryParametersBase()));
        } else {
            return mapCollection(getBackendCollection(SearchType.Disk));
        }
    }

    @Override
    public MovableCopyableDiskResource getDeviceSubResource(String id) {
        return inject(new BackendDiskResource(id));
    }

    protected Disks mapCollection(List<org.ovirt.engine.core.common.businessentities.storage.Disk> entities) {
        Disks collection = new Disks();
        for (org.ovirt.engine.core.common.businessentities.storage.Disk disk : entities) {
            collection.getDisks().add(addLinks(populate(map(disk), disk)));
        }
        return collection;
    }
}
