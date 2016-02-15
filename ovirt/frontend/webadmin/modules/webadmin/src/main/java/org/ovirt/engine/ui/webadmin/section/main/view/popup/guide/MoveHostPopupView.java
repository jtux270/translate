package org.ovirt.engine.ui.webadmin.section.main.view.popup.guide;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.MoveHost;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.guide.MoveHostPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

public class MoveHostPopupView extends AbstractModelBoundPopupView<MoveHost> implements MoveHostPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<MoveHost, MoveHostPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, MoveHostPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<MoveHostPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<MoveHost> table;

    @UiField(provided = true)
    @Path(value = "cluster.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Object> clusterListEditor;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public MoveHostPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initListBoxEditors();
        localize(constants);
        initTable(constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        driver.initialize(this);
    }

    private void localize(ApplicationConstants constants) {
        clusterListEditor.setLabel(constants.moveHostPopupClusterLabel());
    }

    private void initListBoxEditors() {
        clusterListEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((VDSGroup) object).getName();
            }
        });
    }

    private void initTable(ApplicationConstants constants) {
        table = new EntityModelCellTable<MoveHost>(true);
        table.setWidth("100%", true); //$NON-NLS-1$

        TextColumnWithTooltip<EntityModel> nameColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel object) {
                return ((VDS) object.getEntity()).getName();
            }
        };
        table.addColumn(nameColumn, constants.nameHost());

        TextColumnWithTooltip<EntityModel> hostColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel object) {
                return ((VDS) object.getEntity()).getHostName();
            }
        };
        table.addColumn(hostColumn, constants.ipHost());

        TextColumnWithTooltip<EntityModel> statusColumn = new EnumColumn<EntityModel, VDSStatus>() {
            @Override
            public VDSStatus getRawValue(EntityModel object) {
                return ((VDS) object.getEntity()).getStatus();
            }
        };
        table.addColumn(statusColumn, constants.statusHost(), "90px"); //$NON-NLS-1$
    }

    @Override
    public void edit(MoveHost object) {
        if (!object.isMultiSelection()) {
            table.setSelectionModel(new SingleSelectionModel<EntityModel>());
            table.addSelectionChangeHandler();
        }

        driver.edit(object);
        table.asEditor().edit(object);
    }

    @Override
    public MoveHost flush() {
        return table.asEditor().flush();
    }

}
