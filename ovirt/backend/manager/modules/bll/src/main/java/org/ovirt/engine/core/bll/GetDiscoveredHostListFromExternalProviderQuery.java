package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.host.provider.HostProviderProxy;
import org.ovirt.engine.core.bll.provider.ProviderProxyFactory;
import org.ovirt.engine.core.common.businessentities.ExternalDiscoveredHost;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.queries.ProviderQueryParameters;

import java.util.List;

public class GetDiscoveredHostListFromExternalProviderQuery<P extends ProviderQueryParameters> extends QueriesCommandBase<P> {
    public GetDiscoveredHostListFromExternalProviderQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        Provider hostProvider = getParameters().getProvider();
        List<ExternalDiscoveredHost> providerHosts = getProviderHosts(hostProvider);
        getQueryReturnValue().setReturnValue(providerHosts);
    }

    protected List<ExternalDiscoveredHost> getProviderHosts(Provider hostProvider) {
        HostProviderProxy proxy = ((HostProviderProxy) ProviderProxyFactory.getInstance().create(hostProvider));
        return proxy.getDiscoveredHosts();
    }
}
