package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ovirt.engine.core.common.action.RunVmOnceParams;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.InitializationType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmInit;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.ICommandTarget;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NoTrimmingWhitespacesValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;

public abstract class RunOnceModel extends Model
{
    // Boot Options tab

    public static final String RUN_ONCE_COMMAND = "OnRunOnce"; //$NON-NLS-1$

    /** The VM that is about to run */
    protected final VM vm;

    /** Listener for events that are triggered by this model */
    protected ICommandTarget commandTarget;

    protected final UICommand runOnceCommand;
    protected final UICommand cancelCommand;

    private EntityModel<Boolean> privateAttachFloppy;

    public EntityModel<Boolean> getAttachFloppy()
    {
        return privateAttachFloppy;
    }

    private void setAttachFloppy(EntityModel<Boolean> value)
    {
        privateAttachFloppy = value;
    }

    private ListModel<String> privateFloppyImage;

    public ListModel<String> getFloppyImage()
    {
        return privateFloppyImage;
    }

    private void setFloppyImage(ListModel<String> value)
    {
        privateFloppyImage = value;
    }

    private EntityModel<Boolean> privateAttachIso;

    public EntityModel<Boolean> getAttachIso()
    {
        return privateAttachIso;
    }

    private void setAttachIso(EntityModel<Boolean> value)
    {
        privateAttachIso = value;
    }

    private ListModel<String> privateIsoImage;

    public ListModel<String> getIsoImage()
    {
        return privateIsoImage;
    }

    private void setIsoImage(ListModel<String> value)
    {
        privateIsoImage = value;
    }

    private ListModel<EntityModel<DisplayType>> privateDisplayProtocol;

    public ListModel<EntityModel<DisplayType>> getDisplayProtocol()
    {
        return privateDisplayProtocol;
    }

    private void setDisplayProtocol(ListModel<EntityModel<DisplayType>> value)
    {
        privateDisplayProtocol = value;
    }

    private EntityModel<String> privateInitrd_path;

    public EntityModel<String> getInitrd_path()
    {
        return privateInitrd_path;
    }

    private void setInitrd_path(EntityModel<String> value)
    {
        privateInitrd_path = value;
    }

    private EntityModel<String> privateKernel_path;

    public EntityModel<String> getKernel_path()
    {
        return privateKernel_path;
    }

    private void setKernel_path(EntityModel<String> value)
    {
        privateKernel_path = value;
    }

    // Linux Boot Options tab

    private EntityModel<String> privateKernel_parameters;

    public EntityModel<String> getKernel_parameters()
    {
        return privateKernel_parameters;
    }

    private void setKernel_parameters(EntityModel<String> value)
    {
        privateKernel_parameters = value;
    }

    // Initial Boot tab - Sysprep

    private ListModel<String> privateSysPrepDomainName;

    public ListModel<String> getSysPrepDomainName()
    {
        return privateSysPrepDomainName;
    }

    private void setSysPrepDomainName(ListModel<String> value)
    {
        privateSysPrepDomainName = value;
    }

    private EntityModel<String> privateSysPrepSelectedDomainName;

    public EntityModel<String> getSysPrepSelectedDomainName()
    {
        return privateSysPrepSelectedDomainName;
    }

    private void setSysPrepSelectedDomainName(EntityModel<String> value)
    {
        privateSysPrepSelectedDomainName = value;
    }

    private EntityModel<String> privateSysPrepUserName;

    public EntityModel<String> getSysPrepUserName()
    {
        return privateSysPrepUserName;
    }

    private void setSysPrepUserName(EntityModel<String> value)
    {
        privateSysPrepUserName = value;
    }

    private EntityModel<String> privateSysPrepPassword;

    public EntityModel<String> getSysPrepPassword()
    {
        return privateSysPrepPassword;
    }

    private void setSysPrepPassword(EntityModel<String> value)
    {
        privateSysPrepPassword = value;
    }

    private EntityModel<Boolean> privateUseAlternateCredentials;

    public EntityModel<Boolean> getUseAlternateCredentials()
    {
        return privateUseAlternateCredentials;
    }

    private void setUseAlternateCredentials(EntityModel<Boolean> value)
    {
        privateUseAlternateCredentials = value;
    }

    private EntityModel<Boolean> privateIsSysprepEnabled;

    public EntityModel<Boolean> getIsSysprepEnabled()
    {
        return privateIsSysprepEnabled;
    }

    private void setIsSysprepEnabled(EntityModel<Boolean> value)
    {
        privateIsSysprepEnabled = value;
    }

    private EntityModel<Boolean> privateIsSysprepPossible;

