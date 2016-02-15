package org.ovirt.engine.ui.webadmin.widget.table.column;

import java.util.Comparator;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.ui.common.widget.table.column.AbstractColumn;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.widget.table.cell.BrickStatusCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class BrickStatusColumn extends AbstractColumn<GlusterBrickEntity, GlusterBrickEntity> {

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    public BrickStatusColumn() {
        super(new BrickStatusCell());
    }

    @Override
    public GlusterBrickEntity getValue(GlusterBrickEntity object) {
        return object;
    }

    public void makeSortable() {
        makeSortable(new Comparator<GlusterBrickEntity>() {
            @Override
            public int compare(GlusterBrickEntity o1, GlusterBrickEntity o2) {
                return o1.getStatus().ordinal() - o2.getStatus().ordinal();
            }
        });
    }

    @Override
    public SafeHtml getTooltip(GlusterBrickEntity brick) {
        GlusterStatus status = brick.getStatus();
        String tooltip = null;

        switch (status) {
        case DOWN:
            tooltip = constants.down();
            break;
        case UP:
            tooltip = constants.up();
            break;
        case UNKNOWN:
            tooltip = constants.unknown();
            break;
        default:
            tooltip = constants.down();
        }

        return SafeHtmlUtils.fromSafeConstant(tooltip);

    }
}
