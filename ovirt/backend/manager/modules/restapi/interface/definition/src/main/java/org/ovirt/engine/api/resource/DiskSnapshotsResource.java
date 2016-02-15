package org.ovirt.engine.api.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.DiskSnapshot;
import org.ovirt.engine.api.model.DiskSnapshots;

@Path("/disksnapshots")
@Produces({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
public interface DiskSnapshotsResource extends ReadOnlyResources<DiskSnapshot, DiskSnapshots> {

    @DELETE
    @Path("{id}")
    public Response remove(@PathParam("id") String id);

}
