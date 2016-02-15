package org.ovirt.engine.ui.common.widget.uicommon.vm;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.DiskSizeColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundTableWidget;
import org.ovirt.engine.ui.common.widget.uicommon.disks.DisksViewColumns;
import org.ovirt.engine.ui.common.widget.uicommon.disks.DisksViewRadioGroup;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmDiskListModelBase;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.RadioButton;

public class BaseVmDiskListModelTable<T extends VmDiskListModelBase> extends AbstractModelBoundTableWidget<Disk, T> {

    private CommonApplicationConstants constants;
    private DisksViewRadioGroup disksViewRadioGroup;

    private static TextColumnWithTooltip<Disk> aliasColumn;
    private static DiskSizeColumn sizeColumn;
    private static DiskSizeColumn actualSizeColumn;
    private static TextColumnWithTooltip<Disk> allocationColumn;
    private static TextColumnWithTooltip<Disk> dateCreatedColumn;
    private static TextColumnWithTooltip<Disk> statusColumn;
    private static TextColumnWithTooltip<Disk> lunIdColumn;
    private static TextColumnWithTooltip<Disk> lunSerialColumn;
    private static TextColumnWithTooltip<Disk> lunVendorIdColumn;
    private static TextColumnWithTooltip<Disk> lunProductIdColumn;
    private static TextColumnWithTooltip<Disk> interfaceColumn;
    private static TextColumnWithTooltip<Disk> descriptionColumn;

    public BaseVmDiskListModelTable(
            SearchableTableModelProvider<Disk, T> modelProvider,
            EventBus eventBus,
            ClientStorage clientStorage) {
        super(modelProvider, eventBus, clientStorage, false);

        disksViewRadioGroup = new DisksViewRadioGroup();
    }

    final ClickHandler clickHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            if (((RadioButton) event.getSource()).getValue()) {
                handleRadioButtonClick(event);
            }
        }
    };

    void initTableOverhead() {
        disksViewRadioGroup.setClickHandler(clickHandler);
        disksViewRadioGroup.addStyleName("dvrg_radioGroup_pfly_fix"); //$NON-NLS-1$
        getTable().setTableOverhead(disksViewRadioGroup);
        getTable().setTableTopMargin(20);
    }

    @Override
    public void initTable(CommonApplicationConstants constants) {
        this.constants = constants;

        initTableColumns();
        initTableOverhead();
        handleRadioButtonClick(null);

        getModel().getItemsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                disksViewRadioGroup.setDiskStorageType((DiskStorageType) getModel().getDiskViewType().getEntity());
            }
        });
    }

    void handleRadioButtonClick(ClickEvent event) {
        boolean all = disksViewRadioGroup.getAllButton().getValue();
        boolean images = disksViewRadioGroup.getImagesButton().getValue();
        boolean luns = disksViewRadioGroup.getLunsButton().getValue();

        getTable().getSelectionModel().clear();
        getModel().getDiskViewType().setEntity(disksViewRadioGroup.getDiskStorageType());
        getModel().setItems(null);
        getModel().search();

        getTable().ensureColumnPresent(
                DisksViewColumns.diskStatusColumn, constants.empty(), all || images || luns, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                aliasColumn, constants.aliasDisk(), all || images || luns, "120px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.bootableDiskColumn,
                DisksViewColumns.bootableDiskColumn.getHeaderHtml(), all || images || luns, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.shareableDiskColumn,
                DisksViewColumns.shareableDiskColumn.getHeaderHtml(), all || images || luns, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.readOnlyDiskColumn,
                DisksViewColumns.readOnlyDiskColumn.getHeaderHtml(), all || images || luns, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.lunDiskColumn,
                DisksViewColumns.lunDiskColumn.getHeaderHtml(), all, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                sizeColumn, constants.provisionedSizeDisk(), all || images || luns, "110px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                actualSizeColumn, constants.sizeDisk(), images, "110px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                allocationColumn, constants.allocationDisk(), images, "125px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.storageDomainsColumn, constants.storageDomainDisk(), images, "125px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.storageTypeColumn, constants.storageTypeDisk(), images, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                dateCreatedColumn, constants.creationDateDisk(), images, "120px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                lunIdColumn, constants.lunIdSanStorage(), luns, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                lunSerialColumn, constants.serialSanStorage(), luns, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                lunVendorIdColumn, constants.vendorIdSanStorage(), luns, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                lunProductIdColumn, constants.productIdSanStorage(), luns, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.diskContainersColumn, constants.attachedToDisk(), all || images || luns, "110px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                interfaceColumn, constants.interfaceDisk(), all || images || luns, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.diskAlignmentColumn, constants.diskAlignment(), all || images || luns, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                statusColumn, constants.statusDisk(), images, "80px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                descriptionColumn, constants.descriptionDisk(), all || images || luns, "90px"); //$NON-NLS-1$

    }

    void initTableColumns() {
        getTable().enableColumnResizing();

        aliasColumn = DisksViewColumns.getAliasColumn(null);
        sizeColumn = DisksViewColumns.getSizeColumn(null);
        actualSizeColumn = DisksViewColumns.getActualSizeColumn(null);
        allocationColumn = DisksViewColumns.getAllocationColumn(null);
        dateCreatedColumn = DisksViewColumns.getDateCreatedColumn(null);
        statusColumn = DisksViewColumns.getStatusColumn(null);
        lunIdColumn = DisksViewColumns.getLunIdColumn(null);
        lunSerialColumn = DisksViewColumns.getLunSerialColumn(null);
        lunVendorIdColumn = DisksViewColumns.getLunVendorIdColumn(null);
        lunProductIdColumn = DisksViewColumns.getLunProductIdColumn(null);
        interfaceColumn = DisksViewColumns.getInterfaceColumn(null);
        descriptionColumn = DisksViewColumns.getDescriptionColumn(null);
    }
}
