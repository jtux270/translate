package org.ovirt.engine.ui.common.uicommon;

import com.google.inject.Inject;
import org.ovirt.engine.ui.uicommonweb.Configurator;
import org.ovirt.engine.ui.uicommonweb.ConsoleOptionsFrontendPersister;
import org.ovirt.engine.ui.uicommonweb.ConsoleUtils;
import org.ovirt.engine.ui.uicommonweb.ErrorPopupManager;
import org.ovirt.engine.ui.uicommonweb.ILogger;
import org.ovirt.engine.ui.uicommonweb.ITimer;
import org.ovirt.engine.ui.uicommonweb.ITypeResolver;
import org.ovirt.engine.ui.uicommonweb.auth.CurrentUserRole;
import org.ovirt.engine.ui.uicommonweb.models.vms.INoVnc;
import org.ovirt.engine.ui.uicommonweb.models.vms.IRdpNative;
import org.ovirt.engine.ui.uicommonweb.models.vms.IRdpPlugin;
import org.ovirt.engine.ui.uicommonweb.models.vms.ISpiceHtml5;
import org.ovirt.engine.ui.uicommonweb.models.vms.ISpiceNative;
import org.ovirt.engine.ui.uicommonweb.models.vms.ISpicePlugin;
import org.ovirt.engine.ui.uicommonweb.models.vms.IVncNative;

public class UiCommonDefaultTypeResolver implements ITypeResolver {

    private final Configurator configurator;
    private final ILogger logger;

    private final ConsoleOptionsFrontendPersister consoleOptionsFrontendPersister;
    private final ConsoleUtils consoleUtils;
    private final ErrorPopupManager errorPopupManager;
    private CurrentUserRole currentUserRole;

    @Inject
    public UiCommonDefaultTypeResolver(Configurator configurator, ILogger logger,
            ConsoleUtils consoleUtils,  ErrorPopupManager errorPopupManager,
            ConsoleOptionsFrontendPersister consoleOptionsFrontendPersister,
            CurrentUserRole currentUserRole) {
        this.configurator = configurator;
        this.logger = logger;
        this.consoleOptionsFrontendPersister = consoleOptionsFrontendPersister;
        this.consoleUtils = consoleUtils;
        this.errorPopupManager = errorPopupManager;
        this.currentUserRole = currentUserRole;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object resolve(Class type) {
        if (type == Configurator.class) {
            return configurator;
        } else if (type == ILogger.class) {
            return logger;
        } else if (type == ITimer.class) {
            return new TimerImpl();
        } else if (type == ISpicePlugin.class) {
            return new SpicePluginImpl();
        } else if (type == ISpiceNative.class) {
            return new SpiceNativeImpl();
        } else if (type == ISpiceHtml5.class) {
            return new SpiceHtml5Impl();
        } else if (type == IRdpPlugin.class) {
            return new RdpPluginImpl();
        } else if (type == IRdpNative.class) {
            return new RdpNativeImpl();
        } else if (type == INoVnc.class) {
            return new NoVncImpl();
        } else if (type == IVncNative.class) {
            return new VncNativeImpl();
        } else if (type == ConsoleOptionsFrontendPersister.class) {
            return consoleOptionsFrontendPersister;
        } else if (type == ConsoleUtils.class) {
            return consoleUtils;
        } else if (type == ErrorPopupManager.class) {
            return errorPopupManager;
        } else if (type == CurrentUserRole.class) {
            return currentUserRole;
        }

        throw new RuntimeException("UiCommon Resolver cannot resolve type: " + type); //$NON-NLS-1$
    }

}
