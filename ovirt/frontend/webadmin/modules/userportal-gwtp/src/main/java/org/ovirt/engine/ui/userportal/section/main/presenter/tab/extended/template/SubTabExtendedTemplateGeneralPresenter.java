package org.ovirt.engine.ui.userportal.section.main.presenter.tab.extended.template;

import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateGeneralModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.uicommon.model.template.TemplateGeneralModelProvider;
import org.ovirt.engine.ui.uicommonweb.place.UserPortalApplicationPlaces;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.TabDataBasic;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SubTabExtendedTemplateGeneralPresenter
        extends AbstractSubTabExtendedTemplatePresenter<TemplateGeneralModel, SubTabExtendedTemplateGeneralPresenter.ViewDef, SubTabExtendedTemplateGeneralPresenter.ProxyDef> {

    @ProxyCodeSplit
    @NameToken(UserPortalApplicationPlaces.extendedTempplateGeneralSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabExtendedTemplateGeneralPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VmTemplate> {
    }

    @TabInfo(container = ExtendedTemplateSubTabPanelPresenter.class)
    static TabData getTabData(ApplicationConstants applicationConstants) {
        return new TabDataBasic(applicationConstants.extendedTemplateGeneralSubTabLabel(), 0);
    }

    @Inject
    public SubTabExtendedTemplateGeneralPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, TemplateGeneralModelProvider modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider);
    }

}
