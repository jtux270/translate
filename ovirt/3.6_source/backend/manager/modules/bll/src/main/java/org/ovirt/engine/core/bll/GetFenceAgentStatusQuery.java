package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.pm.HostFenceActionExecutor;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.pm.FenceOperationResult;
import org.ovirt.engine.core.common.businessentities.pm.FenceOperationResult.Status;
import org.ovirt.engine.core.common.queries.GetFenceAgentStatusParameters;
import org.ovirt.engine.core.compat.Guid;

public class GetFenceAgentStatusQuery<P extends GetFenceAgentStatusParameters> extends FenceQueryBase<P> {
    public GetFenceAgentStatusQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        HostFenceActionExecutor executor = new HostFenceActionExecutor(getHost());
        FenceOperationResult result = executor.getFenceAgentStatus(getParameters().getAgent());
        getQueryReturnValue().setSucceeded(result.getStatus() == Status.SUCCESS);
        getQueryReturnValue().setReturnValue(result);
    }

    private VDS getHost() {
        Guid id = getParameters().getVdsId();
        VDS vds = new VDS();
        vds.setId(id != null ? id : Guid.Empty);
        vds.setVdsName(getParameters().getVdsName());
        vds.setHostName(getParameters().getHostName());
        vds.setVdsGroupId(getParameters().getVdsGroupId());
        vds.setStoragePoolId(getParameters().getStoragePoolId());
        vds.setFenceProxySources(getParameters().getFenceProxySources());
        return vds;
    }
}
