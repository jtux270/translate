package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.host;

import java.util.Arrays;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.ui.common.place.PlaceRequestFactory;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.uicompat.EnumTranslator;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.HostSelectionChangeEvent;
import org.ovirt.engine.ui.webadmin.widget.alert.InLineAlertWidget.AlertType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;

public class SubTabHostGeneralInfoPresenter extends AbstractSubTabPresenter<VDS, HostListModel<Void>, HostGeneralModel,
    SubTabHostGeneralInfoPresenter.ViewDef, SubTabHostGeneralInfoPresenter.ProxyDef> {

    private final static ApplicationConstants constants = AssetProvider.getConstants();
    private final static ApplicationMessages messages = AssetProvider.getMessages();

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.hostGeneralInfoSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabHostGeneralInfoPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VDS> {
        /**
         * Clear all the alerts currently displayed in the alerts panel of the host.
         */
        void clearAlerts();

        /**
         * Displays a new alert in the alerts panel of the host.
         *
         * @param widget
         *            the widget used to display the alert, usually just a text label, but can also be a text label with
         *            a link to an action embedded
         */
        void addAlert(Widget widget);

        /**
         * Displays a new alert in the alerts panel of the host.
         *
         * @param widget
         *            the widget used to display the alert, usually just a text label, but can also be a text label with
         *            a link to an action embedded
         * @param type
         *            the type of the alert
         */
        void addAlert(Widget widget, AlertType type);
    }



    @TabInfo(container = HostGeneralSubTabPanelPresenter.class)
    static TabData getTabData(
            DetailModelProvider<HostListModel<Void>, HostGeneralModel> modelProvider) {
        return new ModelBoundTabData(constants.hostGeneralInfoSubTabLabel(), 0, modelProvider);
    }

    @Inject
    public SubTabHostGeneralInfoPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, DetailModelProvider<HostListModel<Void>, HostGeneralModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider,
                HostGeneralSubTabPanelPresenter.TYPE_SetTabContent);
    }

    @Override
    public void initializeHandlers() {
        super.initializeHandlers();

        // Initialize the list of alerts:
        final HostGeneralModel model = getModelProvider().getModel();
        updateAlerts(getView(), model);

        // Listen for changes in the properties of the model in order
        // to update the alerts panel:
        model.getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if (args.propertyName.contains("Alert")) { //$NON-NLS-1$
                    updateAlerts(getView(), model);
                }
            }
        });
    }

    /**
     * Review the model and if there are alerts add them to the view.
     *
     * @param view
     *            the view where alerts should be added
     * @param model
     *            the model to review
     */
    private void updateAlerts(final ViewDef view, final HostGeneralModel model) {
        // Clear all the alerts:
        view.clearAlerts();

        // Review the alerts and add those that are active:
        if (model.getHasUpgradeAlert()) {
            if (VdcActionUtils.canExecute(Arrays.asList(model.getEntity()), VDS.class, VdcActionType.UpgradeHost)) {
                addTextAndLinkAlert(view,
                        messages.hostInSupportedStatusHasUpgradeAlert(),
                        model.getUpgradeHostCommand(),
                        AlertType.UPDATE_AVAILABLE);
            } else {
                addTextAlert(view, messages.hostHasUpgradeAlert(), AlertType.UPDATE_AVAILABLE);
            }
        }

        if (model.getHasReinstallAlertNonResponsive()) {
            addTextAlert(view, messages.hostHasReinstallAlertNonResponsive());
        }
        if (model.getHasNICsAlert()) {
            addTextAndLinkAlert(view, messages.hostHasNICsAlert(), model.getSaveNICsConfigCommand());
        }
        if (model.getHasManualFenceAlert()) {
            addTextAlert(view, messages.hostHasManualFenceAlert());
        }
        if (ApplicationModeHelper.getUiMode() != ApplicationMode.GlusterOnly && model.getHasNoPowerManagementAlert()) {
            addTextAndLinkAlert(view, messages.hostHasNoPowerManagementAlert(), model.getEditHostCommand());
        }
        if (model.getNonOperationalReasonEntity() != null) {
            addTextAlert(view, EnumTranslator.getInstance().translate(model.getNonOperationalReasonEntity()));
        }
    }

    private void addTextAlert(final ViewDef view, final String text, AlertType type) {
        final Label label = new Label(text);
        view.addAlert(label, type);
    }

    /**
     * Create a widget containing text and add it to the alerts panel of the host.
     *
     * @param view
     *            the view where the alert should be added
     * @param text
     *            the text content of the alert
     */
    private void addTextAlert(final ViewDef view, final String text) {
        addTextAlert(view, text, AlertType.ALERT);
    }

    /**
     * Create a widget containing text and a link that triggers the execution of a command.
     *
     * @param view
     *            the view where the alert should be added
     * @param text
     *            the text content of the alert
     * @param command
     *            the command that should be executed when the link is clicked
     * @param alertType
     *            the type of the alert
     */
    private void addTextAndLinkAlert(final ViewDef view,
            final String text,
            final UICommand command,
            final AlertType alertType) {
        // Find the open and close positions of the link within the message:
        final int openIndex = text.indexOf("<a>"); //$NON-NLS-1$
        final int closeIndex = text.indexOf("</a>"); //$NON-NLS-1$
        if (openIndex == -1 || closeIndex == -1 || closeIndex < openIndex) {
            return;
        }

        // Extract the text before, inside and after the tags:
        final String beforeText = text.substring(0, openIndex);
        final String betweenText = text.substring(openIndex + 3, closeIndex);
        final String afterText = text.substring(closeIndex + 4);

        // Create a flow panel containing the text and the link:
        final FlowPanel alertPanel = new FlowPanel();

        // Create the label for the text before the tag:
        final Label beforeLabel = new Label(beforeText);
        beforeLabel.getElement().getStyle().setProperty("display", "inline"); //$NON-NLS-1$ //$NON-NLS-2$
        alertPanel.add(beforeLabel);

        // Create the anchor:
        final Anchor betweenAnchor = new Anchor(betweenText);
        betweenAnchor.getElement().getStyle().setProperty("display", "inline"); //$NON-NLS-1$ //$NON-NLS-2$
        alertPanel.add(betweenAnchor);

        // Add a listener to the anchor so that the command is executed when
        // it is clicked:
        betweenAnchor.addClickHandler(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        command.execute();
                    }
                }
                );

        // Create the label for the text after the tag:
        final Label afterLabel = new Label(afterText);
        afterLabel.getElement().getStyle().setProperty("display", "inline"); //$NON-NLS-1$ //$NON-NLS-2$
        alertPanel.add(afterLabel);

        // Add the alert to the view:
        view.addAlert(alertPanel, alertType);
    }

    private void addTextAndLinkAlert(final ViewDef view, final String text, final UICommand command) {
        addTextAndLinkAlert(view, text, command, AlertType.ALERT);
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return PlaceRequestFactory.get(WebAdminApplicationPlaces.hostMainTabPlace);
    }

    @ProxyEvent
    public void onHostSelectionChange(HostSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
    }

}
