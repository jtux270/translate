package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.network.macpoolmanager.MacPoolPerDcSingleton;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.MacPoolParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.MacPool;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;

public class AddMacPoolCommand extends MacPoolCommandBase<MacPoolParameters> {

    public AddMacPoolCommand(MacPoolParameters parameters) {
        super(parameters);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.MAC_POOL_ADD_SUCCESS;
        } else {
            return AuditLogType.MAC_POOL_ADD_FAILED;
        }
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(EngineMessage.VAR__ACTION__CREATE);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(Guid.SYSTEM,
                VdcObjectType.System,
                ActionGroup.CREATE_MAC_POOL));
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        final MacPoolValidator validator = new MacPoolValidator(getMacPoolEntity());
        return validate(validator.defaultPoolFlagIsNotSet()) && validate(validator.hasUniqueName());
    }

    private MacPool getMacPoolEntity() {
        return getParameters().getMacPool();
    }

    @Override
    protected void executeCommand() {
        getMacPoolEntity().setId(Guid.newGuid());
        getMacPoolDao().save(getMacPoolEntity());
        addPermission(getCurrentUser().getId(), getMacPoolEntity().getId());

        MacPoolPerDcSingleton.getInstance().createPool(getMacPoolEntity());
        setSucceeded(true);
        getReturnValue().setActionReturnValue(getMacPoolId());
    }

    //used by introspector
    public Guid getMacPoolId() {
        return getMacPoolEntity().getId();
    }

    //used by introspector
    public String getMacPoolName() {
        return getMacPoolEntity().getName();
    }

    @Override
    public void rollback() {
        super.rollback();
        MacPoolPerDcSingleton.getInstance().removePool(getMacPoolId());
    }

    private void addPermission(Guid userId, Guid macPoolId) {
        MultiLevelAdministrationHandler.addPermission(userId, macPoolId, PredefinedRoles.MAC_POOL_ADMIN, VdcObjectType.MacPool);
    }
}
