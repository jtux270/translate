package org.ovirt.engine.ui.uicommonweb.models.profiles;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.profiles.DiskProfile;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class EditDiskProfileModel extends DiskProfileBaseModel {

    public EditDiskProfileModel(EntityModel sourceModel,
            DiskProfile profile,
            Guid dataCenterId) {
        super(sourceModel,
                dataCenterId,
                profile.getQosId(),
                VdcActionType.UpdateDiskProfile);
        setTitle(ConstantsManager.getInstance().getConstants().diskProfileTitle());
        setHelpTag(HelpTag.edit_disk_profile);
        setHashName("edit_disk_profile"); //$NON-NLS-1$

        setProfile(profile);

        getName().setEntity(profile.getName());
        getDescription().setEntity(profile.getDescription());
    }

    public EditDiskProfileModel(DiskProfile profile) {
        this(null, profile, null);
    }

}