    public EntityModel<Boolean> getIsSysprepPossible()
    {
        return privateIsSysprepPossible;
    }

    private void setIsSysprepPossible(EntityModel<Boolean> value)
    {
        privateIsSysprepPossible = value;
    }

    // Initialization

    private EntityModel<Boolean> privateIsVmFirstRun;

    public EntityModel<Boolean> getIsVmFirstRun()
    {
        return privateIsVmFirstRun;
    }

    private void setIsVmFirstRun(EntityModel<Boolean> value)
    {
        privateIsVmFirstRun = value;
    }

    private EntityModel<Boolean> privateIsLinuxOptionsAvailable;

    public EntityModel<Boolean> getIsLinuxOptionsAvailable()
    {
        return privateIsLinuxOptionsAvailable;
    }

    private void setIsLinuxOptionsAvailable(EntityModel<Boolean> value)
    {
        privateIsLinuxOptionsAvailable = value;
    }

    // Initial Boot tab - Cloud-Init

    private EntityModel<Boolean> privateIsCloudInitEnabled;

    public EntityModel<Boolean> getIsCloudInitEnabled()
    {
        return privateIsCloudInitEnabled;
    }

    private void setIsCloudInitEnabled(EntityModel<Boolean> value)
    {
        privateIsCloudInitEnabled = value;
    }

    public VmInitModel privateVmInitModel;

    public VmInitModel getVmInit()
    {
        return privateVmInitModel;
    }

    public void setVmInit(VmInitModel value)
    {
        privateVmInitModel = value;
    }

    private EntityModel<Boolean> privateIsCloudInitPossible;

    public EntityModel<Boolean> getIsCloudInitPossible()
    {
        return privateIsCloudInitPossible;
    }

    private void setIsCloudInitPossible(EntityModel<Boolean> value)
    {
        privateIsCloudInitPossible = value;
    }

    // Custom Properties tab

    private KeyValueModel customPropertySheet;

    public KeyValueModel getCustomPropertySheet() {
        return customPropertySheet;
    }

    public void setCustomPropertySheet(KeyValueModel customPropertySheet) {
        this.customPropertySheet = customPropertySheet;
    }

    private EntityModel<Boolean> bootMenuEnabled;

    public EntityModel<Boolean> getBootMenuEnabled() {
        return bootMenuEnabled;
    }

    public void setBootMenuEnabled(EntityModel<Boolean> bootMenuEnabled) {
        this.bootMenuEnabled = bootMenuEnabled;
    }

    private EntityModel<Boolean> privateRunAndPause;

    public EntityModel<Boolean> getRunAndPause()
    {
        return privateRunAndPause;
    }

    public void setRunAndPause(EntityModel<Boolean> value)
    {
        privateRunAndPause = value;
    }

    private EntityModel<Boolean> privateRunAsStateless;

    public EntityModel<Boolean> getRunAsStateless()
    {
        return privateRunAsStateless;
    }

    public void setRunAsStateless(EntityModel<Boolean> value)
    {
        privateRunAsStateless = value;
    }

    private EntityModel<Boolean> privateDisplayConsole_Vnc_IsSelected;

    public EntityModel<Boolean> getDisplayConsole_Vnc_IsSelected()
    {
        return privateDisplayConsole_Vnc_IsSelected;
    }

    public void setDisplayConsole_Vnc_IsSelected(EntityModel<Boolean> value)
    {
        privateDisplayConsole_Vnc_IsSelected = value;
    }

    private ListModel<String> vncKeyboardLayout;

    public ListModel<String> getVncKeyboardLayout() {
        return vncKeyboardLayout;
    }

    public void setVncKeyboardLayout(ListModel<String> vncKeyboardLayout) {
        this.vncKeyboardLayout = vncKeyboardLayout;
    }

    // Display Protocol tab

    private EntityModel<Boolean> privateDisplayConsole_Spice_IsSelected;

    public EntityModel<Boolean> getDisplayConsole_Spice_IsSelected()
    {
        return privateDisplayConsole_Spice_IsSelected;
    }

    public void setDisplayConsole_Spice_IsSelected(EntityModel<Boolean> value)
    {
        privateDisplayConsole_Spice_IsSelected = value;
    }

    private EntityModel<Boolean> spiceFileTransferEnabled;

    public EntityModel<Boolean> getSpiceFileTransferEnabled() {
        return spiceFileTransferEnabled;
    }

    public void setSpiceFileTransferEnabled(EntityModel<Boolean> spiceFileTransferEnabled) {
        this.spiceFileTransferEnabled = spiceFileTransferEnabled;
    }

    private EntityModel<Boolean> spiceCopyPasteEnabled;

