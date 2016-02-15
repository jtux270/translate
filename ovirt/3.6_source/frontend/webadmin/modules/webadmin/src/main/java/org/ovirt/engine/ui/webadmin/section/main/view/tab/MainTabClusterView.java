package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.searchbackend.ClusterConditionFieldAutoCompleter;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.action.ActionButtonDefinition;
import org.ovirt.engine.ui.common.widget.action.CommandLocation;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.uicommonweb.ReportInit;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.MainTabClusterPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractMainTabWithDetailsTableView;
import org.ovirt.engine.ui.webadmin.uicommon.ReportActionsHelper;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminImageButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminMenuBarButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.CommentColumn;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.inject.Inject;

public class MainTabClusterView extends AbstractMainTabWithDetailsTableView<VDSGroup, ClusterListModel<Void>> implements
    MainTabClusterPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainTabClusterView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final static ApplicationTemplates templates = AssetProvider.getTemplates();
    private final static ApplicationResources resources = AssetProvider.getResources();
    private final static ApplicationConstants constants = AssetProvider.getConstants();
    private final static ApplicationMessages messages = AssetProvider.getMessages();


    @Inject
    public MainTabClusterView(MainModelProvider<VDSGroup, ClusterListModel<Void>> modelProvider) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable();
        initWidget(getTable());
    }

    void initTable() {
        getTable().enableColumnResizing();

        AbstractTextColumn<VDSGroup> nameColumn = new AbstractTextColumn<VDSGroup>() {
            @Override
            public String getValue(VDSGroup object) {
                return object.getName();
            }
        };
        nameColumn.makeSortable(ClusterConditionFieldAutoCompleter.NAME);
        getTable().addColumn(nameColumn, constants.nameCluster(), "150px"); //$NON-NLS-1$

        CommentColumn<VDSGroup> commentColumn = new CommentColumn<VDSGroup>();
        getTable().addColumnWithHtmlHeader(commentColumn,
                SafeHtmlUtils.fromSafeConstant(constants.commentLabel()),
                "75px"); //$NON-NLS-1$

        if (ApplicationModeHelper.getUiMode() != ApplicationMode.GlusterOnly) {
            AbstractTextColumn<VDSGroup> dataCenterColumn = new AbstractTextColumn<VDSGroup>() {
                @Override
                public String getValue(VDSGroup object) {
                    return object.getStoragePoolName();
                }
            };
            getTable().addColumn(dataCenterColumn, constants.dcCluster(), "150px"); //$NON-NLS-1$
        }

        AbstractTextColumn<VDSGroup> versionColumn = new AbstractTextColumn<VDSGroup>() {
            @Override
            public String getValue(VDSGroup object) {
                return object.getCompatibilityVersion().getValue();
            }
        };
        getTable().addColumn(versionColumn, constants.comptVersCluster(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<VDSGroup> descColumn = new AbstractTextColumn<VDSGroup>() {
            @Override
            public String getValue(VDSGroup object) {
                return object.getDescription();
            }
        };
        descColumn.makeSortable(ClusterConditionFieldAutoCompleter.DESCRIPTION);
        getTable().addColumn(descColumn, constants.descriptionCluster(), "300px"); //$NON-NLS-1$

        if (ApplicationModeHelper.getUiMode() != ApplicationMode.GlusterOnly) {
            AbstractTextColumn<VDSGroup> cpuTypeColumn = new AbstractTextColumn<VDSGroup>() {
                @Override
                public String getValue(VDSGroup object) {
                    return object.getCpuName();
                }
            };
            getTable().addColumn(cpuTypeColumn, constants.cpuTypeCluster(), "150px"); //$NON-NLS-1$
        }

        AbstractTextColumn<VDSGroup> hostCountColumn = new AbstractTextColumn<VDSGroup>() {
            @Override
            public String getValue(VDSGroup object) {
                if (object.getGroupHostsAndVms() == null) {
                    return ""; //$NON-NLS-1$
                }
                return object.getGroupHostsAndVms().getHosts() + ""; //$NON-NLS-1$
            }
        };

        getTable().addColumn(hostCountColumn, constants.hostCount(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<VDSGroup> vmCountColumn = new AbstractTextColumn<VDSGroup>() {
            @Override
            public String getValue(VDSGroup object) {
                if (object.getGroupHostsAndVms() == null) {
                    return ""; //$NON-NLS-1$
                }
                return object.getGroupHostsAndVms().getVms() + ""; //$NON-NLS-1$
            }
        };

        getTable().addColumn(vmCountColumn, constants.vmCount(), "150px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<VDSGroup>(constants.newCluster()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getNewCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VDSGroup>(constants.editCluster()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getEditCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VDSGroup>(constants.removeCluster()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRemoveCommand();
            }
        });

        if (ReportInit.getInstance().isReportsEnabled()) {
            updateReportsAvailability();
        } else {
            getMainModel().getReportsAvailabilityEvent().addListener(new IEventListener<EventArgs>() {
                @Override
                public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                    updateReportsAvailability();
                }
            });
        }

        getTable().addActionButton(new WebAdminImageButtonDefinition<VDSGroup>(constants.guideMeCluster(),
                resources.guideSmallImage(), resources.guideSmallDisabledImage(), true) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getGuideCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<VDSGroup>(constants.resetClusterEmulatedMachine(),
                CommandLocation.OnlyFromContext) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getResetEmulatedMachineCommand();
            }
        });
    }

    public void updateReportsAvailability() {

        if (ReportInit.getInstance().isReportsEnabled()) {
            List<ActionButtonDefinition<VDSGroup>> resourceSubActions =
                    ReportActionsHelper.getInstance().getResourceSubActions("Cluster", getModelProvider()); //$NON-NLS-1$
            if (resourceSubActions != null && resourceSubActions.size() > 0) {
                getTable().addActionButton(new WebAdminMenuBarButtonDefinition<VDSGroup>(constants.showReportCluster(),
                        resourceSubActions));
            }
        }

    }
}
