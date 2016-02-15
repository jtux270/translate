package org.ovirt.engine.ui.uicommonweb.models.hosts;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;

public class SaveNetworkConfigAction {

    private final SearchableListModel listModel;
    private final Model windowModel;
    private final VDS host;

    public SaveNetworkConfigAction(SearchableListModel listModel, Model windowModel, VDS host)
    {
        this.listModel = listModel;
        this.windowModel = windowModel;
        this.host = host;
    }

    public void execute() {
        Frontend.getInstance().runAction(VdcActionType.CommitNetworkChanges, new VdsActionParameters(host.getId()),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {

                        VdcReturnValueBase returnValueBase = result.getReturnValue();
                        if (returnValueBase != null && returnValueBase.getSucceeded())
                        {
                            if (windowModel != null)
                            {
                                windowModel.stopProgress();
                                listModel.setWindow(null);
                                listModel.setConfirmWindow(null);
                                listModel.search();
                            }
                        }

                    }
                }, null);
    }

}
