package org.ovirt.engine.ui.webadmin.section.main.view.popup.host;

import org.ovirt.engine.ui.uicommonweb.models.hosts.HostInterfaceModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.host.SetupNetworksInterfacePopupPresenterWidget;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class SetupNetworksInterfacePopupView extends HostInterfacePopupView
        implements SetupNetworksInterfacePopupPresenterWidget.ViewDef {

    @Inject
    public SetupNetworksInterfacePopupView(EventBus eventBus) {

        super(eventBus);
    }

    @Override
    public void edit(HostInterfaceModel object) {
        super.edit(object);

        info.setVisible(false);
        message.setVisible(false);
        checkConnectivity.setVisible(false);
        bondingModeEditor.setVisible(false);
        commitChanges.setVisible(false);

        isToSync.setVisible(true);
        if (object.getIsToSync().getIsChangable()) {
            isToSyncInfo.setVisible(true);
        }

        // resize
        layoutPanel.remove(infoPanel);
        layoutPanel.setWidgetSize(mainPanel, 510);
        asPopupPanel().setPixelSize(415, 590);

        enableDisableByBootProtocol(object);

        customPropertiesPanel.setVisible(object.getCustomPropertiesModel().getIsAvailable());
        customPropertiesWidget.edit(object.getCustomPropertiesModel());
        customPropertiesLabel.setEnabled(object.getCustomPropertiesModel().getIsChangable());

        if (object.getNetwork().getSelectedItem().getCluster().isDisplay()) {
            displayNetworkChangeWarning.setVisible(true);
        }
    }
}
