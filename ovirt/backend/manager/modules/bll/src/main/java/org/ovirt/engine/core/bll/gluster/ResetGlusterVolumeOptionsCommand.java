package org.ovirt.engine.core.bll.gluster;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.gluster.ResetGlusterVolumeOptionsParameters;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.ResetGlusterVolumeOptionsVDSParameters;

/**
 * BLL Command to Reset Gluster Volume Options
 */
@NonTransactiveCommandAttribute
public class ResetGlusterVolumeOptionsCommand extends GlusterVolumeCommandBase<ResetGlusterVolumeOptionsParameters> {

    private boolean isResetAllOptions;

    public ResetGlusterVolumeOptionsCommand(ResetGlusterVolumeOptionsParameters params) {
        super(params);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution).withWait(true);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__RESET);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_VOLUME_OPTION);
    }

    @Override
    protected void executeCommand() {
        VDSReturnValue returnValue = runVdsCommand(VDSCommandType.ResetGlusterVolumeOptions,
                new ResetGlusterVolumeOptionsVDSParameters(upServer.getId(),
                        getGlusterVolumeName(), getParameters().getVolumeOption(), getParameters().isForceAction()));
        setSucceeded(returnValue.getSucceeded());

        if (getSucceeded()) {

            if (getParameters().getVolumeOption() != null && !(getParameters().getVolumeOption().getKey().isEmpty())) {
                GlusterVolumeOptionEntity entity = getGlusterVolume().getOption(getParameters().getVolumeOption().getKey());
                removeOptionInDb(entity);
                isResetAllOptions = false;
                if(entity != null) {
                    String optionValue = entity.getValue();
                    getParameters().getVolumeOption().setValue(optionValue != null ? optionValue : "");
                    addCustomValue(GlusterConstants.OPTION_KEY, getParameters().getVolumeOption().getKey());
                    addCustomValue(GlusterConstants.OPTION_VALUE, getParameters().getVolumeOption().getValue());
                }
            } else {
                for (GlusterVolumeOptionEntity option : getGlusterVolume().getOptions()) {
                    removeOptionInDb(option);
                }
                isResetAllOptions = true;
            }
        } else {
            handleVdsError(AuditLogType.GLUSTER_VOLUME_OPTIONS_RESET_FAILED, returnValue.getVdsError().getMessage());
            return;
        }
    }


    /**
     * Remove the volume option in DB. If the option with given key already exists for the volume, <br>
     * it will be deleted.
     *
     * @param option
     */
    private void removeOptionInDb(GlusterVolumeOptionEntity option) {
        getGlusterOptionDao().removeVolumeOption(option.getId());
    }


    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return (isResetAllOptions) ? AuditLogType.GLUSTER_VOLUME_OPTIONS_RESET_ALL : AuditLogType.GLUSTER_VOLUME_OPTIONS_RESET;
        } else {
            return errorType == null ? AuditLogType.GLUSTER_VOLUME_OPTIONS_RESET_FAILED : errorType;
        }
    }

}
