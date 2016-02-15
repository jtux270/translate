package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.vdsbroker.vdsbroker.CancelMigrationVDSParameters;

@NonTransactiveCommandAttribute
public class CancelMigrateVmCommand<T extends VmOperationParameterBase> extends VmCommand<T> {

    public CancelMigrateVmCommand(T param) {
        super(param);
    }

    @Override
    protected void executeCommand() {
        VDSReturnValue retVal = runVdsCommand(
                        VDSCommandType.CancelMigrate,
                        new CancelMigrationVDSParameters(getVm().getRunOnVds(),
                                getParameters().getVmId(), false));

        setSucceeded(retVal.getSucceeded());
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM);
        addCanDoActionMessage(EngineMessage.VAR__ACTION__CANCEL_MIGRATE);
    }

    @Override
    protected boolean canDoAction() {
        if (getVm() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        if (getVm().getStatus() != VMStatus.MigratingFrom) {
            return failCanDoAction(EngineMessage.VM_CANNOT_CANCEL_MIGRATION_WHEN_VM_IS_NOT_MIGRATING);
        }

        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.VM_CANCEL_MIGRATION
                : AuditLogType.VM_CANCEL_MIGRATION_FAILED;
    }
}
