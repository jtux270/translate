package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.cluster;

import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.ui.common.place.PlaceRequestFactory;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.ClusterSelectionChangeEvent;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SubTabClusterPermissionPresenter extends AbstractSubTabPresenter<VDSGroup, ClusterListModel, PermissionListModel, SubTabClusterPermissionPresenter.ViewDef, SubTabClusterPermissionPresenter.ProxyDef> {

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.clusterPermissionSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabClusterPermissionPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VDSGroup> {
    }

    @TabInfo(container = ClusterSubTabPanelPresenter.class)
    static TabData getTabData(ApplicationConstants applicationConstants,
            SearchableDetailModelProvider<Permissions, ClusterListModel, PermissionListModel> modelProvider) {
        return new ModelBoundTabData(applicationConstants.clusterPermissionSubTabLabel(), 8, modelProvider);
    }

    @Inject
    public SubTabClusterPermissionPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager,
            SearchableDetailModelProvider<Permissions, ClusterListModel, PermissionListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider,
                ClusterSubTabPanelPresenter.TYPE_SetTabContent);
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return PlaceRequestFactory.get(WebAdminApplicationPlaces.clusterMainTabPlace);
    }

    @ProxyEvent
    public void onClusterSelectionChange(ClusterSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
    }

}
