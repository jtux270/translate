package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.queries.IdQueryParameters;

public class GetAllDisksPartialDataByVmIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    public GetAllDisksPartialDataByVmIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        List<Disk> allDisks =
                getDbFacade().getDiskDao().getAllForVmPartialData
                        (getParameters().getId(), false, getUserID(), getParameters().isFiltered());
        getQueryReturnValue().setReturnValue(allDisks);
    }
}
