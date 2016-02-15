package org.ovirt.engine.core.bll;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.ExternalVariable;
import org.ovirt.engine.core.common.action.FenceVdsManualyParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.KdumpFlowStatus;
import org.ovirt.engine.core.common.businessentities.KdumpStatus;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VdsKdumpStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.SetVdsStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.UpdateVdsVMsClearedVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.ThreadUtils;

/**
 * Tries to detect if host is kdumping.
 */
@NonTransactiveCommandAttribute
public class VdsKdumpDetectionCommand<T extends VdsActionParameters> extends VdsCommand<T> {
    /**
     * Name of external variable to store fence_kdump listener heartbeat
     */
    private static final String LISTENER_HEARTBEAT = "fence-kdump-listener-heartbeat";

    /**
     * Listener timeout interval in milliseconds
     */
    private final long listenerTimeoutInterval;

    /**
     * Kdump detection results
     */
    private enum KdumpDetectionResult {
        LISTENER_NOT_ALIVE,
        KDUMP_NOT_DETECTED,
        KDUMP_FINISHED
    }

    /**
     * Kdump detection result for specified host
     */
    private KdumpDetectionResult kdumpDetectionResult;


    public VdsKdumpDetectionCommand(T parameters) {
        this(parameters, null);
    }

    public VdsKdumpDetectionCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        listenerTimeoutInterval = Config.<Integer>getValue(ConfigValues.FenceKdumpListenerTimeout) * 1000;
        kdumpDetectionResult = null;
    }


    private boolean isListenerAlive() {
        ExternalVariable fkAlive = getDbFacade().getExternalVariableDao().get(LISTENER_HEARTBEAT);
        return fkAlive != null
                && fkAlive.getUpdateDate().getTime() + listenerTimeoutInterval >= System.currentTimeMillis();
    }

    private void restartVdsVms() {
        List<VM> vms = getVmDAO().getAllRunningForVds(getVdsId());
        if (!vms.isEmpty()) {
            RestartVdsVmsOperation restartVmsOper = new RestartVdsVmsOperation(
                    getContext(),
                    getVds()
            );
            restartVmsOper.restartVms(vms);
            runVdsCommand(VDSCommandType.UpdateVdsVMsCleared,
                            new UpdateVdsVMsClearedVDSCommandParameters(getVds().getId()));
        }
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return FenceVdsBaseCommand.createFenceExclusiveLocksMap(getVdsId());
    }

    private void executeFenceVdsManuallyAction() {
        FenceVdsManualyParameters fenceVdsManuallyParams = new FenceVdsManualyParameters(false);
        fenceVdsManuallyParams.setStoragePoolId(getVds().getStoragePoolId());
        fenceVdsManuallyParams.setVdsId(getVdsId());
        fenceVdsManuallyParams.setSessionId(getParameters().getSessionId());
        fenceVdsManuallyParams.setParentCommand(VdcActionType.RestartVds);

        // if fencing succeeded, call to reset irs in order to try select new spm
        runInternalAction(
                VdcActionType.FenceVdsManualy,
                fenceVdsManuallyParams,
                getContext());
    }

    private KdumpDetectionResult detectHostKdumping() {
        VdsKdumpStatus kdumpStatus;
        int messageInterval = Config.<Integer>getValue(ConfigValues.FenceKdumpMessageInterval) * 1000;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, Config.<Integer>getValue(ConfigValues.KdumpStartedTimeout));
        long timeout = cal.getTimeInMillis();
        boolean kdumpDetected = false;

        while (true) {
            if (!isListenerAlive()) {
                // fence_kdump listener is down, continue with hard fencing
                return KdumpDetectionResult.LISTENER_NOT_ALIVE;
            }

            kdumpStatus = getDbFacade().getVdsKdumpStatusDao().get(getVdsId());
            if (kdumpStatus == null) {
                // host kdump flow hasn't started yet
                if (timeout < System.currentTimeMillis()) {
                    // host kdump flow not detected until timeout, continue with hard fencing
                    return KdumpDetectionResult.KDUMP_NOT_DETECTED;
                }
            } else {
                if (!kdumpDetected) {
                    // host kdump status detected
                    kdumpDetected = true;

                    AuditLogableBase base = new AuditLogableBase();
                    base.setVds(getVds());
                    AuditLogDirector.log(base, AuditLogType.KDUMP_FLOW_DETECTED_ON_VDS);

                    // set status to Kdumping to prevent Host Monitoring errors and wait until kdump finishes
                    runVdsCommand(VDSCommandType.SetVdsStatus,
                                    new SetVdsStatusVDSCommandParameters(getVdsId(), VDSStatus.Kdumping)
                            );

                    // restart VMs running on Vds
                    restartVdsVms();

                    // execute all actions needed to manual fence the host (without PM fencing)
                    executeFenceVdsManuallyAction();
                }

                if (kdumpStatus.getStatus() == KdumpFlowStatus.FINISHED) {
                    // host finished its kdump flow, set status to Non Responsive
                    runVdsCommand(VDSCommandType.SetVdsStatus,
                                    new SetVdsStatusVDSCommandParameters(getVdsId(), VDSStatus.NonResponsive)
                            );
                    return KdumpDetectionResult.KDUMP_FINISHED;
                }
            }
            ThreadUtils.sleep(messageInterval);
        }
    }

    @Override
    protected boolean canDoAction() {
        if (getVds().getKdumpStatus() != KdumpStatus.ENABLED) {
            addCanDoActionMessage(VdcBllMessages.KDUMP_DETECTION_NOT_CONFIGURED_ON_VDS);
            return false;
        }

        boolean detectionEnabled = getVds().isPmKdumpDetection();
        if (!detectionEnabled) {
            addCanDoActionMessage(VdcBllMessages.KDUMP_DETECTION_NOT_ENABLED_FOR_VDS);
        }
        return detectionEnabled;
    }

    /**
     * If the VDS is not responding, it tries to detect if VDS is kdumping or not.
     */
    @Override
    protected void executeCommand() {
        setVds(null);
        if (getVds() == null) {
            setCommandShouldBeLogged(false);
            log.infoFormat("Kdump detection will not be executed on host {0}({1}) since it doesn't exist anymore.",
                    getVdsName(),
                    getVdsId()
            );
            getReturnValue().setSucceeded(false);
            return;
        }

        kdumpDetectionResult = detectHostKdumping();

        getReturnValue().setSucceeded(kdumpDetectionResult == KdumpDetectionResult.KDUMP_FINISHED);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (kdumpDetectionResult) {
            case LISTENER_NOT_ALIVE:
                return AuditLogType.FENCE_KDUMP_LISTENER_IS_NOT_ALIVE;

            case KDUMP_NOT_DETECTED:
                return AuditLogType.KDUMP_FLOW_NOT_DETECTED_ON_VDS;

            case KDUMP_FINISHED:
                return AuditLogType.KDUMP_FLOW_FINISHED_ON_VDS;

            default:
                return null;
        }
    }
}
