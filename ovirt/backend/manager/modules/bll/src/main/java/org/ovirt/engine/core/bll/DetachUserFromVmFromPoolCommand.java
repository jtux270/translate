package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.action.VmPoolSimpleUserParameters;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

@DisableInPrepareMode
public class DetachUserFromVmFromPoolCommand<T extends VmPoolSimpleUserParameters> extends
        VmPoolSimpleUserCommandBase<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     * @param commandId
     */
    protected DetachUserFromVmFromPoolCommand(Guid commandId) {
        super(commandId);
    }

    public DetachUserFromVmFromPoolCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VM, getVmId()));

    }

    public DetachUserFromVmFromPoolCommand(T parameters) {
        this(parameters, null);
    }

    protected void detachVmFromUser() {
        VM vm = DbFacade.getInstance().getVmDao().get(getParameters().getVmId());

        if (vm != null && getVmPoolId().equals(vm.getVmPoolId())) {
            Permissions perm = DbFacade
                    .getInstance()
                    .getPermissionDao()
                    .getForRoleAndAdElementAndObject(
                            PredefinedRoles.ENGINE_USER.getId(),
                            getAdUserId(), vm.getId());
            if (perm != null) {
                DbFacade.getInstance().getPermissionDao().remove(perm.getId());
                RestoreVmFromBaseSnapshot(vm);
            }
        }
    }

    private void RestoreVmFromBaseSnapshot(VM vm) {
        if (DbFacade.getInstance().getSnapshotDao().exists(vm.getId(), SnapshotType.STATELESS)) {
            log.infoFormat("Deleting snapshots for stateless vm {0}", vm.getId());
            VmOperationParameterBase restoreParams = new VmOperationParameterBase(vm.getId());

            // setting RestoreStatelessVm to run in new transaction so it could rollback internally if needed,
            // but still not affect this command, in order to keep permissions changes even on restore failure
            restoreParams.setTransactionScopeOption(TransactionScopeOption.RequiresNew);
            runInternalAction(VdcActionType.RestoreStatelessVm, restoreParams,
                    getContext().withCompensationContext(null));
        }
    }

    @Override
    protected void executeCommand() {
        if (getVmPoolId() != null) {
            detachVmFromUser();
        }
        setSucceeded(true);
    }
}
