package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.ui.uicommonweb.Linq;

@SuppressWarnings("unused")
public class CloneVmFromSnapshotModelBehavior extends ExistingVmModelBehavior
{
    public CloneVmFromSnapshotModelBehavior() {
        super(null);
    }

    @Override
    public void template_SelectedItemChanged()
    {
        super.template_SelectedItemChanged();

        getModel().getName().setEntity(""); //$NON-NLS-1$
        getModel().getDescription().setEntity(""); //$NON-NLS-1$
        getModel().getComment().setEntity(""); //$NON-NLS-1$
        getModel().getProvisioning().setEntity(true);
        getModel().getProvisioning().setIsAvailable(true);
        getModel().getProvisioning().setIsChangable(false);

        initDisks();
        initStorageDomains();
    }

    @Override
    public void updateIsDisksAvailable()
    {
        getModel().setIsDisksAvailable(getModel().getDisks() != null);
    }

    @Override
    public void provisioning_SelectedItemChanged()
    {
        boolean provisioning = getModel().getProvisioning().getEntity();
        getModel().getProvisioningThin_IsSelected().setEntity(!provisioning);
        getModel().getProvisioningClone_IsSelected().setEntity(provisioning);
    }

    @Override
    public void initDisks() {
        ArrayList<DiskModel> disks = new ArrayList<DiskModel>();
        for (DiskImage diskImage : vm.getDiskList()) {
            disks.add(Linq.diskToModel(diskImage));
        }
        getModel().setDisks(disks);
        getModel().getDisksAllocationModel().setIsVolumeFormatAvailable(true);
        getModel().getDisksAllocationModel().setIsVolumeFormatChangable(true);
        updateIsDisksAvailable();
    }

    @Override
    public void initStorageDomains()
    {
        postInitStorageDomains();
    }

    @Override
    protected void updateNumaEnabled() {
    }
}
