package org.ovirt.engine.ui.webadmin.section.main.view.tab.virtualMachine;

import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.DataBoundTabModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.view.AbstractSubTabTableWidgetView;
import org.ovirt.engine.ui.common.widget.uicommon.vm.VmSnapshotListModelTable;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmSnapshotListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.virtualMachine.SubTabVirtualMachineSnapshotPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class SubTabVirtualMachineSnapshotView extends AbstractSubTabTableWidgetView<VM, Snapshot, VmListModel, VmSnapshotListModel> implements SubTabVirtualMachineSnapshotPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabVirtualMachineSnapshotView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @SuppressWarnings("unchecked")
    @Inject
    public SubTabVirtualMachineSnapshotView(SearchableDetailModelProvider<Snapshot, VmListModel, VmSnapshotListModel> modelProvider,
            EventBus eventBus,
            ClientStorage clientStorage,
            CommonApplicationConstants constants,
            CommonApplicationMessages messages,
            CommonApplicationTemplates templates) {
        super(new VmSnapshotListModelTable<VmSnapshotListModel>(
                (DataBoundTabModelProvider<Snapshot, VmSnapshotListModel>) modelProvider,
                eventBus, clientStorage, constants, messages, templates));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getModelBoundTableWidget());
    }

    @Override
    public void addModelListeners() {
        getModelBoundTableWidget().addModelListeners();
    }
}
