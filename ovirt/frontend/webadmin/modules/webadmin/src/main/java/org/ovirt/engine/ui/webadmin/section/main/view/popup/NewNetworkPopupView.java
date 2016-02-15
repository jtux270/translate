package org.ovirt.engine.ui.webadmin.section.main.view.popup;

import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NetworkClusterModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NewNetworkModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.datacenter.NewNetworkPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class NewNetworkPopupView extends AbstractNetworkPopupView<NewNetworkModel> implements NewNetworkPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<NewNetworkModel, NewNetworkPopupView> {
    }

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public NewNetworkPopupView(EventBus eventBus,
            ApplicationResources resources,
            ApplicationConstants constants, ApplicationTemplates templates, ApplicationMessages messages) {
        super(eventBus, resources, constants, templates, messages);
        driver.initialize(this);
    }

    @Override
    public void updateVisibility() {
        super.updateVisibility();
        messageLabel.setVisible(false);
    }

    @Override
    protected void localize(ApplicationConstants constants) {
        super.localize(constants);
        mainLabel.setText(constants.dataCenterNewNetworkPopupLabel());
    }

    @Override
    public void edit(NewNetworkModel object) {
        super.edit(object);
        driver.edit(object);
    }

    @Override
    public NewNetworkModel flush() {
        super.flush();
        return driver.flush();
    }

    @Override
    public void setNetworkClusterList(ListModel<NetworkClusterModel> networkClusterList) {
        clustersTable.asEditor().edit(networkClusterList);
    }

}
