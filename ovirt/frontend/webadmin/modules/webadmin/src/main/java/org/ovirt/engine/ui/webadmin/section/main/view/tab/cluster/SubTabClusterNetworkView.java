package org.ovirt.engine.ui.webadmin.section.main.view.tab.cluster;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.SafeHtmlWithSafeHtmlTooltipColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterNetworkListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.cluster.SubTabClusterNetworkPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.NetworkRoleColumnHelper;
import org.ovirt.engine.ui.webadmin.widget.table.column.NetworkStatusColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;

public class SubTabClusterNetworkView extends AbstractSubTabTableView<VDSGroup, Network, ClusterListModel, ClusterNetworkListModel>
        implements SubTabClusterNetworkPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabClusterNetworkView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final SafeHtml displayImage;
    private final SafeHtml migrationImage;
    private final SafeHtml emptyImage;

    @Inject
    public SubTabClusterNetworkView(SearchableDetailModelProvider<Network, ClusterListModel, ClusterNetworkListModel> modelProvider,
            ApplicationConstants constants,
            ApplicationTemplates templates,
            ApplicationResources resources) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        displayImage =
                SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.networkMonitor()).getHTML());
        migrationImage =
                SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.migrationNetwork()).getHTML());
        emptyImage = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.networkEmpty()).getHTML());
        initTable(constants, templates);
        initWidget(getTable());
    }

    void initTable(final ApplicationConstants constants, final ApplicationTemplates templates) {
        getTable().enableColumnResizing();

        getTable().addColumn(new NetworkStatusColumn(), "", "20px"); //$NON-NLS-1$ //$NON-NLS-2$

        TextColumnWithTooltip<Network> nameColumn = new TextColumnWithTooltip<Network>() {
            @Override
            public String getValue(Network object) {
                return object.getName();
            }
        };
        nameColumn.makeSortable();
        getTable().addColumn(nameColumn, constants.nameNetwork(), "400px"); //$NON-NLS-1$

        TextColumnWithTooltip<Network> statusColumn = new EnumColumn<Network, NetworkStatus>() {
            @Override
            public NetworkStatus getRawValue(Network object) {
                return object.getCluster().getStatus();
            }
        };
        statusColumn.makeSortable();
        getTable().addColumn(statusColumn, constants.statusNetwork(), "100px"); //$NON-NLS-1$

        SafeHtmlWithSafeHtmlTooltipColumn<Network> roleColumn =
                new SafeHtmlWithSafeHtmlTooltipColumn<Network>() {
                    @Override
                    public SafeHtml getValue(Network network) {

                        List<SafeHtml> images = new LinkedList<SafeHtml>();

                        if (network.getCluster() != null) {
                            if (network.getCluster().isDisplay()) {

                                images.add(displayImage);
                            } else {
                                images.add(emptyImage);
                            }

                            if (network.getCluster().isMigration()) {
                                images.add(migrationImage);
                            } else {
                                images.add(emptyImage);
                            }
                        }

                        return NetworkRoleColumnHelper.getValue(images);
                    }

                    @Override
                    public SafeHtml getTooltip(Network network) {
                        Map<SafeHtml, String> imagesToText = new LinkedHashMap<SafeHtml, String>();
                        if (network.getCluster() != null) {
                            if (network.getCluster().isDisplay()) {
                                imagesToText.put(displayImage, constants.displayItemInfo());
                            }

                            if (network.getCluster().isMigration()) {
                                imagesToText.put(migrationImage, constants.migrationItemInfo());

                            }
                        }
                        return NetworkRoleColumnHelper.getTooltip(imagesToText);
                    }
                };

        getTable().addColumn(roleColumn, constants.roleNetwork(), "60px"); //$NON-NLS-1$

        TextColumnWithTooltip<Network> descColumn = new TextColumnWithTooltip<Network>() {
            @Override
            public String getValue(Network object) {
                return object.getDescription();
            }
        };
        descColumn.makeSortable();
        getTable().addColumn(descColumn, constants.descriptionNetwork(), "400px"); //$NON-NLS-1$

        if (ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly)) {
            getTable().addActionButton(new WebAdminButtonDefinition<Network>(constants.addNetworkNetwork()) {
                @Override
                protected UICommand resolveCommand() {
                    return getDetailModel().getNewNetworkCommand();
                }
            });

            getTable().addActionButton(new WebAdminButtonDefinition<Network>(constants.assignDetatchNetworksNework()) {
                @Override
                protected UICommand resolveCommand() {
                    return getDetailModel().getManageCommand();
                }
            });

            getTable().addActionButton(new WebAdminButtonDefinition<Network>(constants.setAsDisplayNetwork()) {
                @Override
                protected UICommand resolveCommand() {
                    return getDetailModel().getSetAsDisplayCommand();
                }
            });
        }
    }

}
