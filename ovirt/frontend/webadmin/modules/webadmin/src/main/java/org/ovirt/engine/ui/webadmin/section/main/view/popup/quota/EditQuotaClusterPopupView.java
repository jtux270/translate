package org.ovirt.engine.ui.webadmin.section.main.view.popup.quota;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.LongEntityModelTextBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.quota.EditQuotaClusterModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.quota.EditQuotaClusterPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class EditQuotaClusterPopupView extends AbstractModelBoundPopupView<EditQuotaClusterModel> implements EditQuotaClusterPopupPresenterWidget.ViewDef {

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @Path(value = "unlimitedMem.entity")
    @WithElementId
    EntityModelRadioButtonEditor unlimitedMemRadioButtonEditor;

    @UiField(provided = true)
    @Path(value = "specificMem.entity")
    @WithElementId
    EntityModelRadioButtonEditor specificMemRadioButtonEditor;

    @UiField
    @Path(value = "specificMemValue.entity")
    @WithElementId
    LongEntityModelTextBoxEditor memValueEditor;

    @UiField(provided = true)
    @Path(value = "unlimitedCpu.entity")
    @WithElementId
    EntityModelRadioButtonEditor unlimitedCpuRadioButtonEditor;

    @UiField(provided = true)
    @Path(value = "specificCpu.entity")
    @WithElementId
    EntityModelRadioButtonEditor specificCpuRadioButtonEditor;

    @UiField
    @Path(value = "specificCpuValue.entity")
    @WithElementId
    IntegerEntityModelTextBoxEditor cpuValueEditor;

    @UiField
    @Ignore
    Label memLabel;

    @UiField
    @Ignore
    Label cpuLabel;

    interface Driver extends SimpleBeanEditorDriver<EditQuotaClusterModel, EditQuotaClusterPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, EditQuotaClusterPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<EditQuotaClusterPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public EditQuotaClusterPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initRadioButtonEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        addStyles();
        driver.initialize(this);
    }

    private void addStyles() {
        memValueEditor.addContentWidgetStyleName(style.textBoxWidth());
        memValueEditor.addLabelStyleName(style.labelVisible());
        cpuValueEditor.addContentWidgetStyleName(style.textBoxWidth());
        cpuValueEditor.addLabelStyleName(style.labelVisible());
    }

    private void initRadioButtonEditors() {
        unlimitedMemRadioButtonEditor = new EntityModelRadioButtonEditor("3"); //$NON-NLS-1$
        specificMemRadioButtonEditor = new EntityModelRadioButtonEditor("3"); //$NON-NLS-1$
        unlimitedCpuRadioButtonEditor = new EntityModelRadioButtonEditor("4"); //$NON-NLS-1$
        specificCpuRadioButtonEditor = new EntityModelRadioButtonEditor("4"); //$NON-NLS-1$
    }

    void localize(ApplicationConstants constants) {
        unlimitedMemRadioButtonEditor.setLabel(constants.ultQuotaPopup());
        specificMemRadioButtonEditor.setLabel(constants.useQuotaPopup());
        unlimitedCpuRadioButtonEditor.setLabel(constants.ultQuotaPopup());
        specificCpuRadioButtonEditor.setLabel(constants.useQuotaPopup());
        memLabel.setText(constants.memQuotaPopup());
        cpuLabel.setText(constants.cpuQuotaPopup());
    }

    @Override
    public void edit(EditQuotaClusterModel object) {
        driver.edit(object);
    }

    @Override
    public EditQuotaClusterModel flush() {
        return driver.flush();
    }

    interface WidgetStyle extends CssResource {
        String textBoxWidth();

        String radioButtonWidth();

        String labelVisible();
    }

}
