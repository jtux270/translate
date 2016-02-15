package org.ovirt.engine.ui.userportal.section.main.presenter.tab.extended.vm;

import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalItemModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGeneralModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.uicommon.model.vm.VmGeneralModelProvider;
import org.ovirt.engine.ui.uicommonweb.place.UserPortalApplicationPlaces;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SubTabExtendedVmGeneralPresenter
        extends AbstractSubTabExtendedVmPresenter<VmGeneralModel, SubTabExtendedVmGeneralPresenter.ViewDef, SubTabExtendedVmGeneralPresenter.ProxyDef> {

    @ProxyCodeSplit
    @NameToken(UserPortalApplicationPlaces.extendedVirtualMachineGeneralSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabExtendedVmGeneralPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<UserPortalItemModel> {

        void update();

    }

    @TabInfo(container = ExtendedVmSubTabPanelPresenter.class)
    static TabData getTabData(ApplicationConstants applicationConstants,
            VmGeneralModelProvider modelProvider) {
        return new ModelBoundTabData(applicationConstants.extendedVirtualMachineGeneralSubTabLabel(), 1, modelProvider);
    }

    @Inject
    public SubTabExtendedVmGeneralPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, VmGeneralModelProvider modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider);
    }

    @Override
    protected void onDetailModelEntityChange(Object entity) {
        getView().update();
    }

}
