package org.ovirt.engine.ui.userportal.section.main.presenter.tab.basic;

import java.util.Arrays;

import org.ovirt.engine.ui.common.idhandler.HasElementId;
import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.VmConsoles;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalBasicListModel;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalItemModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelInitEvent;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelInitEvent.UserPortalModelInitHandler;
import org.ovirt.engine.ui.userportal.uicommon.model.basic.UserPortalBasicListProvider;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;

public class MainTabBasicListItemPresenterWidget extends PresenterWidget<MainTabBasicListItemPresenterWidget.ViewDef> implements MouseOutHandler, MouseOverHandler, ClickHandler, DoubleClickHandler {

    public interface ViewDef extends View, HasEditorDriver<UserPortalItemModel>, HasMouseOutHandlers, HasMouseOverHandlers, HasClickHandlers, HasDoubleClickHandlers, HasElementId {

        void showDoubleClickBanner();

        void hideDoubleClickBanner();

        void setVmUpStyle();

        void setVmDownStyle();

        void setMouseOverStyle();

        void setSelected();

        void setNotSelected(boolean vmIsUp);

        void showErrorDialog(String message);

        HasClickHandlers addRunButton();

        void updateRunButton(UICommand command, boolean isPool);

        HasClickHandlers addShutdownButton();

        void updateShutdownButton(UICommand command);

        HasClickHandlers addSuspendButton();

        void updateSuspendButton(UICommand command);

        HasClickHandlers addRebootButton();

        void updateRebootButton(UICommand command);

    }

    private final UserPortalBasicListProvider listModelProvider;

    private UserPortalItemModel model;

    private UserPortalBasicListModel listModel;

    private IEventListener selectedItemChangeListener;

    @Inject
    public MainTabBasicListItemPresenterWidget(EventBus eventBus, ViewDef view, final UserPortalBasicListProvider listModelProvider) {
        super(eventBus, view);
        this.listModelProvider = listModelProvider;

        registerHandler(getEventBus().addHandler(UserPortalModelInitEvent.getType(), new UserPortalModelInitHandler() {
            @Override
            public void onUserPortalModelInit(UserPortalModelInitEvent event) {
                updateListModel(listModelProvider.getModel());
            }
        }));

        updateListModel(listModelProvider.getModel());
    }

    void addSelectedItemChangeHandler() {
        selectedItemChangeListener = new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (!sameEntity(listModel.getSelectedItem(), model)) {
                    getView().setNotSelected(model.isVmUp());
                } else {
                    getView().setSelected();
                }
            }
        };
        listModel.getSelectedItemChangedEvent().addListener(selectedItemChangeListener);
    }

    void removeSelectedItemChangeHandler() {
        if (listModel != null && selectedItemChangeListener != null) {
            listModel.getSelectedItemChangedEvent().removeListener(selectedItemChangeListener);
            selectedItemChangeListener = null;
        }
    }

    void updateListModel(UserPortalBasicListModel newListModel) {
        // Remove SelectedItemChangedEvent listener from current list model
        removeSelectedItemChangeHandler();

        // Update current list model
        this.listModel = newListModel;

        // Add electedItemChangedEvent listener to new list model
        addSelectedItemChangeHandler();
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(getView().addMouseOutHandler(this));
        registerHandler(getView().addMouseOverHandler(this));
        registerHandler(getView().addClickHandler(this));
        registerHandler(getView().addDoubleClickHandler(this));

        // Add buttons to the view
        registerHandler(getView().addRunButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                executeCommand(getRunCommand());
            }
        }));
        registerHandler(getView().addShutdownButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                executeCommand(getShutdownCommand());
            }
        }));
        registerHandler(getView().addSuspendButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                executeCommand(getSuspendCommand());
            }
        }));
        registerHandler(getView().addRebootButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                executeCommand(getRebootCommand());
            }
        }));
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        removeSelectedItemChangeHandler();
    }

    void executeCommand(UICommand command) {
        if (command != null) {
            command.execute();
        }
    }

    UICommand getRunCommand() {
        return model.isPool() ? model.getTakeVmCommand() : model.getRunCommand();
    }

    UICommand getShutdownCommand() {
        return model.getShutdownCommand();
    }

    UICommand getSuspendCommand() {
        return model.getPauseCommand();
    }

    UICommand getRebootCommand() {
        return model.getRebootCommand();
    }

    /**
     * Updates the item presenter widget with new data.
     */
    public void setModel(UserPortalItemModel model) {
        this.model = model;

        setupDefaultVmStyles();

        getView().updateRunButton(getRunCommand(), model.isPool());
        getView().updateShutdownButton(getShutdownCommand());
        getView().updateSuspendButton(getSuspendCommand());
        getView().updateRebootButton(getRebootCommand());

        getView().edit(model);

        if (sameEntity(listModel.getSelectedItem(), model)) {
            setSelectedItem();
        }
    }

    protected boolean sameEntity(UserPortalItemModel prevModel, UserPortalItemModel newModel) {
        if (prevModel == null || newModel == null) {
            return false;
        }
        return listModelProvider.getKey(prevModel).equals(listModelProvider.getKey(newModel));
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        getView().hideDoubleClickBanner();
        setupDefaultVmStyles();
    }

    void setupDefaultVmStyles() {
        if (!isSelected()) {
            if (model.isVmUp()) {
                getView().setVmUpStyle();
            } else {
                getView().setVmDownStyle();
            }
        }
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
        if (!isSelected()) {
            getView().setMouseOverStyle();
        }
        if (!model.isPool() && model.getVmConsoles().canConnectToConsole()) {
            getView().showDoubleClickBanner();
        }
    }

    @Override
    public void onDoubleClick(DoubleClickEvent event) {
        try {
            if (model.getVmConsoles().canConnectToConsole()) {
                model.getVmConsoles().connect();
            }
        } catch (VmConsoles.ConsoleConnectException e) {
            getView().showErrorDialog(e.getLocalizedErrorMessage());
        }
    }

    @Override
    public void onClick(ClickEvent event) {
        if (!isSelected()) {
            setSelectedItem();
        }
    }

    void setSelectedItem() {
        listModel.setSelectedItem(model);
        listModelProvider.setSelectedItems(Arrays.asList(model));
        getView().setSelected();
    }

    boolean isSelected() {
        return sameEntity(listModel.getSelectedItem(), model);
    }
}
