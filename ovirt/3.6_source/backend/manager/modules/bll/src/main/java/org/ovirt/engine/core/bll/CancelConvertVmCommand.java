package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsAndVmIDVDSParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.ovirt.engine.core.vdsbroker.VmManager;

@NonTransactiveCommandAttribute
public class CancelConvertVmCommand<T extends VmOperationParameterBase> extends VmCommand<T> {

    @Inject
    protected ResourceManager resourceManager;

    public CancelConvertVmCommand(T param) {
        super(param);
    }

    @Override
    protected void init() {
        setVdsId(getVmManager().getConvertProxyHostId());
    }

    @Override
    protected void executeVmCommand() {
        VDSReturnValue retVal = runVdsCommand(
                VDSCommandType.CancelConvertVm,
                new VdsAndVmIDVDSParametersBase(getVdsId(), getVmId()));

        setSucceeded(retVal.getSucceeded());
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM);
        addCanDoActionMessage(EngineMessage.VAR__ACTION__CANCEL_CONVERSION);
    }

    @Override
    protected boolean canDoAction() {
        if (getVm() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        if (Guid.isNullOrEmpty(getVdsId())) {
            return failCanDoAction(EngineMessage.VM_CANNOT_CANCEL_CONVERSION_WHEN_VM_IS_NOT_BEING_CONVERTED);
        }

        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ?
                AuditLogType.VM_CANCEL_CONVERSION
                : AuditLogType.VM_CANCEL_CONVERSION_FAILED;
    }

    protected VmManager getVmManager() {
        return resourceManager.getVmManager(getVmId());
    }
}
