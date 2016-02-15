package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.Guid;

public class RemoveVMVDSCommandParameters extends StorageDomainIdParametersBase {
    public RemoveVMVDSCommandParameters(Guid storagePoolId, Guid vmGuid) {
        this(storagePoolId, vmGuid, Guid.Empty);
    }

    public RemoveVMVDSCommandParameters(Guid storagePoolId, Guid vmGuid, Guid storageDomainId) {
        super(storagePoolId);
        setVmGuid(vmGuid);
        setStorageDomainId(storageDomainId);
    }

    private Guid privateVmGuid;

    public Guid getVmGuid() {
        return privateVmGuid;
    }

    public void setVmGuid(Guid value) {
        privateVmGuid = value;
    }

    public RemoveVMVDSCommandParameters() {
        privateVmGuid = Guid.Empty;
    }

    @Override
    public String toString() {
        return String.format("%s, vmGuid = %s", super.toString(), getVmGuid());
    }
}
