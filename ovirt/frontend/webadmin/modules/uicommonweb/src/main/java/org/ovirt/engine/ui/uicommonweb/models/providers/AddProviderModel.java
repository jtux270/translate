package org.ovirt.engine.ui.uicommonweb.models.providers;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class AddProviderModel extends ProviderModel {

    public AddProviderModel(ProviderListModel sourceListModel) {
        super(sourceListModel, VdcActionType.AddProvider, new Provider());
        setTitle(ConstantsManager.getInstance().getConstants().addProviderTitle());
        setHelpTag(HelpTag.add_provider);
        setHashName("add_provider"); //$NON-NLS-1$

        getType().setSelectedItem(Linq.firstOrDefault((Iterable<ProviderType>) getType().getItems()));

        getNeutronAgentModel().init(provider); // this is okay because AdditionalProperties == null at this point
    }

}
