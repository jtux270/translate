package org.ovirt.engine.core.bll;

import java.util.HashMap;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.ActionVersionMap;
import org.ovirt.engine.core.common.queries.CommandVersionsInfo;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;

public class GetCommandsCompatibilityVersionsQuery<P extends VdcQueryParametersBase> extends QueriesCommandBase<P> {

    public GetCommandsCompatibilityVersionsQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        HashMap<VdcActionType, CommandVersionsInfo> resultMap =
                new HashMap<VdcActionType, CommandVersionsInfo>();
        for (ActionVersionMap actionVersionMap : getActionGroupDao().getAllActionVersionMap()) {
            CommandVersionsInfo info =
                    new CommandVersionsInfo(actionVersionMap.getstorage_pool_minimal_version(),
                            actionVersionMap.getcluster_minimal_version());
            resultMap.put(actionVersionMap.getaction_type(), info);
        }
        getQueryReturnValue().setReturnValue(resultMap);
    }

}