    public EntityModel<Boolean> getSpiceCopyPasteEnabled() {
        return spiceCopyPasteEnabled;
    }

    public void setSpiceCopyPasteEnabled(EntityModel<Boolean> spiceCopyPasteEnabled) {
        this.spiceCopyPasteEnabled = spiceCopyPasteEnabled;
    }

    // Misc

    private boolean privateIsLinuxOS;

    public boolean getIsLinuxOS()
    {
        return privateIsLinuxOS;
    }

    public void setIsLinuxOS(boolean value)
    {
        privateIsLinuxOS = value;
    }

    private boolean privateIsWindowsOS;

    public boolean getIsWindowsOS()
    {
        return privateIsWindowsOS;
    }

    public void setIsWindowsOS(boolean value)
    {
        privateIsWindowsOS = value;
    }

    private boolean hwAcceleration;

    public boolean getHwAcceleration()
    {
        return hwAcceleration;
    }

    public void setHwAcceleration(boolean value)
    {
        if (hwAcceleration != value)
        {
            hwAcceleration = value;
            onPropertyChanged(new PropertyChangedEventArgs("HwAcceleration")); //$NON-NLS-1$
        }
    }

    private BootSequenceModel bootSequence;

    public BootSequenceModel getBootSequence()
    {
        return bootSequence;
    }

    public void setBootSequence(BootSequenceModel value)
    {
        if (bootSequence != value)
        {
            bootSequence = value;
            onPropertyChanged(new PropertyChangedEventArgs("BootSequence")); //$NON-NLS-1$
        }
    }

    private boolean isHostTabVisible = false;

    public boolean getIsHostTabVisible() {
        return isHostTabVisible;
    }

    public void setIsHostTabVisible(boolean value) {
        if (isHostTabVisible != value) {
            isHostTabVisible = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsHostTabVisible")); //$NON-NLS-1$
        }
    }

    private boolean isCustomPropertiesSheetVisible = false;

    public boolean getIsCustomPropertiesSheetVisible() {
        return isCustomPropertiesSheetVisible;
    }

    public void setIsCustomPropertiesSheetVisible(boolean value) {
        if (isCustomPropertiesSheetVisible != value) {
            isCustomPropertiesSheetVisible = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsCustomPropertiesSheetVisible")); //$NON-NLS-1$
        }
    }

    // Host tab

    private ListModel<VDS> defaultHost;

    public ListModel<VDS> getDefaultHost() {
        return defaultHost;
    }

    private void setDefaultHost(ListModel<VDS> value) {
        this.defaultHost = value;
    }

    private EntityModel<Boolean> isAutoAssign;

    public EntityModel<Boolean> getIsAutoAssign() {
        return isAutoAssign;
    }

    public void setIsAutoAssign(EntityModel<Boolean> value) {
        this.isAutoAssign = value;
    }

    // The "sysprep" option was moved from a standalone check box to a
    // pseudo floppy disk image. In order not to change the back-end
    // interface, the Reinitialize variable was changed to a read-only
    // property and its value is based on the selected floppy image.
    // A similar comparison is done for cloud-init iso images, so the
    // variable was changed from a boolean to an Enum.
    public InitializationType getInitializationType()
    {
        if (getAttachFloppy().getEntity() != null
                && getAttachFloppy().getEntity()
                && "[sysprep]".equals(getFloppyImage().getSelectedItem())) { //$NON-NLS-1$
            return InitializationType.Sysprep;
        } else if (getIsCloudInitEnabled().getEntity() != null
                && getIsCloudInitEnabled().getEntity()) {
            return InitializationType.CloudInit;
        } else {
            return InitializationType.None;
        }
    }

