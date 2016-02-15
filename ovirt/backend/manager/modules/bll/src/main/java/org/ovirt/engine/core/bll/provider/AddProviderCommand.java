package org.ovirt.engine.core.bll.provider;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ProviderParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;

public class AddProviderCommand<P extends ProviderParameters> extends CommandBase<P> {

    public AddProviderCommand(Guid commandId) {
        super(commandId);
    }

    public AddProviderCommand(P parameters) {
        super(parameters);
    }

    private Provider<?> getProvider() {
        return getParameters().getProvider();
    }

    public String getProviderName() {
        return getProvider().getName();
    }

    @Override
    protected boolean canDoAction() {
        ProviderValidator validator = new ProviderValidator(getProvider());
        return validate(validator.nameAvailable());
    }

    @Override
    protected void executeCommand() {
        getProvider().setId(Guid.newGuid());
        getDbFacade().getProviderDao().save(getProvider());

        ProviderProxy providerProxy = ProviderProxyFactory.getInstance().create(getProvider());
        if (providerProxy != null) {
            providerProxy.onAddition();
        }

        getReturnValue().setActionReturnValue(getProvider().getId());
        setSucceeded(true);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(Guid.SYSTEM,
                VdcObjectType.System,
                ActionGroup.CREATE_STORAGE_POOL));
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__PROVIDER);
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.PROVIDER_ADDED : AuditLogType.PROVIDER_ADDITION_FAILED;
    }
}
