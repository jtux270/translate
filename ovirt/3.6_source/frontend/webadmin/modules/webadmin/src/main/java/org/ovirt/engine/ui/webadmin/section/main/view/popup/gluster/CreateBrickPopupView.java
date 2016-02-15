package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import org.ovirt.engine.core.common.businessentities.RaidType;
import org.ovirt.engine.core.common.businessentities.gluster.StorageDevice;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SizeConverter;
import org.ovirt.engine.core.common.utils.SizeConverter.SizeUnit;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.EntityModelWidgetWithInfo;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelObjectCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.label.EnableableFormLabel;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.CreateBrickModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.UIMessages;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.CreateBrickPopupPresenterWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class CreateBrickPopupView extends AbstractModelBoundPopupView<CreateBrickModel> implements CreateBrickPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<CreateBrickModel, CreateBrickPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, CreateBrickPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<CreateBrickPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    interface WidgetStyle extends CssResource {
        String editorContentWidget();

        String forceEditorWidget();
    }

    @UiField
    @Path(value = "lvName.entity")
    @WithElementId
    StringEntityModelTextBoxEditor lvNameEditor;

    @UiField
    @Path(value = "mountPoint.entity")
    @WithElementId
    StringEntityModelTextBoxEditor mountPointEditor;

    @UiField
    @Ignore
    @WithElementId
    Label deviceHeader;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    ListModelObjectCellTable<StorageDevice, ListModel<StorageDevice>> deviceTable;

    @UiField
    @Path(value = "size.entity")
    @WithElementId
    StringEntityModelLabelEditor sizeEditor;

    @UiField
    @Ignore
    Label raidParamsLabel;

    @UiField(provided = true)
    @Ignore
    InfoIcon raidParamsInfoIcon;

    @UiField(provided = true)
    @Path(value = "raidTypeList.selectedItem")
    @WithElementId
    ListModelListBoxEditor<RaidType> raidTypeEditor;

    @UiField
    @Ignore
    Label deviceSelectionInfo;

    @UiField
    @Path(value = "noOfPhysicalDisksInRaidVolume.entity")
    @WithElementId
    IntegerEntityModelTextBoxEditor noOfPhysicalDisksEditor;

    @UiField(provided = true)
    @WithElementId("stripeSize")
    public EntityModelWidgetWithInfo stripeSizeWithInfo;

    @Ignore
    @WithElementId("stripeSizeLabel")
    public EnableableFormLabel stripeSizeLabel;

    @Path(value = "stripeSize.entity")
    @WithElementId("stripeSizeEditor")
    IntegerEntityModelTextBoxOnlyEditor stripeSizeEditor;

    @UiField
    @Ignore
    Label messageLabel;

    private final static ApplicationConstants constants = AssetProvider.getConstants();
    private final static UIMessages messages = ConstantsManager.getInstance().getMessages();
    private final static ApplicationTemplates templates = AssetProvider.getTemplates();
    private final static ApplicationMessages applicationMessages = AssetProvider.getMessages();

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public CreateBrickPopupView(EventBus eventBus) {
        super(eventBus);
        initListBoxEditors();
        initInfoIcon();
        deviceTable = new ListModelObjectCellTable<StorageDevice, ListModel<StorageDevice>>(true, false);
        stripeSizeLabel = new EnableableFormLabel();
        stripeSizeEditor = new IntegerEntityModelTextBoxOnlyEditor();
        stripeSizeWithInfo = new EntityModelWidgetWithInfo(stripeSizeLabel, stripeSizeEditor);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize();
        initTableColumns();
        driver.initialize(this);
    }

    private void initListBoxEditors() {
        raidTypeEditor = new ListModelListBoxEditor<RaidType>(new EnumRenderer<RaidType>());
    }

    protected void initTableColumns() {
        // Table Entity Columns
        deviceTable.addColumn(new AbstractTextColumn<StorageDevice>() {
            @Override
            public String getValue(StorageDevice entity) {
                return entity.getName();
            }
        }, constants.deviceName());

        deviceTable.addColumnAndSetWidth(new AbstractTextColumn<StorageDevice>() {
            @Override
            public String getValue(StorageDevice entity) {
                return entity.getDevType();
            }
        }, constants.deviceType(), "100px"); //$NON-NLS-1$

        deviceTable.addColumnAndSetWidth(new AbstractTextColumn<StorageDevice>() {
            @Override
            public String getValue(StorageDevice entity) {
                Pair<SizeUnit, Double> convertedSize = SizeConverter.autoConvert(entity.getSize(), SizeUnit.MiB);
                return messages.sizeUnitString(formatSize(convertedSize.getSecond()), convertedSize.getFirst()); //$NON-NLS-1$
            }
        }, constants.size(), "100px"); //$NON-NLS-1$
    }

    private void localize() {
        lvNameEditor.setLabel(constants.logicalVolume());
        mountPointEditor.setLabel(constants.mountPoint());
        sizeEditor.setLabel(constants.lvSize());
        raidParamsLabel.setText(constants.raidParameters());
        raidTypeEditor.setLabel(constants.raidType());
        noOfPhysicalDisksEditor.setLabel(constants.noOfPhysicalDisksInRaidVolume());
        stripeSizeLabel.setText(constants.stripeSize());
        deviceHeader.setText(constants.storageDevices());
    }

    private void initInfoIcon() {
        raidParamsInfoIcon = new InfoIcon(templates.italicText(constants.raidConfigurationWarning()));
    }

    @Override
    public void edit(final CreateBrickModel object) {
        deviceTable.asEditor().edit(object.getStorageDevices());
        driver.edit(object);
        deviceSelectionInfo.setText(null);
        setRaidParamsVisibility(false);
    }

    @Override
    public CreateBrickModel flush() {
        deviceTable.flush();
        return driver.flush();
    }

    public String formatSize(double size) {
        return NumberFormat.getFormat("#.##").format(size);//$NON-NLS-1$
    }

    @Override
    public void setRaidInfoMessages(String raidType, int stripeSize) {
        deviceSelectionInfo.setText(applicationMessages.getStorageDeviceSelectionInfo(raidType)); //$NON-NLS-1$ //$NON-NLS-2$
        this.stripeSizeWithInfo.setExplanation(templates.italicText(applicationMessages.stripSizeInfoForGlusterBricks(stripeSize,
                raidType)));

    }

    @Override
    public void setRaidParamsVisibility(boolean isVisiable) {
        deviceSelectionInfo.setVisible(isVisiable);
        stripeSizeWithInfo.setVisible(isVisiable);
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }
}