    public String getFloppyImagePath() {
        if (getAttachFloppy().getEntity()) {
            return getInitializationType() == InitializationType.Sysprep
                    ? "" : getFloppyImage().getSelectedItem(); //$NON-NLS-1$
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    public String getIsoImagePath() {
        if (getAttachIso().getEntity()) {
            return getIsoImage().getSelectedItem();
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    public void setIsoImagePath(String isoPath) {
        if ("".equals(isoPath)) { //$NON-NLS-1$
            getAttachIso().setEntity(false);
        } else {
            getAttachIso().setEntity(true);
            getIsoImage().setSelectedItem(isoPath);
        }
    }

    public RunOnceModel(VM vm, ICommandTarget commandTarget)
    {
        this.vm = vm;
        this.commandTarget = commandTarget;

        // Boot Options tab
        setAttachFloppy(new EntityModel<Boolean>());
        getAttachFloppy().getEntityChangedEvent().addListener(this);
        setFloppyImage(new ListModel<String>());
        getFloppyImage().getSelectedItemChangedEvent().addListener(this);
        setAttachIso(new EntityModel<Boolean>());
        getAttachIso().getEntityChangedEvent().addListener(this);
        setIsoImage(new ListModel<String>());
        getIsoImage().getSelectedItemChangedEvent().addListener(this);
        setDisplayProtocol(new ListModel<EntityModel<DisplayType>>());
        setBootSequence(new BootSequenceModel());

        // Linux Boot Options tab
        setKernel_parameters(new EntityModel<String>());
        setKernel_path(new EntityModel<String>());
        setInitrd_path(new EntityModel<String>());

        // Initial Boot tab - Sysprep
        setIsCloudInitEnabled(new EntityModel<Boolean>(false));

        setSysPrepDomainName(new ListModel<String>());
        setSysPrepSelectedDomainName(new EntityModel<String>());

        setSysPrepUserName(new EntityModel<String>().setIsChangable(false));
        setSysPrepPassword(new EntityModel<String>().setIsChangable(false));

        setIsSysprepEnabled(new EntityModel<Boolean>(false));
        setIsSysprepPossible(new EntityModel<Boolean>());

        setIsVmFirstRun(new EntityModel<Boolean>(false));
        getIsVmFirstRun().getEntityChangedEvent().addListener(this);
        setUseAlternateCredentials(new EntityModel<Boolean>(false));
        getUseAlternateCredentials().getEntityChangedEvent().addListener(this);

        // Initial Boot tab - Cloud-Init
        setIsCloudInitPossible(new EntityModel<Boolean>());

        setVmInit(new VmInitModel());

        // Custom Properties tab
        setCustomPropertySheet(new KeyValueModel());

        setBootMenuEnabled(new EntityModel<Boolean>(false));
        getBootMenuEnabled().setIsAvailable(AsyncDataProvider.isBootMenuSupported(vm.getVdsGroupCompatibilityVersion().toString()));
        setRunAndPause(new EntityModel<Boolean>(false));
        setRunAsStateless(new EntityModel<Boolean>(false));

        // Display Protocol tab
        setDisplayConsole_Spice_IsSelected(new EntityModel<Boolean>());
        getDisplayConsole_Spice_IsSelected().getEntityChangedEvent().addListener(this);
        setDisplayConsole_Vnc_IsSelected(new EntityModel<Boolean>());
        getDisplayConsole_Vnc_IsSelected().getEntityChangedEvent().addListener(this);

        setVncKeyboardLayout(new ListModel<String>());
        getVncKeyboardLayout().getSelectedItemChangedEvent().addListener(this);
        initVncKeyboardLayout();
        getVncKeyboardLayout().setSelectedItem(vm.getDefaultVncKeyboardLayout());

        setSpiceFileTransferEnabled(new EntityModel<Boolean>());
        getSpiceFileTransferEnabled().setEntity(vm.isSpiceFileTransferEnabled());
        boolean spiceFileTransferToggle = AsyncDataProvider.isSpiceFileTransferToggleSupported(vm.getVdsGroupCompatibilityVersion().toString());
        getSpiceFileTransferEnabled().setIsChangable(spiceFileTransferToggle);
        getSpiceFileTransferEnabled().setIsAvailable(spiceFileTransferToggle);

        setSpiceCopyPasteEnabled(new EntityModel<Boolean>());
        getSpiceCopyPasteEnabled().setEntity(vm.isSpiceCopyPasteEnabled());
        boolean spiceCopyPasteToggle = AsyncDataProvider.isSpiceCopyPasteToggleSupported(vm.getVdsGroupCompatibilityVersion().toString());
        getSpiceCopyPasteEnabled().setIsChangable(spiceCopyPasteToggle);
        getSpiceCopyPasteEnabled().setIsAvailable(spiceCopyPasteToggle);

        // Host tab
        setDefaultHost(new ListModel<VDS>());
        getDefaultHost().getSelectedItemChangedEvent().addListener(this);

        setIsAutoAssign(new EntityModel<Boolean>());
        getIsAutoAssign().getEntityChangedEvent().addListener(this);

        // availability/visibility
        setIsLinuxOptionsAvailable(new EntityModel<Boolean>(false));

        setIsHostTabVisible(true);

        setIsCustomPropertiesSheetVisible(true);

        setIsLinuxOS(false);
        setIsWindowsOS(false);

        runOnceCommand = new UICommand(RunOnceModel.RUN_ONCE_COMMAND, this)
         .setTitle(ConstantsManager.getInstance().getConstants().ok())
         .setIsDefault(true);

        cancelCommand = new UICommand(Model.CANCEL_COMMAND, this)
         .setTitle(ConstantsManager.getInstance().getConstants().cancel())
         .setIsCancel(true);

        getCommands().addAll(Arrays.asList(runOnceCommand, cancelCommand));
    }

    public void init() {
        setTitle(ConstantsManager.getInstance().getConstants().runVirtualMachinesTitle());
        setHelpTag(HelpTag.run_virtual_machine);
        setHashName("run_virtual_machine"); //$NON-NLS-1$
        setIsoImagePath(vm.getIsoPath()); // needs to be called before iso list is updated
        getAttachFloppy().setEntity(false);
        getBootMenuEnabled().setEntity(vm.isBootMenuEnabled());
        getRunAsStateless().setEntity(vm.isStateless());
        getRunAndPause().setEntity(vm.isRunAndPause());
        setHwAcceleration(true);

        // passing Kernel parameters
        getKernel_parameters().setEntity(vm.getKernelParams());
        getKernel_path().setEntity(vm.getKernelUrl());
        getInitrd_path().setEntity(vm.getInitrdUrl());

        setIsLinuxOS(AsyncDataProvider.isLinuxOsType(vm.getVmOsId()));
        getIsLinuxOptionsAvailable().setEntity(getIsLinuxOS());
        setIsWindowsOS(AsyncDataProvider.isWindowsOsType(vm.getVmOsId()));
        getIsVmFirstRun().setEntity(!vm.isInitialized());

        initVmInitEnabled(vm.getVmInit(), vm.isInitialized());
        getVmInit().init(vm.getStaticData());

        updateDomainList();
        updateIsoList();
        updateDisplayProtocols();
        updateFloppyImages();
        updateInitialRunFields();

        // Boot sequence.
        setIsBootFromNetworkAllowedForVm();
        setIsBootFromHardDiskAllowedForVm();

        // Display protocols.
        EntityModel<DisplayType> vncProtocol = new EntityModel<DisplayType>(DisplayType.vnc)
           .setTitle(ConstantsManager.getInstance().getConstants().VNCTitle());

        EntityModel<DisplayType> qxlProtocol = new EntityModel<DisplayType>(DisplayType.qxl)
           .setTitle(ConstantsManager.getInstance().getConstants().spiceTitle());

        boolean hasSpiceSupport = AsyncDataProvider.hasSpiceSupport(vm.getOs(), vm.getVdsGroupCompatibilityVersion());

        if (hasSpiceSupport) {
            getDisplayProtocol().setItems(Arrays.asList(vncProtocol, qxlProtocol));
        } else {
            getDisplayProtocol().setItems(Arrays.asList(vncProtocol));
            getDisplayConsole_Spice_IsSelected().setIsAvailable(false);
        }

        getDisplayProtocol().setSelectedItem(vm.getDefaultDisplayType() == DisplayType.vnc ?
                vncProtocol : qxlProtocol);
        getSpiceFileTransferEnabled().setEntity(vm.isSpiceFileTransferEnabled());
        getSpiceCopyPasteEnabled().setEntity(vm.isSpiceCopyPasteEnabled());
    }

    private void initVmInitEnabled(VmInit vmInit, boolean isInitialized) {
        if (vmInit == null) {
            getIsCloudInitEnabled().setEntity(false);
            getIsSysprepEnabled().setEntity(false);
            getAttachFloppy().setEntity(false);
        } else if (!isInitialized) {
            if (getIsWindowsOS()) {
                getIsSysprepEnabled().setEntity(true);
                getAttachFloppy().setEntity(true);
            } else {
                getIsCloudInitEnabled().setEntity(true);
            }
        }
    }

    protected RunVmOnceParams createRunVmOnceParams() {
        RunVmOnceParams params = new RunVmOnceParams();
        params.setVmId(vm.getId());
        params.setBootSequence(getBootSequence().getSequence());
        params.setDiskPath(getIsoImagePath());
        params.setFloppyPath(getFloppyImagePath());
        params.setKvmEnable(getHwAcceleration());
        params.setBootMenuEnabled(getBootMenuEnabled().getEntity());
        params.setRunAndPause(getRunAndPause().getEntity());
        params.setAcpiEnable(true);
        params.setRunAsStateless(getRunAsStateless().getEntity());
        params.setInitializationType(getInitializationType());
        params.setCustomProperties(getCustomPropertySheet().serialize());

        // kernel params
        if (getKernel_path().getEntity() != null)
        {
            params.setKernelUrl(getKernel_path().getEntity());
        }
        if (getKernel_parameters().getEntity() != null)
        {
            params.setKernelParams(getKernel_parameters().getEntity());
        }
        if (getInitrd_path().getEntity() != null)
        {
            params.setInitrdUrl(getInitrd_path().getEntity());
        }

        // Sysprep params
        if (getSysPrepUserName().getEntity() != null)
        {
            params.setSysPrepUserName(getSysPrepUserName().getEntity());
        }
        if (getSysPrepPassword().getEntity() != null)
        {
            params.setSysPrepPassword(getSysPrepPassword().getEntity());
        }

        if (getIsCloudInitEnabled() != null && getIsCloudInitEnabled().getEntity() ||
                getIsSysprepEnabled() != null && getIsSysprepEnabled().getEntity()) {
            params.setVmInit(getVmInit().buildCloudInitParameters(this));
        }

        EntityModel<? extends DisplayType> displayProtocolSelectedItem = getDisplayProtocol().getSelectedItem();
        params.setUseVnc(displayProtocolSelectedItem.getEntity() == DisplayType.vnc);
        if (getDisplayConsole_Vnc_IsSelected().getEntity()
                || getDisplayConsole_Spice_IsSelected().getEntity())
        {
            params.setUseVnc(getDisplayConsole_Vnc_IsSelected().getEntity());
        }

        params.setVncKeyboardLayout(getVncKeyboardLayout().getSelectedItem());

        String selectedDomain = getSysPrepSelectedDomainName().getEntity();
        if (!StringHelper.isNullOrEmpty(selectedDomain)) {
             params.setSysPrepDomainName(selectedDomain);
        }

        params.setSpiceFileTransferEnabled(getSpiceFileTransferEnabled().getEntity());

        params.setSpiceCopyPasteEnabled(getSpiceCopyPasteEnabled().getEntity());

        return params;
    }

    protected void updateFloppyImages() {
        AsyncDataProvider.getFloppyImageList(new AsyncQuery(this,
                new INewAsyncCallback() {

                    @Override
                    public void onSuccess(Object model, Object returnValue) {
                        VM selectedVM = vm;
                        List<String> images = (List<String>) returnValue;

                        if (AsyncDataProvider.isWindowsOsType(selectedVM.getVmOsId())) {
                            // Add a pseudo floppy disk image used for Windows' sysprep.
                            if (!selectedVM.isInitialized()) {
                                images.add(0, "[sysprep]"); //$NON-NLS-1$
                                getAttachFloppy().setEntity(true);
                            } else {
                                images.add("[sysprep]"); //$NON-NLS-1$
                            }
                        }
                        getFloppyImage().setItems(images);

                        if (getFloppyImage().getIsChangable()
                                && getFloppyImage().getSelectedItem() == null) {
                            getFloppyImage().setSelectedItem(Linq.firstOrDefault(images));
                        }
                    }
                }),
                vm.getStoragePoolId());
    }

    private void setIsBootFromHardDiskAllowedForVm() {
        Frontend.getInstance().runQuery(VdcQueryType.GetAllDisksByVmId, new IdQueryParameters(vm.getId()),
                new AsyncQuery(this, new INewAsyncCallback() {

                    @Override
                    public void onSuccess(Object model, Object returnValue) {
                        ArrayList<Disk> vmDisks = ((VdcQueryReturnValue) returnValue).getReturnValue();

                        if (vmDisks.isEmpty()) {
                            getRunAsStateless().setIsChangable(false);
                            getRunAsStateless()
                                    .setChangeProhibitionReason(ConstantsManager.getInstance()
                                            .getMessages()
                                            .disklessVmCannotRunAsStateless());
                            getRunAsStateless().setEntity(false);
                        }

                        if (!isDisksContainBootableDisk(vmDisks)) {
                            BootSequenceModel bootSequenceModel = getBootSequence();
                            bootSequenceModel.getHardDiskOption().setIsChangable(false);
                            bootSequenceModel.getHardDiskOption()
                                    .setChangeProhibitionReason(ConstantsManager.getInstance()
                                            .getMessages()
                                            .bootableDiskIsRequiredToBootFromDisk());
                        }
                    }
                }));
    }

    private boolean isDisksContainBootableDisk(List<Disk> disks) {
        for (Disk disk : disks) {
            if (disk.isBoot()) {
                return true;
            }
        }
        return false;
    }

    private void setIsBootFromNetworkAllowedForVm() {
        Frontend.getInstance().runQuery(VdcQueryType.GetVmInterfacesByVmId, new IdQueryParameters(vm.getId()),
                new AsyncQuery(this, new INewAsyncCallback() {

                    @Override
                    public void onSuccess(Object model, Object returnValue) {
                        Collection<VmNetworkInterface> nics =
                                ((VdcQueryReturnValue) returnValue).getReturnValue();
                        Collection<VmNetworkInterface> pluggedNics =
                                Linq.where(nics, new Linq.IPredicate<VmNetworkInterface>() {

                                    @Override
                                    public boolean match(VmNetworkInterface vnic) {
                                        return vnic.isPlugged();
                                    }
                                });
                        boolean hasPluggedNics = !pluggedNics.isEmpty();

                        if (!hasPluggedNics) {
                            BootSequenceModel bootSequenceModel = getBootSequence();
                            bootSequenceModel.getNetworkOption().setIsChangable(false);
                            bootSequenceModel.getNetworkOption()
                                    .setChangeProhibitionReason(ConstantsManager.getInstance()
                                            .getMessages()
                                            .interfaceIsRequiredToBootFromNetwork());
                        }
                    }
                }));
    }

    private void updateDisplayProtocols() {
        boolean isVncSelected = vm.getDefaultDisplayType() == DisplayType.vnc;
        getDisplayConsole_Vnc_IsSelected().setEntity(isVncSelected);
        getDisplayConsole_Spice_IsSelected().setEntity(!isVncSelected);
    }

    public void updateIsoList() {
        updateIsoList(false);
    }

    public void updateIsoList(boolean forceRefresh) {
        AsyncDataProvider.getIrsImageList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object model, Object returnValue) {
                        List<String> images = (List<String>) returnValue;
                        final String lastSelectedIso = getIsoImage().getSelectedItem();

                        getIsoImage().setItems(images);

                        if (getIsoImage().getIsChangable()) {
                            // try to preselect last image
                            if (lastSelectedIso != null && images.contains(lastSelectedIso)) {
                                getIsoImage().setSelectedItem(lastSelectedIso);
                            } else {
                                getIsoImage().setSelectedItem(Linq.firstOrDefault(images));
                            }
                        }
                    }
                }),
                vm.getStoragePoolId(), forceRefresh);
    }

