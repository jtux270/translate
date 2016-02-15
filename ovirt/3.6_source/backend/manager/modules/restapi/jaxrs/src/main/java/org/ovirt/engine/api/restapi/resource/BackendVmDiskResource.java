package org.ovirt.engine.api.restapi.resource;

import static org.ovirt.engine.api.restapi.resource.BackendVmDisksResource.SUB_COLLECTIONS;

import java.util.Collections;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.Disks;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.AssignedPermissionsResource;
import org.ovirt.engine.api.resource.StatisticsResource;
import org.ovirt.engine.api.resource.VmDiskResource;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AttachDetachVmDiskParameters;
import org.ovirt.engine.core.common.action.ExportRepoImageParameters;
import org.ovirt.engine.core.common.action.HotPlugDiskToVmParameters;
import org.ovirt.engine.core.common.action.MoveDiskParameters;
import org.ovirt.engine.core.common.action.MoveDisksParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.GetPermissionsForObjectParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmDiskResource
    extends BackendDeviceResource<Disk, Disks, org.ovirt.engine.core.common.businessentities.storage.Disk>
    implements VmDiskResource {

    private Guid vmId;

    protected BackendVmDiskResource(
            Guid vmId,
            String diskId,
            AbstractBackendReadOnlyDevicesResource<Disk, Disks, org.ovirt.engine.core.common.businessentities.storage.Disk> collection,
            VdcActionType updateType,
            ParametersProvider<Disk, org.ovirt.engine.core.common.businessentities.storage.Disk> updateParametersProvider,
            String[] requiredUpdateFields,
            String... subCollections) {
        super(
            Disk.class,
            org.ovirt.engine.core.common.businessentities.storage.Disk.class,
            collection.asGuidOr404(diskId),
            collection,
            updateType,
            updateParametersProvider,
            requiredUpdateFields,
            SUB_COLLECTIONS
        );
        this.vmId = vmId;
    }

    @Override
    public StatisticsResource getStatisticsResource() {
        EntityIdResolver<Guid> resolver = new EntityIdResolver<Guid>() {
            @Override
            public org.ovirt.engine.core.common.businessentities.storage.Disk lookupEntity(
                    Guid guid) throws BackendFailureException {
                return collection.lookupEntity(guid);
            }
        };
        DiskStatisticalQuery query = new DiskStatisticalQuery(resolver, newModel(id));
        return inject(new BackendStatisticsResource<>(entityType, guid, query));
    }

    @Override
    protected Disk doPopulate(Disk model, org.ovirt.engine.core.common.businessentities.storage.Disk entity) {
        return collection.doPopulate(model, entity);
    }

    @Override
    protected Disk deprecatedPopulate(Disk model, org.ovirt.engine.core.common.businessentities.storage.Disk entity) {
        return ((BackendVmDisksResource) collection).deprecatedPopulate(model, entity);
    }

    @Override
    public ActionResource getActionSubresource(String action, String oid) {
        return inject(new BackendActionResource(action, oid));
    }

    @Override
    public Response activate(Action action) {
        HotPlugDiskToVmParameters params =
                new HotPlugDiskToVmParameters(((BackendVmDisksResource) collection).parentId,
                        guid);
        return doAction(VdcActionType.HotPlugDiskToVm, params, action);
    }

    @Override
    public Response deactivate(Action action) {
        HotPlugDiskToVmParameters params =
                new HotPlugDiskToVmParameters(((BackendVmDisksResource) collection).parentId,
                        guid);
        return doAction(VdcActionType.HotUnPlugDiskFromVm, params, action);
    }

    @Override
    public Response move(Action action) {
        validateParameters(action, "storageDomain.id|name");
        Guid storageDomainId = getStorageDomainId(action);
        Disk disk = getDisk();
        Guid sourceStorageDomainId = getSourceStorageDomainId(disk);
        Guid imageId = getDiskImageId(disk.getImageId());
        MoveDiskParameters innerParams = new MoveDiskParameters(
                imageId,
                sourceStorageDomainId,
                storageDomainId);
        innerParams.setImageGroupID(asGuid(disk.getId()));
        MoveDisksParameters params =
                new MoveDisksParameters(Collections.singletonList(innerParams));
        return doAction(VdcActionType.MoveDisks, params, action);
    }

    protected Disk getDisk() {
        return performGet(VdcQueryType.GetDiskByDiskId, new IdQueryParameters(guid));
    }

    protected Guid getSourceStorageDomainId(Disk disk) {
        if (disk.isSetStorageDomains()) {
            StorageDomain storageDomain = disk.getStorageDomains().getStorageDomains().get(0);
            if (storageDomain != null) {
                return asGuid(storageDomain.getId());
            }
        }
        return null;
    }

    protected Guid getDiskImageId(String id) {
        if (id == null) {
            return null;
        }
        return asGuid(id);
    }

    @Override
    public Disk get() {
        return super.get();//explicit call solves REST-Easy confusion
    }

    @Override
    protected Disk addLinks(Disk model, String... subCollectionMembersToExclude) {
        return collection.addLinks(model);
    }

    @Override
    public AssignedPermissionsResource getPermissionsResource() {
        return inject(new BackendAssignedPermissionsResource(guid,
            VdcQueryType.GetPermissionsForObject,
            new GetPermissionsForObjectParameters(guid),
            Disk.class,
            VdcObjectType.Disk));
    }

    @Override
    public Response doExport(Action action) {
        validateParameters(action, "storageDomain.id|name");
        return doAction(VdcActionType.ExportRepoImage,
            new ExportRepoImageParameters(guid, getStorageDomainId(action)), action);
    }

    @Override
    public Disk update(Disk resource) {
        validateEnums(Disk.class, resource);
        return super.update(resource);//explicit call solves REST-Easy confusion
    }

    @Override
    public Response remove() {
        get();
        return performAction(VdcActionType.RemoveDisk, new RemoveDiskParameters(guid));
    }

    @Override
    public Response remove(Action action) {
        get();
        if (action.isSetDetach() && action.isDetach()) {
            return performAction(VdcActionType.DetachDiskFromVm, new AttachDetachVmDiskParameters(vmId, guid));
        }
        else {
            return performAction(VdcActionType.RemoveDisk, new RemoveDiskParameters(guid));
        }
    }
}
