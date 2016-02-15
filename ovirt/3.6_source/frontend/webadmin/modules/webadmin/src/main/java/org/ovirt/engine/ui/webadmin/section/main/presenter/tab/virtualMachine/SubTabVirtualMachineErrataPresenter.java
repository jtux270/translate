package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.virtualMachine;

import org.ovirt.engine.core.common.businessentities.ErrataCounts;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.place.PlaceRequestFactory;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.DetailTabModelProvider;
import org.ovirt.engine.ui.common.widget.AbstractUiCommandButton;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.VmErrataCountModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.VirtualMachineSelectionChangeEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
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
/**
 * Presenter for the sub tab (VMs > Errata) that contains errata (singular: Erratum)
 * for the selected VM.
 */
public class SubTabVirtualMachineErrataPresenter extends AbstractSubTabPresenter<VM, VmListModel<Void>,
        VmErrataCountModel, SubTabVirtualMachineErrataPresenter.ViewDef,
        SubTabVirtualMachineErrataPresenter.ProxyDef> {

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.virtualMachineErrataSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabVirtualMachineErrataPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VM> {
        AbstractUiCommandButton getTotalSecurity();
        AbstractUiCommandButton getTotalBugFix();
        AbstractUiCommandButton getTotalEnhancement();
        void showErrorMessage(SafeHtml errorMessage);
        void clearErrorMessage();
        void showCounts(ErrataCounts counts);
        void showProgress();
    }

    @TabInfo(container = VirtualMachineSubTabPanelPresenter.class)
    static TabData getTabData(DetailTabModelProvider<VmListModel<Void>, VmErrataCountModel> errataCountModelProvider) {
        return new ModelBoundTabData(constants.virtualMachineErrataSubTabLabel(), 9, errataCountModelProvider);
    }

    private final VmErrataCountModel errataCountModel;

    private VM currentSelectedVm;

    @Inject
    public SubTabVirtualMachineErrataPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager,
            DetailTabModelProvider<VmListModel<Void>, VmErrataCountModel> errataCountModelProvider) {
        super(eventBus, view, proxy, placeManager, errataCountModelProvider,
                VirtualMachineSubTabPanelPresenter.TYPE_SetTabContent);
        errataCountModel = errataCountModelProvider.getModel();
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return PlaceRequestFactory.get(WebAdminApplicationPlaces.virtualMachineMainTabPlace);
    }

    @ProxyEvent
    public void onVirtualMachineSelectionChange(VirtualMachineSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
        currentSelectedVm = null;
        if (event.getSelectedItems() != null && !event.getSelectedItems().isEmpty()) {
            currentSelectedVm = event.getSelectedItems().get(0);
        }
        if (isVisible()) {
            updateModel();
        }
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        updateModel();
    }

    @Override
    protected void onBind() {
        super.onBind();

        getView().getTotalSecurity().setCommand(errataCountModel.getShowSecurityCommand());
        getView().getTotalBugFix().setCommand(errataCountModel.getShowBugsCommand());
        getView().getTotalEnhancement().setCommand(errataCountModel.getShowEnhancementsCommand());

        registerHandler(getView().getTotalSecurity().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                getView().getTotalSecurity().getCommand().execute();
            }
        }));

        registerHandler(getView().getTotalBugFix().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                getView().getTotalBugFix().getCommand().execute();
            }
        }));

        registerHandler(getView().getTotalEnhancement().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                getView().getTotalEnhancement().getCommand().execute();
            }
        }));

        // Handle the counts changing -> simple view update.
        //
        errataCountModel.addErrataCountsChangeListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                // bus published message that the counts changed. update view.
                ErrataCounts counts = errataCountModel.getErrataCounts();
                getView().showCounts(counts);
            }
        });

        // Handle the count model getting a query error -> simple view update.
        //
        errataCountModel.addPropertyChangeListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if ("Message".equals(args.propertyName)) { //$NON-NLS-1$
                    // bus published message that an error occurred communicating with Katello. Show the alert panel.
                    if (errataCountModel.getMessage() != null && !errataCountModel.getMessage().isEmpty()) {
                        getView().showErrorMessage(SafeHtmlUtils.fromString(errataCountModel.getMessage()));
                    } else {
                        getView().clearErrorMessage();
                    }
                } else if (PropertyChangedEventArgs.PROGRESS.equals(args.propertyName)) {
                    if (errataCountModel.getProgress() != null) {
                        getView().showProgress();
                    }
                }
            }
        });
    }

    private void updateModel() {
        if (currentSelectedVm != null) {
            // Update the model with data from the backend
            errataCountModel.setGuid(currentSelectedVm.getId());
            errataCountModel.setEntity(currentSelectedVm);
            errataCountModel.runQuery(currentSelectedVm.getId());
        }
    }

}
