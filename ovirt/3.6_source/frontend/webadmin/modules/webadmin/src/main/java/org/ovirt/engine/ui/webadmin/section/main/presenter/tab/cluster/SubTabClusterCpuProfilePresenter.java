package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.cluster;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.ui.common.place.PlaceRequestFactory;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.profiles.CpuProfileListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.ClusterSelectionChangeEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;

public class SubTabClusterCpuProfilePresenter extends
        AbstractSubTabPresenter<VDSGroup, ClusterListModel<Void>, CpuProfileListModel, SubTabClusterCpuProfilePresenter.ViewDef, SubTabClusterCpuProfilePresenter.ProxyDef> {

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.clusterCpuProfileSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabClusterCpuProfilePresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VDSGroup> {

        void addModelListeners();
    }

    @TabInfo(container = ClusterSubTabPanelPresenter.class)
    static TabData getTabData(
            SearchableDetailModelProvider<CpuProfile, ClusterListModel<Void>, CpuProfileListModel> modelProvider) {
        return new ModelBoundTabData(constants.cpuProfilesSubTabLabel(), 7,
                modelProvider);
    }

    @Inject
    public SubTabClusterCpuProfilePresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager,
            SearchableDetailModelProvider<CpuProfile, ClusterListModel<Void>, CpuProfileListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider, ClusterSubTabPanelPresenter.TYPE_SetTabContent);
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return PlaceRequestFactory.get(WebAdminApplicationPlaces.clusterMainTabPlace);
    }

    @ProxyEvent
    public void onClusterSelectionChange(ClusterSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
    }

    @Override
    public void initializeHandlers() {
        super.initializeHandlers();

        getView().addModelListeners();
    }
}
