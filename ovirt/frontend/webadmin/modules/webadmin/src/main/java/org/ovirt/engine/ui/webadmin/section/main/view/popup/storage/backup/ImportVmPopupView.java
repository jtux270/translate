package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage.backup;

import java.util.ArrayList;
import java.util.Date;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.ListModelObjectCellTable;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.renderer.StorageDomainFreeSpaceRenderer;
import org.ovirt.engine.ui.common.widget.table.column.CheckboxColumn;
import org.ovirt.engine.ui.common.widget.table.column.DiskSizeColumn;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.FullDateTimeColumn;
import org.ovirt.engine.ui.common.widget.table.column.ImageResourceColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportDiskData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.backup.ImportVmPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.widget.table.column.CustomSelectionCell;
import org.ovirt.engine.ui.webadmin.widget.table.column.IsProblematicImportVmColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VmTypeColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.WebAdminImageResourceColumn;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

public class ImportVmPopupView extends AbstractModelBoundPopupView<ImportVmModel> implements ImportVmPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ImportVmModel, ImportVmPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ImportVmPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @Path(value = "cluster.selectedItem")
    ListModelListBoxEditor<Object> destClusterEditor;

    @UiField(provided = true)
    @Path(value = "cpuProfiles.selectedItem")
    ListModelListBoxEditor<CpuProfile> cpuProfileEditor;

    @UiField(provided = true)
    @Path(value = "clusterQuota.selectedItem")
    ListModelListBoxEditor<Object> destClusterQuotaEditor;

    @UiField(provided = true)
    @Path(value = "storage.selectedItem")
    ListModelListBoxEditor<Object> destStorageEditor;

    @UiField
    SplitLayoutPanel splitLayoutPanel;

    @UiField
    @Ignore
    Label message;

    @Ignore
    protected ListModelObjectCellTable<Object, ImportVmModel> table;

    @Ignore
    private ListModelObjectCellTable<DiskImage, SearchableListModel> diskTable;

    @Ignore
    private ListModelObjectCellTable<VmNetworkInterface, SearchableListModel> nicTable;

    private ListModelObjectCellTable<String, VmAppListModel> appTable;

    @Ignore
    protected TabLayoutPanel subTabLayoutPanel = null;

    protected ImportVmModel importModel;

    private ImportVmGeneralSubTabView generalView;

    boolean firstSelection = false;

    private CustomSelectionCell customSelectionCellFormatType;

    private CustomSelectionCell customSelectionCellStorageDomain;

    private CustomSelectionCell customSelectionCellQuota;

    private Column<DiskImage, String> storageDomainsColumn;

    private Column<DiskImage, String> quotaColumn;

    protected ImageResourceColumn<Object> isObjectInSystemColumn;

    private final Driver driver = GWT.create(Driver.class);

    protected final ApplicationConstants constants;

    protected final ApplicationResources resources;

    @Inject
    public ImportVmPopupView(EventBus eventBus,
            ApplicationResources resources,
            ApplicationConstants constants) {
        super(eventBus, resources);
        this.constants = constants;
        this.resources = resources;

        initListBoxEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        localize(constants);
        driver.initialize(this);
        initTables();
    }

    private void initSubTabLayoutPanel() {
        if (subTabLayoutPanel == null) {
            subTabLayoutPanel = new TabLayoutPanel(CommonApplicationTemplates.TAB_BAR_HEIGHT, Unit.PX);
            subTabLayoutPanel.addSelectionHandler(new SelectionHandler<Integer>() {

                @Override
                public void onSelection(SelectionEvent<Integer> event) {
                    subTabLayoutPanelSelectionChanged(event.getSelectedItem());
                }
            });

            initGeneralSubTabView();

            ScrollPanel nicPanel = new ScrollPanel();
            nicPanel.add(nicTable);
            subTabLayoutPanel.add(nicPanel, constants.importVmNetworkIntefacesSubTabLabel());

            ScrollPanel diskPanel = new ScrollPanel();
            diskPanel.add(diskTable);
            subTabLayoutPanel.add(diskPanel, constants.importVmDisksSubTabLabel());

            initAppTable();
        }
    }

    protected void subTabLayoutPanelSelectionChanged(Integer selectedItem) {
        if (importModel != null) {
            importModel.setActiveDetailModel(importModel.getDetailModels().get(selectedItem));
        }
    }

    protected void initGeneralSubTabView() {
        ScrollPanel generalPanel = new ScrollPanel();
        DetailModelProvider<VmListModel, VmGeneralModel> modelProvider =
                new DetailModelProvider<VmListModel, VmGeneralModel>() {
                    @Override
                    public VmGeneralModel getModel() {
                        return (VmGeneralModel) importModel.getDetailModels().get(0);
                    }

                    @Override
                    public void onSubTabSelected() {
                    }
                };
        generalView = new ImportVmGeneralSubTabView(modelProvider, constants);
        generalPanel.add(generalView);
        subTabLayoutPanel.add(generalPanel, constants.importVmGeneralSubTabLabel());
    }

    private void initTables() {
        initMainTable();
        initNicsTable();
        initDiskTable();
    }

    protected void initAppTable() {
        appTable = new ListModelObjectCellTable<String, VmAppListModel>();

        appTable.addColumn(new TextColumnWithTooltip<String>() {
            @Override
            public String getValue(String object) {
                return object;
            }
        }, constants.installedApp());

        appTable.getElement().getStyle().setPosition(Position.RELATIVE);

        ScrollPanel appPanel = new ScrollPanel();
        appPanel.add(appTable);
        subTabLayoutPanel.add(appPanel, constants.importVmApplicationslSubTabLabel());
    }

    protected void initMainTable() {
        this.table = new ListModelObjectCellTable<Object, ImportVmModel>();

        CheckboxColumn<Object> collapseSnapshotsColumn =
                new CheckboxColumn<Object>(new FieldUpdater<Object, Boolean>() {
            @Override
            public void update(int index, Object model, Boolean value) {
                        ((ImportVmData) model).getCollapseSnapshots().setEntity(value);
                        customSelectionCellFormatType.setEnabledWithToolTip(value,
                                constants.importAllocationModifiedCollapse());
                        diskTable.asEditor().edit(importModel.getImportDiskListModel());
                    }
                }) {
            @Override
            public Boolean getValue(Object model) {
                return (Boolean) ((ImportVmData) model).getCollapseSnapshots().getEntity();
            }

            @Override
            protected boolean canEdit(Object model) {
                return ((ImportVmData) model).getCollapseSnapshots().getIsChangable();
            }
                    @Override
                    protected String getDisabledMessage(Object model) {
                        return ((ImportVmData) model).getCollapseSnapshots().getChangeProhibitionReason();
                    }
        };
        table.addColumn(collapseSnapshotsColumn, constants.collapseSnapshots(), "10px"); //$NON-NLS-1$

        CheckboxColumn<Object> cloneVMColumn = new CheckboxColumn<Object>(new FieldUpdater<Object, Boolean>() {
            @Override
            public void update(int index, Object model, Boolean value) {
                ((ImportVmData) model).getClone().setEntity(value);
                table.asEditor().edit(importModel);
            }
        }) {
            @Override
            public Boolean getValue(Object model) {
                return (Boolean) ((ImportVmData) model).getClone().getEntity();
            }

            @Override
            protected boolean canEdit(Object model) {
                return ((ImportVmData) model).getClone().getIsChangable();
            }

            @Override
            protected String getDisabledMessage(Object model) {
                return ((ImportVmData) model).getClone().getChangeProhibitionReason();
            }
        };
        table.addColumn(cloneVMColumn, constants.cloneVM(), "50px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> nameColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                return ((ImportVmData) object).getVm().getName();
            }
        };
        table.addColumn(nameColumn, constants.nameVm(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> originColumn = new EnumColumn<Object, OriginType>() {
            @Override
            protected OriginType getRawValue(Object object) {
                return ((ImportVmData) object).getVm().getOrigin();
            }
        };
        table.addColumn(originColumn, constants.originVm(), "100px"); //$NON-NLS-1$

        table.addColumn(
                new WebAdminImageResourceColumn<Object>() {
                    @Override
                    public com.google.gwt.resources.client.ImageResource getValue(Object object) {
                        return new VmTypeColumn().getValue(((ImportVmData) object).getVm());
                    }
                }, constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> memoryColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                return String.valueOf(((ImportVmData) object).getVm().getVmMemSizeMb()) + " MB"; //$NON-NLS-1$
            }
        };
        table.addColumn(memoryColumn, constants.memoryVm(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> cpuColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                return String.valueOf(((ImportVmData) object).getVm().getNumOfCpus());
            }
        };
        table.addColumn(cpuColumn, constants.cpusVm(), "50px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> archColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                return String.valueOf(((ImportVmData) object).getVm().getClusterArch());
            }
        };
        table.addColumn(archColumn, constants.architectureVm(), "50px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> diskColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                return String.valueOf(((ImportVmData) object).getVm().getDiskMap().size());
            }
        };
        table.addColumn(diskColumn, constants.disksVm(), "50px"); //$NON-NLS-1$

        isObjectInSystemColumn = new ImageResourceColumn<Object>() {
            @Override
            public ImageResource getValue(Object object) {
                return ((ImportVmData) object).isExistsInSystem() ? getCommonResources().logNormalImage() : null;
            }
        };
        table.addColumn(isObjectInSystemColumn, constants.vmInSetup(), "60px"); //$NON-NLS-1$

        table.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                ImportVmData selectedObject =
                        ((SingleSelectionModel<ImportVmData>) event.getSource()).getSelectedObject();
                customSelectionCellFormatType.setEnabledWithToolTip((Boolean) selectedObject.getCollapseSnapshots()
                        .getEntity(),
                        constants.importAllocationModifiedCollapse());
                // diskTable.edit(importVmModel.getImportDiskListModel());
            }
        });

        ScrollPanel sp = new ScrollPanel();
        sp.add(table);
        splitLayoutPanel.add(sp);
        table.getElement().getStyle().setPosition(Position.RELATIVE);
    }

    private void initNicsTable() {
        nicTable = new ListModelObjectCellTable<VmNetworkInterface, SearchableListModel>();
        nicTable.enableColumnResizing();
        TextColumnWithTooltip<VmNetworkInterface> nameColumn = new TextColumnWithTooltip<VmNetworkInterface>() {
            @Override
            public String getValue(VmNetworkInterface object) {
                return object.getName();
            }
        };
        nicTable.addColumn(nameColumn, constants.nameInterface(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmNetworkInterface> networkColumn = new TextColumnWithTooltip<VmNetworkInterface>() {
            @Override
            public String getValue(VmNetworkInterface object) {
                return object.getNetworkName();
            }
        };
        nicTable.addColumn(networkColumn, constants.networkNameInterface(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmNetworkInterface> profileColumn = new TextColumnWithTooltip<VmNetworkInterface>() {
            @Override
            public String getValue(VmNetworkInterface object) {
                return object.getVnicProfileName();
            }
        };
        nicTable.addColumn(profileColumn, constants.profileNameInterface(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmNetworkInterface> typeColumn = new EnumColumn<VmNetworkInterface, VmInterfaceType>() {
            @Override
            protected VmInterfaceType getRawValue(VmNetworkInterface object) {
                return VmInterfaceType.forValue(object.getType());
            }
        };
        nicTable.addColumn(typeColumn, constants.typeInterface(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmNetworkInterface> macColumn = new TextColumnWithTooltip<VmNetworkInterface>() {
            @Override
            public String getValue(VmNetworkInterface object) {
                return object.getMacAddress();
            }
        };
        nicTable.addColumn(macColumn, constants.macInterface(), "150px"); //$NON-NLS-1$

        nicTable.getElement().getStyle().setPosition(Position.RELATIVE);
    }

    private void initDiskTable() {
        diskTable = new ListModelObjectCellTable<DiskImage, SearchableListModel>();
        diskTable.enableColumnResizing();
        TextColumnWithTooltip<DiskImage> aliasColumn = new TextColumnWithTooltip<DiskImage>() {
            @Override
            public String getValue(DiskImage object) {
                return object.getDiskAlias();
            }
        };
        diskTable.addColumn(aliasColumn, constants.aliasDisk(), "100px"); //$NON-NLS-1$

        ImageResourceColumn<DiskImage> bootableDiskColumn = new ImageResourceColumn<DiskImage>() {
            @Override
            public ImageResource getValue(DiskImage object) {
                setTitle(object.isBoot() ? getDefaultTitle() : null);
                return object.isBoot() ? getDefaultImage() : null;
            }

            @Override
            public String getDefaultTitle() {
                return constants.bootableDisk();
            }

            @Override
            public ImageResource getDefaultImage() {
                return resources.bootableDiskIcon();
            }
        };
        diskTable.addColumnWithHtmlHeader(bootableDiskColumn, bootableDiskColumn.getHeaderHtml(), "30px"); //$NON-NLS-1$

        DiskSizeColumn<DiskImage> sizeColumn = new DiskSizeColumn<DiskImage>() {
            @Override
            protected Long getRawValue(DiskImage object) {
                return object.getSize();
            }
        };
        diskTable.addColumn(sizeColumn, constants.provisionedSizeDisk(), "130px"); //$NON-NLS-1$

        DiskSizeColumn<DiskImage> actualSizeColumn = new DiskSizeColumn<DiskImage>() {
            @Override
            protected Long getRawValue(DiskImage object) {
                return object.getActualSizeInBytes();
            }
        };
        diskTable.addColumn(actualSizeColumn, constants.sizeDisk(), "130px"); //$NON-NLS-1$

        TextColumnWithTooltip<DiskImage> dateCreatedColumn = new FullDateTimeColumn<DiskImage>() {
            @Override
            protected Date getRawValue(DiskImage object) {
                return object.getCreationDate();
            }
        };
        diskTable.addColumn(dateCreatedColumn, constants.dateCreatedInterface(), "120px"); //$NON-NLS-1$

        diskTable.setSelectionModel(new NoSelectionModel<DiskImage>());

        addAllocationColumn();

        diskTable.getElement().getStyle().setPosition(Position.RELATIVE);
    }

    protected void addAllocationColumn() {
        ArrayList<String> allocationTypes = new ArrayList<String>();
        allocationTypes.add(constants.thinAllocation());
        allocationTypes.add(constants.preallocatedAllocation());

        customSelectionCellFormatType = new CustomSelectionCell(allocationTypes);
        customSelectionCellFormatType.setStyle(style.cellSelectBox());

        Column<DiskImage, String> allocationColumn = new Column<DiskImage, String>(
                customSelectionCellFormatType) {
            @Override
            public String getValue(DiskImage disk) {
                ImportDiskData importData =
                        importModel.getDiskImportData(disk.getId());
                if (importData == null) {
                    return "";
                }
                return new EnumRenderer<VolumeType>().render(VolumeType.forValue(importData.getSelectedVolumeType()
                        .getValue()));
            }
        };

        allocationColumn.setFieldUpdater(new FieldUpdater<DiskImage, String>() {
            @Override
            public void update(int index, DiskImage disk, String value) {
                VolumeType tempVolumeType = VolumeType.Sparse;
                if (value.equals(constants.thinAllocation())) {
                    tempVolumeType = VolumeType.Sparse;
                } else if (value.equals(constants.preallocatedAllocation())) {
                    tempVolumeType = VolumeType.Preallocated;
                }
                ImportDiskData importData =
                        importModel.getDiskImportData(disk.getId());
                if (importData != null) {
                    importData.setSelectedVolumeType(tempVolumeType);
                }
            }
        });

        diskTable.addColumn(allocationColumn, constants.allocationDisk(), "150px"); //$NON-NLS-1$
    }

    private void addStorageDomainsColumn() {
        customSelectionCellStorageDomain = new CustomSelectionCell(new ArrayList<String>());
        customSelectionCellStorageDomain.setStyle(style.cellSelectBox());

        storageDomainsColumn = new Column<DiskImage, String>(customSelectionCellStorageDomain) {
            @Override
            public String getValue(DiskImage disk) {
                ImportDiskData importData = importModel.getDiskImportData(disk.getId());

                ArrayList<String> storageDomainsNameList = new ArrayList<String>();
                StorageDomain selectedStorageDomain = null;
                if (importData != null && importData.getStorageDomains() != null) {
                    for (StorageDomain storageDomain : importData.getStorageDomains()) {
                        storageDomainsNameList.add(
                                new StorageDomainFreeSpaceRenderer<StorageDomain>().render(storageDomain));
                        if (importData.getSelectedStorageDomain() != null) {
                            if (storageDomain.getId().equals(importData.getSelectedStorageDomain().getId())) {
                                selectedStorageDomain = storageDomain;
                            }
                        }
                    }
                }
                ((CustomSelectionCell) getCell()).setOptions(storageDomainsNameList);
                if (!storageDomainsNameList.isEmpty()) {
                    if (selectedStorageDomain != null) {
                        return new StorageDomainFreeSpaceRenderer<StorageDomain>().render(selectedStorageDomain);
                    } else {
                        return storageDomainsNameList.get(0);
                    }
                }
                return "";
            }
        };

        storageDomainsColumn.setFieldUpdater(new FieldUpdater<DiskImage, String>() {
            @Override
            public void update(int index, DiskImage disk, String value) {
                String storageDomainName = value.substring(0, value.lastIndexOf(" (")); //$NON-NLS-1$
                importModel.getDiskImportData(disk.getId()).setSelectedStorageDomainString(storageDomainName);
                diskTable.asEditor().edit(importModel.getImportDiskListModel());
            }
        });

        diskTable.addColumn(storageDomainsColumn, constants.storageDomainDisk(), "180px"); //$NON-NLS-1$

    }

    private void addStorageQuotaColumn() {
        if (!importModel.isQuotaEnabled()) {
            return;
        }

        if (quotaColumn != null) {
            return;
        }

        customSelectionCellQuota = new CustomSelectionCell(new ArrayList<String>());
        customSelectionCellQuota.setStyle(style.cellSelectBox());

        quotaColumn = new Column<DiskImage, String>(customSelectionCellQuota) {
            @Override
            public String getValue(DiskImage disk) {
                ImportDiskData importData = importModel.getDiskImportData(disk.getId());

                ArrayList<String> storageQuotaList = new ArrayList<String>();
                Quota selectedQuota = null;
                if (importData != null && importData.getQuotaList() != null) {
                    for (Quota quota : importData.getQuotaList()) {
                        storageQuotaList.add(quota.getQuotaName());
                        if (importData.getSelectedQuota() != null) {
                            if (quota.getId().equals(importData.getSelectedQuota().getId())) {
                                selectedQuota = quota;
                            }
                        }
                    }
                }
                ((CustomSelectionCell) getCell()).setOptions(storageQuotaList);
                if (!storageQuotaList.isEmpty()) {
                    if (selectedQuota != null) {
                        return selectedQuota.getQuotaName();
                    } else {
                        return storageQuotaList.get(0);
                    }
                }
                return "";
            }
        };

        quotaColumn.setFieldUpdater(new FieldUpdater<DiskImage, String>() {
            @Override
            public void update(int index, DiskImage disk, String value) {
                importModel.getDiskImportData(disk.getId()).setSelectedQuotaString(value);
            }
        });
        diskTable.addColumn(quotaColumn, constants.quota(), "100px"); //$NON-NLS-1$
    }

    private void initListBoxEditors() {
        destClusterEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((VDSGroup) object).getName();
            }
        });
        destClusterQuotaEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((Quota) object).getQuotaName();
            }
        });
        destStorageEditor = new ListModelListBoxEditor<Object>(new StorageDomainFreeSpaceRenderer());

        cpuProfileEditor = new ListModelListBoxEditor<CpuProfile>(new NullSafeRenderer<CpuProfile>() {

            @Override
            protected String renderNullSafe(CpuProfile object) {
                return object.getName();
            }
        });
    }

    private void localize(ApplicationConstants constants) {
        destClusterEditor.setLabel(constants.importVm_destCluster());
        destClusterQuotaEditor.setLabel(constants.importVm_destClusterQuota());
        destStorageEditor.setLabel(constants.defaultStorage());
        cpuProfileEditor.setLabel(constants.cpuProfileLabel());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void edit(final ImportVmModel object) {
        this.importModel = object;
        table.asEditor().edit(object);

        if (object.getDisksToConvert() != null) {
            table.addColumnAt(new IsProblematicImportVmColumn(object.getDisksToConvert()), "", "30px", 0); //$NON-NLS-1$ //$NON-NLS-2$
        }

        table.asEditor().edit(object);

        addStorageDomainsColumn();

        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (args instanceof PropertyChangedEventArgs
                        && ((PropertyChangedEventArgs) args).propertyName.equals(object.ON_DISK_LOAD)) {
                    addStorageQuotaColumn();
                    table.redraw();
                    diskTable.asEditor().edit(object.getImportDiskListModel());
                } else if (args instanceof PropertyChangedEventArgs
                        && ((PropertyChangedEventArgs) args).propertyName.equals("Message")) { ////$NON-NLS-1$
                    message.setText(object.getMessage());
                }
            }
        });

        SingleSelectionModel<Object> selectionModel =
                (SingleSelectionModel<Object>) table.getSelectionModel();
        selectionModel.addSelectionChangeHandler(new Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (!firstSelection) {
                    object.setActiveDetailModel(object.getDetailModels().get(0));
                    setGeneralViewSelection(((ImportEntityData) object.getSelectedItem()).getEntity());
                    firstSelection = true;
                }
                splitLayoutPanel.clear();
                splitLayoutPanel.addSouth(subTabLayoutPanel, 230);
                ScrollPanel sp = new ScrollPanel();
                sp.add(table);
                splitLayoutPanel.add(sp);
                table.getElement().getStyle().setPosition(Position.RELATIVE);
                // subTabLayoutPanel.selectTab(0);
            }

        });

        initSubTabLayoutPanel();
        nicTable.asEditor().edit((SearchableListModel) object.getDetailModels().get(1));
        diskTable.asEditor().edit((SearchableListModel) object.getDetailModels().get(2));
        if (object.getDetailModels().size() > 3) {
            appTable.asEditor().edit((VmAppListModel) object.getDetailModels().get(3));
        }

        driver.edit(object);
    }

    protected void setGeneralViewSelection(Object selectedItem) {
        generalView.setMainTabSelectedItem((VM) selectedItem);
    }

    @Override
    public ImportVmModel flush() {
        table.flush();
        nicTable.flush();
        diskTable.flush();
        if (appTable != null) {
            appTable.flush();
        }
        return driver.flush();
    }

    interface WidgetStyle extends CssResource {
        String checkboxEditor();

        String collapseEditor();

        String cellSelectBox();
    }

}
