package org.ovirt.engine.core.bll.gluster;

import java.util.Map;

import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.UpdateGlusterHostPubKeyToSlaveParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.UpdateGlusterGeoRepKeysVDSParameters;

@InternalCommandAttribute
@NonTransactiveCommandAttribute
public class UpdateGlusterHostPubKeyToSlaveInternalCommand extends GlusterCommandBase<UpdateGlusterHostPubKeyToSlaveParameters> {

    public UpdateGlusterHostPubKeyToSlaveInternalCommand(UpdateGlusterHostPubKeyToSlaveParameters params) {
        this(params, null);
    }

    public UpdateGlusterHostPubKeyToSlaveInternalCommand(UpdateGlusterHostPubKeyToSlaveParameters params, CommandContext commandContext) {
        super(params, commandContext);
        setVdsId(getParameters().getId());
    }

    @Override
    protected VDS getUpServer() {
        return getVdsDao().get(getParameters().getId());
    }

    @Override
    protected boolean canDoAction() {
        if (getParameters().getPubKeys().isEmpty()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_GLUSTER_NO_PUB_KEYS_PASSED);
        }
        if (getParameters().getId() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_HOST_NOT_EXIST);
        }
        return super.canDoAction();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__WRITE_PUB_KEYS);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_WRITE_PUB_KEYS;
        } else {
            return errorType == null ? AuditLogType.GLUSTER_GEOREP_PUBLIC_KEY_WRITE_FAILED : errorType;
        }
    }

    @Override
    public Map<String, String> getCustomValues() {
        addCustomValue(GlusterConstants.VDS_NAME, getVds().getName());
        return super.getCustomValues();
    }

    @Override
    protected void executeCommand() {
        final VDSReturnValue writePubKeysReturnValue =
                runVdsCommand(VDSCommandType.UpdateGlusterGeoRepKeys,
                        new UpdateGlusterGeoRepKeysVDSParameters(getParameters().getId(),
                                getParameters().getPubKeys(),
                                getParameters().getRemoteUserName()));
        setSucceeded(writePubKeysReturnValue.getSucceeded());
        if (!writePubKeysReturnValue.getSucceeded()) {
            String errorMsg = writePubKeysReturnValue.getVdsError().getMessage();
            writePubKeysReturnValue.getVdsError().setMessage(errorMsg + " : " + getVdsDao().get(getParameters().getId()).getName());
            propagateFailure(convertToVdcReturnValueBase(writePubKeysReturnValue));
            return;
        }
    }
}
