package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.StorageConnection;
import org.ovirt.engine.api.model.StorageConnections;
import org.ovirt.engine.api.resource.StorageServerConnectionResource;
import org.ovirt.engine.api.restapi.types.StorageDomainMapper;
import org.ovirt.engine.core.common.action.EditIscsiBondParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.IscsiBond;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendIscsiBondStorageConnectionsResource extends BackendStorageServerConnectionsResource {

    private Guid iscsiBondId;

    public BackendIscsiBondStorageConnectionsResource(String iscsiBondId) {
        super();
        this.iscsiBondId = Guid.createGuidFromString(iscsiBondId);
    }

    @Override
    public StorageConnections list() {
        return mapCollection(
                getBackendCollection(VdcQueryType.GetStorageServerConnectionByIscsiBondId, new IdQueryParameters(iscsiBondId))
        );
    }

    @Override
    public Response add(StorageConnection conn) {
        StorageServerConnections entity = StorageDomainMapper.map(conn, null);

        IscsiBond iscsiBond = getIscsiBond();
        iscsiBond.getStorageConnectionIds().add(entity.getid());
        return performAction(VdcActionType.EditIscsiBond, new EditIscsiBondParameters(iscsiBond));
    }

    @Override
    public StorageServerConnectionResource getStorageConnectionSubResource(String id) {
        return inject(new BackendIscsiBondStorageConnectionResource(id, this));
    }

    @Override
    protected Response performRemove(String id) {
        IscsiBond iscsiBond = getIscsiBond();
        iscsiBond.getStorageConnectionIds().remove(id);
        return performAction(VdcActionType.EditIscsiBond, new EditIscsiBondParameters(iscsiBond));
    }

    protected IscsiBond getIscsiBond() {
        return getEntity(IscsiBond.class, VdcQueryType.GetIscsiBondById, new IdQueryParameters(iscsiBondId), iscsiBondId.toString());
    }

    private StorageConnections mapCollection(List<StorageServerConnections> entities) {
        StorageConnections conns = new StorageConnections();

        for (StorageServerConnections entity : entities) {
            StorageConnection conn = StorageDomainMapper.map(entity, null);
            conns.getStorageConnections().add(addLinks(populate(conn, entity)));
        }

        return conns;
    }
}
