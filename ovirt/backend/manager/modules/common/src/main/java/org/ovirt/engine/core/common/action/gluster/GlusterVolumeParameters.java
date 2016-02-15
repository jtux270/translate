package org.ovirt.engine.core.common.action.gluster;

import javax.validation.constraints.NotNull;

import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.compat.Guid;

/**
 * Parameter class with Gluster Volume Id parameter. <br>
 * This will be used directly by some commands (e.g. start volume), <br>
 * and inherited by others (e.g. set volume option).
 */
public class GlusterVolumeParameters extends VdcActionParametersBase {
    private static final long serialVersionUID = -5148741622108406754L;

    @NotNull(message = "VALIDATION.GLUSTER.VOLUME.ID.NOT_NULL")
    private Guid volumeId;

    public GlusterVolumeParameters() {
    }

    public GlusterVolumeParameters(Guid volumeId) {
        setVolumeId(volumeId);
    }

    public void setVolumeId(Guid volumeId) {
        this.volumeId = volumeId;
    }

    public Guid getVolumeId() {
        return volumeId;
    }
}
