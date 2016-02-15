package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.Guid;

public class VdsAndPoolIDVDSParametersBase extends VdsIdVDSCommandParametersBase {
    private Guid storagePoolId;

    public VdsAndPoolIDVDSParametersBase(Guid vdsId, Guid storagePoolId) {
        super(vdsId);
        this.storagePoolId = storagePoolId;
    }

    public VdsAndPoolIDVDSParametersBase() {
        storagePoolId = Guid.Empty;
    }

    public Guid getStoragePoolId() {
        return storagePoolId;
    }

    @Override
    public String toString() {
        return String.format("%s, storagePoolId=%s", super.toString(), getStoragePoolId());
    }
}
