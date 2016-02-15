package org.ovirt.engine.ui.common.widget.uicommon.popup;

import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.hiddenField;
import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.simpleField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.ConsoleDisconnectAction;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.InstanceType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.NumaTuneMode;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.VmWatchdogAction;
import org.ovirt.engine.core.common.businessentities.VmWatchdogType;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.TabbedView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.EntityModelDetachableWidgetWithInfo;
import org.ovirt.engine.ui.common.widget.EntityModelWidgetWithInfo;
import org.ovirt.engine.ui.common.widget.GenericWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.HasDetachable;
import org.ovirt.engine.ui.common.widget.HasValidation;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.common.widget.dialog.AdvancedParametersExpander;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTabPanel;
import org.ovirt.engine.ui.common.widget.editor.GroupedListModelListBox;
import org.ovirt.engine.ui.common.widget.editor.GroupedListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.IconEditorWidget;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelMultipleSelectListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelTypeAheadChangeableListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelTypeAheadListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.SuggestionMatcher;
import org.ovirt.engine.ui.common.widget.editor.VncKeyMapRenderer;
import org.ovirt.engine.ui.common.widget.editor.generic.DetachableLabel;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelDetachableWidget;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelDetachableWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.MemorySizeEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.form.key_value.KeyValueWidget;
import org.ovirt.engine.ui.common.widget.label.EnableableFormLabel;
import org.ovirt.engine.ui.common.widget.parser.MemorySizeParser;
import org.ovirt.engine.ui.common.widget.profile.ProfilesInstanceTypeEditor;
import org.ovirt.engine.ui.common.widget.renderer.BooleanRendererWithNullText;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.MemorySizeRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NameRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.tooltip.TooltipConfig.Width;
import org.ovirt.engine.ui.common.widget.uicommon.instanceimages.InstanceImagesEditor;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfigMap;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.SerialNumberPolicyWidget;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.VmPopupVmInitWidget;
import org.ovirt.engine.ui.common.widget.uicommon.storage.DisksAllocationView;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.TabName;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateWithVersion;
import org.ovirt.engine.ui.uicommonweb.models.vms.DataCenterWithCluster;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.TimeZoneModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicompat.EnumTranslator;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.external.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractVmPopupWidget extends AbstractModeSwitchingPopupWidget<UnitVmModel>
    implements TabbedView {

    interface Driver extends SimpleBeanEditorDriver<UnitVmModel, AbstractVmPopupWidget> {
    }

    interface ViewUiBinder extends UiBinder<DialogTabPanel, AbstractVmPopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    protected interface Style extends CssResource {
        String longCheckboxContent();

        String provisioningEditorContent();

        String provisioningRadioContent();

        String cdAttachedLabelWidth();

        String assignedVmsLabel();

        String labelDisabled();

        String generalTabExtendedRightWidgetWidth();

        String generalTabExtendedRightWidgetWrapperWidth();

        String cdImageEditor();

        String monitorsStyles();

        String migrationSelectorInner();

        String isVirtioScsiEnabledEditor();

        String totalVcpusStyle();
    }

    @UiField
    protected Style style;

    // ==General Tab==
    @UiField
    protected DialogTab generalTab;

    @UiField(provided = true)
    @Path(value = "dataCenterWithClustersList.selectedItem")
    @WithElementId("dataCenterWithCluster")
    public GroupedListModelListBoxEditor<DataCenterWithCluster> dataCenterWithClusterEditor;

    @UiField(provided = true)
    @Path(value = "quota.selectedItem")
    @WithElementId("quota")
    public ListModelTypeAheadListBoxEditor<Quota> quotaEditor;

    @UiField
    @Ignore
    public Label nameLabel;

    @UiField(provided = true)
    @Path(value = "name.entity")
    @WithElementId("name")
    public StringEntityModelTextBoxOnlyEditor nameEditor;

    @UiField(provided = true)
    @Path(value = "templateVersionName.entity")
    @WithElementId("templateVersionName")
    public StringEntityModelTextBoxEditor templateVersionNameEditor;

    @UiField(provided = true)
    @Ignore
    public InfoIcon poolNameIcon;

    @UiField
    @Path(value = "vmId.entity")
    @WithElementId("vmId")
    public StringEntityModelTextBoxEditor vmIdEditor;

    @UiField(provided = true)
    @Path(value = "description.entity")
    @WithElementId("description")
    public StringEntityModelTextBoxEditor descriptionEditor;

    @UiField
    @Path(value = "comment.entity")
    @WithElementId("comment")
    public StringEntityModelTextBoxEditor commentEditor;

    @UiField(provided = true)
    @Path(value = "baseTemplate.selectedItem")
    @WithElementId("baseTemplate")
    public ListModelTypeAheadListBoxEditor<VmTemplate> baseTemplateEditor;

    @UiField(provided = true)
    @Path(value = "templateWithVersion.selectedItem")
    @WithElementId("templateWithVersion")
    public ListModelTypeAheadListBoxEditor<TemplateWithVersion> templateWithVersionEditor;

    @UiField(provided = true)
    @Path(value = "OSType.selectedItem")
    @WithElementId("osType")
    public ListModelListBoxEditor<Integer> oSTypeEditor;

    @UiField(provided = true)
    @Path(value = "vmType.selectedItem")
    @WithElementId("vmType")
    public ListModelListBoxEditor<VmType> vmTypeEditor;

    @Path(value = "instanceTypes.selectedItem")
    @WithElementId("instanceType")
    public ListModelTypeAheadListBoxEditor<InstanceType> instanceTypesEditor;

    @UiField(provided = true)
    public EntityModelDetachableWidgetWithLabel detachableInstanceTypesEditor;

    @UiField(provided = true)
    @Path(value = "isDeleteProtected.entity")
    @WithElementId("isDeleteProtected")
    public EntityModelCheckBoxEditor isDeleteProtectedEditor;

    @UiField
    public Panel logicalNetworksEditorPanel;

    @UiField
    @Ignore
    @WithElementId("instanceImages")
    public InstanceImagesEditor instanceImagesEditor;

    @UiField
    @Ignore
    @WithElementId("vnicsEditor")
    public ProfilesInstanceTypeEditor profilesInstanceTypeEditor;

    @UiField
    @Ignore
    Label generalWarningMessage;

    // == System ==
    @UiField
    protected DialogTab systemTab;

    @UiField(provided = true)
    public EntityModelDetachableWidgetWithLabel detachableMemSizeEditor;

    @Path(value = "memSize.entity")
    @WithElementId("memSize")
    public MemorySizeEntityModelTextBoxEditor memSizeEditor;

    @Path(value = "totalCPUCores.entity")
    @WithElementId("totalCPUCores")
    public StringEntityModelTextBoxOnlyEditor totalvCPUsEditor;

    @UiField(provided = true)
    @Ignore
    public EntityModelDetachableWidgetWithInfo totalvCPUsEditorWithInfoIcon;

    @UiField
    @Ignore
    AdvancedParametersExpander vcpusAdvancedParameterExpander;

    @UiField
    @Ignore
    Panel vcpusAdvancedParameterExpanderContent;

    @Path(value = "numOfSockets.selectedItem")
    @WithElementId("numOfSockets")
    public ListModelListBoxEditor<Integer> numOfSocketsEditor;

    @Path(value = "coresPerSocket.selectedItem")
    @WithElementId("coresPerSocket")
    public ListModelListBoxEditor<Integer> corePerSocketEditor;

    @UiField(provided = true)
    public EntityModelDetachableWidgetWithLabel numOfSocketsEditorWithDetachable;

    @UiField(provided = true)
    public EntityModelDetachableWidgetWithLabel corePerSocketEditorWithDetachable;

    @UiField(provided = true)
    @Path(value = "emulatedMachine.selectedItem")
    @WithElementId("emulatedMachine")
    public ListModelTypeAheadChangeableListBoxEditor emulatedMachine;

    @UiField(provided = true)
    @Path(value = "customCpu.selectedItem")
    @WithElementId("customCpu")
    public ListModelTypeAheadChangeableListBoxEditor customCpu;

    @UiField
    @Ignore
    public Label ssoMethodLabel;

    @UiField(provided = true)
    @Path(value = "ssoMethodNone.entity")
    @WithElementId("ssoMethodNone")
    public EntityModelRadioButtonEditor ssoMethodNone;

    @UiField(provided = true)
    @Path(value = "ssoMethodGuestAgent.entity")
    @WithElementId("ssoMethodGuestAgent")
    public EntityModelRadioButtonEditor ssoMethodGuestAgent;

    @UiField(provided = true)
    @Path(value = "isSoundcardEnabled.entity")
    @WithElementId("isSoundcardEnabled")
    public EntityModelCheckBoxEditor isSoundcardEnabledEditor;

    @UiField(provided = true)
    @Path("copyPermissions.entity")
    @WithElementId("copyTemplatePermissions")
    public EntityModelCheckBoxEditor copyTemplatePermissionsEditor;

    @UiField
    @Ignore
    @WithElementId("serialNumberPolicy")
    public SerialNumberPolicyWidget serialNumberPolicyEditor;

    // == Pools ==
    @UiField
    protected DialogTab poolTab;

    @UiField(provided = true)
    @Path(value = "poolType.selectedItem")
    @WithElementId("poolType")
    public ListModelListBoxEditor<EntityModel<VmPoolType>> poolTypeEditor;

    @UiField(provided = true)
    @Ignore
    public InfoIcon newPoolPrestartedVmsIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon editPoolPrestartedVmsIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon newPoolMaxAssignedVmsPerUserIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon editPoolMaxAssignedVmsPerUserIcon;

    @UiField(provided = true)
    @Path(value = "prestartedVms.entity")
    @WithElementId("prestartedVms")
    public IntegerEntityModelTextBoxOnlyEditor prestartedVmsEditor;

    @UiField(provided = true)
    @Path("maxAssignedVmsPerUser.entity")
    @WithElementId("maxAssignedVmsPerUser")
    public IntegerEntityModelTextBoxOnlyEditor maxAssignedVmsPerUserEditor;

    @UiField
    @Ignore
    public FlowPanel newPoolEditVmsPanel;

    @UiField
    @Ignore
    public FlowPanel newPoolEditMaxAssignedVmsPerUserPanel;

    @UiField
    @Ignore
    public Label prestartedLabel;

    @UiField(provided = true)
    @Path("numOfDesktops.entity")
    @WithElementId("numOfVms")
    public EntityModelTextBoxEditor<Integer> numOfVmsEditor;

    @UiField
    @Ignore
    public FlowPanel editPoolEditVmsPanel;

    @UiField
    public FlowPanel editIncreaseVmsPanel;

    @UiField
    @Ignore
    public FlowPanel editPoolIncraseNumOfVmsPanel;

    @UiField
    public FlowPanel editPrestartedVmsPanel;
    @UiField
    @Ignore
    public FlowPanel editPoolEditMaxAssignedVmsPerUserPanel;

    @UiField
    @Ignore
    public Label editPrestartedVmsLabel;

    @UiField(provided = true)
    @Path("prestartedVms.entity")
    @WithElementId("editPrestartedVms")
    public IntegerEntityModelTextBoxOnlyEditor editPrestartedVmsEditor;

    @UiField(provided = true)
    @Path("numOfDesktops.entity")
    @WithElementId("incraseNumOfVms")
    public EntityModelTextBoxOnlyEditor<Integer> incraseNumOfVmsEditor;

    @UiField(provided = true)
    @Path("maxAssignedVmsPerUser.entity")
    @WithElementId("editMaxAssignedVmsPerUser")
    public IntegerEntityModelTextBoxOnlyEditor editMaxAssignedVmsPerUserEditor;

    @UiField(provided = true)
    @Path("assignedVms.entity")
    public ValueLabel<Integer> outOfxInPool;

    @UiField
    @Ignore
    // system tab -> general time zone
    public Label generalLabel;

    @Path(value = "timeZone.selectedItem")
    @WithElementId("timeZone")
    public ListModelListBoxOnlyEditor<TimeZoneModel> timeZoneEditor;

    @UiField(provided = true)
    @Ignore
    public EntityModelWidgetWithInfo timeZoneEditorWithInfo;

    // ==Initial run Tab==
    @UiField
    protected DialogTab initialRunTab;

    @UiField
    @Path(value = "vmInitEnabled.entity")
    @WithElementId("vmInitEnabled")
    public EntityModelCheckBoxEditor vmInitEnabledEditor;

    @UiField
    @Ignore
    public VmPopupVmInitWidget vmInitEditor;

    // ==Console Tab==
    @UiField
    protected DialogTab consoleTab;

    @UiField(provided = true)
    @Path(value = "displayType.selectedItem")
    @WithElementId("displayType")
    public ListModelListBoxEditor<DisplayType> displayTypeEditor;

    @UiField(provided = true)
    @Path(value = "graphicsType.selectedItem")
    @WithElementId("graphicsType")
    public ListModelListBoxEditor<UnitVmModel.GraphicsTypes> graphicsTypeEditor;

    @UiField(provided = true)
    @Path(value = "vncKeyboardLayout.selectedItem")
    @WithElementId("vncKeyboardLayout")
    public ListModelListBoxEditor<String> vncKeyboardLayoutEditor;

    @UiField(provided = true)
    @Path(value = "usbPolicy.selectedItem")
    @WithElementId("usbPolicy")
    public ListModelListBoxEditor<UsbPolicy> usbSupportEditor;

    @UiField(provided = true)
    @Path(value = "consoleDisconnectAction.selectedItem")
    @WithElementId("consoleDisconnectAction")
    public ListModelListBoxEditor<ConsoleDisconnectAction> consoleDisconnectActionEditor;

    @UiField(provided = true)
    @Path(value = "numOfMonitors.selectedItem")
    @WithElementId("numOfMonitors")
    public ListModelListBoxEditor<Integer> numOfMonitorsEditor;

    @UiField
    @Ignore
    public GenericWidgetWithLabel monitors;

    @UiField(provided = true)
    @Path(value = "isSingleQxlEnabled.entity")
    @WithElementId("isSingleQxlEnabled")
    public EntityModelCheckBoxEditor isSingleQxlEnabledEditor;

    @UiField(provided = true)
    @Path(value = "isStateless.entity")
    @WithElementId("isStateless")
    public EntityModelCheckBoxEditor isStatelessEditor;

    @UiField(provided = true)
    @Path(value = "isRunAndPause.entity")
    @WithElementId("isRunAndPause")
    public EntityModelCheckBoxEditor isRunAndPauseEditor;

    @UiField(provided = true)
    @Path(value = "isSmartcardEnabled.entity")
    @WithElementId("isSmartcardEnabled")
    public EntityModelCheckBoxEditor isSmartcardEnabledEditor;

    @UiField(provided = true)
    @Path(value = "allowConsoleReconnect.entity")
    @WithElementId("allowConsoleReconnect")
    public EntityModelCheckBoxEditor allowConsoleReconnectEditor;

    @UiField(provided = true)
    @Path(value = "isConsoleDeviceEnabled.entity")
    @WithElementId("isConsoleDeviceEnabled")
    public EntityModelCheckBoxEditor isConsoleDeviceEnabledEditor;

    @UiField
    @Path(value = "spiceProxy.entity")
    @WithElementId
    public StringEntityModelTextBoxEditor spiceProxyEditor;

    @UiField(provided = true)
    @Ignore
    public EntityModelWidgetWithInfo spiceProxyEnabledCheckboxWithInfoIcon;

    @Path(value = "spiceProxyEnabled.entity")
    @WithElementId
    public EntityModelCheckBoxOnlyEditor spiceProxyOverrideEnabledEditor;

    @UiField(provided = true)
    @Path("spiceFileTransferEnabled.entity")
    @WithElementId("spiceFileTransferEnabled")
    public EntityModelCheckBoxEditor spiceFileTransferEnabledEditor;

    @UiField(provided = true)
    @Path("spiceCopyPasteEnabled.entity")
    @WithElementId("spiceCopyPasteEnabled")
    public EntityModelCheckBoxEditor spiceCopyPasteEnabledEditor;

    // == Rng Tab ==
    @UiField
    protected DialogTab rngDeviceTab;

    @Path(value = "isRngEnabled.entity")
    @WithElementId("isRngEnabled")
    public EntityModelCheckBoxOnlyEditor isRngEnabledEditor;

    @UiField(provided = true)
    @WithElementId
    public EntityModelWidgetWithInfo isRngEnabledCheckboxWithInfoIcon;

    @UiField
    @Ignore
    protected FlowPanel rngPanel;

    @UiField(provided = true)
    @Path(value = "rngPeriod.entity")
    @WithElementId("rngPeriodEditor")
    public IntegerEntityModelTextBoxEditor rngPeriodEditor;

    @UiField(provided = true)
    @Path(value = "rngBytes.entity")
    @WithElementId("rngBytesEditor")
    public IntegerEntityModelTextBoxEditor rngBytesEditor;

    @UiField(provided = true)
    @Path(value = "rngSourceRandom.entity")
    @WithElementId("rngSourceRandom")
    public EntityModelRadioButtonEditor rngSourceRandom;

    @UiField(provided = true)
    @Path(value = "rngSourceHwrng.entity")
    @WithElementId("rngSourceHwrng")
    public EntityModelRadioButtonEditor rngSourceHwrng;

    // ==Host Tab==
    @UiField
    protected DialogTab hostTab;

    @UiField(provided = true)
    @Path(value = "hostCpu.entity")
    @WithElementId("hostCpu")
    public EntityModelCheckBoxEditor hostCpuEditor;

    @UiField
    @Ignore
    public FlowPanel numaPanel;

    @UiField(provided = true)
    @Ignore
    public InfoIcon numaInfoIcon;

    @UiField
    @Path(value = "numaNodeCount.entity")
    @WithElementId("numaNodeCount")
    public IntegerEntityModelTextBoxEditor numaNodeCount;

    @UiField(provided = true)
    @Path(value = "numaTuneMode.selectedItem")
    @WithElementId("numaTuneMode")
    public ListModelListBoxEditor<NumaTuneMode> numaTuneMode;

    @UiField
    UiCommandButton numaSupportButton;

    @Path(value = "migrationMode.selectedItem")
    @WithElementId("migrationMode")
    public ListModelListBoxEditor<MigrationSupport> migrationModeEditor;

    @UiField(provided = true)
    public EntityModelDetachableWidget migrationModeEditorWithDetachable;

    @Path(value = "overrideMigrationDowntime.entity")
    @WithElementId("overrideMigrationDowntime")
    public EntityModelCheckBoxOnlyEditor overrideMigrationDowntimeEditor;

    @UiField(provided = true)
    public EntityModelDetachableWidget overrideMigrationDowntimeEditorWithDetachable;

    @UiField
    @Ignore
    public EnableableFormLabel overrideMigrationDowntimeLabel;

    @UiField(provided = true)
    public InfoIcon migrationDowntimeInfoIcon;

    @UiField(provided = true)
    public InfoIcon migrationSelectInfoIcon;

    @UiField(provided = true)
    @Path(value = "migrationDowntime.entity")
    @WithElementId("migrationDowntime")
    public IntegerEntityModelTextBoxOnlyEditor migrationDowntimeEditor;

    @UiField(provided = true)
    @Path(value = "autoConverge.selectedItem")
    @WithElementId("autoConverge")
    public ListModelListBoxEditor<Boolean> autoConvergeEditor;

    @UiField(provided = true)
    @Path(value = "migrateCompressed.selectedItem")
    @WithElementId("migrateCompressed")
    public ListModelListBoxEditor<Boolean> migrateCompressedEditor;

    @UiField(provided = true)
    @Ignore
    @WithElementId("specificHost")
    public RadioButton specificHost;

    @UiField
    @Ignore
    public EnableableFormLabel specificHostLabel;

    @UiField(provided = true)
    @Path(value = "defaultHost.selectedItems")
    @WithElementId("defaultHost")
    public ListModelMultipleSelectListBoxEditor<VDS> defaultHostEditor;

    @UiField(provided = true)
    @Path(value = "isAutoAssign.entity")
    @WithElementId("isAutoAssign")
    public EntityModelRadioButtonEditor isAutoAssignEditor;

    @UiField
    @Ignore
    public FlowPanel startRunningOnPanel;

    @UiField(provided = true)
    @Path(value = "cpuProfiles.selectedItem")
    @WithElementId("cpuProfiles")
    public ListModelListBoxEditor<CpuProfile> cpuProfilesEditor;

    @UiField(provided = true)
    InfoIcon cpuPinningInfo;

    @UiField(provided = true)
    @Path(value = "cpuPinning.entity")
    @WithElementId("cpuPinning")
    public StringEntityModelTextBoxOnlyEditor cpuPinning;

    @UiField(provided = true)
    @Path(value = "cpuSharesAmountSelection.selectedItem")
    @WithElementId("cpuSharesAmountSelection")
    public ListModelListBoxOnlyEditor<UnitVmModel.CpuSharesAmount> cpuSharesAmountSelectionEditor;

    @UiField
    @Ignore
    public Label cpuSharesEditor;

    @UiField
    @Ignore
    public FlowPanel cpuAllocationPanel;

    @UiField(provided = true)
    @Path(value = "cpuSharesAmount.entity")
    @WithElementId("cpuSharesAmount")
    public IntegerEntityModelTextBoxOnlyEditor cpuSharesAmountEditor;

    // ==High Availability Tab==
    @UiField
    protected DialogTab highAvailabilityTab;

    @Path(value = "isHighlyAvailable.entity")
    @WithElementId("isHighlyAvailable")
    public EntityModelCheckBoxEditor isHighlyAvailableEditor;

    @UiField(provided = true)
    @Ignore
    public EntityModelDetachableWidget isHighlyAvailableEditorWithDetachable;

    @UiField(provided = true)
    @Path("priority.selectedItem")
    @WithElementId("priority")
    public ListModelListBoxEditor<EntityModel<Integer>> priorityEditor;

    @UiField
    @Ignore
    public DetachableLabel priorityLabelWithDetachable;

    @UiField
    @Ignore
    public FlowPanel watchdogPanel;

    @UiField(provided = true)
    @Path(value = "watchdogModel.selectedItem")
    @WithElementId("watchdogModel")
    public ListModelListBoxEditor<VmWatchdogType> watchdogModelEditor;

    @UiField(provided = true)
    @Path(value = "watchdogAction.selectedItem")
    @WithElementId("watchdogAction")
    public ListModelListBoxEditor<VmWatchdogAction> watchdogActionEditor;

    // ==Resource Allocation Tab==
    @UiField
    protected DialogTab resourceAllocationTab;

    @UiField
    protected FlowPanel cpuSharesPanel;

    @UiField
    protected FlowPanel cpuPinningPanel;

    @UiField
    protected FlowPanel memAllocationPanel;

    @UiField
    protected FlowPanel ioThreadsPanel;

    @UiField
    protected FlowPanel storageAllocationPanel;

    @UiField
    protected HorizontalPanel provisionSelectionPanel;

    @UiField
    protected FlowPanel disksAllocationPanel;

    @UiField
    protected FlowPanel disksPanel;

    @UiField
    @Ignore
    @WithElementId("provisioning")
    public ListModelListBoxEditor provisioningEditor;

    @UiField
    public DetachableLabel memAllocationLabel;

    @UiField
    public DetachableLabel ioThreadsLabel;

    @UiField(provided = true)
    @Path(value = "numOfIoThreads.entity")
    @WithElementId("numOfIoThreadsEditor")
    public IntegerEntityModelTextBoxEditor numOfIoThreadsEditor;

    @UiField(provided = true)
    @Path(value = "minAllocatedMemory.entity")
    @WithElementId("minAllocatedMemory")
    public EntityModelTextBoxEditor<Integer> minAllocatedMemoryEditor;

    @UiField(provided = true)
    @Path(value = "ioThreadsEnabled.entity")
    EntityModelCheckBoxEditor isIoThreadsEnabled;

    @UiField(provided = true)
    @Path(value = "memoryBalloonDeviceEnabled.entity")
    EntityModelCheckBoxEditor isMemoryBalloonDeviceEnabled;

    @UiField(provided = true)
    @Path(value = "provisioningThin_IsSelected.entity")
    @WithElementId("provisioningThin")
    public EntityModelRadioButtonEditor provisioningThinEditor;

    @UiField(provided = true)
    @Path(value = "provisioningClone_IsSelected.entity")
    @WithElementId("provisioningClone")
    public EntityModelRadioButtonEditor provisioningCloneEditor;

    @UiField(provided = true)
    @Path(value = "isVirtioScsiEnabled.entity")
    @WithElementId("isVirtioScsiEnabled")
    public EntityModelCheckBoxEditor isVirtioScsiEnabled;

    @UiField(provided = true)
    @Ignore
    public InfoIcon isVirtioScsiEnabledInfoIcon;

    @UiField(provided = true)
    @Ignore
    @WithElementId("disksAllocation")
    public DisksAllocationView disksAllocationView;

    // ==Boot Options Tab==
    @UiField
    protected DialogTab bootOptionsTab;

    @UiField(provided = true)
    @Path(value = "firstBootDevice.selectedItem")
    @WithElementId("firstBootDevice")
    public ListModelListBoxEditor<EntityModel<BootSequence>> firstBootDeviceEditor;

    @UiField(provided = true)
    @Path(value = "secondBootDevice.selectedItem")
    @WithElementId("secondBootDevice")
    public ListModelListBoxEditor<EntityModel<BootSequence>> secondBootDeviceEditor;

    @UiField(provided = true)
    @Path(value = "cdImage.selectedItem")
    @WithElementId("cdImage")
    public ListModelListBoxEditor<String> cdImageEditor;

    @UiField(provided = true)
    @Path(value = "cdAttached.entity")
    @WithElementId("cdAttached")
    public EntityModelCheckBoxEditor cdAttachedEditor;

    @UiField
    public HorizontalPanel attachCdPanel;

    @UiField(provided = true)
    @Path("bootMenuEnabled.entity")
    @WithElementId("bootMenuEnabled")
    public EntityModelCheckBoxEditor bootMenuEnabledEditor;

    @UiField
    protected FlowPanel linuxBootOptionsPanel;

    @UiField(provided = true)
    @Path(value = "kernel_path.entity")
    @WithElementId("kernelPath")
    public StringEntityModelTextBoxEditor kernel_pathEditor;

    @UiField(provided = true)
    @Path(value = "initrd_path.entity")
    @WithElementId("initrdPath")
    public StringEntityModelTextBoxEditor initrd_pathEditor;

    @UiField(provided = true)
    @Path(value = "kernel_parameters.entity")
    @WithElementId("kernelParameters")
    public StringEntityModelTextBoxEditor kernel_parametersEditor;

    @UiField
    @Ignore
    Label nativeUsbWarningMessage;

    // ==Custom Properties Tab==
    @UiField
    protected DialogTab customPropertiesTab;

    @UiField
    @Ignore
    protected KeyValueWidget<KeyValueModel> customPropertiesSheetEditor;

    @UiField
    @Ignore
    protected AdvancedParametersExpander expander;

    @UiField
    @Ignore
    Panel expanderContent;

    @UiField
    @Ignore
    public ButtonBase refreshButton;

    @UiField
    @Ignore
    protected DialogTabPanel mainTabPanel;

    @UiField
    @Ignore
    protected DialogTab iconTab;

    @UiField
    @Path("icon.entity")
    protected IconEditorWidget iconEditorWidget;

    @UiField
    @Ignore
    protected DialogTab foremanTab;

    @UiField(provided = true)
    @Path(value = "providers.selectedItem")
    @WithElementId("providers")
    public ListModelListBoxEditor<Provider<OpenstackNetworkProviderProperties>> providersEditor;

    private UnitVmModel unitVmModel;

    private final Driver driver = GWT.create(Driver.class);

    private final static CommonApplicationTemplates templates = AssetProvider.getTemplates();
    private final static CommonApplicationConstants constants = AssetProvider.getConstants();
    private final static CommonApplicationMessages messages = AssetProvider.getMessages();

    private final Map<TabName, DialogTab> tabMap = new HashMap<TabName, DialogTab>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AbstractVmPopupWidget(EventBus eventBus) {

        initListBoxEditors();
        // Contains a special parser/renderer
        memSizeEditor = new MemorySizeEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());

        minAllocatedMemoryEditor = new EntityModelTextBoxEditor<Integer>(
                new MemorySizeRenderer<Integer>(), new MemorySizeParser(), new ModeSwitchingVisibilityRenderer());

        numOfIoThreadsEditor = new IntegerEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());

        // TODO: How to align right without creating the widget manually?
        hostCpuEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isHighlyAvailableEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());

        watchdogModelEditor = new ListModelListBoxEditor<VmWatchdogType>(new NullSafeRenderer<VmWatchdogType>() {
            @Override
            public String render(VmWatchdogType object) {
                return object == null ? constants.noWatchdogLabel() : renderNullSafe(object);
            }

            @Override
            protected String renderNullSafe(VmWatchdogType object) {
                return EnumTranslator.getInstance().translate(object);
            }
        }, new ModeSwitchingVisibilityRenderer());

        watchdogActionEditor = new ListModelListBoxEditor<VmWatchdogAction>(new NullSafeRenderer<VmWatchdogAction>() {
            @Override
            protected String renderNullSafe(VmWatchdogAction object) {
                return EnumTranslator.getInstance().translate(object);
            }
        }, new ModeSwitchingVisibilityRenderer());

        isStatelessEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isRunAndPauseEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isDeleteProtectedEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSmartcardEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isConsoleDeviceEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer(), true);
        isRngEnabledEditor = new EntityModelCheckBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        rngPeriodEditor = new IntegerEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        rngBytesEditor = new IntegerEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        rngSourceRandom = new EntityModelRadioButtonEditor("rndBackendModel"); //$NON-NLS-1$
        rngSourceHwrng = new EntityModelRadioButtonEditor("rndBackendModel"); //$NON-NLS-1$

        cdAttachedEditor = new EntityModelCheckBoxEditor(Align.LEFT, new ModeSwitchingVisibilityRenderer());
        bootMenuEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        allowConsoleReconnectEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSoundcardEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        ssoMethodNone = new EntityModelRadioButtonEditor("ssoMethod", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        ssoMethodGuestAgent = new EntityModelRadioButtonEditor("ssoMethod", new ModeSwitchingVisibilityRenderer());//$NON-NLS-1$
        copyTemplatePermissionsEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isMemoryBalloonDeviceEnabled = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer(), true);
        isIoThreadsEnabled = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer(), true);
        isVirtioScsiEnabled = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSingleQxlEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        cpuPinningInfo =
                new InfoIcon(SafeHtmlUtils.fromTrustedString(templates.italicText(
                        constants.cpuPinningLabelExplanation())
                        .asString()
                        .replaceAll("(\r\n|\n)", "<br />")));//$NON-NLS-1$ //$NON-NLS-2$
        cpuPinningInfo.setTooltipMaxWidth(Width.W420);
        isVirtioScsiEnabledInfoIcon =
                new InfoIcon(templates.italicText(constants.isVirtioScsiEnabledInfo()));
        final Integer defaultMaximumMigrationDowntime = (Integer) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.DefaultMaximumMigrationDowntime);
        migrationDowntimeInfoIcon = new InfoIcon(templates.italicText(messages.migrationDowntimeInfo(defaultMaximumMigrationDowntime)));
        migrationSelectInfoIcon = new InfoIcon(templates.italicText(messages.migrationSelectInfo()));
        priorityEditor = new ListModelListBoxEditor<>(new NullSafeRenderer<EntityModel<Integer>>() {
            @Override
            protected String renderNullSafe(EntityModel<Integer> model) {
                return model.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());
        disksAllocationView = new DisksAllocationView();
        spiceFileTransferEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        spiceCopyPasteEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());

        initPoolSpecificWidgets();
        initTextBoxEditors();
        initSpiceProxy();
        initTotalVcpus();
        initDetachableFields();

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        serialNumberPolicyEditor.setRenderer(new ModeSwitchingVisibilityRenderer());

        expander.initWithContent(expanderContent.getElement());
        vcpusAdvancedParameterExpander.initWithContent(vcpusAdvancedParameterExpanderContent.getElement());

        applyStyles();

        localize();

        generateIds();

        driver.initialize(this);

        initialize();
        populateTabMap();
    }

    protected void populateTabMap() {
        getTabNameMapping().put(TabName.GENERAL_TAB, generalTab);
        getTabNameMapping().put(TabName.BOOT_OPTIONS_TAB, this.bootOptionsTab);
        getTabNameMapping().put(TabName.CONSOLE_TAB, this.consoleTab);
        getTabNameMapping().put(TabName.CUSTOM_PROPERTIES_TAB, this.customPropertiesTab);
        getTabNameMapping().put(TabName.HIGH_AVAILABILITY_TAB, this.highAvailabilityTab);
        getTabNameMapping().put(TabName.HOST_TAB, this.hostTab);
        getTabNameMapping().put(TabName.INITIAL_RUN_TAB, this.initialRunTab);
        getTabNameMapping().put(TabName.POOL_TAB, this.poolTab);
        getTabNameMapping().put(TabName.RESOURCE_ALLOCATION_TAB, this.resourceAllocationTab);
        getTabNameMapping().put(TabName.SYSTEM_TAB, this.systemTab);
        getTabNameMapping().put(TabName.ICON_TAB, this.iconTab);
        getTabNameMapping().put(TabName.FOREMAN_TAB, foremanTab);
    }

    private void initDetachableFields() {
        detachableInstanceTypesEditor = new EntityModelDetachableWidgetWithLabel(instanceTypesEditor);

        detachableMemSizeEditor = new EntityModelDetachableWidgetWithLabel(memSizeEditor);
        isHighlyAvailableEditorWithDetachable = new EntityModelDetachableWidget(isHighlyAvailableEditor, Align.RIGHT);

        overrideMigrationDowntimeEditorWithDetachable = new EntityModelDetachableWidget(overrideMigrationDowntimeEditor, Align.IGNORE);
        overrideMigrationDowntimeEditorWithDetachable.setupContentWrapper(Align.RIGHT);

        overrideMigrationDowntimeEditor.getContentWidgetContainer().getElement().getStyle().setWidth(20, Unit.PX);

        migrationModeEditorWithDetachable = new EntityModelDetachableWidget(migrationModeEditor, Align.IGNORE);
        migrationModeEditorWithDetachable.setupContentWrapper(Align.RIGHT);

        EnableableFormLabel rnglabel = new EnableableFormLabel();
        rnglabel.setText(constants.rngDevEnabled());
        isRngEnabledCheckboxWithInfoIcon = new EntityModelWidgetWithInfo(rnglabel, isRngEnabledEditor);
        isRngEnabledCheckboxWithInfoIcon.setExplanation(SafeHtmlUtils.fromTrustedString(constants.rngDevExplanation()));
    }

    protected void initialize() {

    }

    protected void initSpiceProxy() {
        EnableableFormLabel label = new EnableableFormLabel();
        label.setText(constants.defineSpiceProxyEnable());
        spiceProxyOverrideEnabledEditor = new EntityModelCheckBoxOnlyEditor();
        spiceProxyEnabledCheckboxWithInfoIcon = new EntityModelWidgetWithInfo(label, spiceProxyOverrideEnabledEditor);
    }

    private void initTotalVcpus() {
        EnableableFormLabel label = new EnableableFormLabel();
        label.setText(constants.numOfVCPUs());
        label.addStyleName("numCPUs_pfly_fix"); //$NON-NLS-1$
        totalvCPUsEditor = new StringEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        totalvCPUsEditorWithInfoIcon = new EntityModelDetachableWidgetWithInfo(label, totalvCPUsEditor);
        totalvCPUsEditorWithInfoIcon.setExplanation(templates.italicText(messages.hotPlugUnplugCpuWarning()));
    }

    public void setSpiceProxyOverrideExplanation(String explanation) {
        spiceProxyEnabledCheckboxWithInfoIcon.setExplanation(templates.italicText(explanation));
    }

    private void initTextBoxEditors() {
        templateVersionNameEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        vmIdEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        descriptionEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        commentEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        numOfVmsEditor = new IntegerEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        cpuPinning = new StringEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        cpuSharesAmountEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        kernel_pathEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        initrd_pathEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        kernel_parametersEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        nameEditor = new StringEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        prestartedVmsEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        editPrestartedVmsEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        maxAssignedVmsPerUserEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        editMaxAssignedVmsPerUserEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
    }

    protected void initPoolSpecificWidgets() {
        createNumOfDesktopEditors();

        newPoolPrestartedVmsIcon =
                new InfoIcon(templates.italicText(messages.prestartedHelp()));

        editPoolPrestartedVmsIcon =
                new InfoIcon(templates.italicText(messages.prestartedHelp()));

        poolNameIcon =
                new InfoIcon(templates.italicText(messages.poolNameHelp()));

        newPoolMaxAssignedVmsPerUserIcon =
                new InfoIcon(templates.italicText(messages.maxAssignedVmsPerUserHelp()));

        editPoolMaxAssignedVmsPerUserIcon =
                new InfoIcon(templates.italicText(messages.maxAssignedVmsPerUserHelp()));

        outOfxInPool = new ValueLabel<Integer>(new AbstractRenderer<Integer>() {

            @Override
            public String render(Integer object) {
                return messages.outOfXVMsInPool(object.toString());
            }

        });

        numaInfoIcon = new InfoIcon(SafeHtmlUtils.fromTrustedString("")); //$NON-NLS-1$
    }

    /**
     * There are two editors which edits the same entity - in the correct subclass make sure that the correct one's
     * value is used to edit the model
     * <p>
     * The default implementation just creates the simple editors
     */
    protected void createNumOfDesktopEditors() {
        incraseNumOfVmsEditor = new IntegerEntityModelTextBoxOnlyEditor();
        numOfVmsEditor = new IntegerEntityModelTextBoxEditor();
    }

    protected abstract void generateIds();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initListBoxEditors() {
        // General tab
        dataCenterWithClusterEditor = new GroupedListModelListBoxEditor<>(
                new GroupedListModelListBox<DataCenterWithCluster>(new NameRenderer<DataCenterWithCluster>()) {
            @Override
            public SortedMap<String, List<DataCenterWithCluster>> getGroupedList(List<DataCenterWithCluster> acceptableValues) {
                SortedMap<String, List<DataCenterWithCluster>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                Collections.sort(acceptableValues, new DataCenterWithClusterComparator());
                String currentDataCenter = null;
                List<DataCenterWithCluster> currentClusterWithDcList = null;
                for (DataCenterWithCluster clusterWithDc: acceptableValues) {
                    if (currentDataCenter == null || !currentDataCenter.equals(
                            clusterWithDc.getDataCenter().getName())) {
                        currentClusterWithDcList = new ArrayList<>();
                        currentDataCenter = clusterWithDc.getDataCenter().getName();
                        if (currentDataCenter != null) {
                            result.put(currentDataCenter, currentClusterWithDcList);
                        }
                     }
                    if (currentClusterWithDcList != null) {
                        currentClusterWithDcList.add(clusterWithDc);
                    }
                }
                return result;
            }

            @Override
            public String getModelLabel(DataCenterWithCluster model) {
                return model.getCluster().getName();
            }

            @Override
            public String getGroupLabel(DataCenterWithCluster model) {
                return messages.hostDataCenter(model.getDataCenter().getName());
            }

            public Comparator<DataCenterWithCluster> getComparator() {
                return new DataCenterWithClusterComparator();
            }
            /**
             * Comparator that sorts on data center name first, and then cluster name. Ignoring case.
             */
            final class DataCenterWithClusterComparator implements Comparator<DataCenterWithCluster> {

                 @Override
                 public int compare(DataCenterWithCluster clusterWithDc1, DataCenterWithCluster clusterWithDc2) {
                     if (clusterWithDc1.getDataCenter().getName() != null
                             && clusterWithDc2.getDataCenter().getName() == null) {
                         return -1;
                     } else if (clusterWithDc2.getDataCenter().getName() != null
                             && clusterWithDc1.getDataCenter().getName() == null) {
                         return 1;
                     } else if (clusterWithDc1.getDataCenter().getName() == null
                             && clusterWithDc2.getDataCenter().getName() == null) {
                         return 0;
                     }
                     if (clusterWithDc1.getDataCenter().getName().equals(clusterWithDc2.getDataCenter().getName())) {
                         return clusterWithDc1.getCluster().getName().compareToIgnoreCase(
                                 clusterWithDc2.getCluster().getName());
                     } else {
                         return clusterWithDc1.getDataCenter().getName().compareToIgnoreCase(
                                 clusterWithDc2.getDataCenter().getName());
                     }
                 }
             }

        });

        quotaEditor = new ListModelTypeAheadListBoxEditor<Quota>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<Quota>() {

                    @Override
                    public String getReplacementStringNullSafe(Quota data) {
                        return data.getQuotaName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(Quota data) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                data.getQuotaName(),
                                data.getDescription()
                        );
                    }

                },
                new ModeSwitchingVisibilityRenderer());

        baseTemplateEditor = new ListModelTypeAheadListBoxEditor<VmTemplate>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<VmTemplate>() {

                    @Override
                    public String getReplacementStringNullSafe(VmTemplate data) {
                        return data.getName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(VmTemplate data) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                data.getName(),
                                data.getDescription()
                        );
                    }
                },
                new ModeSwitchingVisibilityRenderer());

        templateWithVersionEditor = new ListModelTypeAheadListBoxEditor<>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<TemplateWithVersion>(){
                    @Override
                    public String getReplacementStringNullSafe(TemplateWithVersion templateWithVersion) {
                        return getFirstColumn(templateWithVersion)
                                + " | " //$NON-NLS-1$
                                + getSecondColumn(templateWithVersion);
                    }

                    @Override
                    public String getDisplayStringNullSafe(TemplateWithVersion templateWithVersion) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                getFirstColumn(templateWithVersion),
                                getSecondColumn(templateWithVersion));
                    }

                    private String getFirstColumn(TemplateWithVersion templateWithVersion) {
                        return templateWithVersion.getBaseTemplate().getName();
                    }

                    private String getSecondColumn(TemplateWithVersion templateWithVersion) {
                        final VmTemplate versionTemplate = templateWithVersion.getTemplateVersion();
                        final String versionName = versionTemplate.getTemplateVersionName() == null
                                ? "" //$NON-NLS-1$
                                : versionTemplate.getTemplateVersionName() + " "; //$NON-NLS-1$
                        return templateWithVersion.isLatest()
                                ? constants.latest()
                                : versionName + "(" //$NON-NLS-1$
                                        + versionTemplate.getTemplateVersionNumber() + ")"; //$NON-NLS-1$
                    }
                },
                new ModeSwitchingVisibilityRenderer(),
                new SuggestionMatcher.ContainsSuggestionMatcher());

        oSTypeEditor = new ListModelListBoxEditor<Integer>(new AbstractRenderer<Integer>() {
            @Override
            public String render(Integer object) {
                return AsyncDataProvider.getInstance().getOsName(object);
            }
        }, new ModeSwitchingVisibilityRenderer());

        vmTypeEditor = new ListModelListBoxEditor<VmType>(new EnumRenderer<VmType>(), new ModeSwitchingVisibilityRenderer());

        instanceTypesEditor = new ListModelTypeAheadListBoxEditor<InstanceType>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<InstanceType>() {

                    @Override
                    public String getReplacementStringNullSafe(InstanceType data) {
                        return data.getName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(InstanceType data) {
                            return typeAheadNameDescriptionTemplateNullSafe(
                                    data.getName(),
                                    data.getDescription()
                            );
                    }
                },
                new ModeSwitchingVisibilityRenderer()
        );


        emulatedMachine = new ListModelTypeAheadChangeableListBoxEditor(
                new ListModelTypeAheadChangeableListBoxEditor.NullSafeSuggestBoxRenderer() {

                    @Override
                    public String getDisplayStringNullSafe(String data) {
                        if (data == null || data.trim().isEmpty()) {
                            data = getDefaultEmulatedMachineLabel();
                        }
                        return typeAheadNameTemplateNullSafe(data);
                    }
                },
                false,
                new ModeSwitchingVisibilityRenderer(),
                constants.clusterDefaultOption());

        customCpu = new ListModelTypeAheadChangeableListBoxEditor(
                new ListModelTypeAheadChangeableListBoxEditor.NullSafeSuggestBoxRenderer() {

                    @Override
                    public String getDisplayStringNullSafe(String data) {
                        if (data == null || data.trim().isEmpty()) {
                            data = getDefaultCpuTypeLabel();
                        }
                        return typeAheadNameTemplateNullSafe(data);
                    }
                },
                false,
                new ModeSwitchingVisibilityRenderer(),
                constants.clusterDefaultOption());

        numOfSocketsEditor = new ListModelListBoxEditor<Integer>(new ModeSwitchingVisibilityRenderer());
        numOfSocketsEditorWithDetachable = new EntityModelDetachableWidgetWithLabel(numOfSocketsEditor);
        corePerSocketEditor = new ListModelListBoxEditor<Integer>(new ModeSwitchingVisibilityRenderer());
        corePerSocketEditorWithDetachable = new EntityModelDetachableWidgetWithLabel(corePerSocketEditor);

        // Pools
        poolTypeEditor = new ListModelListBoxEditor<EntityModel<VmPoolType>>(new NullSafeRenderer<EntityModel<VmPoolType>>() {
            @Override
            public String renderNullSafe(EntityModel<VmPoolType> object) {
                return object.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        timeZoneEditor = new ListModelListBoxOnlyEditor<TimeZoneModel>(new NullSafeRenderer<TimeZoneModel>() {
            @Override
            public String renderNullSafe(TimeZoneModel timeZone) {
                if (timeZone.isDefault()) {
                    return messages.defaultTimeZoneCaption(timeZone.getDisplayValue());
                } else {
                    return timeZone.getDisplayValue();
                }
            }
        }, new ModeSwitchingVisibilityRenderer());

        EnableableFormLabel label = new EnableableFormLabel();
        label.setText(constants.tzVmPopup());
        timeZoneEditorWithInfo = new EntityModelWidgetWithInfo(label, timeZoneEditor);
        timeZoneEditorWithInfo.setExplanation(templates.italicText(constants.timeZoneInfo()));

        // Console tab
        displayTypeEditor = new ListModelListBoxEditor<>(
                new EnumRenderer<DisplayType>(),
                new ModeSwitchingVisibilityRenderer());

        graphicsTypeEditor = new ListModelListBoxEditor<>(new EnumRenderer<UnitVmModel.GraphicsTypes>());

        usbSupportEditor =
                new ListModelListBoxEditor<UsbPolicy>(new EnumRenderer<UsbPolicy>(), new ModeSwitchingVisibilityRenderer());
        consoleDisconnectActionEditor =
                new ListModelListBoxEditor<ConsoleDisconnectAction>(new EnumRenderer<ConsoleDisconnectAction>(), new ModeSwitchingVisibilityRenderer());

        numOfMonitorsEditor = new ListModelListBoxEditor<Integer>(new NullSafeRenderer<Integer>() {
            @Override
            public String renderNullSafe(Integer object) {
                return object.toString();
            }
        }, new ModeSwitchingVisibilityRenderer());

        vncKeyboardLayoutEditor = new ListModelListBoxEditor<String>(new VncKeyMapRenderer(), new ModeSwitchingVisibilityRenderer());

        // Host Tab
        specificHost = new RadioButton("runVmOnHostGroup"); //$NON-NLS-1$
        isAutoAssignEditor =
                new EntityModelRadioButtonEditor("runVmOnHostGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        defaultHostEditor = new ListModelMultipleSelectListBoxEditor<>(new NameRenderer<VDS>(),
                new ModeSwitchingVisibilityRenderer());
        defaultHostEditor.asListBox().setVisibleItemCount(3);

        migrationModeEditor =
                new ListModelListBoxEditor<MigrationSupport>(new EnumRenderer<MigrationSupport>(), new ModeSwitchingVisibilityRenderer());

        overrideMigrationDowntimeEditor = new EntityModelCheckBoxOnlyEditor(new ModeSwitchingVisibilityRenderer(), false);
        migrationDowntimeEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());

        autoConvergeEditor = new ListModelListBoxEditor<Boolean>(
                new BooleanRendererWithNullText(constants.autoConverge(), constants.dontAutoConverge(), constants.inheritFromCluster()),
                new ModeSwitchingVisibilityRenderer());

        migrateCompressedEditor = new ListModelListBoxEditor<Boolean>(
                new BooleanRendererWithNullText(constants.compress(), constants.dontCompress(), constants.inheritFromCluster()),
                new ModeSwitchingVisibilityRenderer());

        // Resource Allocation
        provisioningThinEditor =
                new EntityModelRadioButtonEditor("provisioningGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        provisioningCloneEditor =
                new EntityModelRadioButtonEditor("provisioningGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$

        // Boot Options Tab
        firstBootDeviceEditor = new ListModelListBoxEditor<EntityModel<BootSequence>>(new NullSafeRenderer<EntityModel<BootSequence>>() {
            @Override
            public String renderNullSafe(EntityModel<BootSequence> object) {
                return object.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        secondBootDeviceEditor = new ListModelListBoxEditor<EntityModel<BootSequence>>(new NullSafeRenderer<EntityModel<BootSequence>>() {
            @Override
            public String renderNullSafe(EntityModel<BootSequence> object) {
                return object.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        cdImageEditor = new ListModelListBoxEditor<String>(new NullSafeRenderer<String>() {
            @Override
            public String renderNullSafe(String object) {
                return object;
            }
        }, new ModeSwitchingVisibilityRenderer());

        cpuProfilesEditor = new ListModelListBoxEditor<>(new NameRenderer<CpuProfile>());

        cpuSharesAmountSelectionEditor =
                new ListModelListBoxOnlyEditor<UnitVmModel.CpuSharesAmount>(new EnumRenderer<UnitVmModel.CpuSharesAmount>(), new ModeSwitchingVisibilityRenderer());

        numaTuneMode = new ListModelListBoxEditor<NumaTuneMode>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());

        providersEditor = new ListModelListBoxEditor<>(new NameRenderer<Provider<OpenstackNetworkProviderProperties>>());
        providersEditor.setLabel(constants.providerLabel());
    }

    private String typeAheadNameDescriptionTemplateNullSafe(String name, String description) {
        return templates.typeAheadNameDescription(
                name != null ? name : "",
                description != null ? description : "")
                .asString();
    }

    private String typeAheadNameTemplateNullSafe(String name) {
        if (name != null && !name.trim().isEmpty()) {
            return templates.typeAheadName(name).asString();
        } else {
            return templates.typeAheadEmptyContent().asString();
        }
    }

    protected void localize() {
        // Tabs
        highAvailabilityTab.setLabel(constants.highAvailVmPopup());
        resourceAllocationTab.setLabel(constants.resourceAllocVmPopup());
        bootOptionsTab.setLabel(constants.bootOptionsVmPopup());
        customPropertiesTab.setLabel(constants.customPropsVmPopup());
        systemTab.setLabel(constants.systemVmPopup());

        // General Tab
        generalTab.setLabel(constants.GeneralVmPopup());
        dataCenterWithClusterEditor.setLabel(constants.hostClusterVmPopup());
        quotaEditor.setLabel(constants.quotaVmPopup());
        nameLabel.setText(constants.nameVmPopup());
        templateVersionNameEditor.setLabel(constants.templateVersionName());

        vmIdEditor.setLabel(constants.vmIdPopup());
        descriptionEditor.setLabel(constants.descriptionVmPopup());
        commentEditor.setLabel(constants.commentLabel());

        baseTemplateEditor.setLabel(constants.basedOnTemplateVmPopup());
        templateWithVersionEditor.setLabel(constants.template());
        detachableInstanceTypesEditor.setLabel(constants.instanceType());

        oSTypeEditor.setLabel(constants.osVmPopup());
        vmTypeEditor.setLabel(constants.optimizedFor());
        isStatelessEditor.setLabel(constants.statelessVmPopup());
        isRunAndPauseEditor.setLabel(constants.runAndPauseVmPopup());
        isDeleteProtectedEditor.setLabel(constants.deleteProtectionPopup());
        isConsoleDeviceEnabledEditor.setLabel(constants.consoleDeviceEnabled());
        copyTemplatePermissionsEditor.setLabel(constants.copyTemplatePermissions());
        isSmartcardEnabledEditor.setLabel(constants.smartcardVmPopup());
        isMemoryBalloonDeviceEnabled.setLabel(constants.memoryBalloonDeviceEnabled());
        isIoThreadsEnabled.setLabel(constants.ioThreadsEnabled());
        isVirtioScsiEnabled.setLabel(constants.isVirtioScsiEnabled());

        // Rng device tab
        rngDeviceTab.setLabel(constants.rngDeviceTab());
        isRngEnabledEditor.setLabel(constants.rngDevEnabled());
        rngPeriodEditor.setLabel(constants.rngPeriod());
        rngBytesEditor.setLabel(constants.rngBytes());
        rngSourceRandom.setLabel(constants.rngSourceRandom());
        rngSourceHwrng.setLabel(constants.rngSourceHwrng());


        // Pools Tab
        poolTab.setLabel(constants.poolVmPopup());
        poolTypeEditor.setLabel(constants.poolTypeVmPopup());
        editPrestartedVmsLabel.setText(constants.prestartedVms());

        prestartedLabel.setText(constants.prestartedPoolPopup());
        numOfVmsEditor.setLabel(constants.numOfVmsPoolPopup());
        maxAssignedVmsPerUserEditor.setLabel(constants.maxAssignedVmsPerUser());
        editMaxAssignedVmsPerUserEditor.setLabel(constants.maxAssignedVmsPerUser());

        // initial run Tab
        initialRunTab.setLabel(constants.initialRunVmPopup());

        vmInitEnabledEditor.setLabel(constants.cloudInitOrSysprep());

        // Console Tab
        consoleTab.setLabel(constants.consoleVmPopup());
        displayTypeEditor.setLabel(constants.videoType());
        graphicsTypeEditor.setLabel(constants.graphicsProtocol());
        vncKeyboardLayoutEditor.setLabel(constants.vncKeyboardLayoutVmPopup());
        usbSupportEditor.setLabel(constants.usbPolicyVmPopup());
        consoleDisconnectActionEditor.setLabel(constants.consoleDisconnectActionVmPopup());
        numOfMonitorsEditor.setLabel(constants.monitorsVmPopup());
        allowConsoleReconnectEditor.setLabel(constants.allowConsoleReconnect());
        isSoundcardEnabledEditor.setLabel(constants.soundcardEnabled());
        isSingleQxlEnabledEditor.setLabel(constants.singleQxlEnabled());
        ssoMethodNone.setLabel(constants.none());
        ssoMethodGuestAgent.setLabel(constants.guestAgent());
        spiceProxyEditor.setLabel(constants.overriddenSpiceProxyAddress());
        spiceFileTransferEnabledEditor.setLabel(constants.spiceFileTransferEnabled());
        spiceCopyPasteEnabledEditor.setLabel(constants.spiceCopyPasteEnabled());

        // Host Tab
        hostTab.setLabel(constants.hostVmPopup());
        isAutoAssignEditor.setLabel(constants.anyHostInClusterVmPopup());
        // specificHostEditor.setLabel("Specific");
        hostCpuEditor.setLabel(constants.passThroughHostCpu());
        cpuPinning.setLabel(constants.cpuPinningLabel());

        // numa
        numaTuneMode.setLabel(constants.numaTunaModeLabel());
        numaNodeCount.setLabel(constants.numaNodeCountLabel());
        numaSupportButton.setLabel(constants.numaSupportButtonLabel());
        // High Availability Tab
        isHighlyAvailableEditor.setLabel(constants.highlyAvailableVmPopup());

        // watchdog
        watchdogActionEditor.setLabel(constants.watchdogAction());
        watchdogModelEditor.setLabel(constants.watchdogModel());

        // priority
        priorityEditor.setLabel(constants.priorityVm());

        // Resource Allocation Tab
        cpuProfilesEditor.setLabel(constants.cpuProfileLabel());
        provisioningEditor.setLabel(constants.templateProvisVmPopup());
        provisioningThinEditor.setLabel(constants.thinVmPopup());
        provisioningCloneEditor.setLabel(constants.cloneVmPopup());
        minAllocatedMemoryEditor.setLabel(constants.physMemGuarVmPopup());
        numOfIoThreadsEditor.setLabel(constants.numOfIoThreadsVmPopup());

        // Boot Options
        firstBootDeviceEditor.setLabel(constants.firstDeviceVmPopup());
        secondBootDeviceEditor.setLabel(constants.secondDeviceVmPopup());
        kernel_pathEditor.setLabel(constants.kernelPathVmPopup());
        initrd_pathEditor.setLabel(constants.initrdPathVmPopup());
        kernel_parametersEditor.setLabel(constants.kernelParamsVmPopup());

        // System tab
        memSizeEditor.setLabel(constants.memSizeVmPopup());
        detachableMemSizeEditor.setLabel(constants.memSizeVmPopup());
        totalvCPUsEditor.setLabel(constants.numOfVCPUs());
        corePerSocketEditorWithDetachable.setLabel(constants.coresPerSocket());
        numOfSocketsEditorWithDetachable.setLabel(constants.numOfSockets());
        emulatedMachine.setLabel(constants.emulatedMachineLabel());
        customCpu.setLabel(constants.cpuModelLabel());

        // Icon tab
        iconTab.setLabel(constants.iconTabVmPopup());

        // Foreman tab
        foremanTab.setLabel(constants.foremanLabel());
    }

    protected void applyStyles() {
        hostCpuEditor.addContentWidgetContainerStyleName(style.longCheckboxContent());
        allowConsoleReconnectEditor.addContentWidgetContainerStyleName(style.longCheckboxContent());
        provisioningEditor.addContentWidgetContainerStyleName(style.provisioningEditorContent());
        provisioningThinEditor.addContentWidgetContainerStyleName(style.provisioningRadioContent());
        provisioningCloneEditor.addContentWidgetContainerStyleName(style.provisioningRadioContent());
        cdAttachedEditor.addContentWidgetContainerStyleName(style.cdAttachedLabelWidth());
        cdImageEditor.addContentWidgetContainerStyleName(style.cdImageEditor());
        numOfMonitorsEditor.addContentWidgetContainerStyleName(style.monitorsStyles());
        numOfMonitorsEditor.setStyleName(style.monitorsStyles());
        numOfMonitorsEditor.hideLabel();
        migrationModeEditor.addContentWidgetContainerStyleName(style.migrationSelectorInner());
        isVirtioScsiEnabled.addContentWidgetContainerStyleName(style.isVirtioScsiEnabledEditor());
        applyStylesForDetachableWithInfoIcon();
    }

    protected void applyStylesForDetachableWithInfoIcon() {
        totalvCPUsEditorWithInfoIcon.getContentWidget().setContentWrapperStypeName(style.totalVcpusStyle());
    }

    @Override
    public void edit(UnitVmModel model) {
        super.edit(model);
        unitVmModel = model;

        super.initializeModeSwitching(generalTab);

        driver.edit(model);
        profilesInstanceTypeEditor.edit(model.getNicsWithLogicalNetworks());
        instanceImagesEditor.edit(model.getInstanceImages());
        customPropertiesSheetEditor.edit(model.getCustomPropertySheet());
        vmInitEditor.edit(model.getVmInitModel());
        serialNumberPolicyEditor.edit(model.getSerialNumberPolicy());
        initTabAvailabilityListeners(model);
        initListeners(model);
        hideAlwaysHiddenFields();
        decorateDetachableFields();
        enableNumaSupport(model);
    }

    private void enableNumaSupport(final UnitVmModel model) {
        numaSupportButton.setCommand(model.getNumaSupportCommand());
        numaPanel.setVisible(false);
        enableNumaFields(false);
        model.getNumaEnabled().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                numaPanel.setVisible(true);
                enableNumaFields(model.getNumaEnabled().getEntity());
                setNumaInfoMsg(model.getNumaEnabled().getMessage());
            }
        });
    }

    private void setNumaInfoMsg(String message) {
        if (message == null) {
            message = ""; //$NON-NLS-1$
        }
        numaInfoIcon.setText(templates.italicText(message));
    }

    private void enableNumaFields(boolean enabled) {
        numaNodeCount.setEnabled(enabled);
        numaTuneMode.setEnabled(enabled);
        numaSupportButton.setEnabled(enabled);
    }

    @UiHandler("refreshButton")
    void handleRefreshButtonClick(ClickEvent event) {
        unitVmModel.getBehavior().refreshCdImages();
    }

    protected void setupCustomPropertiesAvailability(UnitVmModel model) {
        changeApplicationLevelVisibility(customPropertiesTab, model.getIsCustomPropertiesTabAvailable());
    }

    protected void initListeners(final UnitVmModel object) {
        // TODO should be handled by the core framework
        object.getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                String propName = args.propertyName;
                if ("IsCustomPropertiesTabAvailable".equals(propName)) { //$NON-NLS-1$
                    setupCustomPropertiesAvailability(object);
                } else if ("IsDisksAvailable".equals(propName)) { //$NON-NLS-1$
                    addDiskAllocation(object);
                }
            }
        });

        object.getIsAutoAssign().getPropertyChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                boolean isAutoAssign = object.getIsAutoAssign().getEntity();
                defaultHostEditor.setEnabled(!isAutoAssign);

                // only this is not bind to the model, so needs to listen to the change explicitly
                specificHost.setValue(!isAutoAssign);
            }
        });

        object.getProvisioning().getPropertyChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                boolean isProvisioningChangable = object.getProvisioning().getIsChangable();
                provisioningThinEditor.setEnabled(isProvisioningChangable);
                provisioningCloneEditor.setEnabled(isProvisioningChangable);

                boolean isProvisioningAvailable = object.getProvisioning().getIsAvailable();
                changeApplicationLevelVisibility(provisionSelectionPanel, isProvisioningAvailable);

                boolean isDisksAvailable = object.getIsDisksAvailable();
                changeApplicationLevelVisibility(disksAllocationPanel, isDisksAvailable ||
                        object.getIsVirtioScsiEnabled().getIsAvailable());

                changeApplicationLevelVisibility(storageAllocationPanel, isProvisioningAvailable);
            }
        });

        object.getIsVirtioScsiEnabled().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {

            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if ("IsAvailable".equals(args.propertyName)) { //$NON-NLS-1$
                    isVirtioScsiEnabledInfoIcon.setVisible(object.getIsVirtioScsiEnabled().getIsAvailable());
                }
            }
        });

        object.getUsbPolicy().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {

            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if ("SelectedItem".equals(args.propertyName)) { //$NON-NLS-1$
                    updateUsbNativeMessageVisibility(object);
                }
            }
        });

        updateUsbNativeMessageVisibility(object);

        object.getEditingEnabled().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                Boolean enabled = object.getEditingEnabled().getEntity();
                if (Boolean.FALSE.equals(enabled)) {
                    disableAllTabs();
                    generalWarningMessage.setText(object.getEditingEnabled().getMessage());
                }
            }
        });

        object.getCpuSharesAmountSelection().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if ("IsAvailable".equals(args.propertyName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(cpuSharesEditor, object.getCpuSharesAmountSelection().getIsAvailable());
                }
            }
        });

        object.getCloudInitEnabled().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if (object.getCloudInitEnabled().getEntity() != null) {
                    vmInitEditor.setCloudInitContentVisible(object.getCloudInitEnabled().getEntity());
                }
            }
        });

        object.getSysprepEnabled().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if (object.getSysprepEnabled().getEntity() != null) {
                    vmInitEditor.setSyspepContentVisible(object.getSysprepEnabled().getEntity());
                }
            }
        });

        object.getDataCenterWithClustersList().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                VDSGroup vdsGroup = object.getSelectedCluster();
                if (vdsGroup != null && vdsGroup.getCompatibilityVersion() != null) {
                    boolean enabled = AsyncDataProvider.getInstance().isSerialNumberPolicySupported(vdsGroup.getCompatibilityVersion().getValue());
                    changeApplicationLevelVisibility(serialNumberPolicyEditor, enabled);
                }
            }
        });

        object.getIsRngEnabled().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                rngPanel.setVisible(object.getIsRngEnabled().getEntity());
            }
        });

        object.getDataCenterWithClustersList().getSelectedItemChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                emulatedMachine.setNullReplacementString(getDefaultEmulatedMachineLabel());
                customCpu.setNullReplacementString(getDefaultCpuTypeLabel());
            }
        });
    }

    private String getDefaultEmulatedMachineLabel() {
        VDSGroup vdsGroup = getModel().getSelectedCluster();
        String newClusterEmulatedMachine = constants.clusterDefaultOption();
        if (vdsGroup != null) {
            String emulatedMachine = (vdsGroup.getEmulatedMachine() == null) ? "" : vdsGroup.getEmulatedMachine(); //$NON-NLS-1$
            newClusterEmulatedMachine +=  "(" + emulatedMachine + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return newClusterEmulatedMachine;
    }

    private String getDefaultCpuTypeLabel() {
        VDSGroup vdsGroup = getModel().getSelectedCluster();
        String newClusterCpuModel = constants.clusterDefaultOption();
        if (vdsGroup != null) {
            String cpuName = (vdsGroup.getCpuName() == null) ? "" : vdsGroup.getCpuName(); //$NON-NLS-1$
            newClusterCpuModel += "(" + cpuName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return newClusterCpuModel;
    }

    /**
     * This raises a warning for USB devices that won't persist a VM migration when using Native USB with SPICE in
     * certain, configurable cluster version.
     */
    protected void updateUsbNativeMessageVisibility(final UnitVmModel object) {
        Version vdsGroupVersion = clusterVersionOrNull(object);
        changeApplicationLevelVisibility(nativeUsbWarningMessage,
                object.getUsbPolicy().getSelectedItem() == UsbPolicy.ENABLED_NATIVE
                        && vdsGroupVersion != null
                        && !(Boolean) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.MigrationSupportForNativeUsb,
                                vdsGroupVersion.getValue()));
    }

    private Version clusterVersionOrNull(UnitVmModel model) {
        VDSGroup vdsGroup = model.getSelectedCluster();

        if (vdsGroup == null || vdsGroup.getCompatibilityVersion() == null) {
            return null;
        }

        return vdsGroup.getCompatibilityVersion();
    }

    private void addDiskAllocation(UnitVmModel model) {
        if (!model.getIsDisksAvailable()) {
            return;
        }
        disksAllocationView.edit(model.getDisksAllocationModel());
        model.getDisksAllocationModel().setDisks(model.getDisks());
    }

    private void initTabAvailabilityListeners(final UnitVmModel vm) {
        // TODO should be handled by the core framework
        vm.getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                String propName = args.propertyName;
                if ("IsHighlyAvailable".equals(propName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(highAvailabilityTab, vm.getIsHighlyAvailable().getEntity());
                } else if ("IsDisksAvailable".equals(propName)) { //$NON-NLS-1$
                    boolean isDisksAvailable = vm.getIsDisksAvailable();
                    changeApplicationLevelVisibility(disksPanel, isDisksAvailable);

                    boolean isProvisioningAvailable = vm.getProvisioning().getIsAvailable();
                    changeApplicationLevelVisibility(storageAllocationPanel, isProvisioningAvailable);

                    changeApplicationLevelVisibility(disksAllocationPanel, isDisksAvailable ||
                            vm.getIsVirtioScsiEnabled().getIsAvailable());

                    if (isDisksAvailable) {
                        // Update warning message by disks status
                        updateDisksWarningByImageStatus(vm.getDisks(), ImageStatus.ILLEGAL);
                        updateDisksWarningByImageStatus(vm.getDisks(), ImageStatus.LOCKED);
                    } else {
                        // Clear warning message
                        generalWarningMessage.setText(""); //$NON-NLS-1$
                    }
                }

            }
        });

        // TODO: Move to a more appropriate method
        vm.getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                String propName = args.propertyName;
                if ("IsLinuxOS".equals(propName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(linuxBootOptionsPanel, vm.getIsLinuxOS());
                }
            }
        });

        defaultHostEditor.setEnabled(false);
        specificHost.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                defaultHostEditor.setEnabled(specificHost.getValue());
                ValueChangeEvent.fire(isAutoAssignEditor.asRadioButton(), false);
            }
        });

        rngSourceRandom.asRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> booleanValueChangeEvent) {
                vm.getRngSourceRandom().setEntity(true);
                vm.getRngSourceHwrng().setEntity(false);
            }
        });

        rngSourceHwrng.asRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> booleanValueChangeEvent) {
                vm.getRngSourceHwrng().setEntity(true);
                vm.getRngSourceRandom().setEntity(false);
            }
        });

        // TODO: This is a hack and should be handled cleanly via model property availability
        isAutoAssignEditor.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                defaultHostEditor.setEnabled(false);
            }
        }, ClickEvent.getType());

        vm.getIsAutoAssign().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {

            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                if (!isAutoAssignEditor.asRadioButton().getValue())
                    specificHost.setValue(true, true);
            }
        });

        ssoMethodGuestAgent.asRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (Boolean.TRUE.equals(event.getValue())) {
                    ValueChangeEvent.fire(ssoMethodNone.asRadioButton(), false);
                }
            }
        });

        ssoMethodNone.asRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (Boolean.TRUE.equals(event.getValue())) {
                    ValueChangeEvent.fire(ssoMethodGuestAgent.asRadioButton(), false);
                }
            }
        });
    }

    private void updateDisksWarningByImageStatus(List<DiskModel> disks, ImageStatus imageStatus) {
        ArrayList<String> disksAliases =
                getDisksAliasesByImageStatus(disks, imageStatus);

        if (!disksAliases.isEmpty()) {
            generalWarningMessage.setText(messages.disksStatusWarning(
                    EnumTranslator.getInstance().translate(imageStatus),
                    (StringUtils.join(disksAliases, ", ")))); //$NON-NLS-1$
        }
    }

    private ArrayList<String> getDisksAliasesByImageStatus(List<DiskModel> disks, ImageStatus status) {
        ArrayList<String> disksAliases = new ArrayList<String>();

        if (disks == null) {
            return disksAliases;
        }

        for (DiskModel diskModel : disks) {
            Disk disk = diskModel.getDisk();
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                    ((DiskImage) disk).getImageStatus() == status) {

                disksAliases.add(disk.getDiskAlias());
            }
        }

        return disksAliases;
    }

    @Override
    public UnitVmModel flush() {
        profilesInstanceTypeEditor.flush();
        vmInitEditor.flush();
        serialNumberPolicyEditor.flush();
        return driver.flush();
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    public interface ButtonCellTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/ButtonCellTable.css" })
        TableStyle cellTableStyle();
    }

    @Override
    public int setTabIndexes(int nextTabIndex) {
        // ==General Tab==
        nextTabIndex = generalTab.setTabIndexes(nextTabIndex);
        quotaEditor.setTabIndex(nextTabIndex++);
        oSTypeEditor.setTabIndex(nextTabIndex++);
        baseTemplateEditor.setTabIndex(nextTabIndex++);
        instanceTypesEditor.setTabIndexes(nextTabIndex++);
        templateWithVersionEditor.setTabIndexes(nextTabIndex++);

        nameEditor.setTabIndex(nextTabIndex++);
        templateVersionNameEditor.setTabIndex(nextTabIndex++);
        vmIdEditor.setTabIndex(nextTabIndex++);
        descriptionEditor.setTabIndex(nextTabIndex++);
        commentEditor.setTabIndex(nextTabIndex++);
        isStatelessEditor.setTabIndex(nextTabIndex++);
        isRunAndPauseEditor.setTabIndex(nextTabIndex++);
        isDeleteProtectedEditor.setTabIndex(nextTabIndex++);
        copyTemplatePermissionsEditor.setTabIndex(nextTabIndex++);

        numOfVmsEditor.setTabIndex(nextTabIndex++);
        prestartedVmsEditor.setTabIndex(nextTabIndex++);
        editPrestartedVmsEditor.setTabIndex(nextTabIndex++);
        incraseNumOfVmsEditor.setTabIndex(nextTabIndex++);
        maxAssignedVmsPerUserEditor.setTabIndex(nextTabIndex++);
        editMaxAssignedVmsPerUserEditor.setTabIndex(nextTabIndex++);

        // ==System Tab==
        nextTabIndex = systemTab.setTabIndexes(nextTabIndex);
        memSizeEditor.setTabIndex(nextTabIndex++);
        totalvCPUsEditor.setTabIndex(nextTabIndex++);

        nextTabIndex = vcpusAdvancedParameterExpander.setTabIndexes(nextTabIndex);
        corePerSocketEditor.setTabIndex(nextTabIndex++);
        numOfSocketsEditor.setTabIndex(nextTabIndex++);
        emulatedMachine.setTabIndex(nextTabIndex++);
        customCpu.setTabIndex(nextTabIndex++);
        nextTabIndex = serialNumberPolicyEditor.setTabIndexes(nextTabIndex);

        // == Pools ==
        nextTabIndex = poolTab.setTabIndexes(nextTabIndex);
        poolTypeEditor.setTabIndex(nextTabIndex++);

        // ==Initial run Tab==
        nextTabIndex = initialRunTab.setTabIndexes(nextTabIndex);
        timeZoneEditor.setTabIndex(nextTabIndex++);

        // ==Console Tab==
        nextTabIndex = consoleTab.setTabIndexes(nextTabIndex);
        displayTypeEditor.setTabIndex(nextTabIndex++);
        graphicsTypeEditor.setTabIndex(nextTabIndex++);
        vncKeyboardLayoutEditor.setTabIndex(nextTabIndex++);
        usbSupportEditor.setTabIndex(nextTabIndex++);
        consoleDisconnectActionEditor.setTabIndexes(nextTabIndex++);
        isSingleQxlEnabledEditor.setTabIndex(nextTabIndex++);
        numOfMonitorsEditor.setTabIndex(nextTabIndex++);
        isSmartcardEnabledEditor.setTabIndex(nextTabIndex++);
        ssoMethodNone.setTabIndex(nextTabIndex++);
        ssoMethodGuestAgent.setTabIndex(nextTabIndex++);
        nextTabIndex = expander.setTabIndexes(nextTabIndex);
        allowConsoleReconnectEditor.setTabIndex(nextTabIndex++);
        isSoundcardEnabledEditor.setTabIndex(nextTabIndex++);
        isConsoleDeviceEnabledEditor.setTabIndex(nextTabIndex++);
        spiceProxyOverrideEnabledEditor.setTabIndex(nextTabIndex++);
        spiceProxyEditor.setTabIndex(nextTabIndex++);
        spiceFileTransferEnabledEditor.setTabIndex(nextTabIndex++);
        spiceCopyPasteEnabledEditor.setTabIndex(nextTabIndex++);

        // ==Host Tab==
        nextTabIndex = hostTab.setTabIndexes(nextTabIndex);
        isAutoAssignEditor.setTabIndex(nextTabIndex++);
        specificHost.setTabIndex(nextTabIndex++);
        defaultHostEditor.setTabIndex(nextTabIndex++);
        migrationModeEditor.setTabIndex(nextTabIndex++);
        overrideMigrationDowntimeEditor.setTabIndex(nextTabIndex++);
        migrationDowntimeEditor.setTabIndex(nextTabIndex++);
        autoConvergeEditor.setTabIndex(nextTabIndex++);
        migrateCompressedEditor.setTabIndex(nextTabIndex++);
        hostCpuEditor.setTabIndex(nextTabIndex++);

        numaNodeCount.setTabIndex(nextTabIndex++);
        numaTuneMode.setTabIndex(nextTabIndex++);
        // ==High Availability Tab==
        nextTabIndex = highAvailabilityTab.setTabIndexes(nextTabIndex);
        isHighlyAvailableEditor.setTabIndex(nextTabIndex++);
        priorityEditor.setTabIndex(nextTabIndex++);

        watchdogModelEditor.setTabIndex(nextTabIndex++);
        watchdogActionEditor.setTabIndex(nextTabIndex++);

        // ==Resource Allocation Tab==
        nextTabIndex = resourceAllocationTab.setTabIndexes(nextTabIndex);
        cpuProfilesEditor.setTabIndex(nextTabIndex++);
        minAllocatedMemoryEditor.setTabIndex(nextTabIndex++);
        numOfIoThreadsEditor.setTabIndex(nextTabIndex++);
        provisioningEditor.setTabIndex(nextTabIndex++);
        provisioningThinEditor.setTabIndex(nextTabIndex++);
        provisioningCloneEditor.setTabIndex(nextTabIndex++);
        cpuPinning.setTabIndex(nextTabIndex++);
        cpuSharesAmountEditor.setTabIndex(nextTabIndex++);
        nextTabIndex = disksAllocationView.setTabIndexes(nextTabIndex);

        // ==Boot Options Tab==
        nextTabIndex = bootOptionsTab.setTabIndexes(nextTabIndex);
        firstBootDeviceEditor.setTabIndex(nextTabIndex++);
        secondBootDeviceEditor.setTabIndex(nextTabIndex++);
        cdAttachedEditor.setTabIndex(nextTabIndex++);
        cdImageEditor.setTabIndex(nextTabIndex++);
        bootMenuEnabledEditor.setTabIndex(nextTabIndex++);
        kernel_pathEditor.setTabIndex(nextTabIndex++);
        initrd_pathEditor.setTabIndex(nextTabIndex++);
        kernel_parametersEditor.setTabIndex(nextTabIndex++);

        // ==Rng Tab==
        nextTabIndex = rngDeviceTab.setTabIndexes(nextTabIndex);
        isRngEnabledEditor.setTabIndex(nextTabIndex++);
        rngPeriodEditor.setTabIndex(nextTabIndex++);
        rngBytesEditor.setTabIndex(nextTabIndex++);
        rngSourceRandom.setTabIndex(nextTabIndex++);
        rngSourceHwrng.setTabIndex(nextTabIndex++);

        // ==Custom Properties Tab==
        nextTabIndex = customPropertiesTab.setTabIndexes(nextTabIndex);

        // ==Icon Tab==
        nextTabIndex = iconTab.setTabIndexes(nextTabIndex);
        iconEditorWidget.setTabIndex(nextTabIndex++);

        // ==Foreman Tab==
        nextTabIndex = foremanTab.setTabIndexes(nextTabIndex);
        return nextTabIndex;
    }

    @Override
    protected PopupWidgetConfigMap createWidgetConfiguration() {
        return super.createWidgetConfiguration().
                putAll(allTabs(), simpleField().visibleInAdvancedModeOnly()).
                putAll(advancedFieldsFromGeneralTab(), simpleField().visibleInAdvancedModeOnly()).
                putAll(consoleTabWidgets(), simpleField().visibleInAdvancedModeOnly()).
                update(consoleTab, simpleField()).
                update(numOfMonitorsEditor, simpleField()).
                update(isSingleQxlEnabledEditor, simpleField()).
                putOne(isSoundcardEnabledEditor, simpleField().visibleInAdvancedModeOnly()).
                putOne(isConsoleDeviceEnabledEditor, simpleField().visibleInAdvancedModeOnly()).
                putOne(spiceFileTransferEnabledEditor, simpleField().visibleInAdvancedModeOnly()).
                putOne(spiceCopyPasteEnabledEditor, simpleField().visibleInAdvancedModeOnly()).
                putOne(instanceImagesEditor, hiddenField());
    }

    protected List<Widget> consoleTabWidgets() {
        return Arrays.<Widget> asList(
                displayTypeEditor,
                graphicsTypeEditor,
                usbSupportEditor,
                consoleDisconnectActionEditor,
                isSmartcardEnabledEditor,
                nativeUsbWarningMessage,
                expander,
                numOfMonitorsEditor,
                vncKeyboardLayoutEditor,
                ssoMethodLabel,
                ssoMethodNone,
                ssoMethodGuestAgent
        );
    }

    protected List<Widget> poolSpecificFields() {
        return Arrays.<Widget> asList(numOfVmsEditor,
                newPoolEditVmsPanel,
                editPoolEditVmsPanel,
                editPoolIncraseNumOfVmsPanel,
                poolTab,
                prestartedVmsEditor,
                poolNameIcon,
                newPoolEditMaxAssignedVmsPerUserPanel,
                editPoolEditMaxAssignedVmsPerUserPanel,
                spiceProxyEditor,
                spiceProxyEnabledCheckboxWithInfoIcon,
                spiceProxyOverrideEnabledEditor);
    }

    protected List<Widget> allTabs() {
        return Arrays.<Widget> asList(initialRunTab,
                consoleTab,
                hostTab,
                resourceAllocationTab,
                bootOptionsTab,
                customPropertiesTab,
                rngDeviceTab,
                highAvailabilityTab,
                poolTab,
                systemTab,
                iconTab,
                foremanTab);
    }

    protected List<Widget> advancedFieldsFromGeneralTab() {
        return Arrays.<Widget> asList(
                memSizeEditor,
                totalvCPUsEditor,
                vcpusAdvancedParameterExpander,
                copyTemplatePermissionsEditor,
                vmIdEditor
                );
    }

    protected List<Widget> detachableWidgets() {
        return Arrays.<Widget> asList(
                totalvCPUsEditorWithInfoIcon,
                numOfSocketsEditorWithDetachable,
                corePerSocketEditorWithDetachable,
                isHighlyAvailableEditorWithDetachable,
                priorityLabelWithDetachable,
                migrationModeEditorWithDetachable,
                memAllocationLabel,
                ioThreadsLabel,
                detachableMemSizeEditor,
                detachableInstanceTypesEditor,
                overrideMigrationDowntimeEditorWithDetachable
        );
    }

    protected List<Widget> adminOnlyWidgets() {
        return Arrays.<Widget> asList(
                // general tab
                vmIdEditor,

                // system tab
                detachableMemSizeEditor,
                totalvCPUsEditorWithInfoIcon,
                vcpusAdvancedParameterExpander,
                serialNumberPolicyEditor,

                // console tab
                usbSupportEditor,
                consoleDisconnectActionEditor,
                monitors,
                isSingleQxlEnabledEditor,
                ssoMethodLabel,
                ssoMethodNone,
                ssoMethodGuestAgent,
                expander,
                spiceProxyEnabledCheckboxWithInfoIcon,
                spiceProxyEditor,

                // rest of the tabs
                initialRunTab,
                hostTab,
                highAvailabilityTab,
                resourceAllocationTab,
                customPropertiesTab,
                rngDeviceTab
        );
    }

    protected void disableAllTabs() {
        for (DialogTab dialogTab : allDialogTabs()) {
            dialogTab.disableContent();
        }

        oSTypeEditor.setEnabled(false);
        quotaEditor.setEnabled(false);
        dataCenterWithClusterEditor.setEnabled(false);
        baseTemplateEditor.setEnabled(false);
        templateWithVersionEditor.setEnabled(false);
        vmTypeEditor.setEnabled(false);
        detachableInstanceTypesEditor.setEnabled(false);
        emulatedMachine.setEnabled(false);
        customCpu.setEnabled(false);
    }

    private List<DialogTab> allDialogTabs() {
        return Arrays.asList(
            generalTab,
            poolTab,
            initialRunTab,
            consoleTab,
            hostTab,
            highAvailabilityTab,
            resourceAllocationTab,
            bootOptionsTab,
            customPropertiesTab,
            systemTab,
            rngDeviceTab,
            iconTab,
            foremanTab
        );
    }

    protected void updateOrAddToWidgetConfiguration(PopupWidgetConfigMap configuration, List<Widget> widgets, WidgetConfigurationUpdater updater) {
        for (Widget widget: widgets) {
            if (configuration.containsKey(widget)) {
                configuration.update(widget, updater.updateConfiguration(configuration.get(widget)));
            } else {
                configuration.putOne(widget, updater.updateConfiguration(simpleField()));
            }
        }
    }

    protected static interface WidgetConfigurationUpdater {
        PopupWidgetConfig updateConfiguration(PopupWidgetConfig original);
    }

    protected static class UpdateToDetachable implements WidgetConfigurationUpdater {
        public static final UpdateToDetachable INSTANCE = new UpdateToDetachable();


        @Override
        public PopupWidgetConfig updateConfiguration(PopupWidgetConfig original) {
            return original.detachable();
        }
    }

    protected static class UpdateToAdminOnly implements WidgetConfigurationUpdater {
        public static final UpdateToAdminOnly INSTANCE = new UpdateToAdminOnly();


        @Override
        public PopupWidgetConfig updateConfiguration(PopupWidgetConfig original) {
            return original.visibleForAdminOnly();
        }
    }

    protected void decorateDetachableFields() {
        for (Widget decoratedWidget : getWidgetConfiguration().getDetachables().keySet()) {
            if (decoratedWidget instanceof HasDetachable) {
                ((HasDetachable) decoratedWidget).setDetachableIconVisible(true);
            }
        }
    }

    public void switchAttachToInstanceType(boolean attached) {
        for (Widget detachable : getWidgetConfiguration().getDetachables().keySet()) {
            if (detachable instanceof HasDetachable) {
                ((HasDetachable) detachable).setAttached(attached);
            }
        }
    }

    public void initCreateInstanceMode() {
        setCreateInstanceMode(true);
        for (Widget adminOnlyField : getWidgetConfiguration().getVisibleForAdminOnly().keySet()) {
            adminOnlyField.setVisible(false);
        }
    }

    public List<HasValidation> getInvalidWidgets() {
        List<HasValidation> hasValidations = new ArrayList<HasValidation>();
        for (DialogTab dialogTab : allDialogTabs()) {
            hasValidations.addAll(dialogTab.getInvalidWidgets());
        }

        return hasValidations;
    }

    @Override
    public DialogTabPanel getTabPanel() {
        return mainTabPanel;
    }

    @Override
    public Map<TabName, DialogTab> getTabNameMapping() {
        return tabMap;
    }

    public UiCommandButton getNumaSupportButton() {
        return numaSupportButton;
    }

    public UnitVmModel getModel() {
        return unitVmModel;
    }
}
