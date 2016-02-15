package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ForceSelectSPMParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.BusinessEntitiesDefinitions;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsSpmStatus;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.irsbroker.SpmStopOnIrsVDSCommandParameters;

@NonTransactiveCommandAttribute
public class ForceSelectSPMCommand<T extends ForceSelectSPMParameters> extends CommandBase<T> {

    private StoragePool storagePoolForVds;

    public ForceSelectSPMCommand(T parameters) {
        super(parameters);
        setVdsId(getParameters().getPreferredSPMId());
    }

    @Override
    protected boolean canDoAction() {
        if (getVds() == null) {
            return failCanDoAction(EngineMessage.VDS_NOT_EXIST);
        }

        if (getVds().getStatus() != VDSStatus.Up) {
            return failCanDoAction(EngineMessage.CANNOT_FORCE_SELECT_SPM_VDS_NOT_UP);
        }

        if (getStoragePoolForVds() == null) {
            return failCanDoAction(EngineMessage.CANNOT_FORCE_SELECT_SPM_VDS_NOT_IN_POOL);
        }

        if (getVds().getSpmStatus() != VdsSpmStatus.None) {
            return failCanDoAction(EngineMessage.CANNOT_FORCE_SELECT_SPM_VDS_ALREADY_SPM);
        }

        if (getVds().getVdsSpmPriority() == BusinessEntitiesDefinitions.HOST_MIN_SPM_PRIORITY) {
            return failCanDoAction(EngineMessage.CANNOT_FORCE_SELECT_SPM_VDS_MARKED_AS_NEVER_SPM);
        }

        if (!validate(new StoragePoolValidator(getStoragePoolForVds()).isUp())) {
            return false;
        }

        if (isAsyncTasksRunningOnPool(getStoragePoolForVds().getId())) {
            return failCanDoAction(EngineMessage.CANNOT_FORCE_SELECT_SPM_STORAGE_POOL_HAS_RUNNING_TASKS);
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        SpmStopOnIrsVDSCommandParameters params =
                new SpmStopOnIrsVDSCommandParameters(getStoragePoolForVds().getId(),
                        getParameters().getPreferredSPMId());

        if (runVdsCommand(VDSCommandType.SpmStopOnIrs, params).getSucceeded()) {
            auditLogDirector.log(this, AuditLogType.USER_FORCE_SELECTED_SPM);
        } else {
            auditLogDirector.log(this, AuditLogType.USER_FORCE_SELECTED_SPM_STOP_FAILED);
        }

        setSucceeded(true);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__FORCE_SELECT);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__SPM);
        addCanDoActionMessageVariable("VdsName", getVds().getName());
    }

    private boolean isAsyncTasksRunningOnPool(Guid storagePoolId) {
        List<Guid> tasks = getAsyncTaskDao().getAsyncTaskIdsByStoragePoolId(storagePoolId);
        return !tasks.isEmpty();
    }

    private StoragePool getStoragePoolForVds() {
        if (storagePoolForVds == null) {
            storagePoolForVds = getStoragePoolDao().getForVds(getVds().getId());
        }
        return storagePoolForVds;
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = super.getJobMessageProperties();
            if (getVds() != null) {
                jobProperties.put(VdcObjectType.StoragePool.name().toLowerCase(), getVds().getStoragePoolName());
            }
        }
        return jobProperties;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissions = new ArrayList<>();
        permissions.add(new PermissionSubject(getParameters().getPreferredSPMId(),
                VdcObjectType.VDS,
                ActionGroup.MANIPULATE_HOST));
        return permissions;
    }
}
