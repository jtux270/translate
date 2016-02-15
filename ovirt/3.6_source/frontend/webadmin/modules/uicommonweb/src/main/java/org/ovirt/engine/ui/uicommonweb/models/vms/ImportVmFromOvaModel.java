package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.action.ImportVmFromOvaParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaListModel;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

import com.google.inject.Inject;

public class ImportVmFromOvaModel extends ImportVmFromExternalProviderModel {

    private String ovaPath;
    private Guid hostId;

    @Inject
    protected ImportVmFromOvaModel(VmImportGeneralModel vmImportGeneralModel,
            VmImportDiskListModel importDiskListModel,
            VmImportInterfaceListModel vmImportInterfaceListModel,
            ClusterListModel<Void> cluster,
            QuotaListModel clusterQuota) {
        super(vmImportGeneralModel, importDiskListModel, vmImportInterfaceListModel, cluster, clusterQuota);
    }

    @Override
    protected void setTargetArchitecture(List<VM> externalVms) {
        setTargetArchitecture(ArchitectureType.x86_64);
    }

    public void setIsoName(String ovaPath) {
        this.ovaPath = ovaPath;
    }

    public void setHostId(Guid hostId) {
        this.hostId = hostId;
    }

    @Override
    public void importVms(IFrontendMultipleActionAsyncCallback callback) {
        Frontend.getInstance().runMultipleAction(
                VdcActionType.ImportVmFromOva,
                buildImportVmFromOvaParameters(),
                callback);
    }

    private List<VdcActionParametersBase> buildImportVmFromOvaParameters() {
        ImportVmData importVmData = (ImportVmData) getItems().iterator().next();
        VM vm = importVmData.getVm();

        ImportVmFromOvaParameters prm = new ImportVmFromOvaParameters();
        prm.setVm(vm);
        prm.setOvaPath(ovaPath);
        prm.setProxyHostId(hostId);
        prm.setDestDomainId(getStorage().getSelectedItem().getId());
        prm.setStoragePoolId(getStoragePool().getId());
        prm.setVdsGroupId(((VDSGroup) getCluster().getSelectedItem()).getId());
        prm.setVirtioIsoName(getIso().getIsChangable() ? getIso().getSelectedItem() : null);

        if (getClusterQuota().getSelectedItem() != null &&
                getClusterQuota().getIsAvailable()) {
            prm.setQuotaId(((Quota) getClusterQuota().getSelectedItem()).getId());
        }

        CpuProfile cpuProfile = getCpuProfiles().getSelectedItem();
        if (cpuProfile != null) {
            prm.setCpuProfileId(cpuProfile.getId());
        }

        prm.setForceOverride(true);
        prm.setCopyCollapse((Boolean) importVmData.getCollapseSnapshots().getEntity());

        for (Map.Entry<Guid, Disk> entry : vm.getDiskMap().entrySet()) {
            DiskImage disk = (DiskImage) entry.getValue();
            ImportDiskData importDiskData = getDiskImportData(disk.getDiskAlias());
            disk.setVolumeType(getAllocation().getSelectedItem());
            disk.setvolumeFormat(AsyncDataProvider.getInstance().getDiskVolumeFormat(
                    disk.getVolumeType(),
                    getStorage().getSelectedItem().getStorageType()));

            if (getDiskImportData(disk.getDiskAlias()).getSelectedQuota() != null) {
                disk.setQuotaId(importDiskData.getSelectedQuota().getId());
            }
        }

        updateNetworkInterfacesForVm(vm);

        if (importVmData.isExistsInSystem() ||
                (Boolean) (importVmData.getClone().getEntity())) {
            prm.setImportAsNewEntity(true);
            prm.setCopyCollapse(true);
        }

        return java.util.Collections.<VdcActionParametersBase>singletonList(prm);
    }

}
