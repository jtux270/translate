package org.ovirt.engine.ui.webadmin.widget.template;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.label.DiskSizeLabel;
import org.ovirt.engine.ui.common.widget.label.EnumLabel;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.common.widget.table.column.EmptyColumn;
import org.ovirt.engine.ui.common.widget.tree.AbstractSubTabTree;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageDomainModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateStorageListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;

import com.google.gwt.user.client.ui.DateLabel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TreeItem;

public class StoragesTree extends AbstractSubTabTree<TemplateStorageListModel, StorageDomainModel, DiskModel> {

    ApplicationResources resources;
    ApplicationConstants constants;

    public StoragesTree(ApplicationResources resources, ApplicationConstants constants, ApplicationTemplates templates) {
        super(resources, constants, templates);
        this.resources = resources;
        this.constants = constants;

        setNodeSelectionEnabled(true);
    }

    @Override
    protected TreeItem getRootItem(StorageDomainModel storageDomainModel) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setSpacing(1);
        panel.setWidth("100%"); //$NON-NLS-1$

        StorageDomain storage = storageDomainModel.getStorageDomain();

        addItemToPanel(panel, new Image(resources.storageImage()), "25px"); //$NON-NLS-1$
        addTextBoxToPanel(panel, new TextBoxLabel(), storage.getStorageName(), ""); //$NON-NLS-1$
        addValueLabelToPanel(panel, new EnumLabel<StorageDomainType>(), storage.getStorageDomainType(), "120px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new EnumLabel<StorageDomainSharedStatus>(), storage.getStorageDomainSharedStatus(), "120px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DiskSizeLabel<Integer>(), storage.getAvailableDiskSize(), "120px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DiskSizeLabel<Integer>(), storage.getUsedDiskSize(), "120px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DiskSizeLabel<Integer>(), storage.getTotalDiskSize(), "90px"); //$NON-NLS-1$

        TreeItem treeItem = new TreeItem(panel);
        treeItem.setUserObject(storage.getId());
        return treeItem;
    }

    @Override
    protected TreeItem getNodeItem(DiskModel diskModel) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setSpacing(1);
        panel.setWidth("100%"); //$NON-NLS-1$

        DiskImage disk = (DiskImage) diskModel.getDisk();

        addItemToPanel(panel, new Image(resources.diskImage()), "25px"); //$NON-NLS-1$
        addTextBoxToPanel(panel, new TextBoxLabel(), disk.getDiskAlias(), ""); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DiskSizeLabel<Long>(), disk.getSizeInGigabytes(), "120px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new EnumLabel<ImageStatus>(), disk.getImageStatus(), "120px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new EnumLabel<VolumeType>(), disk.getVolumeType(), "120px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new EnumLabel<DiskInterface>(), disk.getDiskInterface(), "110px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DateLabel(), disk.getCreationDate(), "90px"); //$NON-NLS-1$

        TreeItem treeItem = new TreeItem(panel);
        treeItem.setUserObject(getEntityId(diskModel));
        return treeItem;
    }

    @Override
    protected TreeItem getNodeHeader() {
        EntityModelCellTable<ListModel> table = new EntityModelCellTable<ListModel>(false, true);
        table.addColumn(new EmptyColumn(), constants.empty(), "25px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.nameStorageTree(), ""); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.sizeStorageTree(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.statusStorageTree(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.allocationStorageTree(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.interfaceStorageTree(), "110px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.creationDateStorageTree(), "100px"); //$NON-NLS-1$
        table.setRowData(new ArrayList());
        table.setWidth("100%", true); //$NON-NLS-1$

        TreeItem item = new TreeItem(table);
        item.setUserObject(NODE_HEADER);
        item.getElement().getStyle().setBackgroundColor("#F0F2FF"); //$NON-NLS-1$
        return item;
    }

    @Override
    protected ArrayList<DiskModel> getNodeObjects(StorageDomainModel storageDomainModel) {
        return storageDomainModel.getDisksModels();
    }

    protected Object getEntityId(Object entity) {
        DiskModel diskModel = (DiskModel) entity;
        StorageDomain storageDomain = (StorageDomain) diskModel.getStorageDomain().getSelectedItem();
        return ((DiskImage) diskModel.getDisk()).getImageId().toString() + storageDomain.getId().toString();
    }

    protected ArrayList<Object> getSelectedEntities() {
        ArrayList<Object> selectedEntities = new ArrayList<Object>();
        for (StorageDomainModel storageDomainModel : (ArrayList<StorageDomainModel>) listModel.getItems()) {
            for (DiskModel entity : storageDomainModel.getDisksModels()) {
                if (selectedItems.contains(getEntityId(entity))) {
                    selectedEntities.add(entity);
                }
            }
        }
        return selectedEntities;
    }
}
