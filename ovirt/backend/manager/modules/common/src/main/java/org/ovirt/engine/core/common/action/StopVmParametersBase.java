package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.Guid;

import java.io.Serializable;

public abstract class StopVmParametersBase extends VmOperationParameterBase implements Serializable {

    public StopVmParametersBase() {
    }

    public StopVmParametersBase(Guid vmId) {
        super(vmId);
    }

    private String stopReason;

    public String getStopReason() { return stopReason; }

    public void setStopReason(String value) { stopReason = value; }
}
