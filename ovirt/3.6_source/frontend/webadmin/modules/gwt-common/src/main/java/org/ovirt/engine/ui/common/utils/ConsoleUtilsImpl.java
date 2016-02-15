package org.ovirt.engine.ui.common.utils;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.common.uicommon.ClientAgentType;
import org.ovirt.engine.ui.uicommonweb.Configurator;
import org.ovirt.engine.ui.uicommonweb.ConsoleUtils;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ConsoleProtocol;
import com.google.inject.Inject;

public class ConsoleUtilsImpl implements ConsoleUtils {

    private final ClientAgentType clientAgentType;
    private final Configurator configurator;

    @Inject
    public ConsoleUtilsImpl(Configurator configurator, ClientAgentType clientAgentType) {
        this.configurator = configurator;
        this.clientAgentType = clientAgentType;
    }

    @Override
    public boolean isSpiceProxyDefined(VM vm) {
        return !StringHelper.isNullOrEmpty((String) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.SpiceProxyDefault)) ||
            !StringHelper.isNullOrEmpty(vm.getVdsGroupSpiceProxy()) ||
            !StringHelper.isNullOrEmpty(vm.getVmPoolSpiceProxy());
    }

    /**
     * HTML5-based console clients are only supported when websocket proxy is configured in the engine and run on
     * browsers that support postMessage correctly.
     * @return true if HTML5 console clients can be used with current engine configuration and client browser.
     */
    @Override
    public boolean webBasedClientsSupported() {
        return configurator.isWebSocketProxyDefined() && !configurator.isClientWindowsExplorer();
    }

    @Override
    public boolean isBrowserPluginSupported(ConsoleProtocol protocol) {
        switch (protocol) {
        case SPICE:
            if ((clientAgentType.os.equalsIgnoreCase("Windows") //$NON-NLS-1$
                    && (clientAgentType.browser.equalsIgnoreCase("Explorer")) //$NON-NLS-1$
                    && (clientAgentType.version >= 7.0))
                    || clientAgentType.isIE11()) {
                return true;
            } else if ((clientAgentType.os.equalsIgnoreCase("Linux")) //$NON-NLS-1$
                    && (clientAgentType.browser.equalsIgnoreCase("Firefox")) //$NON-NLS-1$
                    && (clientAgentType.version >= 2.0)) {
                return true;
            }
            return false;
        case RDP:
            if ((clientAgentType.os.equalsIgnoreCase("Windows")//$NON-NLS-1$
                    && (clientAgentType.browser.equalsIgnoreCase("Explorer"))//$NON-NLS-1$
                    && (clientAgentType.version >= 7.0))
                    || clientAgentType.isIE11()) {
                return true;
            }
            return false;
        default:
            return false;
        }
    }

    public boolean isIE11() {
        return clientAgentType.isIE11();
    }

}
