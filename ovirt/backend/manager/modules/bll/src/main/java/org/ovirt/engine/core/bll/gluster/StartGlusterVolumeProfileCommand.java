package org.ovirt.engine.core.bll.gluster;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeParameters;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeVDSParameters;

/**
 * BLL command to Start Gluster Volume Profile
 */
@NonTransactiveCommandAttribute
public class StartGlusterVolumeProfileCommand extends GlusterVolumeCommandBase<GlusterVolumeParameters> {

    public StartGlusterVolumeProfileCommand(GlusterVolumeParameters params) {
        super(params);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__START_PROFILE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_VOLUME);
    }

    @Override
    protected void executeCommand() {
        VDSReturnValue returnValue =
                runVdsCommand(VDSCommandType.StartGlusterVolumeProfile,
                        new GlusterVolumeVDSParameters(upServer.getId(), getGlusterVolumeName()));
        setSucceeded(returnValue.getSucceeded());
        if (!getSucceeded()) {
            handleVdsError(AuditLogType.GLUSTER_VOLUME_PROFILE_START_FAILED, returnValue.getVdsError().getMessage());
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_VOLUME_PROFILE_START;
        } else {
            return errorType == null ? AuditLogType.GLUSTER_VOLUME_PROFILE_START_FAILED : errorType;
        }
    }
}
