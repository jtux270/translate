package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.compat.Guid;

public class SetVdsStatusVDSCommandParameters extends VdsIdVDSCommandParametersBase {
    private VDSStatus _status;
    private NonOperationalReason nonOperationalReason;

    /**
     * Flag to display SPM stop command failure in audit log
     */
    private boolean stopSpmFailureLogged;

    public SetVdsStatusVDSCommandParameters(Guid vdsId, VDSStatus status) {
        super(vdsId);
        _status = status;
        nonOperationalReason = NonOperationalReason.NONE;
        stopSpmFailureLogged = false;
    }

    public SetVdsStatusVDSCommandParameters(Guid vdsId, VDSStatus status, NonOperationalReason nonOperationalReason) {
        this(vdsId, status);
        this.nonOperationalReason = nonOperationalReason;
    }

    public VDSStatus getStatus() {
        return _status;
    }

    public SetVdsStatusVDSCommandParameters() {
        _status = VDSStatus.Unassigned;
        nonOperationalReason = NonOperationalReason.NONE;
        stopSpmFailureLogged = false;
    }

    public NonOperationalReason getNonOperationalReason() {
        return nonOperationalReason;
    }

    public void setNonOperationalReason(NonOperationalReason nonOperationalReason) {
        this.nonOperationalReason = (nonOperationalReason == null ? NonOperationalReason.NONE : nonOperationalReason);
    }

    public boolean isStopSpmFailureLogged() {
        return stopSpmFailureLogged;
    }

    public void setStopSpmFailureLogged(boolean stopSpmFailureLogged) {
        this.stopSpmFailureLogged = stopSpmFailureLogged;
    }

    @Override
    public String toString() {
        return String.format("%s, status=%s, nonOperationalReason=%s, stopSpmFailureLogged=%s",
                super.toString(),
                getStatus(),
                getNonOperationalReason(),
                isStopSpmFailureLogged());
    }
}
