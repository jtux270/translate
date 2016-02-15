package org.ovirt.engine.ui.common.widget.table.column;

import java.util.Comparator;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.utils.PairQueryable;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class NicActivateStatusColumn<T> extends SafeHtmlWithSafeHtmlTooltipColumn<T> {
    CommonApplicationResources resources = GWT.create(CommonApplicationResources.class);
    CommonApplicationConstants constants = GWT.create(CommonApplicationConstants.class);
    CommonApplicationTemplates templates = GWT.create(CommonApplicationTemplates.class);

    private ImageResource getImage(T object) {
        VmNetworkInterface vnic = null;
        if (object instanceof VmNetworkInterface) {
            vnic = (VmNetworkInterface) object;
        } else if (object instanceof PairQueryable && ((PairQueryable) object).getFirst() instanceof VmNetworkInterface) {
            vnic = ((PairQueryable<VmNetworkInterface, VM>) object).getFirst();
        }

        if (vnic != null) {
            return vnic.isPlugged() && vnic.isLinked() ? resources.upImage() : resources.downImage();
        }

        return null;
    }

    public void makeSortable() {
        makeSortable(new Comparator<T>() {

            private final SimpleStatusImageComparator imageComparator = new SimpleStatusImageComparator();

            @Override
            public int compare(T o1, T o2) {
                return imageComparator.compare(getImage(o1), getImage(o2));
            }
        });
    }

    @Override
    public SafeHtml getValue(T object) {
        ImageResource image = getImage(object);
        return (image == null) ? null : SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(image).getHTML());
    }

    @Override
    public SafeHtml getTooltip(T object) {
        VmNetworkInterface vnic = null;
        if (object instanceof VmNetworkInterface) {
            vnic = (VmNetworkInterface) object;
        } else if (object instanceof PairQueryable && ((PairQueryable) object).getFirst() instanceof VmNetworkInterface) {
            vnic = ((PairQueryable<VmNetworkInterface, VM>) object).getFirst();
        }

        StringBuilder tooltip = new StringBuilder(""); //$NON-NLS-1$
        boolean vnicExist = vnic != null;
        if (vnicExist && vnic.isPlugged()) {
            tooltip =
                    tooltip.append(templates.cardStatus(constants.pluggedNetworkInterface()).asString());
        } else {
            tooltip =
                    tooltip.append(templates.cardStatus(constants.unpluggedNetworkInterface()).asString());
        }

        tooltip = tooltip.append("<BR>"); //$NON-NLS-1$
        if (vnicExist && vnic.isLinked()) {
            tooltip =
                    tooltip.append(templates.linkState(constants.linkedNetworkInterface())
                            .asString());
        } else {
            tooltip =
                    tooltip.append(templates.linkState(constants.unlinkedNetworkInterface())
                            .asString());
        }

        return SafeHtmlUtils.fromTrustedString(tooltip.toString());
    }
}
