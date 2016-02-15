package org.ovirt.engine.ui.common.widget.action;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.logical.shared.InitializeEvent;
import com.google.gwt.event.logical.shared.InitializeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Base implementation of {@link ActionButtonDefinition} interface.
 *
 * @param <T>
 *            Action panel item type.
 */
public abstract class AbstractButtonDefinition<T> implements ActionButtonDefinition<T> {

    protected final EventBus eventBus;

    private final List<HandlerRegistration> handlerRegistrations = new ArrayList<HandlerRegistration>();

    protected final SafeHtml title;

    // Indicates whether the given feature is implemented in WebAdmin
    private final boolean implInWebAdmin;

    // Indicates whether the given feature is implemented in UserPortal
    private final boolean implInUserPortal;

    // Indicates whether the given feature is available only from a context menu
    private final CommandLocation commandLocation;

    // Indicates whether this action button has a title action
    private final boolean subTitledAction;

    private final String toolTip;

    private boolean isCascaded = false;

    public AbstractButtonDefinition(EventBus eventBus, String title,
            boolean implInWebAdmin, boolean implInUserPortal,
            CommandLocation commandLocation, boolean subTitledAction,
            String toolTip) {
        this.eventBus = eventBus;
        this.title = SafeHtmlUtils.fromSafeConstant(title);
        this.implInWebAdmin = implInWebAdmin;
        this.implInUserPortal = implInUserPortal;
        this.commandLocation = commandLocation;
        this.subTitledAction = subTitledAction;
        this.toolTip = toolTip;
    }

    public AbstractButtonDefinition(EventBus eventBus, String title,
            CommandLocation commandLocation, boolean subTitledAction,
            String toolTip) {
        this(eventBus, title, true, false, commandLocation, subTitledAction, toolTip);
    }

    public AbstractButtonDefinition(EventBus eventBus, String title,
            CommandLocation commandLocation) {
        this(eventBus, title, commandLocation, false, null);
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        eventBus.fireEvent(event);
    }

    /**
     * Releases all handlers associated with this button definition.
     */
    public void releaseAllHandlers() {
        for (HandlerRegistration reg : handlerRegistrations) {
            reg.removeHandler();
        }
        handlerRegistrations.clear();
    }

    /**
     * Registers a handler that will be removed when calling {@link #releaseAllHandlers}.
     */
    protected void registerHandler(HandlerRegistration reg) {
        handlerRegistrations.add(reg);
    }

    @Override
    public HandlerRegistration addInitializeHandler(InitializeHandler handler) {
        HandlerRegistration reg = eventBus.addHandler(InitializeEvent.getType(), handler);
        registerHandler(reg);
        return reg;
    }

    @Override
    public boolean isVisible(List<T> selectedItems) {
        return true;
    }

    @Override
    public boolean isCascaded() {
        return isCascaded;
    }

    @Override
    public void setCascaded(final boolean value) {
        isCascaded = value;
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public void update() {
        // Trigger InitializeEvent to update corresponding button widget
        InitializeEvent.fire(this);
    }

    @Override
    public SafeHtml getEnabledHtml() {
        return title;
    }

    @Override
    public SafeHtml getDisabledHtml() {
        return title;
    }

    @Override
    public String getTitle() {
        return title.asString();
    }

    @Override
    public boolean isImplemented() {
        return implInWebAdmin;
    }

    @Override
    public boolean isImplInUserPortal() {
        return implInUserPortal;
    }

    @Override
    public CommandLocation getCommandLocation() {
        return commandLocation;
    }

    @Override
    public boolean isSubTitledAction() {
        return subTitledAction;
    }

    @Override
    public String getButtonToolTip() {
        return null;
    }

    @Override
    public String getMenuItemToolTip() {
        return toolTip;
    }

}
