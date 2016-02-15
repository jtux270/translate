package org.ovirt.engine.ui.webadmin.section.main.view.tab.cluster;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.scheduling.AffinityGroup;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.scheduling.affinity_groups.list.ClusterAffinityGroupListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.cluster.SubTabClusterAffinityGroupPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.tab.AbstractSubTabAffinityGroupsView;

import com.google.gwt.core.client.GWT;

public class SubTabClusterAffinityGroupView extends AbstractSubTabAffinityGroupsView<VDSGroup, ClusterListModel, ClusterAffinityGroupListModel>
        implements SubTabClusterAffinityGroupPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabClusterAffinityGroupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabClusterAffinityGroupView(SearchableDetailModelProvider<AffinityGroup, ClusterListModel, ClusterAffinityGroupListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider, constants);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }
}
