package org.ovirt.engine.ui.uicommonweb.models.profiles;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class EditCpuProfileModel extends CpuProfileBaseModel {

    public EditCpuProfileModel(EntityModel sourceModel,
            CpuProfile profile,
            Guid dataCenterId) {
        super(sourceModel,
                dataCenterId,
                profile.getQosId(),
                VdcActionType.UpdateCpuProfile);
        setTitle(ConstantsManager.getInstance().getConstants().cpuProfileTitle());
        setHelpTag(HelpTag.edit_cpu_profile);
        setHashName("edit_cpu_profile"); //$NON-NLS-1$

        setProfile(profile);

        getName().setEntity(profile.getName());
        getDescription().setEntity(profile.getDescription());
    }

    public EditCpuProfileModel(CpuProfile profile) {
        this(null, profile, null);
    }

}
