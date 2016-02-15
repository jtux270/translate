package org.ovirt.engine.ui.userportal.section.main.view.tab.basic;

import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.ui.common.utils.ElementIdUtils;
import org.ovirt.engine.ui.common.view.AbstractView;
import org.ovirt.engine.ui.uicommonweb.ErrorPopupManager;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalItemModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.ApplicationResources;
import org.ovirt.engine.ui.userportal.section.main.presenter.tab.basic.MainTabBasicListItemPresenterWidget;
import org.ovirt.engine.ui.userportal.widget.basic.MainTabBasicListItemActionButton;
import org.ovirt.engine.ui.userportal.widget.basic.MainTabBasicListItemMessagesTranslator;
import org.ovirt.engine.ui.userportal.widget.basic.OsTypeImage;
import org.ovirt.engine.ui.userportal.widget.basic.VmPausedImage;
import org.ovirt.engine.ui.userportal.widget.basic.VmUpMaskImage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class MainTabBasicListItemView extends AbstractView implements MainTabBasicListItemPresenterWidget.ViewDef {

    interface ViewUiBinder extends UiBinder<Widget, MainTabBasicListItemView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface Driver extends SimpleBeanEditorDriver<UserPortalItemModel, MainTabBasicListItemView> {
    }

    public interface Style extends CssResource {

        String itemOverStyle();

        String itemNotRunningStyle();

        String itemRunningStyle();

        String machineStatusSelectedStyle();

        String machineStatusStyle();

        String itemSelectedStyle();

        String handCursor();

        String defaultCursor();

        String runButtonAdditionalStyle();

        String shutdownButtonAdditionalStyle();

        String suspendButtonAdditionalStyle();

        String rebootButtonAdditionalStyle();
    }

    private final Driver driver = GWT.create(Driver.class);

    @UiField
    @Path("osId")
    OsTypeImage osTypeImage;

    @UiField
    @Path("isVmUp")
    VmUpMaskImage vmUpImage;

    @UiField
    @Path("status")
    VmPausedImage vmPausedImage;

    @UiField(provided = true)
    @Path("status")
    ValueLabel<VMStatus> vmStatus;

    @UiField
    @Path("name")
    Label vmName;

    @UiField
    @Ignore
    FlowPanel buttonsPanel;

    @UiField
    @Ignore
    SimplePanel consoleBaner;

    @UiField
    @Ignore
    LayoutPanel mainContainer;

    @UiField
    Style style;

    private final ApplicationResources resources;
    private final ApplicationConstants constants;
    private final ErrorPopupManager errorPopupManager;

    private MainTabBasicListItemActionButton runButton;
    private MainTabBasicListItemActionButton shutdownButton;
    private MainTabBasicListItemActionButton suspendButton;
    private MainTabBasicListItemActionButton rebootButton;

    @Inject
    public MainTabBasicListItemView(
            ApplicationResources applicationResources,
            ApplicationResources resources,
            ApplicationConstants constants,
            ErrorPopupManager errorPopupManager,
            final MainTabBasicListItemMessagesTranslator translator) {
        this.resources = resources;
        this.constants = constants;
        this.errorPopupManager = errorPopupManager;

        vmStatus = new ValueLabel<VMStatus>(new AbstractRenderer<VMStatus>() {
            @Override
            public String render(VMStatus object) {
                return translator.translate(object.name());
            }
        });

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        consoleBaner.setVisible(false);

        driver.initialize(this);
    }

    @Override
    public HasClickHandlers addRunButton() {
        MainTabBasicListItemActionButton button = new MainTabBasicListItemActionButton(
                null, resources.playIcon(), resources.playDisabledIcon(),
                style.runButtonAdditionalStyle());
        this.runButton = button;
        addButtonToPanel(button);
        return button;
    }

    @Override
    public void updateRunButton(UICommand command, boolean isPool) {
        runButton.setTitle(isPool ? constants.takeVm() : constants.runVm());
        updateButton(runButton, command);
    }

    @Override
    public HasClickHandlers addShutdownButton() {
        MainTabBasicListItemActionButton button = new MainTabBasicListItemActionButton(
                constants.shutdownVm(), resources.stopIcon(), resources.stopDisabledIcon(),
                style.shutdownButtonAdditionalStyle());
        this.shutdownButton = button;
        addButtonToPanel(button);
        return button;
    }

    @Override
    public void updateShutdownButton(UICommand command) {
        updateButton(shutdownButton, command);
    }

    @Override
    public HasClickHandlers addSuspendButton() {
        MainTabBasicListItemActionButton button = new MainTabBasicListItemActionButton(
                constants.suspendVm(), resources.suspendIcon(), resources.suspendDisabledIcon(),
                style.suspendButtonAdditionalStyle());
        this.suspendButton = button;
        addButtonToPanel(button);
        return button;
    }

    @Override
    public void updateSuspendButton(UICommand command) {
        updateButton(suspendButton, command);
    }

    @Override
    public HasClickHandlers addRebootButton() {
        MainTabBasicListItemActionButton button = new MainTabBasicListItemActionButton(
                constants.rebootVm(),
                resources.rebootIcon(),
                resources.rebootDisabledIcon(),
                style.rebootButtonAdditionalStyle());
        this.rebootButton = button;
        addButtonToPanel(button);
        return button;
    }

    @Override
    public void updateRebootButton(UICommand command) {
        updateButton(rebootButton, command);
    }

    void addButtonToPanel(MainTabBasicListItemActionButton button) {
        buttonsPanel.add(button);
    }

    void updateButton(MainTabBasicListItemActionButton button, UICommand command) {
        button.setEnabled(command != null ? command.getIsExecutionAllowed() : false);
    }

    @Override
    public void edit(UserPortalItemModel model) {
        driver.edit(model);
    }

    @Override
    public UserPortalItemModel flush() {
        return driver.flush();
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        // No-op, the handlers are on the widget itself
    }

    @Override
    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
        return asWidget().addDomHandler(handler, MouseOverEvent.getType());
    }

    @Override
    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
        return asWidget().addDomHandler(handler, MouseOutEvent.getType());
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return asWidget().addDomHandler(handler, ClickEvent.getType());
    }

    @Override
    public void showDoubleClickBanner() {
        consoleBaner.setVisible(true);
        mainContainer.addStyleName(style.handCursor());
    }

    @Override
    public void hideDoubleClickBanner() {
        consoleBaner.setVisible(false);
        mainContainer.addStyleName(style.defaultCursor());
    }

    @Override
    public void setVmUpStyle() {
        mainContainer.setStyleName(style.itemRunningStyle());
    }

    @Override
    public void setVmDownStyle() {
        mainContainer.setStyleName(style.itemNotRunningStyle());
    }

    @Override
    public void setMouseOverStyle() {
        mainContainer.setStyleName(style.itemOverStyle());
    }

    @Override
    public void setSelected() {
        vmStatus.setStyleName(style.machineStatusSelectedStyle());
        mainContainer.setStyleName(style.itemSelectedStyle());
    }

    @Override
    public void setNotSelected(boolean vmIsUp) {
        vmStatus.setStyleName(style.machineStatusStyle());
        if (vmIsUp) {
            mainContainer.setStyleName(style.itemRunningStyle());
        } else {
            mainContainer.setStyleName(style.itemNotRunningStyle());
        }
    }

    @Override
    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
        return mainContainer.addDomHandler(handler, DoubleClickEvent.getType());
    }

    @Override
    public void showErrorDialog(String message) {
        errorPopupManager.show(message);
    }

    @Override
    public void setElementId(String elementId) {
        vmName.getElement().setId(
                ElementIdUtils.createElementId(elementId, "name")); //$NON-NLS-1$
        vmStatus.getElement().setId(
                ElementIdUtils.createElementId(elementId, "status")); //$NON-NLS-1$

        runButton.setElementId(
                ElementIdUtils.createElementId(elementId, "runButton")); //$NON-NLS-1$
        shutdownButton.setElementId(
                ElementIdUtils.createElementId(elementId, "shutdownButton")); //$NON-NLS-1$
        suspendButton.setElementId(
                ElementIdUtils.createElementId(elementId, "suspendButton")); //$NON-NLS-1$
        rebootButton.setElementId(
                ElementIdUtils.createElementId(elementId, "rebootButton")); //$NON-NLS-1$
    }

}
