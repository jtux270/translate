package org.ovirt.engine.ui.uicommonweb.models.profiles;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.profiles.ProfileBase;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

public abstract class RemoveProfileModel<P extends ProfileBase> extends ConfirmationModel {

    private final List<P> profiles;
    private final ListModel sourceListModel;

    public RemoveProfileModel(ListModel sourceListModel, List<P> profiles) {
        this.sourceListModel = sourceListModel;
        this.profiles = profiles;

        ArrayList<String> items = new ArrayList<String>();
        for (P profile : profiles) {
            items.add(profile.getName());
        }
        setItems(items);

        getCommands().add(new UICommand("OnRemove", this).setTitle(ConstantsManager.getInstance().getConstants().ok()) //$NON-NLS-1$
                .setIsDefault(true));
        getCommands().add(new UICommand("Cancel", this).setTitle(ConstantsManager.getInstance().getConstants().cancel()) //$NON-NLS-1$
                .setIsCancel(true));
    }

    protected abstract VdcActionType getRemoveActionType();

    protected abstract VdcActionParametersBase getRemoveProfileParams(P profile);

    private void onRemove() {
        if (getProgress() != null) {
            return;
        }

        ArrayList<VdcActionParametersBase> vdcActionParametersBaseList = new ArrayList<VdcActionParametersBase>();
        for (P profile : getProfiles()) {
            VdcActionParametersBase parameters = getRemoveProfileParams(profile);
            vdcActionParametersBaseList.add(parameters);

        }

        startProgress(null);

        Frontend.getInstance().runMultipleAction(getRemoveActionType(), vdcActionParametersBaseList,
                new IFrontendMultipleActionAsyncCallback() {

                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                        stopProgress();
                        cancel();

                    }
                }, null);
    }

    public List<P> getProfiles() {
        return profiles;
    }

    private void cancel() {
        sourceListModel.setWindow(null);
        sourceListModel.setConfirmWindow(null);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if ("Cancel".equals(command.getName())) {//$NON-NLS-1$
            cancel();
        }
        else if ("OnRemove".equals(command.getName())) {//$NON-NLS-1$
            onRemove();
        }
    }

}
