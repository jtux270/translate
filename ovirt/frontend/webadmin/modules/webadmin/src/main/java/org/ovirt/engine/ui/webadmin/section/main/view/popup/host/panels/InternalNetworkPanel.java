package org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels;

import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.LogicalNetworkModel;
import com.google.gwt.resources.client.ImageResource;

public class InternalNetworkPanel extends NetworkPanel {

    public InternalNetworkPanel(LogicalNetworkModel item, NetworkPanelsStyle style) {
        this(item, style, true);
    }

    public InternalNetworkPanel(LogicalNetworkModel item, NetworkPanelsStyle style, boolean draggable) {
        super(item, style, draggable);
        getElement().addClassName(style.networkPanel());
    }

    @Override
    protected ImageResource getStatusImage() {
        NetworkStatus netStatus = ((LogicalNetworkModel) item).getStatus();

        if (netStatus == NetworkStatus.OPERATIONAL) {
            return resources.upImage();
        } else if (netStatus == NetworkStatus.NON_OPERATIONAL) {
            return resources.downImage();
        } else {
            return resources.questionMarkImage();
        }
    }

}
