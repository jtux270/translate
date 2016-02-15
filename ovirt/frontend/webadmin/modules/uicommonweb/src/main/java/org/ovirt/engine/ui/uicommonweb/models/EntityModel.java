package org.ovirt.engine.ui.uicommonweb.models;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.ui.uicommonweb.Convertible;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.ValidationResult;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.IProvidePropertyChangedEvent;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.ProvidePropertyChangedEvent;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

@SuppressWarnings("unused")
public class EntityModel<T> extends Model implements HasHandlers {

    /**
     * The GWT event bus.
     */
    private EventBus eventBus;

    private final List<HandlerRegistration> handlerRegistrations = new ArrayList<HandlerRegistration>();

    public static final EventDefinition entityChangedEventDefinition;
    private Event privateEntityChangedEvent;

    public Event getEntityChangedEvent()
    {
        return privateEntityChangedEvent;
    }

    private void setEntityChangedEvent(Event value)
    {
        privateEntityChangedEvent = value;
    }

    private T entity;

    public T getEntity()
    {
        return entity;
    }

    public void setEntity(T value)
    {
        if (entity != value)
        {
            entityChanging(value, entity);
            entity = value;
            onEntityChanged();
            // EntityChanged(this, EventArgs.Empty);
            getEntityChangedEvent().raise(this, EventArgs.EMPTY);
            onPropertyChanged(new PropertyChangedEventArgs("Entity")); //$NON-NLS-1$
        }
    }

    @Override
    public EntityModel<T> setIsChangable(boolean value) {
        super.setIsChangable(value);
        return this;
    }

    @Override
    public EntityModel<T> setTitle(String value) {
        super.setTitle(value);
        return this;
    }

    public void setEntity(T value, boolean fireEvents) {
        if (fireEvents) {
            setEntity(value);
        }
        else {
            entity = value;
        }
    }

    static
    {
        entityChangedEventDefinition = new EventDefinition("EntityChanged", EntityModel.class); //$NON-NLS-1$
    }

    public EntityModel()
    {
        setEntityChangedEvent(new Event(entityChangedEventDefinition));
    }

    public EntityModel(T entity) {
        this();

        setEntity(entity);
    }

    public EntityModel(String title, T entity) {
        this(entity);

        setTitle(title);
    }

    protected void entityChanging(T newValue, T oldValue)
    {
        IProvidePropertyChangedEvent notifier =
                (IProvidePropertyChangedEvent) ((oldValue instanceof IProvidePropertyChangedEvent) ? oldValue : null);
        if (notifier != null)
        {
            notifier.getPropertyChangedEvent().removeListener(this);
        }

        notifier =
                (IProvidePropertyChangedEvent) ((newValue instanceof IProvidePropertyChangedEvent) ? newValue : null);
        if (notifier != null)
        {
            notifier.getPropertyChangedEvent().addListener(this);
        }
    }

    protected void onEntityChanged()
    {
    }

    /**
     * Invoked whenever some property of the entity was changed.
     */
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(entityChangedEventDefinition))
        {
            onEntityChanged();
        }
        else if (ev.matchesDefinition(ProvidePropertyChangedEvent.definition))
        {
            entityPropertyChanged(sender, (PropertyChangedEventArgs) args);
        }
    }

    public void validateEntity(IValidation[] validations)
    {
        setIsValid(true);

        if (!getIsAvailable() || !getIsChangable())
        {
            return;
        }

        for (IValidation validation : validations)
        {
            ValidationResult result = validation.validate(getEntity());
            if (!result.getSuccess())
            {
                for (String reason : result.getReasons())
                {
                    getInvalidityReasons().add(reason);
                }
                setIsValid(false);

                break;
            }
        }
    }

    public Convertible asConvertible() {
        return new Convertible(this);
    }

    /**
     * Get the GWT event bus.
     * @return The {@code EventBus}, can be null.
     */
    protected final EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Set the GWT event bus.
     * @param eventBus The {@code EventBus}, can be null.
     */
    public final void setEventBus(EventBus eventBus) {
        assert eventBus != null : "EventBus cannot be null"; //$NON-NLS-1$
        assert this.eventBus == null : "EventBus is already set"; //$NON-NLS-1$
        this.eventBus = eventBus;
        registerHandlers();
    }

    /**
     * Unset the GWT event bus, use this when cleaning up models.
     */
    public final void unsetEventBus() {
        unregisterHandlers();
        this.eventBus = null;
    }

    /**
     * Register handlers after the {@code EventBus} has been set.
     * <p>
     * Make sure to use {@link #registerHandler} to ensure proper
     * handler cleanup when {@link #unsetEventBus} is called.
     */
    protected void registerHandlers() {
        // No-op, override as necessary
    }

    /**
     * Register a handler.
     * @param reg The {@code HandlerRegistration} returned from registering a handler.
     */
    public final void registerHandler(HandlerRegistration reg) {
        if (reg != null && !handlerRegistrations.contains(reg)) {
            handlerRegistrations.add(reg);
        }
    }

    /**
     * Unregister all registered handlers.
     */
    public final void unregisterHandlers() {
        for (HandlerRegistration reg: handlerRegistrations) {
            reg.removeHandler(); // can't call unregisterHandler(reg) as that would modify the list during iteration
        }
        handlerRegistrations.clear();
    }

    /**
     * Unregister a specific handler using its {@code HandlerRegistration}.
     * @param reg The {@code HandlerRegistration} to use to remove the handler.
     */
    public final void unregisterHandler(HandlerRegistration reg) {
        if (reg != null) {
            reg.removeHandler();
            handlerRegistrations.remove(reg);
        }
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        getEventBus().fireEvent(event);
    }

}
