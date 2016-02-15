package org.ovirt.engine.ui.webadmin.section.main.view.tab.datacenter;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import org.ovirt.engine.core.common.businessentities.IscsiBond;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.DataCenterIscsiBondListModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.DataCenterListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.datacenter.SubTabDataCenterIscsiBondPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

public class SubTabDataCenterIscsiBondView extends AbstractSubTabTableView<StoragePool, IscsiBond, DataCenterListModel, DataCenterIscsiBondListModel>
        implements SubTabDataCenterIscsiBondPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabDataCenterIscsiBondView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabDataCenterIscsiBondView(SearchableDetailModelProvider<IscsiBond, DataCenterListModel,
            DataCenterIscsiBondListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        getTable().enableColumnResizing();

        getTable().addColumn(new TextColumnWithTooltip<IscsiBond>() {
            @Override
            public String getValue(IscsiBond object) {
                return object.getName();
            }
        }, constants.name(), "400px"); //$NON-NLS-1$

        getTable().addColumn(new TextColumnWithTooltip<IscsiBond>() {
            @Override
            public String getValue(IscsiBond object) {
                return object.getDescription();
            }
        }, constants.description(), "400px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<IscsiBond>(constants.addIscsiBond()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getAddCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<IscsiBond>(constants.editIscsiBond()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getEditCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<IscsiBond>(constants.removeIscsiBond()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRemoveCommand();
            }
        });
    }
}
