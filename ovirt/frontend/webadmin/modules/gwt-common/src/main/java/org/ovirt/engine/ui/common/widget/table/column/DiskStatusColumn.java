package org.ovirt.engine.ui.common.widget.table.column;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.ImageStatus;

import com.google.gwt.resources.client.ImageResource;

public class DiskStatusColumn extends ImageResourceColumn<Disk> {
    @Override
    public ImageResource getValue(Disk disk) {
        if (disk.getDiskStorageType().equals(DiskStorageType.IMAGE)) {
            DiskImage diskImage = (DiskImage) disk;

            if (diskImage.getImageStatus().equals(ImageStatus.LOCKED)) {
                setEnumTitle(diskImage.getImageStatus());
                return new DiskImageStatusColumn().getValue(diskImage);
            }
        }

        boolean isDiskPlugged = disk.getPlugged() != null && disk.getPlugged().booleanValue();
        setTitle(isDiskPlugged ? getCommonConstants().active() : getCommonConstants().inactive());
        return isDiskPlugged ? getCommonResources().upImage() : getCommonResources().downImage();
    }
}
