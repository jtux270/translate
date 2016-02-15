package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.DetachGlusterHostsModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.DetachGlusterHostsPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class DetachGlusterHostsPopupView extends AbstractModelBoundPopupView<DetachGlusterHostsModel> implements DetachGlusterHostsPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<DetachGlusterHostsModel, DetachGlusterHostsPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, DetachGlusterHostsPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<DetachGlusterHostsPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel> hostsTable;

    @UiField(provided = true)
    @Path(value = "force.entity")
    @WithElementId
    EntityModelCheckBoxEditor forceEditor;

    @UiField
    @Ignore
    Label messageLabel;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public DetachGlusterHostsPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        hostsTable = new EntityModelCellTable<ListModel>(true, false, true);
        forceEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        initTableColumns(constants);
        driver.initialize(this);
    }

    protected void initTableColumns(ApplicationConstants constants){
        // Table Entity Columns
        hostsTable.addEntityModelColumn(new EntityModelTextColumn<String>() {
            @Override
            public String getText(String hostAddress) {
                return hostAddress;
            }
        }, constants.detachGlusterHostsHostAddress());
    }

    private void localize(ApplicationConstants constants) {
        forceEditor.setLabel(constants.detachGlusterHostsForcefully());
    }

    @Override
    public void edit(DetachGlusterHostsModel object) {
        hostsTable.asEditor().edit(object.getHosts());
        driver.edit(object);
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    @Override
    public DetachGlusterHostsModel flush() {
        hostsTable.flush();
        return driver.flush();
    }

}
