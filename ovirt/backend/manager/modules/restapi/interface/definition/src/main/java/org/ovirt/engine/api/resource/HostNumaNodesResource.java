package org.ovirt.engine.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.ovirt.engine.api.model.NumaNodes;

@Produces({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
public interface HostNumaNodesResource {

    @GET
    @Formatted
    public NumaNodes list();

    @Path("{id}")
    public HostNumaNodeResource getHostNumaNodeSubResource(@PathParam("id") String id);

}
