package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.StorageConnection;
import org.ovirt.engine.api.model.StorageConnections;
import org.ovirt.engine.api.resource.StorageServerConnectionResource;
import org.ovirt.engine.api.resource.StorageServerConnectionsResource;
import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.queries.StorageServerConnectionQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendStorageServerConnectionsResource extends AbstractBackendCollectionResource<StorageConnection, StorageServerConnections> implements StorageServerConnectionsResource {
    private final EntityIdResolver<String> ENTITY_RETRIEVER =
            new QueryIdResolver<String>(VdcQueryType.GetStorageServerConnectionById,
                    StorageServerConnectionQueryParametersBase.class);

    public BackendStorageServerConnectionsResource() {
        super(StorageConnection.class, org.ovirt.engine.core.common.businessentities.StorageServerConnections.class);
    }

    @Override
    public StorageConnections list() {
        return mapCollection(getBackendCollection(VdcQueryType.GetAllStorageServerConnections,
                new VdcQueryParametersBase()));
    }

    @Override
    protected StorageConnection doPopulate(StorageConnection model, StorageServerConnections entity) {
        return model;
    }

    private StorageConnections mapCollection(List<StorageServerConnections> entities) {
        StorageConnections collection = new StorageConnections();
        for (org.ovirt.engine.core.common.businessentities.StorageServerConnections entity : entities) {
            StorageConnection connection = map(entity);
            if (connection != null) {
                collection.getStorageConnections().add(addLinks(populate(connection, entity)));
            }
        }
        return collection;
    }

    @Override
    public Response add(StorageConnection storageConn) {
        validateParameters(storageConn, "type");
        // map to backend object
        StorageServerConnections storageConnection =
                getMapper(StorageConnection.class, StorageServerConnections.class).map(storageConn, null);

        Guid hostId = Guid.Empty;
        if (storageConn.getHost() != null) {
           hostId = getHostId(storageConn.getHost());
        }
        switch (storageConnection.getstorage_type()) {
        case ISCSI:
            validateParameters(storageConn, "address", "target", "port");
            break;
        case NFS:
            validateParameters(storageConn, "address", "path");
            break;
        case LOCALFS:
            validateParameters(storageConn, "path");
            break;
        case POSIXFS:
        case GLUSTERFS:
            // address is possible, but is optional, non mandatory
            validateParameters(storageConn, "path", "vfsType");
            break;
        default:
            break;
        }
        return performCreate(VdcActionType.AddStorageServerConnection,
                getAddParams(storageConnection, hostId),
                ENTITY_RETRIEVER);
    }

    private StorageServerConnectionParametersBase getAddParams(StorageServerConnections entity, Guid hostId) {
        StorageServerConnectionParametersBase params = new StorageServerConnectionParametersBase(entity, hostId);
        params.setVdsId(hostId);
        return params;
    }

    @Override
    public Response remove(String id, Action action) {
        getEntity(id);
        StorageServerConnections connection = new StorageServerConnections();
        connection.setid(id);
        Guid hostId = Guid.Empty;

        if (action != null && action.isSetHost()) {
            hostId = getHostId(action.getHost());
        }

        StorageServerConnectionParametersBase parameters =
                new StorageServerConnectionParametersBase(connection, hostId);
        return performAction(VdcActionType.RemoveStorageServerConnection, parameters);
    }

    @Override
    protected Response performRemove(String id) {
        StorageServerConnections connection = new StorageServerConnections();
        connection.setid(id);
        Guid hostId = Guid.Empty;

        StorageServerConnectionParametersBase parameters =
                new StorageServerConnectionParametersBase(connection, hostId);
        return performAction(VdcActionType.RemoveStorageServerConnection, parameters);
    }

    @Override
    @SingleEntityResource
    public StorageServerConnectionResource getStorageConnectionSubResource(String id) {
        return inject(new BackendStorageServerConnectionResource(id, this));
    }
}
