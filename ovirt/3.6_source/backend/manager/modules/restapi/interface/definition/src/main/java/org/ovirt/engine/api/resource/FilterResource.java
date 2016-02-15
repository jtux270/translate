package org.ovirt.engine.api.resource;

import javax.ws.rs.Produces;

import org.ovirt.engine.api.model.Filter;

@Produces({ ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML })
public interface FilterResource extends PolicyUnitResource<Filter> {
}
