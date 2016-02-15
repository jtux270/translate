package org.ovirt.engine.ui.webadmin.section.main.view.tab.virtualMachine;

import java.util.List;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.scheduling.AffinityGroup;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.configure.scheduling.affinity_groups.list.VmAffinityGroupListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.virtualMachine.SubTabVirtualMachineAffinityGroupPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.tab.AbstractSubTabAffinityGroupsView;

import com.google.gwt.core.client.GWT;

public class SubTabVirtualMachineAffinityGroupView extends AbstractSubTabAffinityGroupsView<VM, VmListModel, VmAffinityGroupListModel>
        implements SubTabVirtualMachineAffinityGroupPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabVirtualMachineAffinityGroupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabVirtualMachineAffinityGroupView(SearchableDetailModelProvider<AffinityGroup, VmListModel, VmAffinityGroupListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider, constants);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

    @Override
    protected List<String> getEntityNames(AffinityGroup object) {
        List<String> entityNames = super.getEntityNames(object);
        String vmName = getDetailModel().getParentEntity().getName();
        for (int i = 0; i < entityNames.size(); i++) {
            if (entityNames.get(i).equals(vmName)) {
                entityNames.remove(i);
                break;
            }
        }
        return entityNames;
    }
}
