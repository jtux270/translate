package org.ovirt.engine.core.common.action;

import java.util.List;

import org.ovirt.engine.core.compat.Guid;

public class DestroyImageParameters extends StorageDomainParametersBase {
    private static final long serialVersionUID = -345424889699747593L;
    private Guid vmId;
    private Guid imageGroupId;
    private List<Guid> imageList;
    private boolean postZero;
    private boolean force;

    private DestroyImageParameters() {}

    public DestroyImageParameters(
            Guid vdsId,
            Guid vmId,
            Guid storagePoolId,
            Guid storageDomainId,
            Guid imageGroupId,
            List<Guid> imageList,
            boolean postZero,
            boolean force) {
        super(storagePoolId, storageDomainId);
        setVdsId(vdsId);
        this.vmId = vmId;
        this.imageGroupId = imageGroupId;
        this.imageList = imageList;
        this.postZero = postZero;
        this.force = force;
    }

    public Guid getVmId() {
        return vmId;
    }

    public Guid getImageGroupId() {
        return imageGroupId;
    }

    public List<Guid> getImageList() {
        return imageList;
    }

    public boolean isPostZero() {
        return postZero;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String toString() {
        return new StringBuilder("DestroyImageParameters{")
                .append(super.toString())
                .append(", vmId=").append(vmId)
                .append(", imageGroupId=").append(imageGroupId)
                .append(", imageList=").append(imageList)
                .append(", postZero=").append(postZero)
                .append(", force=").append(force)
                .append('}').toString();
    }
}
