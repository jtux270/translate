package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;

import org.ovirt.engine.core.common.action.ImportVmTemplateParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

public class RegisterTemplateModel extends RegisterEntityModel {

    protected void onSave() {
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();
        for (ImportEntityData entityData : getEntities().getItems()) {
            VmTemplate vmTemplate = (VmTemplate) entityData.getEntity();
            VDSGroup vdsGroup = entityData.getCluster().getSelectedItem();

            ImportVmTemplateParameters params = new ImportVmTemplateParameters();
            params.setContainerId(vmTemplate.getId());
            params.setStorageDomainId(getStorageDomainId());
            params.setImagesExistOnTargetStorageDomain(true);
            params.setVdsGroupId(vdsGroup != null ? vdsGroup.getId() : null);

            if (isQuotaEnabled()) {
                Quota quota = entityData.getClusterQuota().getSelectedItem();
                params.setQuotaId(quota != null ? quota.getId() : null);
                params.setDiskTemplateMap(vmTemplate.getDiskTemplateMap());
                updateDiskQuotas(new ArrayList<Disk>(params.getDiskTemplateMap().values()));
            }

            parameters.add(params);
        }

        startProgress(null);
        Frontend.getInstance().runMultipleAction(VdcActionType.ImportVmTemplateFromConfiguration, parameters, new IFrontendMultipleActionAsyncCallback() {
            @Override
            public void executed(FrontendMultipleActionAsyncResult result) {
                stopProgress();
                cancel();
            }
        }, this);
    }

}
