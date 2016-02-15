package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.disk;

import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.ui.common.place.PlaceRequestFactory;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.DiskSelectionChangeEvent;
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

public class SubTabDiskGeneralPresenter extends AbstractSubTabPresenter<Disk, DiskListModel, DiskGeneralModel, SubTabDiskGeneralPresenter.ViewDef, SubTabDiskGeneralPresenter.ProxyDef> {

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.diskGeneralSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabDiskGeneralPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<Disk> {
    }

    @TabInfo(container = DiskSubTabPanelPresenter.class)
    static TabData getTabData(
            DetailModelProvider<DiskListModel, DiskGeneralModel> modelProvider) {
        return new ModelBoundTabData(constants.diskGeneralSubTabLabel(), 0, modelProvider);
    }

    @Inject
    public SubTabDiskGeneralPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager,
            DetailModelProvider<DiskListModel, DiskGeneralModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider,
                DiskSubTabPanelPresenter.TYPE_SetTabContent);
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return PlaceRequestFactory.get(WebAdminApplicationPlaces.diskMainTabPlace);
    }

    @ProxyEvent
    public void onDiskSelectionChange(DiskSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
    }

}
