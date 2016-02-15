package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import com.google.gwt.user.client.ui.ScrollPanel;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.storage.RegisterEntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmData;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.storage.backup.ImportVmGeneralSubTabView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterVmInfoPanel extends RegisterEntityInfoPanel {

    private ImportVmGeneralSubTabView generalView;
    private VmGeneralModel vmGeneralModel;

    public RegisterVmInfoPanel(RegisterEntityModel model) {
        super(model);
    }

    @Override
    protected void init() {
        // Initialize Tables
        initGeneralForm();
        initDisksTable();
        initNicsTable();
        initAppsTable();

        // Add Tabs
        add(new ScrollPanel(generalView.asWidget()), constants.generalLabel());
        add(new ScrollPanel(disksTable), constants.disksLabel());
        add(new ScrollPanel(nicsTable), constants.nicsLabel());
        add(new ScrollPanel(appsTable), constants.applicationsLabel());
    }

    @Override
    public void updateTabsData(ImportEntityData importEntityData) {
        VM vm = ((ImportVmData) importEntityData).getVm();

        vmGeneralModel.setEntity(vm);
        generalView.setMainTabSelectedItem(vm);

        disksTable.setRowData((List) Arrays.asList(vm.getDiskMap().values().toArray()));
        nicsTable.setRowData((List) Arrays.asList(vm.getInterfaces().toArray()));
        appsTable.setRowData((List) Arrays.asList(vm.getAppList() != null ?
                vm.getAppList().split("[,]", -1) : new ArrayList<String>())); //$NON-NLS-1$
    }

    private void initGeneralForm() {
        DetailModelProvider<VmListModel, VmGeneralModel> modelProvider =
            new DetailModelProvider<VmListModel, VmGeneralModel>() {
                @Override
                public VmGeneralModel getModel() {
                    return getVmGeneralModel();
                }

                @Override
                public void onSubTabSelected() {
                }
            };
        generalView = new ImportVmGeneralSubTabView(modelProvider, constants);
    }

    public VmGeneralModel getVmGeneralModel() {
        if (vmGeneralModel == null) {
            vmGeneralModel = new VmGeneralModel();
        }
        return vmGeneralModel;
    }
}
