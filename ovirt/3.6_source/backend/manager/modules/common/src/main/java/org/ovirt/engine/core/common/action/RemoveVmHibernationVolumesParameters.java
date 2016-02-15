package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.Guid;

public class RemoveVmHibernationVolumesParameters extends VdcActionParametersBase {
    private Guid vmId;

    public RemoveVmHibernationVolumesParameters(Guid vmId) {
        this.vmId = vmId;
    }

    public RemoveVmHibernationVolumesParameters() {
        vmId = Guid.Empty;
    }

    public void setVmId(Guid vmId) {
        this.vmId = vmId;
    }

    public Guid getVmId() {
        return vmId;
    }
}
