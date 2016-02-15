package org.ovirt.engine.ui.common.widget.uicommon.vm;

import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundTableWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;

import com.google.gwt.event.shared.EventBus;

public class VmAppListModelTable extends AbstractModelBoundTableWidget<String, VmAppListModel> {

    public VmAppListModelTable(
            SearchableTableModelProvider<String, VmAppListModel> modelProvider,
            EventBus eventBus, ClientStorage clientStorage, CommonApplicationConstants constants) {
        super(modelProvider, eventBus, clientStorage, false);
    }

    @Override
    public void initTable(CommonApplicationConstants constants) {
        TextColumnWithTooltip<String> appNameColumn = new TextColumnWithTooltip<String>() {
            @Override
            public String getValue(String appName) {
                return appName;
            }
        };
        getTable().addColumn(appNameColumn, constants.installedApp());
    }

}
