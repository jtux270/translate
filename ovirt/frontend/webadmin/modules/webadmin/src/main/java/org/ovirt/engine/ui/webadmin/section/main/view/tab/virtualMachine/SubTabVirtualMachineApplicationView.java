package org.ovirt.engine.ui.webadmin.section.main.view.tab.virtualMachine;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.view.AbstractSubTabTableWidgetView;
import org.ovirt.engine.ui.common.widget.uicommon.vm.VmAppListModelTable;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.virtualMachine.SubTabVirtualMachineApplicationPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class SubTabVirtualMachineApplicationView extends AbstractSubTabTableWidgetView<VM, String, VmListModel, VmAppListModel> implements SubTabVirtualMachineApplicationPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabVirtualMachineApplicationView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabVirtualMachineApplicationView(
            SearchableDetailModelProvider<String, VmListModel, VmAppListModel> modelProvider,
            EventBus eventBus, ClientStorage clientStorage, ApplicationConstants constants) {
        super(new VmAppListModelTable(modelProvider, eventBus, clientStorage, constants));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getModelBoundTableWidget());
    }

}
