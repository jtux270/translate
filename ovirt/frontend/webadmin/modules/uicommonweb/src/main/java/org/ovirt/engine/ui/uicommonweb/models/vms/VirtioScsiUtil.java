package org.ovirt.engine.ui.uicommonweb.models.vms;

import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.UIConstants;

import java.util.ArrayList;

public class VirtioScsiUtil {

    private final UIConstants constants = ConstantsManager.getInstance().getConstants();

    private VirtioScasiEnablingFinished finishedCallback;

    private UnitVmModel model;

    public VirtioScsiUtil(UnitVmModel model) {
        this.model = model;
    }

    public void updateVirtioScsiEnabled(final Guid vmId, int osId, VirtioScasiEnablingFinished finishedCallback) {
        this.finishedCallback = finishedCallback;

        final VDSGroup cluster = model.getSelectedCluster();
        if (cluster == null) {
            return;
        }

        AsyncDataProvider.getDiskInterfaceList(osId, cluster.getcompatibility_version(),
                new AsyncQuery(model, new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object parentModel, Object returnValue) {
                        ArrayList<DiskInterface> diskInterfaces = (ArrayList<DiskInterface>) returnValue;
                        boolean isOsSupportVirtioScsi = diskInterfaces.contains(DiskInterface.VirtIO_SCSI);

                        callBeforeUpdates();
                        model.getIsVirtioScsiEnabled().setIsChangable(isOsSupportVirtioScsi);

                        if (!isOsSupportVirtioScsi) {
                            model.getIsVirtioScsiEnabled().setEntity(false);
                            model.getIsVirtioScsiEnabled().setChangeProhibitionReason(constants.cannotEnableVirtioScsiForOs());
                            callAfterUpdates();
                        } else {
                            if (Guid.isNullOrEmpty(vmId)) {
                                VDSGroup cluster = model.getSelectedCluster();
                                boolean isVirtioScsiEnabled = (Boolean) AsyncDataProvider.getConfigValuePreConverted(
                                        ConfigurationValues.VirtIoScsiEnabled, cluster.getcompatibility_version().getValue());
                                model.getIsVirtioScsiEnabled().setEntity(isVirtioScsiEnabled);
                                callAfterUpdates();
                            } else {
                                AsyncDataProvider.isVirtioScsiEnabledForVm(new AsyncQuery(model, new INewAsyncCallback() {
                                    @Override
                                    public void onSuccess(Object parentModel, Object returnValue) {
                                        model.getIsVirtioScsiEnabled().setEntity((Boolean) returnValue);
                                        callAfterUpdates();
                                    }
                                }), vmId);
                            }
                        }
                    }
                }));
    }

    public void callBeforeUpdates() {
        if (finishedCallback != null) {
            finishedCallback.beforeUpdates();
        }
    }

    public void callAfterUpdates() {
        if (finishedCallback != null) {
            finishedCallback.afterUpdates();
        }
    }

    public static interface VirtioScasiEnablingFinished {
        void beforeUpdates();

        void afterUpdates();
    }
}
