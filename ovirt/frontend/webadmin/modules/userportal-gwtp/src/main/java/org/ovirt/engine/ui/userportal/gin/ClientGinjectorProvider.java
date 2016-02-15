package org.ovirt.engine.ui.userportal.gin;

import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.ClientAgentType;
import org.ovirt.engine.ui.userportal.ApplicationResources;
import org.ovirt.engine.ui.userportal.ApplicationResourcesWithLookup;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Provides static access to common dependencies.
 *
 * @deprecated This class is meant to support existing code that used to access {@code ClientGinjector} directly. New
 *             code should <b>not</b> use this class, always {@code @Inject} specific dependencies directly.
 */
@Deprecated
public class ClientGinjectorProvider {

    @Inject
    static Provider<EventBus> eventBusProvider;

    public static EventBus getEventBus() {
        return eventBusProvider.get();
    }

    @Inject
    static Provider<ApplicationResources> applicationResourcesProvider;

    public static ApplicationResources getApplicationResources() {
        return applicationResourcesProvider.get();
    }

    @Inject
    static Provider<ApplicationResourcesWithLookup> applicationResourcesWithLookupProvider;

    public static ApplicationResourcesWithLookup getApplicationResourcesWithLookup() {
        return applicationResourcesWithLookupProvider.get();
    }

    @Inject
    static Provider<ClientStorage> clientStorageProvider;

    public static ClientStorage getClientStorage() {
        return clientStorageProvider.get();
    }

    @Inject
    static Provider<ClientAgentType> clientAgentTypeProvider;

    public static ClientAgentType getClientAgentType() {
        return clientAgentTypeProvider.get();
    }

}
