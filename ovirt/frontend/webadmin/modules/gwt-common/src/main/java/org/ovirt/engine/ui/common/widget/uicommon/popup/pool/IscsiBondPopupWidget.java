package org.ovirt.engine.ui.common.widget.uicommon.popup.pool;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.ValidatedPanelWidget;
import org.ovirt.engine.ui.common.widget.editor.ListModelObjectCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.IscsiBondModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class IscsiBondPopupWidget extends AbstractModelBoundPopupWidget<IscsiBondModel> {

    interface Driver extends SimpleBeanEditorDriver<IscsiBondModel, IscsiBondPopupWidget> {
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, IscsiBondPopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<IscsiBondPopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    @Path("name.entity")
    @WithElementId("name")
    StringEntityModelTextBoxEditor nameEditor;

    @UiField
    @Path("description.entity")
    @WithElementId("description")
    StringEntityModelTextBoxEditor descriptionEditor;

    @UiField
    ValidatedPanelWidget logicalNetworksPanel;

    @UiField
    ValidatedPanelWidget storageTargetsPanel;

    @Ignore
    @WithElementId
    ListModelObjectCellTable<Network, ListModel> networksTable;

    @Ignore
    @WithElementId
    ListModelObjectCellTable<StorageServerConnections, ListModel> connectionsTable;

    private final Driver driver = GWT.create(Driver.class);

    public IscsiBondPopupWidget(CommonApplicationConstants constants) {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(constants);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initLogicalNetworksPanel();
        initNetworksTable(constants);
        initStorageConnectionsPanel();
        initConnectionsTable(constants);
        driver.initialize(this);
    }

    private void initLogicalNetworksPanel() {
        VerticalPanel panel = new VerticalPanel();
        networksTable = new ListModelObjectCellTable<Network, ListModel>(true);
        panel.add(networksTable);
        logicalNetworksPanel.setWidget(panel);
    }

    private void initStorageConnectionsPanel() {
        VerticalPanel panel = new VerticalPanel();
        connectionsTable = new ListModelObjectCellTable<StorageServerConnections, ListModel>(true);
        panel.add(connectionsTable);
        storageTargetsPanel.setWidget(panel);
    }

    private void initNetworksTable(CommonApplicationConstants constants) {
        networksTable.enableColumnResizing();

        TextColumnWithTooltip<Network> nameColumn = new TextColumnWithTooltip<Network>() {
            @Override
            public String getValue(Network network) {
                return network.getName();
            }
        };
        networksTable.addColumn(nameColumn, constants.name(), "40%"); //$NON-NLS-1$

        TextColumnWithTooltip<Network> descriptionColumn = new TextColumnWithTooltip<Network>() {
            @Override
            public String getValue(Network network) {
                return network.getDescription();
            }
        };
        networksTable.addColumn(descriptionColumn, constants.description(), "60%"); //$NON-NLS-1$

        networksTable.setWidth("100%", true); //$NON-NLS-1$
    }

    private void initConnectionsTable(CommonApplicationConstants constants) {
        connectionsTable.enableColumnResizing();

        TextColumnWithTooltip<StorageServerConnections> iqnColumn = new TextColumnWithTooltip<StorageServerConnections>() {
            @Override
            public String getValue(StorageServerConnections conn) {
                return conn.getiqn();
            }
        };
        connectionsTable.addColumn(iqnColumn, constants.iqn(), "40%"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageServerConnections> addressColumn = new TextColumnWithTooltip<StorageServerConnections>() {
            @Override
            public String getValue(StorageServerConnections conn) {
                return conn.getconnection();
            }
        };
        connectionsTable.addColumn(addressColumn, constants.addressSanStorage(), "30%"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageServerConnections> portColumn = new TextColumnWithTooltip<StorageServerConnections>() {
            @Override
            public String getValue(StorageServerConnections conn) {
                return conn.getport();
            }
        };
        connectionsTable.addColumn(portColumn, constants.portSanStorage(), "30%"); //$NON-NLS-1$

        connectionsTable.setWidth("100%", true); //$NON-NLS-1$
    }

    private void localize(CommonApplicationConstants constants) {
        nameEditor.setLabel(constants.name());
        descriptionEditor.setLabel(constants.description());
    }

    @Override
    public void edit(final IscsiBondModel model) {
        driver.edit(model);
        networksTable.asEditor().edit(model.getNetworks());
        connectionsTable.asEditor().edit(model.getStorageTargets());

        model.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).propertyName;
                if (propName.equals("IsValid")) { //$NON-NLS-1$
                    if (model.getIsValid()) {
                        logicalNetworksPanel.markAsValid();
                    } else {
                        logicalNetworksPanel.markAsInvalid(model.getInvalidityReasons());
                    }
                }
            }
        });
    }

    @Override
    public IscsiBondModel flush() {
        return driver.flush();
    }
}
