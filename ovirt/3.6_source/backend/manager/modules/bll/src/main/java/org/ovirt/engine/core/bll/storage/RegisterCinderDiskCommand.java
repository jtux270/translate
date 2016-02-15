package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.Arrays;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.bll.validator.storage.CinderDisksValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.GetCinderEntityByStorageDomainIdParameters;
import org.ovirt.engine.core.common.action.RegisterCinderDiskParameters;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterCinderDiskCommand<T extends RegisterCinderDiskParameters> extends AddCinderDiskCommand<T> {

    private static final Logger log = LoggerFactory.getLogger(RegisterCinderDiskCommand.class);

    public RegisterCinderDiskCommand(T parameters) {
        this(parameters, null);
    }

    public RegisterCinderDiskCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    public boolean canDoAction() {
        CinderDisk cinderDisk = getCinderDisk();
        cinderDisk.setStorageIds(new ArrayList<>(Arrays.asList(getParameters().getStorageDomainId())));
        CinderDisksValidator cinderDiskValidator = getCinderDisksValidator(cinderDisk);
        return validate(cinderDiskValidator.validateCinderDisksAlreadyRegistered());
    }

    @Override
    public void executeCommand() {
        VdcQueryReturnValue returnValue = runInternalQuery(VdcQueryType.GetUnregisteredCinderDiskByIdAndStorageDomainId,
                new GetCinderEntityByStorageDomainIdParameters(
                        getCinderDisk().getId(), getParameters().getStorageDomainId()));
        CinderDisk cinderDisk = returnValue.getReturnValue();
        if (cinderDisk != null) {
            addCinderDiskToDB(cinderDisk);
            getReturnValue().setActionReturnValue(cinderDisk.getId());
            setSucceeded(true);
        }
    }

    @Override
    public CommandCallback getCallback() {
        return null;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        addAuditLogCustomValues();
        return getSucceeded() ? AuditLogType.USER_REGISTER_DISK_FINISHED_SUCCESS : AuditLogType.USER_REGISTER_DISK_FINISHED_FAILURE;
    }

    private void addAuditLogCustomValues() {
        this.addCustomValue("DiskAlias", getCinderDisk().getDiskAlias());
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__REGISTER);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM_DISK);
    }

    protected CinderDisk getCinderDisk() {
        return getParameters().getCinderDisk();
    }

    protected CinderDisksValidator getCinderDisksValidator(CinderDisk disk) {
        return new CinderDisksValidator(disk);
    }
}
