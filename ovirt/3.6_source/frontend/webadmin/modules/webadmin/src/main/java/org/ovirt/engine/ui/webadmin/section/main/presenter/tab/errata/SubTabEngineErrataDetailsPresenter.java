package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.errata;

import org.ovirt.engine.core.common.businessentities.Erratum;
import org.ovirt.engine.ui.common.place.PlaceRequestFactory;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.DetailTabModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.EngineErrataListModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.ErrataSelectionChangeEvent;

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

/**
 * Presenter for the sub tab that shows details for an engine Erratum selected in the main tab.
 * Note: this tab is only show when 'Errata' is selected in the System Tree and an Erratum is selected
 * in the main tab.
 */
public class SubTabEngineErrataDetailsPresenter extends AbstractSubTabPresenter<Erratum,
    EngineErrataListModel, EntityModel<Erratum>, SubTabEngineErrataDetailsPresenter.ViewDef,
    SubTabEngineErrataDetailsPresenter.ProxyDef> {

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.errataDetailsSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabEngineErrataDetailsPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<Erratum> {
    }

    @TabInfo(container = ErrataSubTabPanelPresenter.class)
    static TabData getTabData(DetailTabModelProvider<EngineErrataListModel, EntityModel<Erratum>> modelProvider) {
        return new ModelBoundTabData(constants.errataDetailsSubTabLabel(), 1, modelProvider);
    }

    @Inject
    public SubTabEngineErrataDetailsPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, DetailTabModelProvider<EngineErrataListModel, EntityModel<Erratum>> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider, ErrataSubTabPanelPresenter.TYPE_SetTabContent);
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return PlaceRequestFactory.get(WebAdminApplicationPlaces.errataMainTabPlace);
    }

    @ProxyEvent
    public void onErrataSelectionChange(ErrataSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
    }

}
