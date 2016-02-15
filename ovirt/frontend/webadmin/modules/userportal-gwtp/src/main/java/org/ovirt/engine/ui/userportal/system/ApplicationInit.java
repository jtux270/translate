package org.ovirt.engine.ui.userportal.system;

import org.ovirt.engine.ui.common.auth.CurrentUser;
import org.ovirt.engine.ui.common.system.BaseApplicationInit;
import org.ovirt.engine.ui.common.system.LockInteractionManager;
import org.ovirt.engine.ui.common.uicommon.ClientAgentType;
import org.ovirt.engine.ui.common.uicommon.FrontendEventsHandlerImpl;
import org.ovirt.engine.ui.common.uicommon.FrontendFailureEventListener;
import org.ovirt.engine.ui.common.uicommon.model.ModelInitializedEvent;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.ITypeResolver;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalLoginModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.userportal.ApplicationDynamicMessages;
import org.ovirt.engine.ui.userportal.auth.UserPortalCurrentUserRole;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelInitEvent;
import org.ovirt.engine.ui.userportal.utils.ConnectAutomaticallyManager;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.gwtplatform.mvp.client.proxy.PlaceManager;

public class ApplicationInit extends BaseApplicationInit<UserPortalLoginModel> {

    private final PlaceManager placeManager;
    private final UserPortalCurrentUserRole userRole;
    private final ConnectAutomaticallyManager connectAutomaticallyManager;
    private final ClientAgentType clientAgentType;
    private final ApplicationDynamicMessages dynamicMessages;

    @Inject
    public ApplicationInit(ITypeResolver typeResolver,
            FrontendEventsHandlerImpl frontendEventsHandler,
            FrontendFailureEventListener frontendFailureEventListener,
            CurrentUser user,
            EventBus eventBus,
            Provider<UserPortalLoginModel> loginModelProvider,
            LockInteractionManager lockInteractionManager,
            Frontend frontend,
            PlaceManager placeManager,
            UserPortalCurrentUserRole userRole,
            ConnectAutomaticallyManager connectAutomaticallyManager,
            ClientAgentType clientAgentType,
            ApplicationDynamicMessages dynamicMessages) {
        super(typeResolver, frontendEventsHandler, frontendFailureEventListener,
                user, eventBus, loginModelProvider, lockInteractionManager, frontend, userRole);
        this.placeManager = placeManager;
        this.userRole = userRole;
        this.connectAutomaticallyManager = connectAutomaticallyManager;
        this.clientAgentType = clientAgentType;
        this.dynamicMessages = dynamicMessages;
    }

    @Override
    public void onBootstrap() {
        super.onBootstrap();
        Window.setTitle(dynamicMessages.applicationTitle());

        // Initiate transition to requested application place
        placeManager.revealCurrentPlace();
    }

    @Override
    protected void beforeLogin(UserPortalLoginModel loginModel) {
        UserPortalModelInitEvent.fire(eventBus);
        ModelInitializedEvent.fire(eventBus);
    }

    @Override
    protected boolean filterFrontendQueries() {
        return true;
    }

    @Override
    protected void initLoginModel() {
        super.initLoginModel();

        final UserPortalLoginModel loginModel = getLoginModel();

        // Login model "IsENGINEUser" property determines the availability
        // of the "Extended" main tab and starts the actual login operation
        loginModel.getIsENGINEUser().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                Boolean isEngineUser = loginModel.getIsENGINEUser().getEntity();

                if (isEngineUser != null) {
                    userRole.setEngineUser(isEngineUser);

                    // Proceed with login operation
                    performLogin(loginModel);
                }
            }
        });
    }

    @Override
    protected void onLogin(UserPortalLoginModel loginModel) {
        // Instead of performing login now, request update for "IsENGINEUser" property
        loginModel.updateIsENGINEUser(loginModel.getLoggedUser());
    }

}
