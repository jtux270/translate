package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.action.ImportVmFromExternalProviderParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
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

public class ImportVmFromVmwareModel extends ImportVmFromExternalProviderModel {

    private String url;
    private String username;
    private String password;
    private Guid proxyHostId;

    @Inject
    public ImportVmFromVmwareModel(VmImportGeneralModel vmImportGeneralModel,
            VmImportDiskListModel importDiskListModel,
            VmImportInterfaceListModel vmImportInterfaceListModel,
            ClusterListModel<Void> cluster,
            QuotaListModel clusterQuota) {
        super(vmImportGeneralModel, importDiskListModel, vmImportInterfaceListModel, cluster, clusterQuota);
    }

    void setUrl(String url) {
        this.url = url;
    }

    void setUsername(String username) {
        this.username = username;
    }


    void setPassword(String password) {
        this.password = password;
    }

    void setProxyHostId(Guid proxyHostId) {
        this.proxyHostId = proxyHostId;
    }

    @Override
    public void importVms(IFrontendMultipleActionAsyncCallback callback) {
        Frontend.getInstance().runMultipleAction(
                VdcActionType.ImportVmFromExternalProvider,
                buildImportVmFromExternalProviderParameters(),
                callback);
    }

    private List<VdcActionParametersBase> buildImportVmFromExternalProviderParameters() {
        List<VdcActionParametersBase> prms = new ArrayList<VdcActionParametersBase>();

        for (Object item : getItems()) {
            VM vm = ((ImportVmData) item).getVm();

            ImportVmFromExternalProviderParameters prm =
                    new ImportVmFromExternalProviderParameters();
            prm.setVm(vm);
            prm.setUrl(url);
            prm.setUsername(username);
            prm.setPassword(password);
            prm.setProxyHostId(proxyHostId);
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
            prm.setCopyCollapse((Boolean) ((ImportVmData) item).getCollapseSnapshots().getEntity());

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

            if (((ImportVmData) item).isExistsInSystem() ||
                    (Boolean) ((ImportVmData) item).getClone().getEntity()) {
                prm.setImportAsNewEntity(true);
                prm.setCopyCollapse(true);
            }

            prms.add(prm);
        }

        return prms;
    }
}
