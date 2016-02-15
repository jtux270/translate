package org.ovirt.engine.ui.common.uicommon.model;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.ModelBoundPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.HasEntity;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Base {@link ModelProvider} implementation. Decorates a Provider<M> that gets injected by GIN.
 *
 * Provides a model to something, usually preparing it along the way.
 *
 * TODO rename BaseModelProvider
 *
 * @param <M> model type being provided
 */
public abstract class TabModelProvider<M extends HasEntity> implements ModelProvider<M>, ModelBoundPopupResolver<M>, HasHandlers {

    private final EventBus eventBus;
    private final ModelBoundPopupHandler<M> popupHandler;
    private boolean modelInitialized = false;

    // Decorated model provider that gets setter-injected by GIN.
    protected Provider<M> modelProvider;

    public TabModelProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider) {
        this.eventBus = eventBus;

        // Configure UiCommon dialog handler
        this.popupHandler = new ModelBoundPopupHandler<M>(this, eventBus);
        this.popupHandler.setDefaultConfirmPopupProvider(defaultConfirmPopupProvider);
    }

    @Override
    public M getModel() {
        M model = modelProvider.get();
        if (!modelInitialized) {
            modelInitialized = true;
            initializeModelHandlers(model);
        }
        return model;
    }


    @Inject
    public void setModelProvider(Provider<M> modelProvider) {
        this.modelProvider = modelProvider;
    }

    protected EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Adds a property change listener to the model that calls popupHandler.handleWindowModelChange()
     * when a new window model is set.
     *
     * Register WidgetModel property change listener that calls modelBoundWidgetChange().
     *
     * Override this method to register custom listeners on the corresponding model.
     *
     * TODO: rename initializeModelListeners / 'Handlers' is the wrong word
     */
    protected void initializeModelHandlers(M model) {
        // Add a property change listener to the model that responds when a new window model is set.
        // TODO should be refactored to read "model.addDialogModelListener(popupHandler)", which is semantically correct
        popupHandler.addDialogModelListener(model);

        // Register WidgetModel property change listener
        model.getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                String propName = args.propertyName;

                if ("WidgetModel".equals(propName)) { //$NON-NLS-1$
                    modelBoundWidgetChange();
                }
            }
        });
    }

    /**
     * Called when the widget-model property of a model is changed
     * TODO this seems important. What does it do? What is a widget model?
     */
    @SuppressWarnings("unchecked")
    void modelBoundWidgetChange() {
        UICommand lastExecutedCommand = getModel().getLastExecutedCommand();
        ModelBoundPresenterWidget<?> modelBoundPresenterWidget = getModelBoundWidget(lastExecutedCommand);
        ((ModelBoundPresenterWidget<Model>) modelBoundPresenterWidget).init(getModel().getWidgetModel());
    }

    @Override
    public String[] getWindowPropertyNames() {
        return new String[] { "Window" }; //$NON-NLS-1$
    }

    @Override
    public Model getWindowModel(M source, String propertyName) {
        return source.getWindow();
    }

    @Override
    public void clearWindowModel(M source, String propertyName) {
        source.setWindow(null);
    }

    @Override
    public String[] getConfirmWindowPropertyNames() {
        return new String[] { "ConfirmWindow" }; //$NON-NLS-1$
    }

    @Override
    public Model getConfirmWindowModel(M source, String propertyName) {
        return source.getConfirmWindow();
    }

    @Override
    public void clearConfirmWindowModel(M source, String propertyName) {
        source.setConfirmWindow(null);
    }

    /* (non-Javadoc)
     * @see org.ovirt.engine.ui.common.uicommon.model.ModelBoundPopupResolver#getModelPopup(org.ovirt.engine.ui.uicommonweb.models.IModel, org.ovirt.engine.ui.uicommonweb.UICommand, org.ovirt.engine.ui.uicommonweb.models.Model)
     */
    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(M sourceModel,
            UICommand lastExecutedCommand, Model windowModel) {

        // No-op by default.
        // Override if you need to figure out which popup presenter widget is responsible for rendering the popup for windowModel.

        return null;
    }

    /* (non-Javadoc)
     * @see org.ovirt.engine.ui.common.uicommon.model.ModelBoundPopupResolver#getConfirmModelPopup(org.ovirt.engine.ui.uicommonweb.models.IModel, org.ovirt.engine.ui.uicommonweb.UICommand)
     */
    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(M sourceModel,
            UICommand lastExecutedCommand) {

        // No-op by default.
        // Override if you need to figure out which popup presenter widget is responsible for rendering the popup for windowModel.

        return null;
    }

    protected ModelBoundPresenterWidget<? extends Model> getModelBoundWidget(UICommand lastExecutedCommand) {
        // No-op, override as necessary
        return null;
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        getEventBus().fireEvent(event);
    }

}
