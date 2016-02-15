package org.ovirt.engine.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.ovirt.engine.api.model.SchedulingPolicyUnits;

@Path("/schedulingpolicyunits")
@Produces({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
public interface SchedulingPolicyUnitsResource {

    @GET
    public SchedulingPolicyUnits list();

    @Path("{id}")
    public SchedulingPolicyUnitResource getSchedulingPolicyUnitSubResource(@PathParam("id") String id);

}
