package org.ovirt.engine.core.common.action;

import java.io.Serializable;
import org.ovirt.engine.core.compat.Guid;

public class VmPoolParametersBase extends VdcActionParametersBase implements Serializable {
    private static final long serialVersionUID = -4244908570752388901L;
    private Guid _vmPoolId;

    public VmPoolParametersBase(Guid vmPoolId) {
        _vmPoolId = vmPoolId;
        privateStorageDomainId = Guid.Empty;
    }

    public Guid getVmPoolId() {
        return _vmPoolId;
    }

    public void setVmPoolId(Guid value) {
        _vmPoolId = value;
    }

    private Guid privateStorageDomainId;

    public Guid getStorageDomainId() {
        return privateStorageDomainId;
    }

    public void setStorageDomainId(Guid value) {
        privateStorageDomainId = value;
    }

    public VmPoolParametersBase() {
        privateStorageDomainId = Guid.Empty;
    }
}
