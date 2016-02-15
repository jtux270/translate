package org.ovirt.engine.core.bll.storage;

import java.util.EnumSet;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.SetStoragePoolStatusParameters;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.vdsbroker.storage.StoragePoolDomainHelper;

public class SetStoragePoolStatusCommand<T extends SetStoragePoolStatusParameters> extends
        StorageHandlingCommandBase<T> {
    public SetStoragePoolStatusCommand(T parameters) {
        this(parameters, null);
    }

    public SetStoragePoolStatusCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected void executeCommand() {
        getStoragePool().setStatus(getParameters().getStatus());
        setVdsIdRef(getStoragePool().getspm_vds_id());
        DbFacade.getInstance().getStoragePoolDao().updateStatus(getStoragePool().getId(), getStoragePool().getStatus());
        if (getParameters().getStatus() == StoragePoolStatus.NonResponsive
                || getParameters().getStatus() == StoragePoolStatus.NotOperational) {
            StoragePoolDomainHelper.updateApplicablePoolDomainsStatuses(getStoragePool().getId(),
                    EnumSet.of(StorageDomainStatus.Active),
                    StorageDomainStatus.Unknown,
                    null);
        }
        StoragePoolStatusHandler.poolStatusChanged(getStoragePool().getId(), getStoragePool().getStatus());
        setSucceeded(true);
    }

    public String getError() {
        return Backend.getInstance().getVdsErrorsTranslator()
                .TranslateErrorTextSingle(getParameters().getError().toString());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            if (StringUtils.isEmpty(getVdsName())) {
                setVdsName("Unavailable");
            }
            return getParameters().getAuditLogType();
        } else {
            return AuditLogType.SYSTEM_FAILED_CHANGE_STORAGE_POOL_STATUS;
        }
    }

    @Override
    protected boolean canDoAction() {
        return checkStoragePool();
    }
}
