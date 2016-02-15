package org.ovirt.engine.ui.webadmin.section.main.view.popup.qos;

import org.ovirt.engine.core.common.businessentities.qos.StorageQos;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.qos.StorageQosParametersModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;

public class StorageQosWidget extends QosWidget<StorageQos, StorageQosParametersModel> {

    interface Driver extends SimpleBeanEditorDriver<StorageQosParametersModel, StorageQosWidget> {
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, StorageQosWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<StorageQosWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    interface WidgetStyle extends CssResource {
        String valueWidth();
    }

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @Path(value = "throughput.choiceGroupTotal.entity")
    @WithElementId
    EntityModelRadioButtonEditor throughputTotalRadioButton;

    @UiField(provided = true)
    @Path(value = "throughput.choiceGroupNone.entity")
    @WithElementId
    EntityModelRadioButtonEditor throughputNoneRadioButton;

    @UiField(provided = true)
    @Path(value = "throughput.choiceGroupReadWrite.entity")
    @WithElementId
    EntityModelRadioButtonEditor throughputReadWriteRadioButton;

    @UiField(provided = true)
    @Path(value = "iops.choiceGroupTotal.entity")
    @WithElementId
    EntityModelRadioButtonEditor iopsTotalRadioButton;

    @UiField(provided = true)
    @Path(value = "iops.choiceGroupNone.entity")
    @WithElementId
    EntityModelRadioButtonEditor iopsNoneRadioButton;

    @UiField(provided = true)
    @Path(value = "iops.choiceGroupReadWrite.entity")
    @WithElementId
    EntityModelRadioButtonEditor iopsReadWriteRadioButton;


    @UiField
    @Path(value = "throughput.total.entity")
    @WithElementId
    IntegerEntityModelTextBoxOnlyEditor throughputTotalEditor;

    @UiField
    @Path(value = "throughput.read.entity")
    @WithElementId
    IntegerEntityModelTextBoxOnlyEditor throughputReadEditor;

    @UiField
    @Path(value = "throughput.write.entity")
    @WithElementId
    IntegerEntityModelTextBoxOnlyEditor throughputWriteEditor;

    @UiField
    @Path(value = "iops.total.entity")
    @WithElementId
    IntegerEntityModelTextBoxOnlyEditor iopsTotalEditor;

    @UiField
    @Path(value = "iops.read.entity")
    @WithElementId
    IntegerEntityModelTextBoxOnlyEditor iopsReadEditor;

    @UiField
    @Path(value = "iops.write.entity")
    @WithElementId
    IntegerEntityModelTextBoxOnlyEditor iopsWriteEditor;

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    public StorageQosWidget() {
        throughputTotalRadioButton = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        throughputNoneRadioButton = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        throughputReadWriteRadioButton = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$

        iopsTotalRadioButton = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
        iopsNoneRadioButton = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
        iopsReadWriteRadioButton = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);

        setStyle();
        localize();

        driver = GWT.create(Driver.class);
        driver.initialize(this);
    }

    private void setStyle() {
        throughputTotalEditor.setContentWidgetContainerStyleName(style.valueWidth());
        throughputReadEditor.setContentWidgetContainerStyleName(style.valueWidth());
        throughputWriteEditor.setContentWidgetContainerStyleName(style.valueWidth());
        iopsTotalEditor.setContentWidgetContainerStyleName(style.valueWidth());
        iopsReadEditor.setContentWidgetContainerStyleName(style.valueWidth());
        iopsWriteEditor.setContentWidgetContainerStyleName(style.valueWidth());
    }

    private void localize() {
        throughputTotalEditor.setTitle(constants.totalStorageQosPopup() + constants.mbpsLabelStorageQosPopup());
        throughputReadEditor.setTitle(constants.readStorageQosPopup() + constants.mbpsLabelStorageQosPopup());
        throughputWriteEditor.setTitle(constants.writeStorageQosPopup() + constants.mbpsLabelStorageQosPopup());
        iopsTotalEditor.setTitle(constants.totalStorageQosPopup() + constants.iopsCountLabelQosPopup());
        iopsReadEditor.setTitle(constants.readStorageQosPopup() + constants.iopsCountLabelQosPopup());
        iopsWriteEditor.setTitle(constants.writeStorageQosPopup() + constants.iopsCountLabelQosPopup());
    }

}
