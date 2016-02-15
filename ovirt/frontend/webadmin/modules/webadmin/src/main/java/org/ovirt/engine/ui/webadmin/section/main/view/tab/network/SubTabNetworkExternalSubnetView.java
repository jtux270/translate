package org.ovirt.engine.ui.webadmin.section.main.view.tab.network;

import org.ovirt.engine.core.common.businessentities.network.ExternalSubnet;
import org.ovirt.engine.core.common.businessentities.network.ExternalSubnet.IpVersion;
import org.ovirt.engine.core.common.businessentities.network.NetworkView;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.networks.NetworkExternalSubnetListModel;
import org.ovirt.engine.ui.uicommonweb.models.networks.NetworkListModel;
import org.ovirt.engine.ui.uicompat.external.StringUtils;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.network.SubTabNetworkExternalSubnetPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

import com.google.inject.Inject;

public class SubTabNetworkExternalSubnetView extends AbstractSubTabTableView<NetworkView, ExternalSubnet, NetworkListModel, NetworkExternalSubnetListModel>
        implements SubTabNetworkExternalSubnetPresenter.ViewDef {

    @Inject
    public SubTabNetworkExternalSubnetView(SearchableDetailModelProvider<ExternalSubnet, NetworkListModel, NetworkExternalSubnetListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        getTable().enableColumnResizing();

        TextColumnWithTooltip<ExternalSubnet> nameColumn =
                new TextColumnWithTooltip<ExternalSubnet>() {
                    @Override
                    public String getValue(ExternalSubnet object) {
                        return object.getName();
                    }
                };
        nameColumn.makeSortable();
        getTable().addColumn(nameColumn, constants.nameExternalSubnet(), "200px"); //$NON-NLS-1$

        TextColumnWithTooltip<ExternalSubnet> cidrColumn =
                new TextColumnWithTooltip<ExternalSubnet>() {
            @Override
            public String getValue(ExternalSubnet object) {
                return object.getCidr();
            }
        };
        cidrColumn.makeSortable();
        getTable().addColumn(cidrColumn, constants.cidrExternalSubnet(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<ExternalSubnet> ipVersionColumn =
                new EnumColumn<ExternalSubnet, IpVersion>() {
            @Override
            protected IpVersion getRawValue(ExternalSubnet object) {
                return object.getIpVersion();
            }
        };
        ipVersionColumn.makeSortable();
        getTable().addColumn(ipVersionColumn, constants.ipVersionExternalSubnet(), "80px"); //$NON-NLS-1$

        TextColumnWithTooltip<ExternalSubnet> gatewayColumn =
                new TextColumnWithTooltip<ExternalSubnet>() {
                    @Override
                    public String getValue(ExternalSubnet object) {
                        return object.getGateway();
                    }
                };
        gatewayColumn.makeSortable();
        getTable().addColumn(gatewayColumn, constants.gatewayExternalSubnet(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<ExternalSubnet> dnsServersColumn =
                new TextColumnWithTooltip<ExternalSubnet>() {
                    @Override
                    public String getValue(ExternalSubnet object) {
                        return StringUtils.join(object.getDnsServers(), ", "); //$NON-NLS-1$
                    }
                };
        dnsServersColumn.makeSortable();
        getTable().addColumn(dnsServersColumn, constants.dnsServersExternalSubnet(), "200px"); //$NON-NLS-1$

        TextColumnWithTooltip<ExternalSubnet> externalIdColumn =
                new TextColumnWithTooltip<ExternalSubnet>() {
            @Override
            public String getValue(ExternalSubnet object) {
                return object.getId();
            }
        };
        externalIdColumn.makeSortable();
        getTable().addColumn(externalIdColumn, constants.externalIdExternalSubnet(), "300px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<ExternalSubnet>(constants.newNetworkExternalSubnet()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getNewCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<ExternalSubnet>(constants.removeNetworkExternalSubnet()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRemoveCommand();
            }
        });
    }
}
