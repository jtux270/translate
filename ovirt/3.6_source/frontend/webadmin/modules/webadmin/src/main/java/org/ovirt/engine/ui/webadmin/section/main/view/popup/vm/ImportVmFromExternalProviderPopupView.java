package org.ovirt.engine.ui.webadmin.section.main.view.popup.vm;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VnicProfileView;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelObjectCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.renderer.StorageDomainFreeSpaceRenderer;
import org.ovirt.engine.ui.common.widget.renderer.StringRenderer;
import org.ovirt.engine.ui.common.widget.table.column.AbstractCheckboxColumn;
import org.ovirt.engine.ui.common.widget.table.column.AbstractDiskSizeColumn;
import org.ovirt.engine.ui.common.widget.table.column.AbstractEnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.AbstractImageResourceColumn;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.common.widget.table.header.ImageResourceHeader;
import org.ovirt.engine.ui.common.widget.uicommon.disks.DisksViewColumns;
import org.ovirt.engine.ui.uicommonweb.models.HasEntity;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportNetworkData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportSource;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmFromExternalProviderModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmImportGeneralModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.ImportVmFromExternalProviderPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.storage.backup.ImportVmGeneralSubTabView;
import org.ovirt.engine.ui.webadmin.widget.table.cell.CustomSelectionCell;
import org.ovirt.engine.ui.webadmin.widget.table.column.VmTypeColumn;

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
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
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

