package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.errors.VDSError;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.SpmStopVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.lock.EngineLock;
import org.ovirt.engine.core.utils.lock.LockManagerFactory;
import org.ovirt.engine.core.vdsbroker.ResourceManager;

public class SpmStopVDSCommand<P extends SpmStopVDSCommandParameters> extends VdsBrokerCommand<P> {
    private EngineLock lock;

    public SpmStopVDSCommand(P parameters) {
        super(parameters, DbFacade.getInstance().getVdsDao().get(parameters.getVdsId()));
    }

    private EngineLock retrieveVdsExecutionLock() {
        if (lock == null) {
            Map<String, Pair<String, String>> exsluciveLock = Collections.singletonMap(getParameters().getVdsId().toString(), new Pair<>(LockingGroup.VDS_EXECUTION.toString(), VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED.toString()));
            lock = new EngineLock(exsluciveLock, null);
        }
        return  lock;
    }

    @Override
    protected void executeVdsBrokerCommand() {
        boolean lockAcquired = false;
        try {
            if (canVdsBeReached()) {
                lockAcquired = LockManagerFactory.getLockManager().acquireLock(retrieveVdsExecutionLock()).getFirst();
                if (!lockAcquired) {
                    getVDSReturnValue().setVdsError(new VDSError(VdcBllErrors.ENGINE,
                            "Failed to acquire vds execution lock - related operation is under execution"));
                    getVDSReturnValue().setSucceeded(false);
                    return;
                }

                boolean performSpmStop = true;
                try {
                    VDSReturnValue vdsReturnValue = ResourceManager
                            .getInstance()
                            .runVdsCommand(VDSCommandType.HSMGetAllTasksStatuses,
                                    new VdsIdVDSCommandParametersBase(getVds().getId()));

                    if (isNotSPM(vdsReturnValue)) {
                        return;
                    }

                    getVDSReturnValue().setSucceeded(vdsReturnValue.getSucceeded());
                    getVDSReturnValue().setVdsError(vdsReturnValue.getVdsError());

                    if (vdsReturnValue.getReturnValue() != null) {
                        performSpmStop = ((HashMap<Guid, AsyncTaskStatus>) vdsReturnValue.getReturnValue()).isEmpty();
                    }
                } catch (Exception e) {
                    performSpmStop = false;
                    log.infoFormat("SpmStopVDSCommand::Could not get tasks on vds {0}, reason: {1}",
                            getVds().getName(),
                            e.getMessage());
                }
                if (performSpmStop) {
                    log.infoFormat("SpmStopVDSCommand::Stopping SPM on vds {0}, pool id {1}", getVds().getName(),
                            getParameters().getStoragePoolId());
                    status = getBroker().spmStop(getParameters().getStoragePoolId().toString());
                    proceedProxyReturnValue();
                } else {
                    getVDSReturnValue().setSucceeded(false);
                    if (getVDSReturnValue().getVdsError() == null) {
                        log.infoFormat("SpmStopVDSCommand::Not stopping SPM on vds {0}, pool id {1} as there are uncleared tasks",
                                getVds().getName(),
                                getParameters().getStoragePoolId());
                        VDSError error = new VDSError();
                        error.setCode(VdcBllErrors.TaskInProgress);
                        getVDSReturnValue().setVdsError(error);
                    } else if (getVDSReturnValue().getVdsError().getCode() == VdcBllErrors.VDS_NETWORK_ERROR) {
                        log.infoFormat(
                                "SpmStopVDSCommand::Could not get tasks on vds {0} - network exception, not stopping spm! pool id {1}",
                                getVds().getName(),
                                getParameters().getStoragePoolId());
                    }
                }
            } else {
                log.infoFormat("SpmStopVDSCommand:: vds {0} is in {1} status - not performing spm stop, pool id {2}",
                        getVds().getName(), getVds().getStatus(), getParameters().getStoragePoolId());
                getVDSReturnValue().setVdsError(new VDSError(VdcBllErrors.VDS_NETWORK_ERROR,
                        "Vds is in incorrect status"));
                getVDSReturnValue().setSucceeded(false);
            }
        } catch (RuntimeException exp) {
            log.warnFormat("could not stop spm of pool {0} on vds {1} - reason: {2}", getParameters()
                    .getStoragePoolId(), getParameters().getVdsId(), exp.toString());
            getVDSReturnValue().setExceptionObject(exp);
            getVDSReturnValue().setSucceeded(false);
        } finally {
            if (lockAcquired) {
                LockManagerFactory.getLockManager().releaseLock(retrieveVdsExecutionLock());
            }
        }
    }

    /**
     * Checks if the VDS is in a state where it can be reached or not, since if it can't be reached we don't want to
     * try to stop the SPM because the command won't work.
     * @return Can the VDS be reached or not?
     */
    private boolean canVdsBeReached() {
        VDSStatus vdsStatus = getVds().getStatus();
        if (vdsStatus == VDSStatus.Down ||
                vdsStatus == VDSStatus.Reboot ||
                vdsStatus == VDSStatus.Kdumping) {
            vdsStatus = getVds().getPreviousStatus();
        }
        return vdsStatus != VDSStatus.NonResponsive && getVds().getStatus() != VDSStatus.Connecting;
    }

    private boolean isNotSPM(VDSReturnValue returnValue) {
        return returnValue.getVdsError() != null &&
                returnValue.getVdsError().getCode() == VdcBllErrors.SpmStatusError;
    }

    @Override
    protected void proceedProxyReturnValue() {
        VdcBllErrors returnStatus = getReturnValueFromStatus(getReturnStatus());
        switch (returnStatus) {
        case StoragePoolUnknown:
        case SpmStatusError:
            // ignore this, the parser can handle the empty result.
            break;
        case TaskInProgress:
            getVDSReturnValue().setVdsError(new VDSError(returnStatus, getReturnStatus().mMessage));
            getVDSReturnValue().setSucceeded(false);
            break;
        default:
            super.proceedProxyReturnValue();
            initializeVdsError(returnStatus);
            break;
        }
    }
}
