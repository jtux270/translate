package org.ovirt.engine.api.restapi.resource;

import static org.ovirt.engine.api.restapi.resource.BackendStorageDomainsResource.SUB_COLLECTIONS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.ovirt.engine.api.common.util.StatusUtils;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.CreationStatus;
import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.model.LogicalUnit;
import org.ovirt.engine.api.model.Storage;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageDomainStatus;
import org.ovirt.engine.api.model.StorageDomainType;
import org.ovirt.engine.api.model.Template;
import org.ovirt.engine.api.model.Templates;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.model.VMs;
import org.ovirt.engine.api.resource.AssignedDiskProfilesResource;
import org.ovirt.engine.api.resource.AssignedPermissionsResource;
import org.ovirt.engine.api.resource.DiskSnapshotsResource;
import org.ovirt.engine.api.resource.DisksResource;
import org.ovirt.engine.api.resource.FilesResource;
import org.ovirt.engine.api.resource.ImagesResource;
import org.ovirt.engine.api.resource.StorageDomainContentsResource;
import org.ovirt.engine.api.resource.StorageDomainResource;
import org.ovirt.engine.api.resource.StorageDomainServerConnectionsResource;
import org.ovirt.engine.api.restapi.util.StorageDomainHelper;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ExtendSANStorageDomainParameters;
import org.ovirt.engine.core.common.action.RemoveStorageDomainParameters;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.action.StorageDomainParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.queries.GetPermissionsForObjectParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.StorageDomainsAndStoragePoolIdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendStorageDomainResource extends
        AbstractBackendActionableResource<StorageDomain, org.ovirt.engine.core.common.businessentities.StorageDomain> implements StorageDomainResource {

    private final BackendStorageDomainsResource parent;

    public BackendStorageDomainResource(String id, BackendStorageDomainsResource parent) {
        super(id,
                StorageDomain.class,
                org.ovirt.engine.core.common.businessentities.StorageDomain.class,
                SUB_COLLECTIONS);
        this.parent = parent;
    }

    BackendStorageDomainsResource getParent() {
        return parent;
    }

    @Override
    public StorageDomain get() {
        StorageDomain storageDomain = performGet(VdcQueryType.GetStorageDomainById, new IdQueryParameters(guid));
        return addLinks(storageDomain, getLinksToExclude(storageDomain));
    }

    @Override
    public StorageDomain update(StorageDomain incoming) {
        validateEnums(StorageDomain.class, incoming);
        QueryIdResolver<Guid> storageDomainResolver =
                new QueryIdResolver<Guid>(VdcQueryType.GetStorageDomainById, IdQueryParameters.class);
        org.ovirt.engine.core.common.businessentities.StorageDomain entity = getEntity(storageDomainResolver, true);
        StorageDomain model = map(entity, new StorageDomain());
        StorageType storageType = entity.getStorageType();
        if (storageType != null) {
            switch (storageType) {
            case ISCSI:
            case FCP:
                extendStorageDomain(incoming, model, storageType);
                break;
            default:
                break;
            }
        }

        return addLinks(performUpdate(incoming,
                entity,
                model,
                storageDomainResolver,
                VdcActionType.UpdateStorageDomain,
                new UpdateParametersProvider()),
                new String[] { "templates", "vms" });
    }

    @Override
    public Response remove(StorageDomain storageDomain) {
        if (storageDomain == null) {
            Fault fault = new Fault();
            fault.setReason("storage-domain parameter is missing");
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(fault).build());
        }
        validateParameters(storageDomain, "host.id|name");
        get();
        if (storageDomain.isSetDestroy() && storageDomain.isDestroy()) {
            StorageDomainParametersBase parameters = new StorageDomainParametersBase(guid);
            parameters.setVdsId(getHostId(storageDomain));
            return performAction(VdcActionType.ForceRemoveStorageDomain, parameters);
        } else {
            RemoveStorageDomainParameters parameters = new RemoveStorageDomainParameters(guid);
            parameters.setVdsId(getHostId(storageDomain));
            if (storageDomain.isSetFormat()) {
                parameters.setDoFormat(storageDomain.isFormat());
            }
            return performAction(VdcActionType.RemoveStorageDomain, parameters);
        }
    }

    @Override
    public Response getIsAttached(Action action) {
        validateParameters(action, "host.id|name");
        Guid hostId = getHostId(action);
        org.ovirt.engine.core.common.businessentities.StorageDomain storageDomainToAttach = getEntity(
            org.ovirt.engine.core.common.businessentities.StorageDomain.class,
            VdcQueryType.GetStorageDomainById,
            new IdQueryParameters(guid),
            guid.toString()
        );
        StorageDomainsAndStoragePoolIdQueryParameters parameters =
                new StorageDomainsAndStoragePoolIdQueryParameters(storageDomainToAttach, null, hostId);
        parameters.setCheckStoragePoolStatus(false);
        List<StorageDomainStatic> attachedStorageDomains = getEntity(
            List.class,
            VdcQueryType.GetStorageDomainsWithAttachedStoragePoolGuid,
            parameters,
            guid.toString(),
            true
        );

        // This is an atypical action, as it doesn't invoke a backend action, but a query. As a result we need to
        // create and populate the returned action object so that it looks like a real action result.
        Action result = new Action();
        result.setIsAttached(!attachedStorageDomains.isEmpty());
        result.setStatus(StatusUtils.create(CreationStatus.COMPLETE));

        return Response.ok().entity(result).build();
    }

    @Override
    public Response refreshLuns(Action action) {
        List<LogicalUnit> incomingLuns;
        if (action.isSetLogicalUnits()) {
            incomingLuns = action.getLogicalUnits().getLogicalUnits();
        }
        else {
            incomingLuns = Collections.emptyList();
        }
        ExtendSANStorageDomainParameters params = createParameters(guid, incomingLuns, false);
        return performAction(VdcActionType.RefreshLunsSize, params);
    }

    @Override
    public FilesResource getFilesResource() {
        return inject(new BackendFilesResource(id));
    }

    public static synchronized boolean isIsoDomain(StorageDomain storageDomain) {
        StorageDomainType type = StorageDomainType.fromValue(storageDomain.getType());
        return type != null && type == StorageDomainType.ISO ? true : false;
    }

    public static synchronized boolean isIsoDomain(org.ovirt.engine.core.common.businessentities.StorageDomain storageDomain) {
        org.ovirt.engine.core.common.businessentities.StorageDomainType type = storageDomain.getStorageDomainType();
        return type != null && type == org.ovirt.engine.core.common.businessentities.StorageDomainType.ISO ? true
                : false;
    }

    public static synchronized boolean isExportDomain(StorageDomain storageDomain) {
        StorageDomainType type = StorageDomainType.fromValue(storageDomain.getType());
        return type != null && type == StorageDomainType.EXPORT ? true : false;
    }

    public static synchronized boolean isImageDomain(StorageDomain storageDomain) {
        StorageDomainType type = StorageDomainType.fromValue(storageDomain.getType());
        return type != null && type == StorageDomainType.IMAGE;
    }

    public static synchronized String[] getLinksToExclude(StorageDomain storageDomain) {
        return isIsoDomain(storageDomain) ? new String[] { "templates", "vms", "disks", "images" }
                : isExportDomain(storageDomain) ? new String[] { "files", "images" }
                        : isImageDomain(storageDomain) ? new String[] { "templates", "vms", "files", "disks",
                                "storageconnections" }
                                : new String[] { "files", "images" };
    }

    /**
     * if user added new LUNs - extend the storage domain.
     *
     * @param incoming
     */
    private void extendStorageDomain(StorageDomain incoming, StorageDomain storageDomain, StorageType storageType) {
        if (incoming.getStorage() == null) {
            // LUNs info was not supplied in the request so no need to check whether to extend
            return;
        }
        List<LogicalUnit> existingLuns = storageDomain.getStorage().getVolumeGroup().getLogicalUnits();
        List<LogicalUnit> incomingLuns = getIncomingLuns(incoming.getStorage());
        List<LogicalUnit> newLuns = findNewLuns(existingLuns, incomingLuns);
        boolean overrideLuns = incoming.getStorage().isSetOverrideLuns() ?
                incoming.getStorage().isOverrideLuns() : false;
        if (!newLuns.isEmpty()) {
            // If there are new LUNs, this means the user wants to extend the storage domain.
            addLunsToStorageDomain(newLuns, overrideLuns);
            // Remove the new LUNs from the incoming LUns before update, since they have already been dealt with.
            incomingLuns.removeAll(newLuns);
        }
    }

    private void addLunsToStorageDomain(List<LogicalUnit> newLuns, boolean overrideLuns) {

        ExtendSANStorageDomainParameters params = createParameters(guid, newLuns, overrideLuns);
        performAction(VdcActionType.ExtendSANStorageDomain, params);
    }

    @Override
    public AssignedPermissionsResource getPermissionsResource() {
        return inject(new BackendAssignedPermissionsResource(guid,
                VdcQueryType.GetPermissionsForObject,
                new GetPermissionsForObjectParameters(guid),
                StorageDomain.class,
                VdcObjectType.Storage));
    }

    @Override
    protected StorageDomain map(org.ovirt.engine.core.common.businessentities.StorageDomain entity,
            StorageDomain template) {
        return parent.map(entity, template);
    }

    @Override
    protected StorageDomain deprecatedPopulate(StorageDomain model,
            org.ovirt.engine.core.common.businessentities.StorageDomain entity) {
        if (StorageDomainSharedStatus.Unattached.equals(entity.getStorageDomainSharedStatus())) {
            model.setStatus(StatusUtils.create(StorageDomainStatus.UNATTACHED));
        } else {
            model.setStatus(null);
        }
        return super.deprecatedPopulate(model, entity);
    }

    private List<LogicalUnit> getIncomingLuns(Storage storage) {
        // user may pass the LUNs under Storage, or Storage-->VolumeGroup; both are supported.
        if (storage.getLogicalUnits().isEmpty()) {
            if (storage.getVolumeGroup() != null) {
                return storage.getVolumeGroup().getLogicalUnits();
            }
            else {
                return new ArrayList<LogicalUnit>();
            }
        }
        else {
            return storage.getLogicalUnits();
        }
    }

    private Guid getHostId(StorageDomain storageDomain) {
        // presence of host ID or name already validated
        return storageDomain.getHost().isSetId()
                ? new Guid(storageDomain.getHost().getId())
                : storageDomain.getHost().isSetName()
                        ? getEntity(VdsStatic.class,
                                VdcQueryType.GetVdsStaticByName,
                                new NameQueryParameters(storageDomain.getHost().getName()),
                                "Hosts: name=" + storageDomain.getHost().getName()).getId()
                        : null;

    }

    private ExtendSANStorageDomainParameters createParameters(Guid storageDomainId,
            List<LogicalUnit> newLuns,
            boolean force) {
        ExtendSANStorageDomainParameters params = new ExtendSANStorageDomainParameters();
        params.setStorageDomainId(storageDomainId);
        ArrayList<String> lunIds = new ArrayList<String>();
        for (LogicalUnit newLun : newLuns) {
            lunIds.add(newLun.getId());
        }
        params.setLunIds(lunIds);
        params.setForce(force);
        return params;
    }

    private List<LogicalUnit> findNewLuns(List<LogicalUnit> existingLuns, List<LogicalUnit> incomingLuns) {
        List<LogicalUnit> newLuns = new LinkedList<LogicalUnit>();
        for (LogicalUnit incomingLun : incomingLuns) {
            boolean found = false;
            for (LogicalUnit existingLun : existingLuns) {
                if (lunsEqual(incomingLun, existingLun)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newLuns.add(incomingLun);
            }
        }
        return newLuns;
    }

    private boolean lunsEqual(LogicalUnit firstLun, LogicalUnit secondLun) {
        return firstLun.getId().equals(secondLun.getId());
    }

    protected class UpdateParametersProvider implements
            ParametersProvider<StorageDomain, org.ovirt.engine.core.common.businessentities.StorageDomain> {
        @Override
        public VdcActionParametersBase getParameters(StorageDomain incoming,
                org.ovirt.engine.core.common.businessentities.StorageDomain entity) {
            // save SD type before mapping
            org.ovirt.engine.core.common.businessentities.StorageDomainType currentType =
                    entity.getStorageStaticData() == null ? null : entity.getStorageStaticData().getStorageDomainType();
            StorageDomainStatic updated = getMapper(modelType, StorageDomainStatic.class).map(
                    incoming, entity.getStorageStaticData());
            // if SD type was 'Master', and user gave 'Data', they are the same, this is not a real update, so exchange
            // data back to master.
            if (currentType == org.ovirt.engine.core.common.businessentities.StorageDomainType.Master
                    && updated.getStorageDomainType() == org.ovirt.engine.core.common.businessentities.StorageDomainType.Data) {
                updated.setStorageDomainType(org.ovirt.engine.core.common.businessentities.StorageDomainType.Master);
            }
            return new StorageDomainManagementParameter(updated);
        }
    }

    @Override
    public StorageDomainContentsResource<Templates, Template> getStorageDomainTemplatesResource() {
        return inject(new BackendStorageDomainTemplatesResource(guid));
    }

    @Override
    public StorageDomainContentsResource<VMs, VM> getStorageDomainVmsResource() {
        return inject(new BackendStorageDomainVmsResource(guid));
    }

    @Override
    public DisksResource getDisksResource() {
        return inject(new BackendStorageDomainDisksResource(guid));
    }

    @Override
    public StorageDomainServerConnectionsResource getStorageConnectionsResource() {
        return inject(new BackendStorageDomainServerConnectionsResource(guid));
    }

    @Override
    public ImagesResource getImagesResource() {
        return inject(new BackendStorageDomainImagesResource(guid));
    }

    @Override
    public DiskSnapshotsResource getDiskSnapshotsResource() {
        return inject(new BackendStorageDomainDiskSnapshotsResource(guid));
    }

    @Override
    public AssignedDiskProfilesResource getDiskProfilesResource() {
        return inject(new BackendAssignedDiskProfilesResource(id));
    }

    @Override
    protected StorageDomain addParents(StorageDomain model) {
        StorageDomainHelper.addAttachedDataCenterReferences(this, model);
        return model;
    }
}
