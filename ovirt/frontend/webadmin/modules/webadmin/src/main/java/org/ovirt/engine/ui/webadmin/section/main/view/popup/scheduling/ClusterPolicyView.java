package org.ovirt.engine.ui.webadmin.section.main.view.popup.scheduling;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.scheduling.ClusterPolicy;
import org.ovirt.engine.ui.common.MainTableHeaderlessResources;
import org.ovirt.engine.ui.common.MainTableResources;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.uicommon.model.ClusterPolicyClusterModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.ClusterPolicyModelProvider;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.WebAdminImageResourceColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;

public class ClusterPolicyView extends Composite {

    interface ViewUiBinder extends UiBinder<VerticalPanel, ClusterPolicyView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    SimplePanel clusterPolicyTabContent;

    private SimpleActionTable<ClusterPolicy> table;
    private SimpleActionTable<VDSGroup> clusterTable;
    private SplitLayoutPanel splitLayoutPanel;

    private final ClusterPolicyModelProvider clusterPolicyModelProvider;
    private final ClusterPolicyClusterModelProvider clusterPolicyClusterModelProvider;

    private final EventBus eventBus;
    private final ClientStorage clientStorage;
    private final ApplicationResources applicationResources;

    @Inject
    public ClusterPolicyView(ApplicationConstants constants,
            ApplicationResources applicationResources,
            ClusterPolicyModelProvider clusterPolicyModelProvider,
            ClusterPolicyClusterModelProvider clusterPolicyClusterModelProvider,
            EventBus eventBus, ClientStorage clientStorage) {
        this.applicationResources = applicationResources;
        this.clusterPolicyModelProvider = clusterPolicyModelProvider;
        this.clusterPolicyClusterModelProvider = clusterPolicyClusterModelProvider;
        this.eventBus = eventBus;
        this.clientStorage = clientStorage;

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        //        localize(constants);

        initSplitLayoutPanel();

        initClusterPolicyTable(constants);
        initClustersTable(constants);
    }

    private void initSplitLayoutPanel() {
        splitLayoutPanel = new SplitLayoutPanel();
        splitLayoutPanel.setHeight("100%"); //$NON-NLS-1$
        splitLayoutPanel.setWidth("100%"); //$NON-NLS-1$
        clusterPolicyTabContent.add(splitLayoutPanel);
    }

    public void setSubTabVisibility(boolean visible) {
        splitLayoutPanel.clear();
        if (visible) {
            splitLayoutPanel.addSouth(clusterTable, 150);
        }
        splitLayoutPanel.add(table);
    }

    private void localize(ApplicationConstants constants) {
    }

    private void initClusterPolicyTable(ApplicationConstants constants) {
        table = new SimpleActionTable<ClusterPolicy>(clusterPolicyModelProvider,
                getTableHeaderlessResources(), getTableResources(), eventBus, clientStorage);

        table.addColumn(new WebAdminImageResourceColumn<ClusterPolicy>() {
            @Override
            public ImageResource getValue(ClusterPolicy object) {
                if (object.isLocked()) {
                    return applicationResources.lockImage();
                }
                return null;
            }

        }, constants.empty(), "20px"); //$NON-NLS-1$

        TextColumnWithTooltip<ClusterPolicy> nameColumn = new TextColumnWithTooltip<ClusterPolicy>() {
            @Override
            public String getValue(ClusterPolicy object) {
                return object.getName();
            }
        };
        table.addColumn(nameColumn, constants.clusterPolicyNameLabel(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<ClusterPolicy> descColumn = new TextColumnWithTooltip<ClusterPolicy>() {
            @Override
            public String getValue(ClusterPolicy object) {
                return object.getDescription();
            }
        };
        table.addColumn(descColumn, constants.clusterPolicyDescriptionLabel(), "300px"); //$NON-NLS-1$

        table.addActionButton(new WebAdminButtonDefinition<ClusterPolicy>(constants.newClusterPolicy()) {
            @Override
            protected UICommand resolveCommand() {
                return clusterPolicyModelProvider.getModel().getNewCommand();
            }
        });

        table.addActionButton(new WebAdminButtonDefinition<ClusterPolicy>(constants.editClusterPolicy()) {
            @Override
            protected UICommand resolveCommand() {
                return clusterPolicyModelProvider.getModel().getEditCommand();
            }
        });

        table.addActionButton(new WebAdminButtonDefinition<ClusterPolicy>(constants.copyClusterPolicy()) {
            @Override
            protected UICommand resolveCommand() {
                return clusterPolicyModelProvider.getModel().getCloneCommand();
            }
        });

        table.addActionButton(new WebAdminButtonDefinition<ClusterPolicy>(constants.removeClusterPolicy()) {
            @Override
            protected UICommand resolveCommand() {
                return clusterPolicyModelProvider.getModel().getRemoveCommand();
            }
        });

        table.addActionButton(new WebAdminButtonDefinition<ClusterPolicy>(constants.managePolicyUnits()) {
            @Override
            protected UICommand resolveCommand() {
                return clusterPolicyModelProvider.getModel().getManagePolicyUnitCommand();
            }
        });

        splitLayoutPanel.add(table);

        table.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                clusterPolicyModelProvider.setSelectedItems(table.getSelectionModel().getSelectedList());
                if (table.getSelectionModel().getSelectedList().size() > 0) {
                    setSubTabVisibility(true);
                } else {
                    setSubTabVisibility(false);
                }
            }
        });

    }

    private void initClustersTable(ApplicationConstants constants) {
        clusterTable = new SimpleActionTable<VDSGroup>(clusterPolicyClusterModelProvider,
                getTableHeaderlessResources(), getTableResources(), eventBus, clientStorage);

        TextColumnWithTooltip<VDSGroup> clusterColumn = new TextColumnWithTooltip<VDSGroup>() {
            @Override
            public String getValue(VDSGroup object) {
                return object.getName();
            }
        };
        clusterTable.addColumn(clusterColumn, constants.clusterPolicyAttachedCluster());
    }

    protected Resources getTableHeaderlessResources() {
        return (Resources) GWT.create(MainTableHeaderlessResources.class);
    }

    protected Resources getTableResources() {
        return (Resources) GWT.create(MainTableResources.class);
    }

}
