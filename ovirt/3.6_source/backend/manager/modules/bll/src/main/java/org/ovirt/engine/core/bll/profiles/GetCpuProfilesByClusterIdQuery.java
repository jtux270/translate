package org.ovirt.engine.core.bll.profiles;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.queries.IdQueryParameters;

public class GetCpuProfilesByClusterIdQuery extends QueriesCommandBase<IdQueryParameters> {

    public GetCpuProfilesByClusterIdQuery(IdQueryParameters parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(getDbFacade().getCpuProfileDao()
                .getAllForCluster(getParameters().getId(), getUserID(), getParameters().isFiltered()));
    }

}
