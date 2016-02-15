package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.host.provider.HostProviderProxy;
import org.ovirt.engine.core.bll.provider.ProviderProxyFactory;
import org.ovirt.engine.core.common.businessentities.ExternalHostGroup;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.queries.ProviderQueryParameters;

import java.util.List;

public class GetHostGroupsFromExternalProviderQuery<P extends ProviderQueryParameters> extends QueriesCommandBase<P> {
    public GetHostGroupsFromExternalProviderQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        Provider hostProvider = getParameters().getProvider();
        List<ExternalHostGroup> providerHostGroups = getProviderHostGroups(hostProvider);
        getQueryReturnValue().setReturnValue(providerHostGroups);
    }

    protected List<ExternalHostGroup> getProviderHostGroups(Provider hostProvider) {
        HostProviderProxy proxy = ((HostProviderProxy) ProviderProxyFactory.getInstance().create(hostProvider));
        return proxy.getHostGroups();
    }
}