public class ImportVmFromExternalProviderPopupView extends AbstractModelBoundPopupView<ImportVmFromExternalProviderModel> implements ImportVmFromExternalProviderPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ImportVmFromExternalProviderModel, ImportVmFromExternalProviderPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ImportVmFromExternalProviderPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @Path(value = "cluster.selectedItem")
    ListModelListBoxEditor<VDSGroup> destClusterEditor;

    @UiField(provided = true)
    @Path(value = "cpuProfiles.selectedItem")
    ListModelListBoxEditor<CpuProfile> cpuProfileEditor;

    @UiField(provided = true)
    @Path(value = "clusterQuota.selectedItem")
    ListModelListBoxEditor<Quota> destClusterQuotaEditor;

    @UiField(provided = true)
    @Path(value = "storage.selectedItem")
    ListModelListBoxEditor<StorageDomain> destStorageEditor;

    @UiField(provided = true)
    @Path(value = "allocation.selectedItem")
    ListModelListBoxEditor<VolumeType> disksAllocationEditor;

    @UiField(provided = true)
    @Path(value = "iso.selectedItem")
    @WithElementId("iso")
    public ListModelListBoxOnlyEditor<String> cdImageEditor;

    @UiField(provided = true)
    @Path(value = "attachDrivers.entity")
    @WithElementId("attachDrivers")
    public EntityModelCheckBoxEditor attachDriversEditor;

    @UiField
    SplitLayoutPanel splitLayoutPanel;

    @UiField
    @Ignore
    Label message;

    @Ignore
    protected ListModelObjectCellTable<ImportVmData, ImportVmFromExternalProviderModel> table;

    @Ignore
    private ListModelObjectCellTable<DiskImage, SearchableListModel> diskTable;

    @Ignore
    private ListModelObjectCellTable<VmNetworkInterface, SearchableListModel> nicTable;

    @Ignore
    protected TabLayoutPanel subTabLayoutPanel = null;

    boolean firstSelection = false;

    private ImportVmGeneralSubTabView generalView;

    private CustomSelectionCell customSelectionCellFormatType;

    private CustomSelectionCell customSelectionCellNetwork;

    protected ImportVmFromExternalProviderModel importModel;

    private final Driver driver = GWT.create(Driver.class);

    protected final ApplicationConstants constants;

    protected final ApplicationResources resources;

    @Inject
    public ImportVmFromExternalProviderPopupView(EventBus eventBus, ApplicationResources resources,
            ApplicationConstants constants) {
        super(eventBus);
        this.constants = constants;
        this.resources = resources;

        initListBoxEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        applyStyles();
        localize(constants);
        initTables();
        driver.initialize(this);
    }

    protected void applyStyles() {
        attachDriversEditor.addContentWidgetContainerStyleName(style.cdAttachedLabelWidth());
    }

    private void initTables() {
        initMainTable();
        initNicsTable();
        initDiskTable();
    }

    protected void initMainTable() {
        this.table = new ListModelObjectCellTable<ImportVmData, ImportVmFromExternalProviderModel>();

        AbstractCheckboxColumn<ImportVmData> cloneVMColumn = new AbstractCheckboxColumn<ImportVmData>(new FieldUpdater<ImportVmData, Boolean>() {
            @Override
            public void update(int index, ImportVmData model, Boolean value) {
                ((ImportVmData) model).getClone().setEntity(value);
                table.asEditor().edit(importModel);
            }
        }) {
            @Override
            public Boolean getValue(ImportVmData model) {
                return (Boolean) ((ImportVmData) model).getClone().getEntity();
            }

            @Override
            protected boolean canEdit(ImportVmData model) {
                return ((ImportVmData) model).getClone().getIsChangable();
            }

            @Override
            protected String getDisabledMessage(ImportVmData model) {
                return ((ImportVmData) model).getClone().getChangeProhibitionReason();
            }
        };
        table.addColumn(cloneVMColumn, constants.cloneVM(), "50px"); //$NON-NLS-1$

        AbstractTextColumn<ImportVmData> nameColumn = new AbstractTextColumn<ImportVmData>() {
            @Override
            public String getValue(ImportVmData object) {
                return object.getName();
            }
        };
        table.addColumn(nameColumn, constants.nameVm(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<ImportVmData> originColumn = new AbstractEnumColumn<ImportVmData, OriginType>() {
            @Override
            protected OriginType getRawValue(ImportVmData object) {
                return ((ImportVmData) object).getVm().getOrigin();
            }
        };
        table.addColumn(originColumn, constants.originVm(), "100px"); //$NON-NLS-1$

        table.addColumn(
                new AbstractImageResourceColumn<ImportVmData>() {
                    @Override
                    public com.google.gwt.resources.client.ImageResource getValue(ImportVmData object) {
                        return new VmTypeColumn().getValue(((ImportVmData) object).getVm());
                    }
                }, constants.empty(), "30px"); //$NON-NLS-1$

        AbstractTextColumn<ImportVmData> memoryColumn = new AbstractTextColumn<ImportVmData>() {
            @Override
            public String getValue(ImportVmData object) {
                return String.valueOf(((ImportVmData) object).getVm().getVmMemSizeMb()) + " MB"; //$NON-NLS-1$
            }
        };
        table.addColumn(memoryColumn, constants.memoryVm(), "100px"); //$NON-NLS-1$

        AbstractTextColumn<ImportVmData> cpuColumn = new AbstractTextColumn<ImportVmData>() {
            @Override
            public String getValue(ImportVmData object) {
                return String.valueOf(((ImportVmData) object).getVm().getNumOfCpus());
            }
        };
        table.addColumn(cpuColumn, constants.cpusVm(), "50px"); //$NON-NLS-1$

        AbstractTextColumn<ImportVmData> archColumn = new AbstractTextColumn<ImportVmData>() {
            @Override
            public String getValue(ImportVmData object) {
                return String.valueOf(((ImportVmData) object).getVm().getClusterArch());
            }
        };
        table.addColumn(archColumn, constants.architectureVm(), "50px"); //$NON-NLS-1$

        AbstractTextColumn<ImportVmData> diskColumn = new AbstractTextColumn<ImportVmData>() {
            @Override
            public String getValue(ImportVmData object) {
                return String.valueOf(((ImportVmData) object).getVm().getDiskMap().size());
            }
        };
        table.addColumn(diskColumn, constants.disksVm(), "50px"); //$NON-NLS-1$

        ScrollPanel sp = new ScrollPanel();
        sp.add(table);
        splitLayoutPanel.add(sp);
        table.getElement().getStyle().setPosition(Position.RELATIVE);
    }


    private void localize(ApplicationConstants constants) {
        destClusterEditor.setLabel(constants.importVm_destCluster());
        destClusterQuotaEditor.setLabel(constants.importVm_destClusterQuota());
        destStorageEditor.setLabel(constants.storageDomainDisk());
        cpuProfileEditor.setLabel(constants.cpuProfileLabel());
        disksAllocationEditor.setLabel(constants.allocationDisk());
        attachDriversEditor.setLabel(constants.attachVirtioDrivers());
    }

    private void initListBoxEditors() {
        destClusterEditor = new ListModelListBoxEditor<VDSGroup>(new NullSafeRenderer<VDSGroup>() {
            @Override
            public String renderNullSafe(VDSGroup object) {
                return object.getName();
            }
        });
        destClusterQuotaEditor = new ListModelListBoxEditor<Quota>(new NullSafeRenderer<Quota>() {
            @Override
            public String renderNullSafe(Quota object) {
                return object.getQuotaName();
            }
        });
        destStorageEditor = new ListModelListBoxEditor<StorageDomain>(new StorageDomainFreeSpaceRenderer());

        cpuProfileEditor = new ListModelListBoxEditor<CpuProfile>(new NullSafeRenderer<CpuProfile>() {

            @Override
            protected String renderNullSafe(CpuProfile object) {
                return object.getName();
            }
        });

        disksAllocationEditor = new ListModelListBoxEditor<VolumeType>(new NullSafeRenderer<VolumeType>() {
            @Override
            protected String renderNullSafe(VolumeType object) {
                return new EnumRenderer<VolumeType>().render(object);
            }
        });

        attachDriversEditor = new EntityModelCheckBoxEditor(Align.LEFT);
        cdImageEditor = new ListModelListBoxOnlyEditor<>(new StringRenderer<String>());
    }

    @Override
    public void edit(final ImportVmFromExternalProviderModel importModel) {
        this.importModel = importModel;
        table.asEditor().edit(importModel);

        importModel.getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if (args.propertyName.equals(importModel.ON_DISK_LOAD)) {
                    table.asEditor().edit(table.asEditor().flush());
                } else if (args.propertyName.equals("Message")) { ////$NON-NLS-1$
                    message.setText(importModel.getMessage());
                }
            }
        });

        SingleSelectionModel<Object> selectionModel =
                (SingleSelectionModel<Object>) table.getSelectionModel();
        selectionModel.addSelectionChangeHandler(new Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (!firstSelection) {
                    importModel.setActiveDetailModel((HasEntity<?>) importModel.getDetailModels().get(0));
                    setGeneralViewSelection(((ImportEntityData) importModel.getSelectedItem()).getEntity());
                    firstSelection = true;
                }
                splitLayoutPanel.clear();
                splitLayoutPanel.addSouth(subTabLayoutPanel, 230);
                ScrollPanel sp = new ScrollPanel();
                sp.add(table);
                splitLayoutPanel.add(sp);
                table.getElement().getStyle().setPosition(Position.RELATIVE);
            }

        });

        initSubTabLayoutPanel();
        nicTable.asEditor().edit((SearchableListModel) importModel.getDetailModels().get(1));
        diskTable.asEditor().edit((SearchableListModel) importModel.getDetailModels().get(2));

        driver.edit(importModel);
    }

    private void addNetworkColumn() {
        customSelectionCellNetwork = new CustomSelectionCell(new ArrayList<String>());
        customSelectionCellNetwork.setStyle(style.cellSelectBox());

        Column<VmNetworkInterface, String> networkColumn = new Column<VmNetworkInterface, String>(customSelectionCellNetwork) {
            @Override
            public String getValue(VmNetworkInterface iface) {
                ImportNetworkData importNetworkData = importModel.getNetworkImportData(iface);
                List<String> networkNames = importNetworkData.getNetworkNames();
                ((CustomSelectionCell) getCell()).setOptions(networkNames);
                if (networkNames.isEmpty()) {
                    return ""; //$NON-NLS-1$
                }
                String selectedNetworkName = importNetworkData.getSelectedNetworkName();
                return selectedNetworkName != null ? selectedNetworkName : networkNames.get(0);
            }
        };

        networkColumn.setFieldUpdater(new FieldUpdater<VmNetworkInterface, String>() {
            @Override
            public void update(int index, VmNetworkInterface iface, String value) {
                importModel.getNetworkImportData(iface).setSelectedNetworkName(value);
                nicTable.asEditor().edit(importModel.getImportNetworkInterfaceListModel());
            }
        });

        nicTable.addColumn(networkColumn, constants.networkNameInterface(), "150px"); //$NON-NLS-1$
    }

    private void addNetworkProfileColumn() {
        customSelectionCellNetwork = new CustomSelectionCell(new ArrayList<String>());
        customSelectionCellNetwork.setStyle(style.cellSelectBox());

        Column<VmNetworkInterface, String> profileColumn = new Column<VmNetworkInterface, String>(customSelectionCellNetwork) {
            @Override
            public String getValue(VmNetworkInterface iface) {
                ImportNetworkData importNetworkData = importModel.getNetworkImportData(iface);
                List<String> networkProfileNames = new ArrayList<>();
                for (VnicProfileView networkProfile : importNetworkData.getFilteredNetworkProfiles()) {
                    networkProfileNames.add(networkProfile.getName());
                }
                ((CustomSelectionCell) getCell()).setOptions(networkProfileNames);
                if (networkProfileNames.isEmpty()) {
                    return ""; //$NON-NLS-1$
                }
                VnicProfileView selectedNetworkProfile = importModel.getNetworkImportData(iface).getSelectedNetworkProfile();
                return selectedNetworkProfile != null ? selectedNetworkProfile.getName() : networkProfileNames.get(0);
            }
        };

        profileColumn.setFieldUpdater(new FieldUpdater<VmNetworkInterface, String>() {
            @Override
            public void update(int index, VmNetworkInterface iface, String value) {
                importModel.getNetworkImportData(iface).setSelectedNetworkProfile(value);
            }
        });

        nicTable.addColumn(profileColumn, constants.profileNameInterface(), "150px"); //$NON-NLS-1$
    }

    protected void setGeneralViewSelection(Object selectedItem) {
        generalView.setMainTabSelectedItem((VM) selectedItem);
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
        }
    }

    protected void subTabLayoutPanelSelectionChanged(Integer selectedItem) {
        if (importModel != null) {
            importModel.setActiveDetailModel((HasEntity<?>) importModel.getDetailModels().get(selectedItem));
        }
    }

    protected void initGeneralSubTabView() {
        ScrollPanel generalPanel = new ScrollPanel();
        DetailModelProvider<ImportVmModel, VmImportGeneralModel> modelProvider =
                new DetailModelProvider<ImportVmModel, VmImportGeneralModel>() {
                    @Override
                    public VmImportGeneralModel getModel() {
                        VmImportGeneralModel model = (VmImportGeneralModel) importModel.getDetailModels().get(0);
                        model.setSource(ImportSource.VMWARE);
                        return model;
                    }

                    @Override
                    public void onSubTabSelected() {
                    }

                    @Override
                    public void onSubTabDeselected() {
                    }
                };
        generalView = new ImportVmGeneralSubTabView(modelProvider);
        generalPanel.add(generalView);
        subTabLayoutPanel.add(generalPanel, constants.importVmGeneralSubTabLabel());
    }

    @Override
    public ImportVmFromExternalProviderModel flush() {
        return driver.flush();
    }

    private void initNicsTable() {
        nicTable = new ListModelObjectCellTable<VmNetworkInterface, SearchableListModel>();
        nicTable.enableColumnResizing();
        AbstractTextColumn<VmNetworkInterface> nameColumn = new AbstractTextColumn<VmNetworkInterface>() {
            @Override
            public String getValue(VmNetworkInterface object) {
                return object.getName();
            }
        };
        nicTable.addColumn(nameColumn, constants.nameInterface(), "125px"); //$NON-NLS-1$

        AbstractTextColumn<VmNetworkInterface> originalNetworkNameColumn = new AbstractTextColumn<VmNetworkInterface>() {
            @Override
            public String getValue(VmNetworkInterface object) {
                return object.getRemoteNetworkName();
            }
        };
        nicTable.addColumn(originalNetworkNameColumn, constants.originalNetworkNameInterface(), "160px"); //$NON-NLS-1$

        addNetworkColumn();
        addNetworkProfileColumn();

        AbstractTextColumn<VmNetworkInterface> typeColumn = new AbstractEnumColumn<VmNetworkInterface, VmInterfaceType>() {
            @Override
            protected VmInterfaceType getRawValue(VmNetworkInterface object) {
                return VmInterfaceType.forValue(object.getType());
            }
        };
        nicTable.addColumn(typeColumn, constants.typeInterface(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<VmNetworkInterface> macColumn = new AbstractTextColumn<VmNetworkInterface>() {
            @Override
            public String getValue(VmNetworkInterface object) {
                return object.getMacAddress();
            }
        };
        nicTable.addColumn(macColumn, constants.macInterface(), "150px"); //$NON-NLS-1$

        nicTable.getElement().getStyle().setPosition(Position.RELATIVE);

        nicTable.setSelectionModel(new NoSelectionModel<VmNetworkInterface>());
    }

    private void initDiskTable() {
        diskTable = new ListModelObjectCellTable<DiskImage, SearchableListModel>();
        diskTable.enableColumnResizing();
        AbstractTextColumn<DiskImage> aliasColumn = new AbstractTextColumn<DiskImage>() {
            @Override
            public String getValue(DiskImage object) {
                return object.getDiskAlias();
            }
        };
        diskTable.addColumn(aliasColumn, constants.aliasDisk(), "300px"); //$NON-NLS-1$

        AbstractImageResourceColumn<DiskImage> bootableDiskColumn = new AbstractImageResourceColumn<DiskImage>() {
            @Override
            public ImageResource getValue(DiskImage object) {
                return object.isBoot() ? getDefaultImage() : null;
            }

            @Override
            public ImageResource getDefaultImage() {
                return resources.bootableDiskIcon();
            }

            @Override
            public SafeHtml getTooltip(DiskImage object) {
                if (object.isBoot()) {
                    return SafeHtmlUtils.fromSafeConstant(constants.bootableDisk());
                }
                return null;
            }
        };
        diskTable.addColumn(bootableDiskColumn,
                new ImageResourceHeader(DisksViewColumns.bootableDiskColumn.getDefaultImage(),
                        SafeHtmlUtils.fromSafeConstant(constants.bootableDisk())),
                        "30px"); //$NON-NLS-1$

        AbstractDiskSizeColumn<DiskImage> sizeColumn = new AbstractDiskSizeColumn<DiskImage>() {
            @Override
            protected Long getRawValue(DiskImage object) {
                return object.getSize();
            }
        };
        diskTable.addColumn(sizeColumn, constants.provisionedSizeDisk(), "130px"); //$NON-NLS-1$

        AbstractDiskSizeColumn<DiskImage> actualSizeColumn = new AbstractDiskSizeColumn<DiskImage>() {
            @Override
            protected Long getRawValue(DiskImage object) {
                return object.getActualSizeInBytes();
            }
        };
        diskTable.addColumn(actualSizeColumn, constants.sizeDisk(), "130px"); //$NON-NLS-1$

        diskTable.setSelectionModel(new NoSelectionModel<DiskImage>());

        diskTable.getElement().getStyle().setPosition(Position.RELATIVE);
    }

    interface WidgetStyle extends CssResource {
        String cellSelectBox();

        String cdAttachedLabelWidth();
    }
}
