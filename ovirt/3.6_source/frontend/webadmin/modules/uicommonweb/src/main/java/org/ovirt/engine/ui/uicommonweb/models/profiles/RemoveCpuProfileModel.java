package org.ovirt.engine.ui.uicommonweb.models.profiles;

import java.util.List;

import org.ovirt.engine.core.common.action.CpuProfileParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class RemoveCpuProfileModel extends RemoveProfileModel<CpuProfile> {

    public RemoveCpuProfileModel(ListModel sourceListModel, List<CpuProfile> profiles) {
        super(sourceListModel, profiles);
        setHelpTag(HelpTag.remove_cpu_profile);
        setTitle(ConstantsManager.getInstance().getConstants().removeCpuProfileTitle());
        setHashName("remove_cpu_prfoile"); //$NON-NLS-1$
    }

    @Override
    protected VdcActionType getRemoveActionType() {
        return VdcActionType.RemoveCpuProfile;
    }

    @Override
    protected VdcActionParametersBase getRemoveProfileParams(CpuProfile profile) {
        return new CpuProfileParameters(profile, profile.getId());
    }

}
