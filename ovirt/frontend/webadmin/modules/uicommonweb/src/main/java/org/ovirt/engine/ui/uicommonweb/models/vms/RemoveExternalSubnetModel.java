package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.action.ExternalSubnetParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.network.ExternalSubnet;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

public class RemoveExternalSubnetModel extends ConfirmationModel {

    private final List<ExternalSubnet> subnets;
    private final SearchableListModel<?> sourceListModel;

    public RemoveExternalSubnetModel(SearchableListModel<?> sourceListModel, List<ExternalSubnet> subnets) {
        setHelpTag(HelpTag.remove_external_subnet);
        setHashName("remove_external_subnet"); //$NON-NLS-1$

        this.sourceListModel = sourceListModel;
        this.subnets = subnets;

        ArrayList<String> items = new ArrayList<String>();
        for (ExternalSubnet subnet : subnets) {
            items.add(subnet.getName());
        }
        setItems(items);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        getCommands().add(tempVar2);
    }

    private void onRemove() {
        ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (ExternalSubnet subnet : getSubnets()) {
            VdcActionParametersBase parameters = new ExternalSubnetParameters(subnet);
            list.add(parameters);

        }

        startProgress(null);

        Frontend.getInstance().runMultipleAction(VdcActionType.RemoveSubnetFromProvider,
                list,
                false,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                        stopProgress();
                        sourceListModel.getSearchCommand().execute();
                        cancel();
                    }
                },
                null);
    }

    private List<ExternalSubnet> getSubnets() {
        return subnets;
    }

    private void cancel() {
        sourceListModel.setWindow(null);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if ("Cancel".equals(command.getName())) { //$NON-NLS-1$
            cancel();
        } else if ("OnRemove".equals(command.getName())) { //$NON-NLS-1$
            onRemove();
        }
    }

}
