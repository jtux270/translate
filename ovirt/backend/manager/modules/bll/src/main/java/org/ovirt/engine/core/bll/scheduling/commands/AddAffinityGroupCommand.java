package org.ovirt.engine.core.bll.scheduling.commands;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.scheduling.AffinityGroup;
import org.ovirt.engine.core.common.scheduling.parameters.AffinityGroupCRUDParameters;
import org.ovirt.engine.core.compat.Guid;

public class AddAffinityGroupCommand extends AffinityGroupCRUDCommand {

    public AddAffinityGroupCommand(AffinityGroupCRUDParameters parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        if (getAffinityGroupDao().getByName(getParameters().getAffinityGroup().getName()) != null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_AFFINITY_GROUP_NAME_EXISTS);
        }
        return validateParameters();
    }

    @Override
    protected AffinityGroup getAffinityGroup() {
        return getParameters().getAffinityGroup();
    }

    @Override
    protected void executeCommand() {
        getAffinityGroup().setId(Guid.newGuid());
        getAffinityGroupDao().save(getAffinityGroup());
        getReturnValue().setActionReturnValue(getAffinityGroup().getId());
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_ADDED_AFFINITY_GROUP : AuditLogType.USER_FAILED_TO_ADD_AFFINITY_GROUP;
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
    }
}
