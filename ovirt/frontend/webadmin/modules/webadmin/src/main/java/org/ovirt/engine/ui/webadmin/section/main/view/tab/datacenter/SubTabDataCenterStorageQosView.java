package org.ovirt.engine.ui.webadmin.section.main.view.tab.datacenter;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.qos.StorageQos;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.DataCenterListModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.qos.DataCenterStorageQosListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.datacenter.SubTabDataCenterStorageQosPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

import com.google.gwt.core.client.GWT;

public class SubTabDataCenterStorageQosView extends AbstractSubTabTableView<StoragePool,
        StorageQos, DataCenterListModel, DataCenterStorageQosListModel>
        implements SubTabDataCenterStorageQosPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabDataCenterStorageQosView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabDataCenterStorageQosView(SearchableDetailModelProvider<StorageQos,
            DataCenterListModel, DataCenterStorageQosListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(final ApplicationConstants constants) {
        getTable().enableColumnResizing();

        TextColumnWithTooltip<StorageQos> nameColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getName() == null ? "" : object.getName(); //$NON-NLS-1$
            }
        };
        nameColumn.makeSortable();
        getTable().addColumn(nameColumn, constants.storageQosName(), "200px"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageQos> descColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getDescription() == null ? "" : object.getDescription(); //$NON-NLS-1$
            }
        };
        descColumn.makeSortable();
        getTable().addColumn(descColumn, constants.storageQosDescription(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageQos> throughputColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getMaxThroughput() == null ? constants.unlimitedQos()
                        : object.getMaxThroughput().toString();
            }
        };
        throughputColumn.makeSortable();
        getTable().addColumn(throughputColumn, constants.storageQosThroughputTotal(), "105px"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageQos> readThroughputColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getMaxReadThroughput() == null ? constants.unlimitedQos()
                        : object.getMaxReadThroughput().toString();
            }
        };
        readThroughputColumn.makeSortable();
        getTable().addColumn(readThroughputColumn, constants.storageQosThroughputRead(), "105px"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageQos> writeThroughputColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getMaxWriteThroughput() == null ? constants.unlimitedQos()
                        : object.getMaxWriteThroughput().toString();
            }
        };
        writeThroughputColumn.makeSortable();
        getTable().addColumn(writeThroughputColumn, constants.storageQosThroughputWrite(), "105px"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageQos> iopsColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getMaxIops() == null ? constants.unlimitedQos()
                        : object.getMaxIops().toString();
            }
        };
        iopsColumn.makeSortable();
        getTable().addColumn(iopsColumn, constants.storageQosIopsTotal(), "105px"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageQos> readIopsColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getMaxReadIops() == null ? constants.unlimitedQos()
                        : object.getMaxReadIops().toString();
            }
        };
        readIopsColumn.makeSortable();
        getTable().addColumn(readIopsColumn, constants.storageQosIopsRead(), "105px"); //$NON-NLS-1$

        TextColumnWithTooltip<StorageQos> writeIopsColumn = new TextColumnWithTooltip<StorageQos>() {
            @Override
            public String getValue(StorageQos object) {
                return object.getMaxWriteIops() == null ? constants.unlimitedQos()
                        : object.getMaxWriteIops().toString();
            }
        };
        writeIopsColumn.makeSortable();
        getTable().addColumn(writeIopsColumn, constants.storageQosIopsWrite(), "105px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<StorageQos>(constants.newStorageQos()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getNewCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<StorageQos>(constants.editStorageQos()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getEditCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<StorageQos>(constants.removeStorageQos()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRemoveCommand();
            }
        });
    }
}
