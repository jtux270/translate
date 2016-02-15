package org.ovirt.engine.api.resource;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.ovirt.engine.api.model.Balance;
import org.ovirt.engine.api.model.Balances;

@Path("/balances")
@Produces({ ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML })
public interface BalancesResource extends PolicyUnitsResource<Balances, Balance> {
    @Override
    @Path("{id}")
    public BalanceResource getSubResource(@PathParam("id") String id);
}
