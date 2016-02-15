package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.StorageConnectionExtension;
import org.ovirt.engine.api.model.StorageConnectionExtensions;
import org.ovirt.engine.api.resource.StorageServerConnectionExtensionResource;
import org.ovirt.engine.api.resource.StorageServerConnectionExtensionsResource;
import org.ovirt.engine.api.restapi.types.StorageServerConnectionExtensionMapper;
import org.ovirt.engine.core.common.action.StorageServerConnectionExtensionParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.storage.StorageServerConnectionExtension;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendStorageServerConnectionExtensionsResource
        extends AbstractBackendCollectionResource<StorageConnectionExtension, StorageServerConnectionExtension>
        implements StorageServerConnectionExtensionsResource {
    protected final Guid hostId;

    public BackendStorageServerConnectionExtensionsResource(Guid hostId) {
        super(StorageConnectionExtension.class, StorageServerConnectionExtension.class);
        this.hostId = hostId;
    }

    @Override
    public StorageConnectionExtensions list() {
        return mapCollection(getBackendCollection(VdcQueryType.GetStorageServerConnectionExtensionsByHostId,
                new IdQueryParameters(hostId)));
    }

    private StorageConnectionExtensions mapCollection(List<StorageServerConnectionExtension> connectionExtensions) {
        StorageConnectionExtensions mappedConnectionExtensions = new StorageConnectionExtensions();
        for (StorageServerConnectionExtension connectionExtension : connectionExtensions) {
            mappedConnectionExtensions.getStorageConnectionExtension().add(addLinks(populate(map(connectionExtension), connectionExtension)));
        }

        return mappedConnectionExtensions;
    }

    @Override
    public Response add(StorageConnectionExtension storageConnectionExtension) {
        StorageServerConnectionExtension connExt = StorageServerConnectionExtensionMapper.map(storageConnectionExtension, null);
        connExt.setHostId(hostId);
        StorageServerConnectionExtensionParameters params = new StorageServerConnectionExtensionParameters(connExt);
        return performCreate(VdcActionType.AddStorageServerConnectionExtension, params,
                new QueryIdResolver<Guid>(VdcQueryType.GetStorageServerConnectionExtensionById,
                        IdQueryParameters.class));
    }

    @Override
    public StorageServerConnectionExtensionResource getStorageConnectionExtensionSubResource(String id) {
        return inject(new BackendStorageServerConnectionExtensionResource(id, this));
    }
}
