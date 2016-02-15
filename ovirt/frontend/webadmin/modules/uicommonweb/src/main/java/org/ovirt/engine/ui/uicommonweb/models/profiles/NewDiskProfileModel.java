package org.ovirt.engine.ui.uicommonweb.models.profiles;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class NewDiskProfileModel extends DiskProfileBaseModel {

    public NewDiskProfileModel(EntityModel sourceModel,
            Guid dcId) {
        super(sourceModel, dcId, null, VdcActionType.AddDiskProfile);
        setTitle(ConstantsManager.getInstance().getConstants().diskProfileTitle());
        setHelpTag(HelpTag.new_disk_profile);
        setHashName("new_disk_profile"); //$NON-NLS-1$
    }

    public NewDiskProfileModel() {
        this(null, null);
    }

}