    private void updateDomainList() {
        // Update Domain list
        AsyncDataProvider.getAAAProfilesList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        List<String> domains = (List<String>) returnValue;
                        String oldDomain = getSysPrepDomainName().getSelectedItem();
                        if (oldDomain != null && !oldDomain.equals("") && !domains.contains(oldDomain)) { //$NON-NLS-1$
                            domains.add(0, oldDomain);
                        }
                        getSysPrepDomainName().setItems(domains);
                        String selectedDomain = (oldDomain != null) ? oldDomain : Linq.firstOrDefault(domains);
                        if (!StringHelper.isNullOrEmpty(selectedDomain)) {
                            getSysPrepDomainName().setSelectedItem(selectedDomain);
                        }
                    }
                }));
    }

    public void sysPrepListBoxChanged() {
        getSysPrepSelectedDomainName().setEntity(getSysPrepDomainName().getSelectedItem());
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(ListModel.selectedItemChangedEventDefinition))
        {
            if (sender == getIsoImage())
            {
                IsoImage_SelectedItemChanged();
            }
            else if (sender == getFloppyImage())
            {
                floppyImage_SelectedItemChanged();
            }
        }
        else if (ev.matchesDefinition(EntityModel.entityChangedEventDefinition))
        {
            if (sender == getAttachFloppy())
            {
                attachFloppy_EntityChanged();
            }
            else if (sender == getAttachIso())
            {
                attachIso_EntityChanged();
            }
            else if (sender == getIsVmFirstRun())
            {
                isVmFirstRun_EntityChanged();
            }
            else if (sender == getUseAlternateCredentials())
            {
                useAlternateCredentials_EntityChanged();
            }
            else if (sender == getDisplayConsole_Vnc_IsSelected() && ((EntityModel<Boolean>) sender).getEntity())
            {
                getDisplayConsole_Spice_IsSelected().setEntity(false);
                getVncKeyboardLayout().setIsChangable(true);
                getSpiceFileTransferEnabled().setIsChangable(false);
                getSpiceCopyPasteEnabled().setIsChangable(false);
            }
            else if (sender == getDisplayConsole_Spice_IsSelected() && ((EntityModel<Boolean>) sender).getEntity())
            {
                getDisplayConsole_Vnc_IsSelected().setEntity(false);
                getVncKeyboardLayout().setIsChangable(false);
                getSpiceFileTransferEnabled().setIsChangable(true);
                getSpiceCopyPasteEnabled().setIsChangable(true);
            }
            else if (sender == getIsAutoAssign())
            {
                isAutoAssign_EntityChanged(sender, args);
            }
        }
    }

    private void attachIso_EntityChanged()
    {
        getIsoImage().setIsChangable(getAttachIso().getEntity());
        getBootSequence().getCdromOption().setIsChangable(getAttachIso().getEntity());
        updateInitialRunFields();
    }

    private void attachFloppy_EntityChanged()
    {
        getFloppyImage().setIsChangable(getAttachFloppy().getEntity());
        updateInitialRunFields();
    }

    private void useAlternateCredentials_EntityChanged()
    {
        boolean useAlternateCredentials = getUseAlternateCredentials().getEntity();

        getSysPrepUserName().setIsChangable(getUseAlternateCredentials().getEntity());
        getSysPrepPassword().setIsChangable(getUseAlternateCredentials().getEntity());

        getSysPrepUserName().setEntity(useAlternateCredentials ? "" : null); //$NON-NLS-1$
        getSysPrepPassword().setEntity(useAlternateCredentials ? "" : null); //$NON-NLS-1$
    }

    private void isVmFirstRun_EntityChanged()
    {
        updateInitialRunFields();
    }

    private void floppyImage_SelectedItemChanged()
    {
        updateInitialRunFields();
    }

    private void IsoImage_SelectedItemChanged()
    {
        updateInitialRunFields();
    }

    private void isAutoAssign_EntityChanged(Object sender, EventArgs args) {
        if (getIsAutoAssign().getEntity() == false) {
            getDefaultHost().setIsChangable(true);
        }
    }

    // Sysprep/cloud-init sections displayed only with proper OS type (Windows
    // or Linux, respectively) and when proper floppy or CD is attached.
    // Currently vm.isFirstRun() status is not considered.
    public void updateInitialRunFields()
    {
        getIsSysprepPossible().setEntity(getIsWindowsOS());
        getIsSysprepEnabled().setEntity(getInitializationType() == InitializationType.Sysprep);
        // also other can be cloud inited
        getIsCloudInitPossible().setEntity(!getIsWindowsOS());
        getIsCloudInitEnabled().setEntity(getInitializationType() == InitializationType.CloudInit);
        getIsCloudInitEnabled().setIsAvailable(!getIsWindowsOS());
    }

    public boolean validate() {
        getIsoImage().setIsValid(true);
        if (getAttachIso().getEntity()) {
            getIsoImage().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }

        getFloppyImage().setIsValid(true);
        if (getAttachFloppy().getEntity()) {
            getFloppyImage().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }

        boolean customPropertyValidation = getCustomPropertySheet().validate();

        if (getIsLinuxOS()) {
            getKernel_path().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });
            getInitrd_path().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });
            getKernel_parameters().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });

            // initrd path and kernel params require kernel path to be filled
            if (StringHelper.isNullOrEmpty(getKernel_path().getEntity())) {
                final UIConstants constants = ConstantsManager.getInstance().getConstants();

                if (!StringHelper.isNullOrEmpty(getInitrd_path().getEntity())) {
                    getInitrd_path().getInvalidityReasons().add(constants.initrdPathInvalid());
                    getInitrd_path().setIsValid(false);
                    getKernel_path().getInvalidityReasons().add(constants.initrdPathInvalid());
                    getKernel_path().setIsValid(false);
                }

                if (!StringHelper.isNullOrEmpty(getKernel_parameters().getEntity())) {
                    getKernel_parameters().getInvalidityReasons().add(constants.kernelParamsInvalid());
                    getKernel_parameters().setIsValid(false);
                    getKernel_path().getInvalidityReasons().add(constants.kernelParamsInvalid());
                    getKernel_path().setIsValid(false);
                }
            }
        }

        if (getIsAutoAssign().getEntity() != null && getIsAutoAssign().getEntity() == false) {
            getDefaultHost().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }
        else {
            getDefaultHost().setIsValid(true);
        }

        boolean cloudInitIsValid = getVmInit().validate();

        return getIsoImage().getIsValid()
                && getFloppyImage().getIsValid()
                && getKernel_path().getIsValid()
                && getInitrd_path().getIsValid()
                && getKernel_parameters().getIsValid()
                && getDefaultHost().getIsValid()
                && customPropertyValidation
                && cloudInitIsValid;
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);
        if (command == runOnceCommand)
        {
            if (validate()) {
                onRunOnce();
            }
        }
        else if (command == cancelCommand)
        {
            commandTarget.executeCommand(command);
        }
    }

    protected abstract void onRunOnce();

    private void initVncKeyboardLayout() {

        List<String> layouts =
                (List<String>) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.VncKeyboardLayoutValidValues);
        List<String> vncKeyboardLayoutItems = new ArrayList<String>();
        vncKeyboardLayoutItems.add(null);
        vncKeyboardLayoutItems.addAll(layouts);
        getVncKeyboardLayout().setItems(vncKeyboardLayoutItems);

        getVncKeyboardLayout().setIsChangable(false);
    }

}
