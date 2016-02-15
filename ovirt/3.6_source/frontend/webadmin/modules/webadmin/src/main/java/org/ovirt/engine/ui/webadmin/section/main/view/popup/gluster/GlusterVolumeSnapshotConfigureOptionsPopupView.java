package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.table.column.AbstractEntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.GlusterVolumeSnapshotConfigModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.VolumeSnapshotOptionModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.GlusterVolumeSnapshotConfigureOptionsPopupPresenterWidget;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.inject.Inject;

public class GlusterVolumeSnapshotConfigureOptionsPopupView extends AbstractModelBoundPopupView<GlusterVolumeSnapshotConfigModel> implements GlusterVolumeSnapshotConfigureOptionsPopupPresenterWidget.ViewDef {
    interface Driver extends SimpleBeanEditorDriver<GlusterVolumeSnapshotConfigModel, GlusterVolumeSnapshotConfigureOptionsPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, GlusterVolumeSnapshotConfigureOptionsPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<GlusterVolumeSnapshotConfigureOptionsPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    @Ignore
    @WithElementId
    Label snapshotConfigHeader;

    @UiField
    @Path(value = "clusterName.entity")
    @WithElementId
    StringEntityModelLabelEditor clusterNameEditor;

    @UiField
    @Path(value = "volumeName.entity")
    @WithElementId
    StringEntityModelLabelEditor volumeNameEditor;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel<EntityModel<VolumeSnapshotOptionModel>>> configsTable;

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public GlusterVolumeSnapshotConfigureOptionsPopupView(EventBus eventBus) {
        super(eventBus);
        initEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize();
        driver.initialize(this);
    }

    private void initEditors() {
        configsTable =
                new EntityModelCellTable<ListModel<EntityModel<VolumeSnapshotOptionModel>>>(false, true);
        configsTable.setSelectionModel(new NoSelectionModel());

        configsTable.addColumn(new AbstractEntityModelTextColumn<VolumeSnapshotOptionModel>() {
            @Override
            public String getText(VolumeSnapshotOptionModel object) {
                return object.getOptionName();
            }
        }, constants.volumeSnapshotConfigName(), "150px"); //$NON-NLS-1$

        configsTable.addColumn(new AbstractEntityModelTextColumn<VolumeSnapshotOptionModel>() {
            @Override
            public String getText(VolumeSnapshotOptionModel object) {
                return object.getCorrespodingClusterValue();
            }
        }, constants.clusterSnapshotConfigValue(), "200px"); //$NON-NLS-1$

        Column<EntityModel, String> valueColumn = new Column<EntityModel, String>(new TextInputCell()) {
            @Override
            public String getValue(EntityModel object) {
                return ((VolumeSnapshotOptionModel)object.getEntity()).getOptionValue();
            }
        };
        configsTable.addColumn(valueColumn, constants.volumeSnapshotConfigValue(), "100px"); //$NON-NLS-1$

        valueColumn.setFieldUpdater(new FieldUpdater<EntityModel, String>() {

            @Override
            public void update(int index, EntityModel object, String value) {
                ((VolumeSnapshotOptionModel)object.getEntity()).setOptionValue(value);
            }
        });
    }

    private void localize() {
        clusterNameEditor.setLabel(constants.volumeClusterLabel());
        volumeNameEditor.setLabel(constants.volumeNameLabel());
        snapshotConfigHeader.setText(constants.snapshotConfigHeaderLabel());
    }

    @Override
    public void edit(final GlusterVolumeSnapshotConfigModel object) {
        driver.edit(object);
        configsTable.asEditor().edit(object.getConfigOptions());
    }

    @Override
    public GlusterVolumeSnapshotConfigModel flush() {
        return driver.flush();
    }
}
