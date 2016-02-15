package org.ovirt.engine.core.bll.provider.storage;

import java.util.Date;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.LibvirtSecretParameters;
import org.ovirt.engine.core.common.errors.EngineMessage;

public class AddLibvirtSecretCommand extends LibvirtSecretCommandBase {

    public AddLibvirtSecretCommand(LibvirtSecretParameters parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        LibvirtSecretValidator libvirtSecretValidator =
                new LibvirtSecretValidator(getParameters().getLibvirtSecret());
        return validate(libvirtSecretValidator.uuidNotEmpty())
                && validate(libvirtSecretValidator.uuidNotExist())
                && validate(libvirtSecretValidator.valueNotEmpty())
                && validate(libvirtSecretValidator.providerExist());
    }

    @Override
    protected void executeCommand() {
        super.executeCommand();
        getParameters().getLibvirtSecret().setCreationDate(new Date());
        getLibvirtSecretDao().save(getParameters().getLibvirtSecret());
        registerLibvirtSecret();
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_ADDED_LIBVIRT_SECRET : AuditLogType.USER_FAILED_TO_ADD_LIBVIRT_SECRET;
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(EngineMessage.VAR__ACTION__ADD);
    }
}
