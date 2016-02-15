package org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels;

import org.ovirt.engine.ui.uicommonweb.models.hosts.network.LogicalNetworkModel;

import com.google.gwt.resources.client.ImageResource;

public class ExternalNetworkPanel extends NetworkPanel {

    public ExternalNetworkPanel(LogicalNetworkModel item, NetworkPanelsStyle style) {
        super(item, style, false);
        getElement().addClassName(style.disabledNetworkPanel());
    }

    @Override
    protected ImageResource getStatusImage() {
        return resources.openstackImage();
    }

}
