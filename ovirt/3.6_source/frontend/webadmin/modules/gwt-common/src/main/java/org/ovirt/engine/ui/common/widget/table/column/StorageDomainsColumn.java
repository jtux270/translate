package org.ovirt.engine.ui.common.widget.table.column;

import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.widget.table.cell.TextCell;
import org.ovirt.engine.ui.uicompat.external.StringUtils;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class StorageDomainsColumn extends AbstractTextColumn<Disk> implements ColumnWithElementId {

    private final static CommonApplicationConstants constants = AssetProvider.getConstants();

    @Override
    public String getValue(Disk object) {
        if (object.getDiskStorageType() != DiskStorageType.IMAGE
                && object.getDiskStorageType() != DiskStorageType.CINDER) {
            return constants.empty();
        }

        DiskImage diskImage = (DiskImage) object;

        int numOfStorageDomains = diskImage.getStoragesNames() != null ?
                diskImage.getStoragesNames().size() : 0;
        if (numOfStorageDomains == 0) {
            return constants.empty();
        }
        else if (numOfStorageDomains == 1) {
            return diskImage.getStoragesNames().get(0);
        }
        else {
            return numOfStorageDomains + constants.space() + constants.storageDomainsLabelDisk();
        }
    }

    @Override
    public TextCell getCell() {
        return (TextCell) super.getCell();
    }

    @Override
    public SafeHtml getTooltip(Disk object) {
        DiskImage diskImage = (DiskImage) object;
        return SafeHtmlUtils.fromString(StringUtils.join(diskImage.getStoragesNames(), ", "));  //$NON-NLS-1$
    }
}
