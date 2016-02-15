package org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels;

import java.util.List;
import java.util.Map;

import org.ovirt.engine.ui.common.utils.ElementUtils;
import org.ovirt.engine.ui.common.widget.tooltip.TooltipConfig.Width;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.LogicalNetworkModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkCommand;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkOperation;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public abstract class NetworkPanel extends NetworkItemPanel<LogicalNetworkModel> {

    private final static ApplicationResources resources = AssetProvider.getResources();
    private final static ApplicationMessages messages = AssetProvider.getMessages();

    Label titleLabel;

    public NetworkPanel(LogicalNetworkModel item, NetworkPanelsStyle style, boolean draggable) {
        super(item, style, draggable);
        getElement().addClassName(item.getAttachedToNic() != null ? style.networkPanelAttached()
                : style.networkPanelNotAttached());
        if (item.isManagement()) {
            getElement().addClassName(style.mgmtNetwork());
        }

        // Not managed
        if (!item.isManaged()) {
            actionButton.getUpFace().setImage(new Image(resources.butEraseNetHover()));
            actionButton.getDownFace().setImage(new Image(resources.butEraseNetMousedown()));
        }
        actionButton.setStyleName(style.actionButtonNetwork());
        actionButton.addStyleName("np_actionButton_pfly_fix"); //$NON-NLS-1$
    }

    @Override
    protected Widget getContents() {

        Image mgmtNetworkImage;
        Image vmImage;
        Image monitorImage;
        Image migrationImage;
        Image notSyncImage;
        Image alertImage;
        Image glusterNwImage;

        if (!item.isManaged()) {
            monitorImage = null;
            mgmtNetworkImage = null;
            vmImage = null;
            migrationImage = null;
            glusterNwImage = null;
            notSyncImage = null;
            alertImage = null;
        } else {
            monitorImage = item.getNetwork().getCluster().isDisplay() ?
                    new Image(resources.networkMonitor()) : null;
            mgmtNetworkImage = item.isManagement() ? new Image(resources.mgmtNetwork()) : null;
            vmImage = item.getNetwork().isVmNetwork() ? new Image(resources.networkVm()) : null;
            migrationImage = item.getNetwork().getCluster().isMigration() ?
                    new Image(resources.migrationNetwork()) : null;
            glusterNwImage = item.getNetwork().getCluster().isGluster() ?
                    new Image(resources.glusterNetwork()) : null;
            notSyncImage = !item.isInSync() ? new Image(resources.networkNotSyncImage()) : null;
            alertImage = item.getErrorMessage() != null ? new Image(resources.alertImage()) : null;

            if (item.isManagement()) {
                mgmtNetworkImage.setStylePrimaryName(style.networkImageBorder());
            }

            if (item.getNetwork().isVmNetwork()) {
                vmImage.setStylePrimaryName(style.networkImageBorder());
            }

            if (item.getNetwork().getCluster().isDisplay()) {
                monitorImage.setStylePrimaryName(style.networkImageBorder());
            }

            if (item.getNetwork().getCluster().isMigration()) {
                migrationImage.setStylePrimaryName(style.networkImageBorder());
            }

            if (item.getNetwork().getCluster().isGluster()) {
                glusterNwImage.setStylePrimaryName(style.networkImageBorder());
            }

            if (!item.isInSync()) {
                notSyncImage.setStylePrimaryName(style.syncImageBorder());
            }
        }

        actionButton.setVisible(item.getAttachedToNic() != null
                && (item.isManaged() || !item.isAttachedViaLabel()));

        Grid rowPanel = new Grid(1, 10);
        rowPanel.setCellSpacing(0);
        rowPanel.setWidth("100%"); //$NON-NLS-1$
        rowPanel.setHeight("100%"); //$NON-NLS-1$

        ColumnFormatter columnFormatter = rowPanel.getColumnFormatter();
        columnFormatter.setWidth(0, "5px"); //$NON-NLS-1$
        columnFormatter.setWidth(1, "20px"); //$NON-NLS-1$
        columnFormatter.setWidth(2, "100%"); //$NON-NLS-1$

        rowPanel.setWidget(0, 0, dragImage);

        Panel statusPanel = new HorizontalPanel();
        statusPanel.setStylePrimaryName(style.networkStatusPanel());
        rowPanel.setWidget(0, 1, statusPanel);
        if (alertImage != null) {
            statusPanel.add(alertImage);
        }

        ImageResource statusImage = getStatusImage();
        if (statusImage != null) {
            statusPanel.add(new Image(statusImage));
        }

        if (notSyncImage != null) {
            statusPanel.add(notSyncImage);
        }

        rowPanel.setWidget(0, 2, createTitlePanel());
        rowPanel.setWidget(0, 3, mgmtNetworkImage);
        rowPanel.setWidget(0, 4, monitorImage);
        rowPanel.setWidget(0, 5, vmImage);
        rowPanel.setWidget(0, 6, migrationImage);
        rowPanel.setWidget(0, 7, glusterNwImage);
        rowPanel.setWidget(0, 8, actionButton);

        return rowPanel;
    }

    private Panel createTitlePanel() {
        titleLabel = new Label(item.getName());
        titleLabel.getElement().addClassName(style.titleLabel());
        Panel titlePanel = new HorizontalPanel();
        titlePanel.add(titleLabel);
        if (item.hasVlan()) {
            Label vlanLabel = new Label(messages.vlanNetwork(item.getVlanId()));
            vlanLabel.getElement().addClassName(style.vlanLabel());
            titlePanel.add(vlanLabel);
        }
        return titlePanel;
    }

    @Override
    protected void initTooltip() {
        super.initTooltip();
        setToolTipMaxWidth(Width.W520);
    }

    protected abstract ImageResource getStatusImage();

    @Override
    protected void onAction() {
        if (item.isManaged()) {
            item.edit();
        } else {
            Map<NetworkOperation, List<NetworkCommand>> operationMap = item.getSetupModel().commandsFor(item);
            final NetworkCommand detach = operationMap.get(NetworkOperation.REMOVE_UNMANAGED_NETWORK).get(0);
            item.getSetupModel().onOperation(NetworkOperation.REMOVE_UNMANAGED_NETWORK, detach);
        }
    }

    @Override
    protected void onLoad() {
        super.onLoad();

        while (ElementUtils.detectOverflowUsingScrollWidth(getElement())) {
            titleLabel.getElement().getStyle().setWidth(titleLabel.getElement().getClientWidth() - 1, Unit.PX);
        }
    }

}
