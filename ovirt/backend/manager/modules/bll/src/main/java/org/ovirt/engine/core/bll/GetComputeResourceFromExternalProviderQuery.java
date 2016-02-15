package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.host.provider.HostProviderProxy;
import org.ovirt.engine.core.bll.provider.ProviderProxyFactory;
import org.ovirt.engine.core.common.businessentities.ExternalComputeResource;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.queries.ProviderQueryParameters;

import java.util.List;

public class GetComputeResourceFromExternalProviderQuery<P extends ProviderQueryParameters> extends QueriesCommandBase<P> {
    public GetComputeResourceFromExternalProviderQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        Provider hostProvider = getParameters().getProvider();
        List<ExternalComputeResource> providerComputeResources = getProviderComputeResource(hostProvider);
        getQueryReturnValue().setReturnValue(providerComputeResources);
    }

    protected List<ExternalComputeResource> getProviderComputeResource(Provider hostProvider) {
        HostProviderProxy proxy = ((HostProviderProxy) ProviderProxyFactory.getInstance().create(hostProvider));
        return proxy.getComputeResources();
    }
}
