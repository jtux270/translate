package org.ovirt.engine.ui.webadmin.section.main.view.tab.virtualMachine;

import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.virtualMachine.SubTabVirtualMachinePermissionPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.tab.AbstractSubTabPermissionsView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class SubTabVirtualMachinePermissionView extends AbstractSubTabPermissionsView<VM, VmListModel>
        implements SubTabVirtualMachinePermissionPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabVirtualMachinePermissionView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabVirtualMachinePermissionView(SearchableDetailModelProvider<Permissions, VmListModel, PermissionListModel> modelProvider,
            EventBus eventBus,
            ClientStorage clientStorage, ApplicationConstants constants) {
        super(modelProvider, eventBus, clientStorage, constants);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

}
