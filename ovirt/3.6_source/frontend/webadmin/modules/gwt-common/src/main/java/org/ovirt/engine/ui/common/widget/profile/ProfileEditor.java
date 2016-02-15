package org.ovirt.engine.ui.common.widget.profile;

import org.ovirt.engine.core.common.businessentities.network.VnicProfileView;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.widget.editor.ListModelTypeAheadListBoxEditor;

public class ProfileEditor extends ListModelTypeAheadListBoxEditor<VnicProfileView> {

    private final static CommonApplicationTemplates templates = AssetProvider.getTemplates();
    private final static CommonApplicationMessages messages = AssetProvider.getMessages();

    public ProfileEditor() {
        super(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<VnicProfileView>() {

                    @Override
                    public String getReplacementStringNullSafe(VnicProfileView profile) {
                        return (profile == VnicProfileView.EMPTY) ? messages.emptyProfile().asString()
                                : messages.profileAndNetworkSelected(profile.getName(), profile.getNetworkName())
                                        .asString();
                    }

                    @Override
                    public String getDisplayStringNullSafe(VnicProfileView profile) {
                        if (profile == VnicProfileView.EMPTY) {
                            return templates.typeAheadNameDescription(messages.emptyProfile().asString(),
                                    messages.emptyProfileDescription().asString()).asString();
                        }

                        String profileDescription = profile.getDescription();
                        String profileAndNetwork =
                                messages.profileAndNetwork(profile.getName(), profile.getNetworkName()).asString();

                        return templates.typeAheadNameDescription(profileAndNetwork,
                                profileDescription != null ? profileDescription : "").asString(); //$NON-NLS-1$
                    }

                }, false);
    }

}
