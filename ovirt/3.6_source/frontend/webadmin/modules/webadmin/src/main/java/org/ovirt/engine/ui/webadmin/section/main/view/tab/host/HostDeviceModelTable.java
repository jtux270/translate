package org.ovirt.engine.ui.webadmin.section.main.view.tab.host;

import org.ovirt.engine.core.common.businessentities.HostDeviceView;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.vms.hostdev.HostDeviceListModel;
import com.google.gwt.event.shared.EventBus;

public class HostDeviceModelTable extends HostDeviceModelBaseTable<HostDeviceListModel> {

    public HostDeviceModelTable(
            SearchableTableModelProvider<HostDeviceView, HostDeviceListModel> modelProvider,
            EventBus eventBus, ClientStorage clientStorage) {
        super(modelProvider, eventBus, clientStorage);
    }
}
