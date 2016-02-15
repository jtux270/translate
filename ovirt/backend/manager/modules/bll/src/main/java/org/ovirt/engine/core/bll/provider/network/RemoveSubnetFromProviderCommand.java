package org.ovirt.engine.core.bll.provider.network;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.provider.ProviderProxyFactory;
import org.ovirt.engine.core.bll.provider.ProviderValidator;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ExternalSubnetParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.network.ExternalSubnet;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.validation.group.RemoveEntity;
import org.ovirt.engine.core.compat.Guid;

@NonTransactiveCommandAttribute
public class RemoveSubnetFromProviderCommand<T extends ExternalSubnetParameters> extends CommandBase<T> {

    private Provider<?> provider;

    public RemoveSubnetFromProviderCommand(T parameters) {
        super(parameters);
    }


    private Provider<?> getProvider() {
        if (provider == null) {
            provider = getDbFacade().getProviderDao().get(getSubnet().getExternalNetwork().getProviderId());
        }

        return provider;
    }

    public String getProviderName() {
        return getProvider().getName();
    }

    private ExternalSubnet getSubnet() {
        return getParameters().getSubnet();
    }

    public String getSubnetName() {
        return getSubnet().getName();
    }

    @Override
    protected boolean canDoAction() {
        ProviderValidator validator = new ProviderValidator(getProvider());

        return validate(validator.providerIsSet());
    }

    @Override
    protected void executeCommand() {
        NetworkProviderProxy proxy = ProviderProxyFactory.getInstance().create(getProvider());
        proxy.removeSubnet(getSubnet().getId());
        setSucceeded(true);
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(RemoveEntity.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REMOVE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__SUBNET);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.SUBNET_REMOVED : AuditLogType.SUBNET_REMOVAL_FAILED;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(Guid.SYSTEM,
                VdcObjectType.System,
                ActionGroup.CREATE_STORAGE_POOL));
    }
}
