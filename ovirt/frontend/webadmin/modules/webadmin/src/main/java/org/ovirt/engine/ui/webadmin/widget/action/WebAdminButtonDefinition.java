package org.ovirt.engine.ui.webadmin.widget.action;

import org.ovirt.engine.ui.common.widget.action.CommandLocation;
import org.ovirt.engine.ui.common.widget.action.UiCommandButtonDefinition;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;

import com.google.gwt.event.shared.EventBus;

public abstract class WebAdminButtonDefinition<T> extends UiCommandButtonDefinition<T> {

    public WebAdminButtonDefinition(String title, CommandLocation commandLocation,
            boolean subTitledAction, String toolTip) {
        super(getEventBus(), title, commandLocation, subTitledAction, toolTip);
    }

    public WebAdminButtonDefinition(String title, CommandLocation commandLocation) {
        super(getEventBus(), title, commandLocation);
    }

    public WebAdminButtonDefinition(String title) {
        super(getEventBus(), title);
    }

    static EventBus getEventBus() {
        return ClientGinjectorProvider.getEventBus();
    }

}
