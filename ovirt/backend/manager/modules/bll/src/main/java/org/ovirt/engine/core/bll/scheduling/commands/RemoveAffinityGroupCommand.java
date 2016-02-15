package org.ovirt.engine.core.bll.scheduling.commands;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.scheduling.parameters.AffinityGroupCRUDParameters;


public class RemoveAffinityGroupCommand extends AffinityGroupCRUDCommand {

    public RemoveAffinityGroupCommand(AffinityGroupCRUDParameters parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        if (getAffinityGroup() == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_INVALID_AFFINITY_GROUP_ID);
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        getAffinityGroupDao().remove(getParameters().getAffinityGroupId());
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_REMOVED_AFFINITY_GROUP
                : AuditLogType.USER_FAILED_TO_REMOVE_AFFINITY_GROUP;
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REMOVE);
    }
}
