package org.ovirt.engine.ui.webadmin.section.main.view.popup.provider;

import org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelPasswordBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.uicommonweb.models.storage.LibvirtSecretModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.provider.ProviderSecretPopupPresenterWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.inject.Inject;

public class ProviderSecretPopupView extends AbstractModelBoundPopupView<LibvirtSecretModel> implements ProviderSecretPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<LibvirtSecretModel, ProviderSecretPopupView> {
    }

    private final Driver driver = GWT.create(Driver.class);

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ProviderSecretPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ProviderSecretPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField(provided = true)
    @Path(value = "usageType.selectedItem")
    @WithElementId
    ListModelListBoxEditor<LibvirtSecretUsageType> usageTypeEditor;

    @UiField
    @Path(value = "uuid.entity")
    @WithElementId
    StringEntityModelTextBoxEditor uuidEditor;

    @UiField
    @Path(value = "value.entity")
    @WithElementId
    StringEntityModelPasswordBoxEditor valueEditor;

    @UiField
    @Path(value = "description.entity")
    @WithElementId
    StringEntityModelTextBoxEditor descriptionEditor;

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Inject
    public ProviderSecretPopupView(EventBus eventBus) {
        super(eventBus);
        initManualWidgets();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize();
        driver.initialize(this);
    }

    @SuppressWarnings({ "unchecked" })
    private void initManualWidgets() {
        usageTypeEditor = new ListModelListBoxEditor<>(new EnumRenderer());
    }

    void localize() {
        usageTypeEditor.setLabel(constants.usageTypeLibvirtSecret());
        descriptionEditor.setLabel(constants.descriptionLabel());
        uuidEditor.setLabel(constants.idLibvirtSecret());
        valueEditor.setLabel(constants.valueLibvirtSecret());
    }

    @Override
    public void edit(LibvirtSecretModel model) {
        driver.edit(model);
    }

    @Override
    public LibvirtSecretModel flush() {
        return driver.flush();
    }

    @Override
    public void focusInput() {
        uuidEditor.setFocus(true);
    }

    @Override
    public int setTabIndexes(int nextTabIndex) {
        usageTypeEditor.setTabIndex(nextTabIndex++);
        uuidEditor.setTabIndex(nextTabIndex++);
        valueEditor.setTabIndex(nextTabIndex++);
        descriptionEditor.setTabIndex(nextTabIndex++);
        return nextTabIndex;
    }

}
