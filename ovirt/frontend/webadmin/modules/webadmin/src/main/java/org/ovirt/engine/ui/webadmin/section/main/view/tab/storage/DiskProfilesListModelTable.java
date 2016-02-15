package org.ovirt.engine.ui.webadmin.section.main.view.tab.storage;

import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.profiles.DiskProfile;
import org.ovirt.engine.core.common.businessentities.qos.StorageQos;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundTableWidget;
import org.ovirt.engine.ui.common.widget.uicommon.permissions.PermissionWithInheritedPermissionListModelTable;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.profiles.DiskProfileListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.webadmin.uicommon.model.DiskProfilePermissionModelProvider;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;

public class DiskProfilesListModelTable extends AbstractModelBoundTableWidget<DiskProfile, DiskProfileListModel> {

    interface WidgetUiBinder extends UiBinder<Widget, DiskProfilesListModelTable> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    private final PermissionWithInheritedPermissionListModelTable<PermissionListModel> permissionListModelTable;

    @UiField
    SplitLayoutPanel splitLayoutPanel;

    @UiField
    SimplePanel tableContainer;

    SimplePanel permissionContainer;

    private boolean permissionPanelVisible = false;

    private final DiskProfilePermissionModelProvider diskProfilePermissionModelProvider;

    public DiskProfilesListModelTable(SearchableTableModelProvider<DiskProfile, DiskProfileListModel> modelProvider,
            DiskProfilePermissionModelProvider diskProfilePermissionModelProvider,
            EventBus eventBus,
            ClientStorage clientStorage,
            CommonApplicationConstants constants) {
        super(modelProvider, eventBus, clientStorage, false);
        this.diskProfilePermissionModelProvider = diskProfilePermissionModelProvider;
        // Create disk profile table
        tableContainer.add(getTable());

        // Create permission panel
        permissionListModelTable =
                new PermissionWithInheritedPermissionListModelTable<PermissionListModel>(diskProfilePermissionModelProvider,
                        eventBus,
                        clientStorage);
        permissionListModelTable.initTable(constants);
        permissionContainer = new SimplePanel();
        permissionContainer.add(permissionListModelTable);
    }

    @Override
    protected Widget getWrappedWidget() {
        return WidgetUiBinder.uiBinder.createAndBindUi(this);
    }

    @Override
    public void initTable(final CommonApplicationConstants constants) {
        getTable().enableColumnResizing();

        TextColumnWithTooltip<DiskProfile> nameColumn =
                new TextColumnWithTooltip<DiskProfile>() {
                    @Override
                    public String getValue(DiskProfile object) {
                        return object.getName();
                    }
                };
        getTable().addColumn(nameColumn, constants.profileNameLabel(), "200px"); //$NON-NLS-1$
        nameColumn.makeSortable();

        TextColumnWithTooltip<DiskProfile> descriptionColumn =
                new TextColumnWithTooltip<DiskProfile>() {
                    @Override
                    public String getValue(DiskProfile object) {
                        return object.getDescription();
                    }
                };
        getTable().addColumn(descriptionColumn, constants.profileDescriptionLabel(), "200px"); //$NON-NLS-1$
        descriptionColumn.makeSortable();

        TextColumnWithTooltip<DiskProfile> qosColumn = new TextColumnWithTooltip<DiskProfile>() {
            @Override
            public String getValue(DiskProfile object) {
                String name = constants.unlimitedQos();
                if (object.getQosId() != null) {
                    StorageQos storageQos = getModel().getQos(object.getQosId());
                    if (storageQos != null) {
                        name = storageQos.getName();
                    }
                }
                return name;
            }
        };
        getTable().addColumn(qosColumn, constants.storageQosName(), "200px"); //$NON-NLS-1$
        qosColumn.makeSortable();

        getTable().addActionButton(new WebAdminButtonDefinition<DiskProfile>(constants.newProfile()) {
            @Override
            protected UICommand resolveCommand() {
                return getModel().getNewCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<DiskProfile>(constants.editProfile()) {
            @Override
            protected UICommand resolveCommand() {
                return getModel().getEditCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<DiskProfile>(constants.removeProfile()) {
            @Override
            protected UICommand resolveCommand() {
                return getModel().getRemoveCommand();
            }
        });

        // Add selection listener
        getModel().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                updatePermissionPanel();
            }
        });

        getModel().getItemsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                updatePermissionPanel();
            }
        });
    }

    private void updatePermissionPanel() {
        final DiskProfile diskProfile = (DiskProfile) getModel().getSelectedItem();
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                if (permissionPanelVisible && diskProfile == null) {
                    splitLayoutPanel.clear();
                    splitLayoutPanel.add(tableContainer);
                    permissionPanelVisible = false;
                } else if (!permissionPanelVisible && diskProfile != null) {
                    splitLayoutPanel.clear();
                    splitLayoutPanel.addEast(permissionContainer, 600);
                    splitLayoutPanel.add(tableContainer);
                    permissionPanelVisible = true;
                }
            }
        });
    }

    @Override
    public void addModelListeners() {
        final SimpleActionTable<Permissions> table = permissionListModelTable.getTable();
        table.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
                    @Override
                    public void onSelectionChange(SelectionChangeEvent event) {
                        diskProfilePermissionModelProvider.setSelectedItems(table.getSelectedItems());
                    }
                });
    }
}
