package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.common.businessentities.ImageOperation;
import org.ovirt.engine.core.compat.Guid;

public class MoveImageGroupVDSCommandParameters
        extends TargetDomainImageGroupVDSCommandParameters implements PostZero {
    private Guid privateVmId;

    public Guid getVmId() {
        return privateVmId;
    }

    private void setVmId(Guid value) {
        privateVmId = value;
    }

    private ImageOperation privateOp;

    public ImageOperation getOp() {
        return privateOp;
    }

    private void setOp(ImageOperation value) {
        privateOp = value;
    }

    public MoveImageGroupVDSCommandParameters(Guid storagePoolId, Guid storageDomainId, Guid imageGroupId,
            Guid dstStorageDomainId, Guid vmId, ImageOperation op, boolean postZero, boolean force) {
        super(storagePoolId, storageDomainId, imageGroupId, dstStorageDomainId);
        setVmId(vmId);
        setOp(op);
        setPostZero(postZero);
        setForce(force);
    }

    private boolean privatePostZero;

    @Override
    public boolean getPostZero() {
        return privatePostZero;
    }

    @Override
    public void setPostZero(boolean postZero) {
        privatePostZero = postZero;
    }

    private boolean privateForce;

    public boolean getForce() {
        return privateForce;
    }

    public void setForce(boolean value) {
        privateForce = value;
    }

    public MoveImageGroupVDSCommandParameters() {
        privateVmId = Guid.Empty;
        privateOp = ImageOperation.Unassigned;
    }

    @Override
    public String toString() {
        return String.format("%s, vmId = %s, op = %s, postZero = %s, force = %s",
                super.toString(),
                getVmId(),
                getOp(),
                getPostZero(),
                getForce());
    }
}
