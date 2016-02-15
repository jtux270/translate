package org.ovirt.engine.core.bll.provider;

import org.ovirt.engine.core.bll.host.provider.foreman.ForemanHostProviderProxy;
import org.ovirt.engine.core.bll.provider.network.openstack.OpenstackNetworkProviderProxy;
import org.ovirt.engine.core.common.businessentities.OpenStackImageProviderProperties;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties;
import org.ovirt.engine.core.common.businessentities.Provider;

/**
 * The provider proxy factory can create a provider proxy according to the provider definition.
 */
public class ProviderProxyFactory {

    private static final ProviderProxyFactory INSTANCE = new ProviderProxyFactory();

    private ProviderProxyFactory() {
        // Singleton private c'tor
    }

    /**
     * Create the proxy used to communicate with the given provider.
     *
     * @param provider
     *            The provider to create the proxy for.
     * @return The proxy for communicating with the provider
     */
    @SuppressWarnings("unchecked")
    public <P extends ProviderProxy> P create(Provider<?> provider) {
        switch (provider.getType()) {
        case FOREMAN:
            return (P) new ForemanHostProviderProxy(provider);

        case OPENSTACK_NETWORK:
            return (P) new OpenstackNetworkProviderProxy((Provider<OpenstackNetworkProviderProperties>) provider);

        case OPENSTACK_IMAGE:
            return (P) new OpenStackImageProviderProxy((Provider<OpenStackImageProviderProperties>) provider);

        default:
            return null;
        }
    }

    /**
     * Return the {@link ProviderProxyFactory} for using as a dependency.
     *
     * @return The {@link ProviderProxyFactory}
     */
    public static ProviderProxyFactory getInstance() {
        return INSTANCE;
    }
}
