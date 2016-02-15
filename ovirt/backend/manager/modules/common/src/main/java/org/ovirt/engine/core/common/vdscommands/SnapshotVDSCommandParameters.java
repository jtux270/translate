package org.ovirt.engine.core.common.vdscommands;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.compat.Guid;

public class SnapshotVDSCommandParameters extends VdsAndVmIDVDSParametersBase {
    /** The disks images that snapshot should be taken for */
    private List<DiskImage> images;
    /** String representation of the volume in which the memory will be dump to */
    private String memoryVolume;

    public SnapshotVDSCommandParameters(Guid vdsId, Guid vmId, List<DiskImage> images) {
        super(vdsId, vmId);
        this.images = images;
    }

    public SnapshotVDSCommandParameters(Guid vdsId, Guid vmId, List<DiskImage> images, String memoryVolume) {
        this(vdsId, vmId, images);
        this.memoryVolume = memoryVolume;
    }

    public SnapshotVDSCommandParameters() {
    }

    public List<DiskImage> getImages() {
        return images;
    }

    public String getMemoryVolume() {
        return memoryVolume;
    }

    public boolean isMemoryVolumeExists() {
        return memoryVolume != null;
    }
}
