package org.ovirt.engine.ui.common.uicommon.model;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.ModelBoundPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.CommonModel;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.inject.Provider;

/**
 * Basic {@link ModelProvider} implementation that uses {@link CommonModelManager} for accessing the CommonModel
 * instance.
 *
 * @param <M>
 *            Model type.
 */
public abstract class TabModelProvider<M extends EntityModel> implements ModelProvider<M>, ModelBoundPopupResolver<M>, HasHandlers {

    private final EventBus eventBus;
    private final ModelBoundPopupHandler<M> popupHandler;

    public TabModelProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider) {
        this.eventBus = eventBus;

        // Configure UiCommon dialog handler
        this.popupHandler = new ModelBoundPopupHandler<M>(this, eventBus);
        this.popupHandler.setDefaultConfirmPopupProvider(defaultConfirmPopupProvider);

        // Handle UiCommon model initialization to attach appropriate listeners
        eventBus.addHandler(ModelInitializedEvent.getType(), new ModelInitializedEvent.ModelInitializedHandler() {
            @Override
            public void onModelInitialized(ModelInitializedEvent event) {
                initializeModelHandlers();
            }
        });
    }

    protected boolean hasModel() {
        return getCommonModel() != null && getModel() != null;
    }

    protected EventBus getEventBus() {
        return eventBus;
    }

    protected CommonModel getCommonModel() {
        return CommonModelManager.instance();
    }

    /**
     * Callback fired when the {@link CommonModel} reference changes.
     * <p>
     * Override this method to register custom listeners on the corresponding model.
     */
    protected void initializeModelHandlers() {
        // Register dialog model property change listener
        popupHandler.addDialogModelListener(getModel());

        // Register WidgetModel property change listener
        getModel().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).propertyName;

                if ("WidgetModel".equals(propName)) { //$NON-NLS-1$
                    modelBoundWidgetChange();
                }
            }
        });

        // Initialize model if necessary
        if (!reusesModel()) {
            getModel().setEventBus(getEventBus());
        }
    }

    /**
     * Override this and return true if your model provider uses the same model as another model provider.
     * This way this model provider will not try to initialize the model again.
     * @return {@code true if the provider re-uses a model}, {@code false otherwise}
     */
    protected boolean reusesModel() {
        return false;
    }

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

    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(M source,
            UICommand lastExecutedCommand, Model windowModel) {
        // No-op, override as necessary
        return null;
    }

    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(M source,
            UICommand lastExecutedCommand) {
        // No-op, override as necessary
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
