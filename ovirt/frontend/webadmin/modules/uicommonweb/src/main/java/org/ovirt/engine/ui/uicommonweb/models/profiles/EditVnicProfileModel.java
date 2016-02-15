package org.ovirt.engine.ui.uicommonweb.models.profiles;

import java.util.Collection;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.network.VnicProfile;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class EditVnicProfileModel extends VnicProfileModel {

    public EditVnicProfileModel(EntityModel sourceModel,
            Version dcCompatibilityVersion,
            VnicProfile profile,
            Guid dcId,
            boolean customPropertiesVisible) {
        super(sourceModel, dcCompatibilityVersion, customPropertiesVisible, dcId, profile.getNetworkQosId());
        setTitle(ConstantsManager.getInstance().getConstants().vnicProfileTitle());
        setHelpTag(HelpTag.edit_vnic_profile);
        setHashName("edit_vnic_profile"); //$NON-NLS-1$

        setProfile(profile);

        getName().setEntity(profile.getName());
        getDescription().setEntity(profile.getDescription());
        getPortMirroring().setEntity(getProfile().isPortMirroring());
        getPublicUse().setIsAvailable(false);

        updatePortMirroringChangability();
    }

    public EditVnicProfileModel(EntityModel sourceModel, Version dcCompatibilityVersion, VnicProfile profile, Guid dcId) {
        this(sourceModel, dcCompatibilityVersion, profile, dcId, true);
    }

    public EditVnicProfileModel(VnicProfile profile) {
        this(null, null, profile, null, false);
    }

    @Override
    protected void initCustomProperties() {
        getCustomPropertySheet().deserialize(KeyValueModel
                .convertProperties(getProfile().getCustomProperties()));
    }

    @Override
    protected VdcActionType getVdcActionType() {
        return VdcActionType.UpdateVnicProfile;
    }

    private void updatePortMirroringChangability() {
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object ReturnValue)
            {
                Collection<VM> vms = (Collection<VM>) ((VdcQueryReturnValue) ReturnValue).getReturnValue();
                if (vms != null && !vms.isEmpty()) {
                    getPortMirroring().setChangeProhibitionReason(ConstantsManager.getInstance()
                            .getConstants()
                            .portMirroringNotChangedIfUsedByVms());
                    getPortMirroring().setIsChangable(false);
                }
                stopProgress();
            }
        };

        IdQueryParameters params =
                new IdQueryParameters(getProfile().getId());
        startProgress(null);
        Frontend.getInstance().runQuery(VdcQueryType.GetVmsByVnicProfileId,
                params,
                asyncQuery);
    }
}
