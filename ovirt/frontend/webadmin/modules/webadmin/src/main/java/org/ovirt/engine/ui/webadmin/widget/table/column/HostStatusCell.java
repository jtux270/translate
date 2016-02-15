package org.ovirt.engine.ui.webadmin.widget.table.column;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class HostStatusCell extends AbstractCell<VDS> {

    @Override
    public void render(Context context, VDS vds, SafeHtmlBuilder sb) {
        // Nothing to render if no host is provided:
        if (vds == null) {
            return;
        }

        // Get a reference to the application resources:
        ApplicationResources resources = ClientGinjectorProvider.getApplicationResources();

        // Find the image corresponding to the status of the host:
        VDSStatus status = vds.getStatus();
        ImageResource statusImage = null;
        switch (status) {
        case Unassigned:
        case NonResponsive:
        case InstallFailed:
        case Connecting:
        case Down:
            statusImage = resources.downImage();
            break;
        case PreparingForMaintenance:
            statusImage = resources.prepareImage();
            break;
        case Maintenance:
            statusImage = resources.maintenanceImage();
            break;
        case Up:
            statusImage = resources.upImage();
            break;
        case Error:
            statusImage = resources.errorImage();
            break;
        case Installing:
            statusImage = resources.hostInstallingImage();
            break;
        case Reboot:
            statusImage = resources.waitImage();
            break;
        case NonOperational:
            statusImage = resources.nonOperationalImage();
            break;
        case PendingApproval:
        case InstallingOS:
            statusImage = resources.unconfiguredImage();
            break;
        case Initializing:
            statusImage = resources.waitImage();
            break;
        case Kdumping:
            statusImage = resources.waitImage();
            break;
        default:
            statusImage = resources.downImage();
        }

        // Find the image corresponding to the alert:
        ImageResource alertImage = resources.alertImage();

        // Generate the HTML for the images:
        SafeHtml statusImageHtml =
                SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(statusImage).getHTML());
        SafeHtml alertImageHtml = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(alertImage).getHTML());

        // Generate the HTML for the cell including the exclamation mark only if
        // power management is not enabled or there are network configuration
        // changes that haven't been saved yet:
        sb.appendHtmlConstant("<div style=\"text-align: center;\">"); //$NON-NLS-1$
        sb.append(statusImageHtml);
        boolean getnet_config_dirty =
                vds.getNetConfigDirty() == null ? false : vds.getNetConfigDirty().booleanValue();
        boolean showPMAlert = vds.getVdsGroupSupportsVirtService() && !vds.getpm_enabled();
        if (showPMAlert || getnet_config_dirty) {
            sb.append(alertImageHtml);
        }
        sb.appendHtmlConstant("</div>"); //$NON-NLS-1$
    }
}
