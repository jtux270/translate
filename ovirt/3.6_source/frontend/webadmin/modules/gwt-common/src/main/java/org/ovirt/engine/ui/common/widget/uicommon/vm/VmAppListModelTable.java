package org.ovirt.engine.ui.common.widget.uicommon.vm;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundTableWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;
import com.google.gwt.event.shared.EventBus;

public class VmAppListModelTable extends AbstractModelBoundTableWidget<String, VmAppListModel<VM>> {

    private final static CommonApplicationConstants constants = AssetProvider.getConstants();

    public VmAppListModelTable(
            SearchableTableModelProvider<String, VmAppListModel<VM>> modelProvider,
            EventBus eventBus, ClientStorage clientStorage) {
        super(modelProvider, eventBus, clientStorage, false);
    }

    @Override
    public void initTable() {
        AbstractTextColumn<String> appNameColumn = new AbstractTextColumn<String>() {
            @Override
            public String getValue(String appName) {
                return appName;
            }
        };
        getTable().addColumn(appNameColumn, constants.installedApp());
    }

}
