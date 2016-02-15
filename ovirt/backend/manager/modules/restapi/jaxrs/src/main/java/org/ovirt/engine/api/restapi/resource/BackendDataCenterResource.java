package org.ovirt.engine.api.restapi.resource;


import static org.ovirt.engine.api.restapi.resource.BackendDataCentersResource.SUB_COLLECTIONS;

import java.util.List;

import org.ovirt.engine.api.model.DataCenter;
import org.ovirt.engine.api.resource.AssignedPermissionsResource;
import org.ovirt.engine.api.resource.AttachedStorageDomainsResource;
import org.ovirt.engine.api.resource.ClustersResource;
import org.ovirt.engine.api.resource.DataCenterResource;
import org.ovirt.engine.api.resource.IscsiBondsResource;
import org.ovirt.engine.api.resource.NetworksResource;
import org.ovirt.engine.api.resource.QoSsResource;
import org.ovirt.engine.api.resource.QuotasResource;
import org.ovirt.engine.api.restapi.utils.MalformedIdException;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.StoragePoolManagementParameter;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.queries.GetPermissionsForObjectParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendDataCenterResource extends AbstractBackendSubResource<DataCenter, StoragePool>
        implements DataCenterResource {

    private final BackendDataCentersResource parent;

    public BackendDataCenterResource(String id, BackendDataCentersResource parent) {
        super(id, DataCenter.class, StoragePool.class, SUB_COLLECTIONS);
        this.parent = parent;
    }

    @Override
    public DataCenter get() {
        return performGet(VdcQueryType.GetStoragePoolById, new IdQueryParameters(guid));
    }

    @Override
    public DataCenter update(DataCenter incoming) {
        validateEnums(DataCenter.class, incoming);
        return performUpdate(incoming,
                             new QueryIdResolver<Guid>(VdcQueryType.GetStoragePoolById, IdQueryParameters.class),
                             VdcActionType.UpdateStoragePool,
                             new UpdateParametersProvider());
    }

    @Override
    public AssignedPermissionsResource getPermissionsResource() {
        return inject(new BackendAssignedPermissionsResource(guid,
                                                             VdcQueryType.GetPermissionsForObject,
                                                             new GetPermissionsForObjectParameters(guid),
                                                             DataCenter.class,
                                                             VdcObjectType.StoragePool));
    }

    @Override
    public AttachedStorageDomainsResource getAttachedStorageDomainsResource() {
        return inject(new BackendAttachedStorageDomainsResource(id));
    }

    @Override
    public NetworksResource getNetworksResource() {
        return inject(new BackendDataCenterNetworksResource(id));
    }

    @Override
    public ClustersResource getClustersResource() {
        return inject(new BackendDataCenterClustersResource(id));
    }

    @Override
    public QuotasResource getQuotasResource() {
         return inject(new BackendQuotasResource(id));
    }

    @Override
    public IscsiBondsResource getIscsiBondsResource() {
        return inject(new BackendIscsiBondsResource(id));
    }

    public BackendDataCentersResource getParent() {
        return parent;
    }

    @Override
    protected DataCenter doPopulate(DataCenter model, StoragePool entity) {
        return parent.doPopulate(model, entity);
    }

    @Override
    protected DataCenter deprecatedPopulate(DataCenter model, StoragePool entity) {
        return parent.deprecatedPopulate(model, entity);
    }

    protected class UpdateParametersProvider implements
            ParametersProvider<DataCenter, StoragePool> {
        @Override
        public VdcActionParametersBase getParameters(DataCenter incoming, StoragePool entity) {
            return new StoragePoolManagementParameter(map(incoming, entity));
        }
    }

    /**
     * Get the storage pool (i.e. datacenter entity) associated with the given
     * cluster.
     */
    @SuppressWarnings("unchecked")
    public static StoragePool getStoragePool(DataCenter dataCenter, AbstractBackendResource parent) {
        StoragePool pool = null;
        if (dataCenter.isSetId()) {
            String id = dataCenter.getId();
            Guid guid;
            try {
                guid = new Guid(id); // can't use asGuid() because the method is static.
            } catch (IllegalArgumentException e) {
                throw new MalformedIdException(e);
            }
            pool = parent.getEntity(StoragePool.class, VdcQueryType.GetStoragePoolById,
                    new IdQueryParameters(guid), "Datacenter: id=" + id);
        } else {
            String clusterName = dataCenter.getName();
            pool = parent.getEntity(StoragePool.class, VdcQueryType.GetStoragePoolByDatacenterName,
                    new NameQueryParameters(clusterName), "Datacenter: name="
                            + clusterName);
            dataCenter.setId(pool.getId().toString());
        }
        return pool;
    }

    /**
     * Get the storage pools (i.e. datacenter entity) associated with the given
     * storagedomain.
     */
    @SuppressWarnings("unchecked")
    public static  List<StoragePool> getStoragePools(Guid storageDomainId, AbstractBackendResource parent) {
        return parent.getEntity(List.class,
                                                    VdcQueryType.GetStoragePoolsByStorageDomainId,
                                                    new IdQueryParameters(storageDomainId),
                                                    "Datacenters",
                                                    true);
    }

    @Override
    public QoSsResource getQossResource() {
        return inject(new BackendQossResource(id));
    }
}
