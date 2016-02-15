package org.ovirt.engine.ui.webadmin.section.main.view.tab.datacenter;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.qos.CpuQos;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.DataCenterListModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.qos.DataCenterCpuQosListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.datacenter.SubTabDataCenterCpuQosPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

import com.google.gwt.core.client.GWT;

public class SubTabDataCenterCpuQosView extends AbstractSubTabTableView<StoragePool,
        CpuQos, DataCenterListModel, DataCenterCpuQosListModel>
        implements SubTabDataCenterCpuQosPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabDataCenterCpuQosView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabDataCenterCpuQosView(SearchableDetailModelProvider<CpuQos,
            DataCenterListModel, DataCenterCpuQosListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(final ApplicationConstants constants) {
        getTable().enableColumnResizing();

        TextColumnWithTooltip<CpuQos> nameColumn = new TextColumnWithTooltip<CpuQos>() {
            @Override
            public String getValue(CpuQos object) {
                return object.getName() == null ? "" : object.getName(); //$NON-NLS-1$
            }
        };
        nameColumn.makeSortable();
        getTable().addColumn(nameColumn, constants.cpuQosName(), "200px"); //$NON-NLS-1$

        TextColumnWithTooltip<CpuQos> descColumn = new TextColumnWithTooltip<CpuQos>() {
            @Override
            public String getValue(CpuQos object) {
                return object.getDescription() == null ? "" : object.getDescription(); //$NON-NLS-1$
            }
        };
        descColumn.makeSortable();
        getTable().addColumn(descColumn, constants.cpuQosDescription(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<CpuQos> cpuLimitColumn = new TextColumnWithTooltip<CpuQos>() {
            @Override
            public String getValue(CpuQos object) {
                return object.getCpuLimit() == null ? constants.unlimitedQos()
                        : object.getCpuLimit().toString();
            }
        };
        cpuLimitColumn.makeSortable();
        getTable().addColumn(cpuLimitColumn, constants.cpuQosCpuLimit(), "105px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<CpuQos>(constants.newCpuQos()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getNewCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<CpuQos>(constants.editCpuQos()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getEditCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<CpuQos>(constants.removeStorageQos()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRemoveCommand();
            }
        });
    }
}
