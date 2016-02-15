package org.ovirt.engine.ui.uicommonweb.models.templates;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.scheduling.affinity_groups.list.VmAffinityGroupListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmsModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmDevicesListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmDiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmEventListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGuestInfoModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmInterfaceListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmSnapshotListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.hostdev.VmHostDeviceListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class TemplateVmListModel extends VmListModel<VmTemplate> {

    @Inject
    public TemplateVmListModel(final VmGeneralModel vmGeneralModel, final VmInterfaceListModel vmInterfaceListModel,
            final VmDiskListModel vmDiskListModel, final VmSnapshotListModel vmSnapshotListModel,
            final VmEventListModel vmEventListModel, final VmAppListModel<VM> vmAppListModel,
            final PermissionListModel<VM> permissionListModel,
            final VmAffinityGroupListModel vmAffinityGroupListModel, final VmGuestInfoModel vmGuestInfoModel,
            Provider<ImportVmsModel> importVmsModelProvider, VmHostDeviceListModel vmHostDeviceListModel,
            final VmDevicesListModel vmDevicesListModel) {
        super(vmGeneralModel, vmInterfaceListModel, vmDiskListModel, vmSnapshotListModel, vmEventListModel,
                vmAppListModel, permissionListModel, vmAffinityGroupListModel, vmGuestInfoModel, importVmsModelProvider,
                vmHostDeviceListModel,  vmDevicesListModel);
        setTitle(ConstantsManager.getInstance().getConstants().virtualMachinesTitle());
        setHelpTag(HelpTag.virtual_machines);
        setHashName("virtual_machines"); //$NON-NLS-1$
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    public void search() {
        if (getEntity() != null) {
            setSearchString("Vms: template.name=" + getEntity().getName()); //$NON-NLS-1$
            super.search();
        }
    }
}
