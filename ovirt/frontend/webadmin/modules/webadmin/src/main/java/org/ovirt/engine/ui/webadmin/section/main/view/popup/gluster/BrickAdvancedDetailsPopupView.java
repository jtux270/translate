package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import com.google.gwt.text.shared.Parser;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterClientInfo;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.Mempool;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.DoubleEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextAreaLabelEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.BrickAdvancedDetailsModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.BrickAdvancedDetailsPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import java.text.ParseException;

public class BrickAdvancedDetailsPopupView extends AbstractModelBoundPopupView<BrickAdvancedDetailsModel> implements BrickAdvancedDetailsPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<BrickAdvancedDetailsModel, BrickAdvancedDetailsPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, BrickAdvancedDetailsPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<BrickAdvancedDetailsPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    @WithElementId
    DialogTab generalTab;

    @UiField
    @Path(value = "brick.entity")
    @WithElementId
    StringEntityModelLabelEditor brickEditor;

    @UiField(provided = true)
    @Path(value = "brickProperties.status.entity")
    @WithElementId
    EntityModelLabelEditor<GlusterStatus> statusEditor;

    @UiField
    @Path(value = "brickProperties.port.entity")
    @WithElementId
    IntegerEntityModelLabelEditor portEditor;

    @UiField
    @Path(value = "brickProperties.pid.entity")
    @WithElementId
    IntegerEntityModelLabelEditor pidEditor;

    @UiField
    @Path(value = "brickProperties.totalSize.entity")
    @WithElementId
    DoubleEntityModelLabelEditor totalSizeEditor;

    @UiField
    @Path(value = "brickProperties.freeSize.entity")
    @WithElementId
    DoubleEntityModelLabelEditor freeSizeEditor;

    @UiField
    @Path(value = "brickProperties.device.entity")
    @WithElementId
    StringEntityModelLabelEditor deviceEditor;

    @UiField
    @Path(value = "brickProperties.blockSize.entity")
    @WithElementId
    IntegerEntityModelLabelEditor blockSizeEditor;

    @UiField
    @Path(value = "brickProperties.mountOptions.entity")
    @WithElementId
    StringEntityModelTextAreaLabelEditor mountOptionsEditor;

    @UiField
    @Path(value = "brickProperties.fileSystem.entity")
    @WithElementId
    StringEntityModelLabelEditor fileSystemEditor;

    @UiField
    @WithElementId
    DialogTab clientsTab;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel> clientsTable;

    @UiField
    @WithElementId
    DialogTab memoryStatsTab;

    @UiField
    @Path(value = "memoryStatistics.totalAllocated.entity")
    @WithElementId
    IntegerEntityModelLabelEditor totalAllocatedEditor;

    @UiField
    @Path(value = "memoryStatistics.freeBlocks.entity")
    @WithElementId
    IntegerEntityModelLabelEditor freeBlocksEditor;

    @UiField
    @Path(value = "memoryStatistics.freeFastbin.entity")
    @WithElementId
    IntegerEntityModelLabelEditor freeFastbinBlocksEditor;

    @UiField
    @Path(value = "memoryStatistics.mmappedBlocks.entity")
    @WithElementId
    IntegerEntityModelLabelEditor mmappedBlocksEditor;

    @UiField
    @Path(value = "memoryStatistics.spaceAllocatedMmapped.entity")
    @WithElementId
    IntegerEntityModelLabelEditor spaceAllocatedMmappedEditor;

    @UiField
    @Path(value = "memoryStatistics.maxTotalAllocated.entity")
    @WithElementId
    IntegerEntityModelLabelEditor maxTotalAllocatedEditor;

    @UiField
    @Path(value = "memoryStatistics.spaceFreedFastbin.entity")
    @WithElementId
    IntegerEntityModelLabelEditor spaceFreedFastbinEditor;

    @UiField
    @Path(value = "memoryStatistics.totalAllocatedSpace.entity")
    @WithElementId
    IntegerEntityModelLabelEditor totalAllocatedSpaceEditor;

    @UiField
    @Path(value = "memoryStatistics.totalFreeSpace.entity")
    @WithElementId
    IntegerEntityModelLabelEditor totalFreeSpaceEditor;

    @UiField
    @Path(value = "memoryStatistics.releasableFreeSpace.entity")
    @WithElementId
    IntegerEntityModelLabelEditor releasableFreeSpaceEditor;

    @UiField
    @WithElementId
    DialogTab memoryPoolsTab;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel> memoryPoolsTable;

    @UiField
    @Ignore
    Label messageLabel;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public BrickAdvancedDetailsPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        addStyles();
        initTableColumns(constants);
        localize(constants);
        driver.initialize(this);
    }

    private void initEditors() {
        statusEditor = new EntityModelLabelEditor<GlusterStatus>(new EnumRenderer<GlusterStatus>(), new Parser<GlusterStatus>() {
            @Override
            public GlusterStatus parse(CharSequence text) throws ParseException {
                if (StringHelper.isNullOrEmpty(text.toString())) {
                    return null;
                }
                return GlusterStatus.valueOf(text.toString().toUpperCase());
            }
        });
        clientsTable = new EntityModelCellTable<ListModel>(false, true);
        memoryPoolsTable = new EntityModelCellTable<ListModel>(false, true);
    }

    private void addStyles() {
        brickEditor.addContentWidgetStyleName(style.generalValue());
        statusEditor.addContentWidgetStyleName(style.generalValue());
        portEditor.addContentWidgetStyleName(style.generalValue());
        pidEditor.addContentWidgetStyleName(style.generalValue());
        totalSizeEditor.addContentWidgetStyleName(style.generalValue());
        freeSizeEditor.addContentWidgetStyleName(style.generalValue());
        deviceEditor.addContentWidgetStyleName(style.generalValue());
        blockSizeEditor.addContentWidgetStyleName(style.generalValue());
        mountOptionsEditor.addContentWidgetStyleName(style.generalValue());
        fileSystemEditor.addContentWidgetStyleName(style.generalValue());

        totalAllocatedEditor.addLabelStyleName(style.memStatLabel());
        freeBlocksEditor.addLabelStyleName(style.memStatLabel());
        freeFastbinBlocksEditor.addLabelStyleName(style.memStatLabel());
        mmappedBlocksEditor.addLabelStyleName(style.memStatLabel());
        spaceAllocatedMmappedEditor.addLabelStyleName(style.memStatLabel());
        maxTotalAllocatedEditor.addLabelStyleName(style.memStatLabel());
        spaceFreedFastbinEditor.addLabelStyleName(style.memStatLabel());
        totalAllocatedSpaceEditor.addLabelStyleName(style.memStatLabel());
        totalFreeSpaceEditor.addLabelStyleName(style.memStatLabel());
        releasableFreeSpaceEditor.addLabelStyleName(style.memStatLabel());

        totalAllocatedEditor.addContentWidgetStyleName(style.memStatValue());
        freeBlocksEditor.addContentWidgetStyleName(style.memStatValue());
        freeFastbinBlocksEditor.addContentWidgetStyleName(style.memStatValue());
        mmappedBlocksEditor.addContentWidgetStyleName(style.memStatValue());
        spaceAllocatedMmappedEditor.addContentWidgetStyleName(style.memStatValue());
        maxTotalAllocatedEditor.addContentWidgetStyleName(style.memStatValue());
        spaceFreedFastbinEditor.addContentWidgetStyleName(style.memStatValue());
        totalAllocatedSpaceEditor.addContentWidgetStyleName(style.memStatValue());
        totalFreeSpaceEditor.addContentWidgetStyleName(style.memStatValue());
        releasableFreeSpaceEditor.addContentWidgetStyleName(style.memStatValue());
    }

    private void initTableColumns(ApplicationConstants constants) {
        clientsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterClientInfo>() {
            @Override
            public String getText(GlusterClientInfo entity) {
                return entity.getHostname();
            }
        }, constants.clientBrickAdvancedLabel());

        clientsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterClientInfo>() {
            @Override
            public String getText(GlusterClientInfo entity) {
                return String.valueOf(entity.getClientPort());
            }
        }, constants.clientPortBrickAdvancedLabel());

        clientsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterClientInfo>() {
            @Override
            public String getText(GlusterClientInfo entity) {
                return String.valueOf(entity.getBytesRead());
            }
        }, constants.bytesReadBrickAdvancedLabel());

        clientsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterClientInfo>() {
            @Override
            public String getText(GlusterClientInfo entity) {
                return String.valueOf(entity.getBytesWritten());
            }
        }, constants.bytesWrittenBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return entity.getName();
            }
        }, constants.nameBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getHotCount());
            }
        }, constants.hotCountBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getColdCount());
            }
        }, constants.coldCountBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getPadddedSize());
            }
        }, constants.paddedSizeBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getAllocCount());
            }
        }, constants.allocatedCountBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getMaxAlloc());
            }
        }, constants.maxAllocatedBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getPoolMisses());
            }
        }, constants.poolMissesBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getMaxStdAlloc());
            }
        }, constants.maxStdAllocatedBrickAdvancedLabel());
    }

    private void localize(ApplicationConstants constants) {
        generalTab.setLabel(constants.generalBrickAdvancedPopupLabel());
        brickEditor.setLabel(constants.brickAdvancedLabel());
        statusEditor.setLabel(constants.statusBrickAdvancedLabel());
        portEditor.setLabel(constants.portBrickAdvancedLabel());
        pidEditor.setLabel(constants.pidBrickAdvancedLabel());
        totalSizeEditor.setLabel(constants.totalSizeBrickAdvancedLabel());
        freeSizeEditor.setLabel(constants.freeSizeBrickAdvancedLabel());
        deviceEditor.setLabel(constants.deviceBrickAdvancedLabel());
        blockSizeEditor.setLabel(constants.blockSizeBrickAdvancedLabel());
        mountOptionsEditor.setLabel(constants.mountOptionsBrickAdvancedLabel());
        fileSystemEditor.setLabel(constants.fileSystemBrickAdvancedLabel());

        clientsTab.setLabel(constants.clientsBrickAdvancedPopupLabel());

        memoryStatsTab.setLabel(constants.memoryStatsBrickAdvancedPopupLabel());
        totalAllocatedEditor.setLabel(constants.totalAllocatedBrickAdvancedLabel());
        freeBlocksEditor.setLabel(constants.freeBlocksBrickAdvancedLabel());
        freeFastbinBlocksEditor.setLabel(constants.freeFastbinBlocksBrickAdvancedLabel());
        mmappedBlocksEditor.setLabel(constants.mmappedBlocksBrickAdvancedLabel());
        spaceAllocatedMmappedEditor.setLabel(constants.allocatedInMmappedBlocksBrickAdvancedLabel());
        maxTotalAllocatedEditor.setLabel(constants.maxTotalAllocatedSpaceBrickAdvancedLabel());
        spaceFreedFastbinEditor.setLabel(constants.spaceInFreedFasbinBlocksBrickAdvancedLabel());
        totalAllocatedSpaceEditor.setLabel(constants.totalAllocatedSpaceBrickAdvancedLabel());
        totalFreeSpaceEditor.setLabel(constants.totalFreeSpaceBrickAdvancedLabel());
        releasableFreeSpaceEditor.setLabel(constants.releasableFreeSpaceBrickAdvancedLabel());

        memoryPoolsTab.setLabel(constants.memoryPoolsBrickAdvancedPopupLabel());
    }

    @Override
    public void edit(BrickAdvancedDetailsModel object) {
        driver.edit(object);
        clientsTable.asEditor().edit(object.getClients());
        memoryPoolsTable.asEditor().edit(object.getMemoryPools());
    }

    @Override
    public BrickAdvancedDetailsModel flush() {
        return driver.flush();
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    interface WidgetStyle extends CssResource {
        String memStatLabel();

        String memStatValue();

        String generalValue();
    }

}
