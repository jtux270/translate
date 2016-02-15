package org.ovirt.engine.ui.webadmin.uicommon.model;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTabModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.configure.scheduling.ClusterPolicyClusterListModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ClusterPolicyClusterModelProvider extends SearchableTabModelProvider<VDSGroup, ClusterPolicyClusterListModel> {

    @Inject
    public ClusterPolicyClusterModelProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider) {
        super(eventBus, defaultConfirmPopupProvider);
    }

    @Override
    public ClusterPolicyClusterListModel getModel() {
        return (ClusterPolicyClusterListModel) getCommonModel().getClusterPolicyListModel().getDetailModels().get(0);
    }

}
