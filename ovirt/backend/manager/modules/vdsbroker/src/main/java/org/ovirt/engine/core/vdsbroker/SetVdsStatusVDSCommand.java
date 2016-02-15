package org.ovirt.engine.core.vdsbroker;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsSpmStatus;
import org.ovirt.engine.core.common.vdscommands.ResetIrsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SetVdsStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public class SetVdsStatusVDSCommand<P extends SetVdsStatusVDSCommandParameters> extends VdsIdVDSCommandBase<P> {
    public SetVdsStatusVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsIdCommand() {
        final SetVdsStatusVDSCommandParameters parameters = getParameters();

        if (_vdsManager != null) {

            final VDS vds = getVds();
            if (vds.getSpmStatus() != VdsSpmStatus.None && parameters.getStatus() != VDSStatus.Up) {
                log.infoFormat("VDS {0} is spm and moved from up calling resetIrs.", vds.getName());
                // check if this host was spm and reset if do.
                getVDSReturnValue().setSucceeded(
                        ResourceManager
                                .getInstance()
                                .runVdsCommand(
                                        VDSCommandType.ResetIrs,
                                        new ResetIrsVDSCommandParameters(vds.getStoragePoolId(), vds.getId()))
                                .getSucceeded());

                if (!getVDSReturnValue().getSucceeded()) {
                    if (getParameters().isStopSpmFailureLogged()) {
                        AuditLogableBase base = new AuditLogableBase();
                        base.setVds(vds);
                        AuditLogDirector.log(base, AuditLogType.VDS_STATUS_CHANGE_FAILED_DUE_TO_STOP_SPM_FAILURE);
                    }

                    if (parameters.getStatus() == VDSStatus.PreparingForMaintenance) {
                        // ResetIrs command failed, SPM host status cannot be moved to Preparing For Maintenance
                        return;
                    }
                }

            }

            updateVdsFromParameters(parameters, vds);
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                    _vdsManager.setStatus(parameters.getStatus(), vds);
                    _vdsManager.updateDynamicData(vds.getDynamicData());
                    _vdsManager.updateStatisticsData(vds.getStatisticsData());
                    return null;
                }
            });
        } else {
            getVDSReturnValue().setSucceeded(false);
        }
    }

    private void updateVdsFromParameters(SetVdsStatusVDSCommandParameters parameters, VDS vds) {
        vds.getDynamicData().setNonOperationalReason(parameters.getNonOperationalReason());
    }

    private static final Log log = LogFactory.getLog(SetVdsStatusVDSCommand.class);
}
