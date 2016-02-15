package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;

public class GetAllStorageServerConnectionsQuery <P extends VdcQueryParametersBase> extends QueriesCommandBase<P>  {

    public GetAllStorageServerConnectionsQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(getDbFacade().getStorageServerConnectionDao()
                .getAll());
    }
}
