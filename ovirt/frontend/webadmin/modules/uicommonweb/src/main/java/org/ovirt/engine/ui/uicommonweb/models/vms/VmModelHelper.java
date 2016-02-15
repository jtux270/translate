package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.UIMessages;
import org.ovirt.engine.ui.uicompat.external.StringUtils;

public class VmModelHelper {

    public static enum WarningType {
        VM_EXPORT,
        VM_SNAPSHOT,
        VM_TEMPLATE
    }

    public static void sendWarningForNonExportableDisks(Model model, ArrayList<Disk> vmDisks, WarningType warningType) {
        final ArrayList<Disk> sharedImageDisks = new ArrayList<Disk>();
        final ArrayList<Disk> directLunDisks = new ArrayList<Disk>();
        final ArrayList<Disk> snapshotDisks = new ArrayList<Disk>();

        for (Disk disk : vmDisks) {
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE) {
                if (disk.isShareable()) {
                    sharedImageDisks.add(disk);
                } else if (disk.isDiskSnapshot()) {
                    snapshotDisks.add(disk);
                }
            } else if (disk.getDiskStorageType() == DiskStorageType.LUN) {
                directLunDisks.add(disk);
            }
        }

        final UIMessages messages = ConstantsManager.getInstance().getMessages();

        // check if VM provides any disk for the export
        if (vmDisks.size() - (sharedImageDisks.size() + directLunDisks.size() + snapshotDisks.size()) == 0) {
            switch (warningType) {
            case VM_EXPORT:
                model.setMessage(messages.noExportableDisksFoundForTheExport());
                break;
            case VM_SNAPSHOT:
                model.setMessage(messages.noExportableDisksFoundForTheSnapshot());
                break;
            case VM_TEMPLATE:
                model.setMessage(messages.noExportableDisksFoundForTheTemplate());
                break;
            }
        }

        String diskLabels = getDiskLabelList(sharedImageDisks);
        if (diskLabels != null) {
            switch (warningType) {
            case VM_EXPORT:
                model.setMessage(messages.sharedDisksWillNotBePartOfTheExport(diskLabels));
                break;
            case VM_SNAPSHOT:
                model.setMessage(messages.sharedDisksWillNotBePartOfTheSnapshot(diskLabels));
                break;
            case VM_TEMPLATE:
                model.setMessage(messages.sharedDisksWillNotBePartOfTheTemplate(diskLabels));
                break;
            }
        }

        diskLabels = getDiskLabelList(directLunDisks);
        if (diskLabels != null) {
            switch (warningType) {
            case VM_EXPORT:
                model.setMessage(messages.directLUNDisksWillNotBePartOfTheExport(diskLabels));
                break;
            case VM_SNAPSHOT:
                model.setMessage(messages.directLUNDisksWillNotBePartOfTheSnapshot(diskLabels));
                break;
            case VM_TEMPLATE:
                model.setMessage(messages.directLUNDisksWillNotBePartOfTheTemplate(diskLabels));
                break;
            }
        }

        diskLabels = getDiskLabelList(snapshotDisks);
        if (diskLabels != null) {
            switch (warningType) {
                case VM_EXPORT:
                    model.setMessage(messages.snapshotDisksWillNotBePartOfTheExport(diskLabels));
                    break;
                case VM_SNAPSHOT:
                    model.setMessage(messages.snapshotDisksWillNotBePartOfTheSnapshot(diskLabels));
                    break;
                case VM_TEMPLATE:
                    model.setMessage(messages.snapshotDisksWillNotBePartOfTheTemplate(diskLabels));
                    break;
            }
        }
    }

    private static String getDiskLabelList(ArrayList<Disk> disks) {
        if (disks.isEmpty()) {
            return null;
        }

        final List<String> labels = new ArrayList<String>();
        for (Disk disk : disks) {
            labels.add(disk.getDiskAlias());
        }

        return StringUtils.join(labels, ", "); //$NON-NLS-1$
    }

}
