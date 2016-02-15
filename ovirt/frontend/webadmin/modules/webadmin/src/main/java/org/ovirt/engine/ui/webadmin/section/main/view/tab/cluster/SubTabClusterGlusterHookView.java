package org.ovirt.engine.ui.webadmin.section.main.view.tab.cluster;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookContentType;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookStage;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookStatus;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterGlusterHookListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.cluster.SubTabClusterGlusterHookPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.GlusterHookSyncStatusColumn;

public class SubTabClusterGlusterHookView
        extends
        AbstractSubTabTableView<VDSGroup, GlusterHookEntity, ClusterListModel, ClusterGlusterHookListModel>
        implements SubTabClusterGlusterHookPresenter.ViewDef {

    @Inject
    public SubTabClusterGlusterHookView(
            SearchableDetailModelProvider<GlusterHookEntity, ClusterListModel, ClusterGlusterHookListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(final ApplicationConstants constants) {
        getTable().enableColumnResizing();

        getTable().addColumn(new GlusterHookSyncStatusColumn(),
                constants.empty(), "10px"); //$NON-NLS-1$

        TextColumnWithTooltip<GlusterHookEntity> nameColumn = new TextColumnWithTooltip<GlusterHookEntity>() {
            @Override
            public String getValue(GlusterHookEntity object) {
                return object.getName();
            }
        };
        getTable().addColumn(nameColumn, constants.nameHook(), "200px"); //$NON-NLS-1$

        TextColumnWithTooltip<GlusterHookEntity> statusColumn = new EnumColumn<GlusterHookEntity, GlusterHookStatus>() {

            @Override
            protected GlusterHookStatus getRawValue(GlusterHookEntity object) {
                return object.getStatus();
            }
        };
        getTable().addColumn(statusColumn, constants.statusHook(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<GlusterHookEntity> glusterCommandColumn = new TextColumnWithTooltip<GlusterHookEntity>() {
            @Override
            public String getValue(GlusterHookEntity object) {
                return object.getGlusterCommand();
            }
        };

        getTable().addColumn(glusterCommandColumn,
                constants.glusterVolumeEventHook(), "100px"); //$NON-NLS-1$;

        TextColumnWithTooltip<GlusterHookEntity> stageColumn = new EnumColumn<GlusterHookEntity, GlusterHookStage>() {

            @Override
            protected GlusterHookStage getRawValue(GlusterHookEntity object) {
                return object.getStage();
            }
        };
        getTable().addColumn(stageColumn, constants.stageHook(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<GlusterHookEntity> contentTypeColumn =
                new EnumColumn<GlusterHookEntity, GlusterHookContentType>() {

                    @Override
                    protected GlusterHookContentType getRawValue(
                            GlusterHookEntity object) {
                        return object.getContentType();
                    }
                };
        getTable().addColumn(contentTypeColumn, constants.contentTypeHook(), "150px"); //$NON-NLS-1$

        getTable().addActionButton(
                new WebAdminButtonDefinition<GlusterHookEntity>(constants
                        .enableHook()) {
                    @Override
                    protected UICommand resolveCommand() {
                        return getDetailModel().getEnableHookCommand();
                    }
                });

        getTable().addActionButton(
                new WebAdminButtonDefinition<GlusterHookEntity>(constants
                        .disableHook()) {
                    @Override
                    protected UICommand resolveCommand() {
                        return getDetailModel().getDisableHookCommand();
                    }
                });
        getTable().addActionButton(
                new WebAdminButtonDefinition<GlusterHookEntity>(constants
                        .viewHookContent()) {
                    @Override
                    protected UICommand resolveCommand() {
                        return getDetailModel().getViewHookCommand();
                    }
                });
        getTable().addActionButton(
                new WebAdminButtonDefinition<GlusterHookEntity>(constants
                        .resolveConflictsGlusterHook()) {
                    @Override
                    protected UICommand resolveCommand() {
                        return getDetailModel().getResolveConflictsCommand();
                    }
                });
        getTable().addActionButton(
                new WebAdminButtonDefinition<GlusterHookEntity>(constants
                        .syncWithServersGlusterHook()) {
                    @Override
                    protected UICommand resolveCommand() {
                        return getDetailModel().getSyncWithServersCommand();
                    }
                });
    }
}
