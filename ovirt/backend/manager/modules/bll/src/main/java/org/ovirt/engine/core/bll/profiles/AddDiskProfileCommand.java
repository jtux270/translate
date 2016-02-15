package org.ovirt.engine.core.bll.profiles;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.MultiLevelAdministrationHandler;
import org.ovirt.engine.core.bll.PredefinedRoles;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.DiskProfileParameters;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.profiles.DiskProfile;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.dao.profiles.ProfilesDao;

public class AddDiskProfileCommand extends AddProfileCommandBase<DiskProfileParameters, DiskProfile, DiskProfileValidator> {

    public AddDiskProfileCommand(DiskProfileParameters parameters) {
        super(parameters);
    }

    @Override
    protected DiskProfileValidator getProfileValidator() {
        return new DiskProfileValidator(getProfile());
    }

    @Override
    protected ProfilesDao<DiskProfile> getProfileDao() {
        return getDiskProfileDao();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(getParameters().getProfile() != null ? getParameters().getProfile()
                .getStorageDomainId()
                : null,
                VdcObjectType.Storage, getActionType().getActionGroup()));
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__DISK_PROFILE);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_ADDED_DISK_PROFILE : AuditLogType.USER_FAILED_TO_ADD_DISK_PROFILE;
    }

    @Override
    protected void executeCommand() {
        super.executeCommand();
        addPermission();
    }

    private void addPermission() {
        MultiLevelAdministrationHandler.addPermission(new Permissions(MultiLevelAdministrationHandler.EVERYONE_OBJECT_ID,
                PredefinedRoles.DISK_PROFILE_USER.getId(),
                getProfileId(),
                VdcObjectType.DiskProfile));
    }
}
