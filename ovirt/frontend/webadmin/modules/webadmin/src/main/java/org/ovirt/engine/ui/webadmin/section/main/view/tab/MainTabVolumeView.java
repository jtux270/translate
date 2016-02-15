package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterTaskSupport;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.action.ActionButtonDefinition;
import org.ovirt.engine.ui.common.widget.action.CommandLocation;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.frontend.utils.GlusterVolumeUtils;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.volumes.VolumeListModel;
import org.ovirt.engine.ui.uicompat.EnumTranslator;
import org.ovirt.engine.ui.uicompat.Translator;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.MainTabVolumePresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractMainTabWithDetailsTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminMenuBarButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.MenuCell;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeActivityColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeActivityCompositeCell;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeActivitySeperatorCell;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeActivityStatusColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeBrickStatusColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeCapacityCell;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeStatusColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VolumeTaskWaitingCell;

import com.google.gwt.cell.client.HasCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;

public class MainTabVolumeView extends AbstractMainTabWithDetailsTableView<GlusterVolumeEntity, VolumeListModel> implements MainTabVolumePresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainTabVolumeView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    Translator transportTypeTranslator = EnumTranslator.create(TransportType.class);

    @Inject
    public MainTabVolumeView(MainModelProvider<GlusterVolumeEntity, VolumeListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        getTable().enableColumnResizing();

        VolumeStatusColumn statusColumn = new VolumeStatusColumn();
        statusColumn.makeSortable(new Comparator<GlusterVolumeEntity>() {
            @Override
            public int compare(GlusterVolumeEntity o1, GlusterVolumeEntity o2) {
                return GlusterVolumeUtils.getVolumeStatus(o1).ordinal() - GlusterVolumeUtils.getVolumeStatus(o2).ordinal();
            }
        });
        getTable().addColumn(statusColumn, constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<GlusterVolumeEntity> nameColumn = new TextColumnWithTooltip<GlusterVolumeEntity>() {
            @Override
            public String getValue(GlusterVolumeEntity object) {
                return object.getName();
            }
        };
        nameColumn.makeSortable();

        getTable().addColumn(nameColumn, constants.NameVolume(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<GlusterVolumeEntity> clusterColumn = new TextColumnWithTooltip<GlusterVolumeEntity>() {
            @Override
            public String getValue(GlusterVolumeEntity object) {
                return object.getVdsGroupName();
            }
        };
        clusterColumn.makeSortable();
        getTable().addColumn(clusterColumn, constants.clusterVolume(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<GlusterVolumeEntity> volumeTypeColumn =
                new EnumColumn<GlusterVolumeEntity, GlusterVolumeType>() {

                    @Override
                    protected GlusterVolumeType getRawValue(GlusterVolumeEntity object) {
                        return object.getVolumeType();
                    }
                };
        volumeTypeColumn.makeSortable();
        getTable().addColumn(volumeTypeColumn, constants.volumeTypeVolume(), "150px"); //$NON-NLS-1$

        getTable().addColumn(new VolumeBrickStatusColumn(), constants.bricksStatusVolume(), "150px"); //$NON-NLS-1$

        MenuCell<GlusterTaskSupport> rebalanceMenuCell = getRebalanceActivityMenu(constants);
        MenuCell<GlusterTaskSupport> removeBricksMenuCell = getRemoveBrickActivityMenu(constants);
        List<HasCell<GlusterTaskSupport, ?>> list = new ArrayList<HasCell<GlusterTaskSupport, ?>>();
        list.add(new VolumeActivityStatusColumn<GlusterTaskSupport>());
        list.add(new Column<GlusterTaskSupport, GlusterTaskSupport>(new VolumeActivitySeperatorCell<GlusterTaskSupport>()) {
            @Override
            public GlusterTaskSupport getValue(GlusterTaskSupport object) {
                return object;
            }
        });
        list.add(new Column<GlusterTaskSupport, GlusterTaskSupport>(rebalanceMenuCell) {
            @Override
            public GlusterTaskSupport getValue(GlusterTaskSupport object) {
                return object;
            }
        });
        list.add(new Column<GlusterTaskSupport, GlusterTaskSupport>(removeBricksMenuCell) {
            @Override
            public GlusterTaskSupport getValue(GlusterTaskSupport object) {
                return object;
            }
        });

        List<HasCell<GlusterTaskSupport, ?>> compositeList = new ArrayList<HasCell<GlusterTaskSupport, ?>>();
        compositeList.add(new Column<GlusterTaskSupport, GlusterTaskSupport>(new VolumeTaskWaitingCell<GlusterTaskSupport>()) {
            @Override
            public GlusterTaskSupport getValue(GlusterTaskSupport object) {
                return object;
            }
        });
        compositeList.add(new Column<GlusterTaskSupport, GlusterTaskSupport>(new VolumeActivityCompositeCell<GlusterTaskSupport>(list)) {
            @Override
            public GlusterTaskSupport getValue(GlusterTaskSupport object) {
                return object;
            }
        });

        getTable().addColumn(new Column<GlusterVolumeEntity, GlusterVolumeEntity>(new VolumeCapacityCell()) {
            @Override
            public GlusterVolumeEntity getValue(GlusterVolumeEntity object) {
                return object;
            }
        }, constants.volumeCapacity(), "60px");//$NON-NLS-1$

        getTable().addColumn(new VolumeActivityColumn<GlusterVolumeEntity>(new VolumeActivityCompositeCell<GlusterTaskSupport>(compositeList) {
                @Override
                protected boolean isVisible(GlusterTaskSupport value) {
                    return !(value == null || value.getAsyncTask() == null);
                }
                }),
                constants.activitiesOnVolume(),
                "100px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.newVolume()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getNewVolumeCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.removeVolume()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRemoveVolumeCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.startVolume()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStartCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.stopVolume()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStopCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.rebalanceVolume()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStartRebalanceCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.optimizeForVirtStore()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getOptimizeForVirtStoreCommand();
            }
        });

        List<ActionButtonDefinition<GlusterVolumeEntity>> volumeProfilingActions = new LinkedList<ActionButtonDefinition<GlusterVolumeEntity>>();
        volumeProfilingActions.add(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.startVolumeProfiling()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStartVolumeProfilingCommand();
            }
        });
        volumeProfilingActions.add(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.volumeProfileDetails()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getShowVolumeProfileDetailsCommand();
            }
        });
        volumeProfilingActions.add(new WebAdminButtonDefinition<GlusterVolumeEntity>(constants.stopVolumeProfiling()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStopVolumeProfilingCommand();
            }
        });
        getTable().addActionButton(new WebAdminMenuBarButtonDefinition<GlusterVolumeEntity>(constants.volumeProfilingAction(), volumeProfilingActions, CommandLocation.ContextAndToolBar));
    }

    private MenuCell<GlusterTaskSupport> getRebalanceActivityMenu(ApplicationConstants constants) {
        MenuCell<GlusterTaskSupport> menuCell = new MenuCell<GlusterTaskSupport>() {
            @Override
            protected boolean isVisible(GlusterTaskSupport value) {
                return value.getAsyncTask() != null && value.getAsyncTask().getType() == GlusterTaskType.REBALANCE;
            }
        };

        menuCell.addMenuItem(new WebAdminButtonDefinition<GlusterTaskSupport>(constants.statusRebalance()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStatusRebalanceCommand();
            }
        });

        menuCell.addMenuItem(new WebAdminButtonDefinition<GlusterTaskSupport>(constants.stopRebalance()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStopRebalanceCommand();
            }
        });

        return menuCell;
    }

    private MenuCell<GlusterTaskSupport> getRemoveBrickActivityMenu(ApplicationConstants constants) {
        MenuCell<GlusterTaskSupport> menuCell = new MenuCell<GlusterTaskSupport>() {
            @Override
            protected boolean isVisible(GlusterTaskSupport value) {
                return value.getAsyncTask() != null && value.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK;
            }
        };

        menuCell.addMenuItem(new WebAdminButtonDefinition<GlusterTaskSupport>(constants.removeBricksStatus()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getBrickListModel()
                        .getStatusRemoveBricksCommand();
            }
        });

        menuCell.addMenuItem(new WebAdminButtonDefinition<GlusterTaskSupport>(constants.removeBricksStop()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getBrickListModel()
                        .getStopRemoveBricksCommand();
            }
        });

        menuCell.addMenuItem(new WebAdminButtonDefinition<GlusterTaskSupport>(constants.removeBricksCommit()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getBrickListModel()
                        .getCommitRemoveBricksCommand();
            }
        });

        menuCell.addMenuItem(new WebAdminButtonDefinition<GlusterTaskSupport>(constants.retainBricks()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getBrickListModel()
                        .getRetainBricksCommand();
            }
        });

        return menuCell;
    }

}
