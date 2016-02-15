package org.ovirt.engine.core.bll;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.validator.LocalizedVmStatus;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.ChangeDiskCommandParameters;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.common.vdscommands.ChangeDiskVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;

public class ChangeDiskCommand<T extends ChangeDiskCommandParameters> extends VmOperationCommandBase<T> {
    private String cdImagePath;

    public ChangeDiskCommand(T parameters) {
        super(parameters);
        cdImagePath = getParameters().getCdImagePath();
    }

    public String getDiskName() {
        return new File(cdImagePath).getName();
    }

    @Override
    protected void setActionMessageParameters() {
        // An empty 'cdImagePath' means eject CD
        if (!StringUtils.isEmpty(cdImagePath)) {
            addCanDoActionMessage(EngineMessage.VAR__ACTION__CHANGE_CD);
        } else {
            addCanDoActionMessage(EngineMessage.VAR__ACTION__EJECT_CD);
        }
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM);
    }

    @Override
    protected boolean canDoAction() {
        if (shouldSkipCommandExecutionCached()) {
            return true;
        }

        if (getVm() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        if (!getVm().isRunningOrPaused()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL, LocalizedVmStatus.from(getVm().getStatus()));
        }

        if ((IsoDomainListSyncronizer.getInstance().findActiveISODomain(getVm().getStoragePoolId()) == null)
                && !StringUtils.isEmpty(cdImagePath)) {
            return failCanDoAction(EngineMessage.VM_CANNOT_WITHOUT_ACTIVE_STORAGE_DOMAIN_ISO);
        }

        if (StringUtils.isNotEmpty(cdImagePath) && !StringUtils.endsWithIgnoreCase(cdImagePath, ValidationUtils.ISO_SUFFIX)) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_INVALID_CDROM_DISK_FORMAT);
        }

        return true;
    }

    @Override
    protected boolean shouldSkipCommandExecution() {
        if (getVm() == null) {
            return false;
        }

        return StringUtils.equals(getVm().getCurrentCd(), getParameters().getCdImagePath());
    }

    @Override
    protected void perform() {
        cdImagePath = ImagesHandler.cdPathWindowsToLinux(getParameters().getCdImagePath(), getVm().getStoragePoolId(), getVm().getRunOnVds());
        setActionReturnValue(runVdsCommand(VDSCommandType.ChangeDisk,
                        new ChangeDiskVDSCommandParameters(getVdsId(), getVm().getId(), cdImagePath))
                .getReturnValue());
        VmHandler.updateCurrentCd(getVdsId(), getVm(), getParameters().getCdImagePath());
        setSucceeded(true);

    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (shouldSkipCommandExecutionCached()) {
            return "".equals(cdImagePath) ? AuditLogType.VM_DISK_ALREADY_EJECTED
                    : AuditLogType.VM_DISK_ALREADY_CHANGED;
        }

        if (!getSucceeded()) {
            return AuditLogType.USER_FAILED_CHANGE_DISK_VM;
        }

        return "".equals(cdImagePath) ? AuditLogType.USER_EJECT_VM_DISK
                : AuditLogType.USER_CHANGE_DISK_VM;
    }
}
