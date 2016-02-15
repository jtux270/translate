package org.ovirt.engine.ui.common.widget.uicommon.popup.vm;

import java.util.ArrayList;
import java.util.Collection;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.profiles.DiskProfile;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.RadioButtonsHorizontalPanel;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NameRenderer;
import org.ovirt.engine.ui.common.widget.renderer.StorageDomainFreeSpaceRenderer;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.common.widget.uicommon.storage.AbstractStorageView;
import org.ovirt.engine.ui.common.widget.uicommon.storage.FcpStorageView;
import org.ovirt.engine.ui.common.widget.uicommon.storage.IscsiStorageView;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.FcpStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.IStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.IscsiStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.NewEditStorageModelBehavior;
import org.ovirt.engine.ui.uicommonweb.models.storage.SanStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.AbstractDiskModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class VmDiskPopupWidget extends AbstractModelBoundPopupWidget<AbstractDiskModel> {

    interface Driver extends SimpleBeanEditorDriver<AbstractDiskModel, VmDiskPopupWidget> {
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, VmDiskPopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<VmDiskPopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    @Path("size.entity")
    @WithElementId("size")
    IntegerEntityModelTextBoxEditor sizeEditor;

    @UiField
    @Path("sizeExtend.entity")
    StringEntityModelTextBoxEditor sizeExtendEditor;

    @UiField
    @Path("alias.entity")
    @WithElementId("alias")
    StringEntityModelTextBoxEditor aliasEditor;

    @UiField
    @Path("description.entity")
    @WithElementId("description")
    StringEntityModelTextBoxEditor descriptionEditor;

    @UiField(provided = true)
    @Path("diskInterface.selectedItem")
    @WithElementId("interface")
    ListModelListBoxEditor<DiskInterface> interfaceEditor;

    @UiField(provided = true)
    @Path("dataCenter.selectedItem")
    @WithElementId("dataCenter")
    ListModelListBoxEditor<StoragePool> datacenterEditor;

    @UiField(provided = true)
    @Path("storageDomain.selectedItem")
    @WithElementId("storageDomain")
    ListModelListBoxEditor<StorageDomain> storageDomainEditor;

    @UiField(provided = true)
    @Path("volumeType.selectedItem")
    @WithElementId("volumeType")
    ListModelListBoxEditor<VolumeType> volumeTypeEditor;

    @UiField(provided = true)
    @Path("cinderVolumeType.selectedItem")
    @WithElementId("cinderVolumeType")
    ListModelListBoxEditor<String> cinderVolumeTypeEditor;

    @UiField(provided = true)
    @Path("diskProfile.selectedItem")
    @WithElementId("diskProfile")
    ListModelListBoxEditor<DiskProfile> diskProfileEditor;

    @UiField(provided = true)
    @Path("quota.selectedItem")
    @WithElementId("quota")
    ListModelListBoxEditor<Quota> quotaEditor;

    @UiField(provided = true)
    @Path(value = "host.selectedItem")
    @WithElementId("host")
    ListModelListBoxEditor<VDS> hostListEditor;

    @UiField(provided = true)
    @Path(value = "storageType.selectedItem")
    @WithElementId("storageType")
    ListModelListBoxEditor<StorageType> storageTypeEditor;

    @UiField(provided = true)
    @Path(value = "isPlugged.entity")
    @WithElementId("plugDiskToVm")
    EntityModelCheckBoxEditor plugDiskToVmEditor;

    @UiField(provided = true)
    @Path("isWipeAfterDelete.entity")
    @WithElementId("wipeAfterDelete")
    EntityModelCheckBoxEditor wipeAfterDeleteEditor;

    @UiField(provided = true)
    @Path("isBootable.entity")
    @WithElementId("isBootable")
    EntityModelCheckBoxEditor isBootableEditor;

    @UiField(provided = true)
    @Path("isShareable.entity")
    @WithElementId("isShareable")
    EntityModelCheckBoxEditor isShareableEditor;

    @UiField(provided = true)
    @Path("isReadOnly.entity")
    @WithElementId("isReadOnly")
    EntityModelCheckBoxEditor isReadOnlyEditor;

    @UiField(provided = true)
    @Path("isUsingScsiReservation.entity")
    @WithElementId("isUsingScsiReservation")
    EntityModelCheckBoxEditor isUsingScsiReservationEditor;

    @UiField(provided = true)
    @Path("isScsiPassthrough.entity")
    @WithElementId("isScsiPassthrough")
    EntityModelCheckBoxEditor isScsiPassthroughEditor;

    @UiField(provided = true)
    @Path("isSgIoUnfiltered.entity")
    @WithElementId("isSgIoUnfiltered")
    EntityModelCheckBoxEditor isSgIoUnfilteredEditor;

    @UiField(provided = true)
    @Ignore
    InfoIcon interfaceInfoIcon;

    @UiField(provided = true)
    @Ignore
    InfoIcon cinderVolumeTypeInfoIcon;

    @UiField(provided = true)
    @Ignore
    InfoIcon scsiReservationInfoIcon;

    @UiField
    VerticalPanel createDiskPanel;

    @UiField
    FlowPanel externalDiskPanel;

    @UiField
    RadioButtonsHorizontalPanel diskTypePanel;

    @UiField
    Label message;

    @Ignore
    IscsiStorageView iscsiStorageView;

    @Ignore
    FcpStorageView fcpStorageView;

    @Ignore
    AbstractStorageView storageView;

    private final static CommonApplicationTemplates templates = AssetProvider.getTemplates();
    @UiField(provided=true)
    final static CommonApplicationConstants constants = AssetProvider.getConstants();

    private final Driver driver = GWT.create(Driver.class);

    boolean isNewLunDiskEnabled;
    StorageModel storageModel;
    IscsiStorageModel iscsiStorageModel;
    FcpStorageModel fcpStorageModel;
    SanStorageModel sanStorageModel;

    public VmDiskPopupWidget(boolean isLunDiskEnabled) {
        this.isNewLunDiskEnabled = isLunDiskEnabled;
        initManualWidgets();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize();
        ViewIdHandler.idHandler.generateAndSetIds(this);
        driver.initialize(this);
    }

    private void localize() {
        aliasEditor.setLabel(constants.aliasVmDiskPopup());
        sizeEditor.setLabel(constants.sizeVmDiskPopup());
        sizeExtendEditor.setLabel(constants.extendImageSizeBy());
        descriptionEditor.setLabel(constants.descriptionVmDiskPopup());
        datacenterEditor.setLabel(constants.dcVmDiskPopup());
        storageDomainEditor.setLabel(constants.storageDomainVmDiskPopup());
        hostListEditor.setLabel(constants.hostVmDiskPopup());
        quotaEditor.setLabel(constants.quotaVmDiskPopup());
        diskProfileEditor.setLabel(constants.diskProfileVmDiskPopup());
        interfaceEditor.setLabel(constants.interfaceVmDiskPopup());
        volumeTypeEditor.setLabel(constants.allocationDisk());
        cinderVolumeTypeEditor.setLabel(constants.cinderVolumeTypeDisk());
        storageTypeEditor.setLabel(constants.storageTypeVmDiskPopup());
        plugDiskToVmEditor.setLabel(constants.activateVmDiskPopup());
        wipeAfterDeleteEditor.setLabel(constants.wipeAfterDeleteVmDiskPopup());
        isBootableEditor.setLabel(constants.isBootableVmDiskPopup());
        isShareableEditor.setLabel(constants.isShareableVmDiskPopup());
        isReadOnlyEditor.setLabel(constants.isReadOnlyVmDiskPopup());
        isUsingScsiReservationEditor.setLabel(constants.isUsingScsiReservationEditor());
        isScsiPassthroughEditor.setLabel(constants.isScsiPassthroughEditor());
        isSgIoUnfilteredEditor.setLabel(constants.isSgIoUnfilteredEditor());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initManualWidgets() {
        storageDomainEditor = new ListModelListBoxEditor<>(new StorageDomainFreeSpaceRenderer());

        hostListEditor = new ListModelListBoxEditor<>(new NameRenderer<VDS>());

        diskProfileEditor = new ListModelListBoxEditor<>(new NameRenderer<DiskProfile>());

        quotaEditor = new ListModelListBoxEditor<>(new NameRenderer<Quota>());

        interfaceEditor = new ListModelListBoxEditor<>(new EnumRenderer());

        datacenterEditor = new ListModelListBoxEditor<>(new NameRenderer<StoragePool>());

        cinderVolumeTypeEditor = new ListModelListBoxEditor<>();

        volumeTypeEditor = new ListModelListBoxEditor<>(new EnumRenderer());
        storageTypeEditor = new ListModelListBoxEditor<>(new EnumRenderer());
        plugDiskToVmEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        wipeAfterDeleteEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isBootableEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isShareableEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isReadOnlyEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isUsingScsiReservationEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isScsiPassthroughEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isSgIoUnfilteredEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        interfaceInfoIcon = new InfoIcon(templates.italicText(constants.diskInterfaceInfo()));
        cinderVolumeTypeInfoIcon = new InfoIcon(templates.italicText(constants.cinderVolumeTypeInfoIcon()));
        scsiReservationInfoIcon = new InfoIcon(templates.italicText(constants.scsiReservationInfoIcon()));
    }

    @Override
    public void focusInput() {
        sizeEditor.setFocus(true);
    }

    @Override
    public void edit(final AbstractDiskModel disk) {
        driver.edit(disk);

        disk.getIsDirectLunDiskAvaialable().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                boolean isDirectLunDiskAvaialable = ((EntityModel<Boolean>) sender).getEntity();
                externalDiskPanel.setVisible(isDirectLunDiskAvaialable);
            }
        });

        disk.getIsUsingScsiReservation().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                scsiReservationInfoIcon.setVisible(disk.getIsUsingScsiReservation().getEntity());
            }
        });

        disk.getIsVirtioScsiEnabled().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                if (disk.getVm() == null) {
                    // not relevant for floating disks
                    return;
                }

                boolean isVirtioScsiEnabled = Boolean.TRUE.equals(((EntityModel) sender).getEntity());
                Version clusterVersion = disk.getVm().getVdsGroupCompatibilityVersion();

                // Show the info icon if VirtIO-SCSI is supported by the cluster but disabled for the VM
                interfaceInfoIcon.setVisible(clusterVersion.compareTo(Version.v3_3) >= 0 && !isVirtioScsiEnabled);
            }
        });

        disk.getCinderVolumeType().getItemsChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                Collection<String> volumeTypes = disk.getCinderVolumeType().getItems();
                cinderVolumeTypeInfoIcon.setVisible(volumeTypes == null || volumeTypes.isEmpty());
            }
        });

        disk.getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                String propName = args.propertyName;
                if ("Message".equals(propName)) { //$NON-NLS-1$
                    if (!StringHelper.isNullOrEmpty(disk.getMessage())) {
                        disableWidget(getWidget());
                        enableWidget(diskTypePanel);
                        enableWidget(datacenterEditor);
                        disk.getDefaultCommand().setIsExecutionAllowed(false);
                        disk.setIsChangeable(false);
                    } else {
                        enableWidget(getWidget());
                        disk.getDefaultCommand().setIsExecutionAllowed(true);
                        disk.setIsChangeable(true);
                    }
                }
            }
        });

        diskTypePanel.addRadioButton(
                constants.imageDisk(),
                disk.getDisk() == null || disk.getDisk().getDiskStorageType() == DiskStorageType.IMAGE,
                disk.getIsNew(),
                new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    disk.getDiskStorageType().setEntity(DiskStorageType.IMAGE);
                    revealDiskPanel(disk);
                }
            });

        diskTypePanel.addRadioButton(
                constants.directLunDisk(),
                disk.getDisk() != null && disk.getDisk().getDiskStorageType() == DiskStorageType.LUN,
                disk.getIsNew(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        disk.getDiskStorageType().setEntity(DiskStorageType.LUN);
                        revealStorageView(disk);
                        revealDiskPanel(disk);
                    }
                });

        diskTypePanel.addRadioButton(
                constants.cinderDisk(),
                disk.getDisk() != null && disk.getDisk().getDiskStorageType() == DiskStorageType.CINDER,
                disk.getIsNew(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        disk.getDiskStorageType().setEntity(DiskStorageType.CINDER);
                        revealDiskPanel(disk);
                    }
                });

        if (disk.getStorageModel() == null) {
            storageModel = new StorageModel(new NewEditStorageModelBehavior());

            // Create IscsiStorageModel
            iscsiStorageModel = new IscsiStorageModel();
            iscsiStorageModel.setContainer(storageModel);
            iscsiStorageModel.setIsGrouppedByTarget(true);
            iscsiStorageModel.setIgnoreGrayedOut(true);

            // Create FcpStorageModel
            fcpStorageModel = new FcpStorageModel();
            fcpStorageModel.setContainer(storageModel);
            fcpStorageModel.setIsGrouppedByTarget(false);
            fcpStorageModel.setIgnoreGrayedOut(true);

            // Set 'StorageModel' items
            ArrayList<IStorageModel> items = new ArrayList<>();
            items.add(iscsiStorageModel);
            items.add(fcpStorageModel);
            storageModel.setStorageModels(items);
            storageModel.setHost(disk.getHost());

            disk.setStorageModel(storageModel);
        } else {
            storageModel = disk.getStorageModel();

            iscsiStorageModel = Linq.findByType(storageModel.getStorageModels(), IscsiStorageModel.class);
            iscsiStorageModel.getPropertyChangedEvent().clearListeners();

            fcpStorageModel = Linq.findByType(storageModel.getStorageModels(), FcpStorageModel.class);
            fcpStorageModel.getPropertyChangedEvent().clearListeners();
        }

        iscsiStorageView = new IscsiStorageView(false, 115, 214, 244, 275, 142, 55, -67);
        iscsiStorageView.edit(iscsiStorageModel);

        fcpStorageView = new FcpStorageView(false, 278, 240);
        fcpStorageView.edit(fcpStorageModel);

        // SelectedItemChangedEvent handlers
        disk.getStorageType().getSelectedItemChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                revealStorageView(disk);
            }
        });

        disk.getHost().getSelectedItemChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                revealStorageView(disk);
            }
        });

        disk.getDiskStorageType().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                String propName = args.propertyName;
                if ("IsChangable".equals(propName)) { //$NON-NLS-1$
                    if (disk.getDiskStorageType().getIsChangable()) {
                        enableWidget(diskTypePanel);
                    } else {
                        disableWidget(diskTypePanel);
                    }
                }
            }
        });

        revealStorageView(disk);
        revealDiskPanel(disk);
    }

    private void revealDiskPanel(final AbstractDiskModel disk) {
        boolean isInVm = disk.getVm() != null;

        // Disk type (internal/external) selection panel is visible only when
        // 'Attach disk' mode is enabled or new LunDisk creation is enabled
        diskTypePanel.setVisible(isNewLunDiskEnabled);
        externalDiskPanel.setVisible(isNewLunDiskEnabled && disk.getDiskStorageType().getEntity() == DiskStorageType.LUN);

        aliasEditor.setFocus(!isInVm);
    }

    private void revealStorageView(final AbstractDiskModel diskModel) {
        if (!diskModel.getIsNew()) {
            return;
        }

        StorageType storageType = diskModel.getStorageType().getSelectedItem();

        // Set view and model by storage type
        if (storageType == StorageType.ISCSI) {
            storageView = iscsiStorageView;
            sanStorageModel = iscsiStorageModel;
        }
        else if (storageType == StorageType.FCP) {
            storageView = fcpStorageView;
            sanStorageModel = fcpStorageModel;
        }

        storageModel.setCurrentStorageItem(sanStorageModel);
        diskModel.setSanStorageModel(sanStorageModel);

        // Execute 'UpdateCommand' to call 'GetDeviceList'
        sanStorageModel.getUpdateCommand().execute();

        sanStorageModel.setWidgetModel(diskModel);
        externalDiskPanel.clear();
        externalDiskPanel.add(storageView);
    }

    public boolean handleEnterKeyDisabled() {
        return storageView != null && storageView.isSubViewFocused();
    }

    @Override
    public AbstractDiskModel flush() {
        return driver.flush();
    }

    @Override
    public int setTabIndexes(int nextTabIndex) {
        sizeEditor.setTabIndex(nextTabIndex++);
        sizeExtendEditor.setTabIndex(nextTabIndex++);
        aliasEditor.setTabIndex(nextTabIndex++);
        descriptionEditor.setTabIndex(nextTabIndex++);
        interfaceEditor.setTabIndex(nextTabIndex++);
        datacenterEditor.setTabIndex(nextTabIndex++);
        storageDomainEditor.setTabIndex(nextTabIndex++);
        volumeTypeEditor.setTabIndex(nextTabIndex++);
        cinderVolumeTypeEditor.setTabIndex(nextTabIndex++);
        diskProfileEditor.setTabIndex(nextTabIndex++);
        quotaEditor.setTabIndex(nextTabIndex++);
        hostListEditor.setTabIndex(nextTabIndex++);
        storageTypeEditor.setTabIndex(nextTabIndex++);
        plugDiskToVmEditor.setTabIndex(nextTabIndex++);
        wipeAfterDeleteEditor.setTabIndex(nextTabIndex++);
        isBootableEditor.setTabIndex(nextTabIndex++);
        isShareableEditor.setTabIndex(nextTabIndex++);
        isReadOnlyEditor.setTabIndex(nextTabIndex++);
        isScsiPassthroughEditor.setTabIndexes(nextTabIndex++);
        isSgIoUnfilteredEditor.setTabIndex(nextTabIndex++);
        isUsingScsiReservationEditor.setTabIndex(nextTabIndex++);

        return nextTabIndex;
    }
}
