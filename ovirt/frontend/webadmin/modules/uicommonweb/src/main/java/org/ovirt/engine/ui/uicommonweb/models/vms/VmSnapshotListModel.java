package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.RemoveSnapshotParameters;
import org.ovirt.engine.core.common.action.RestoreAllSnapshotsParameters;
import org.ovirt.engine.core.common.action.TryBackToAllSnapshotsOfVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.SnapshotActionEnum;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotStatus;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.Linq.SnapshotByCreationDateCommparer;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.builders.BuilderExecutor;
import org.ovirt.engine.ui.uicommonweb.builders.vm.FullUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.VmSpecificUnitToVmBuilder;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class VmSnapshotListModel extends SearchableListModel
{
    // This constant is intendend to be exported to a generic UTILS class later on
    private final static String DATE_FORMAT = "yyyy-MM-dd, HH:mm"; //$NON-NLS-1$

    private UICommand privateNewCommand;

    public UICommand getNewCommand() {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }

    private UICommand privatePreviewCommand;

    public UICommand getPreviewCommand()
    {
        return privatePreviewCommand;
    }

    private void setPreviewCommand(UICommand value)
    {
        privatePreviewCommand = value;
    }

    private UICommand customPreviewCommand;

    public UICommand getCustomPreviewCommand()
    {
        return customPreviewCommand;
    }

    private void setCustomPreviewCommand(UICommand value)
    {
        customPreviewCommand = value;
    }

    private UICommand privateCommitCommand;

    public UICommand getCommitCommand()
    {
        return privateCommitCommand;
    }

    private void setCommitCommand(UICommand value)
    {
        privateCommitCommand = value;
    }

    private UICommand privateUndoCommand;

    public UICommand getUndoCommand()
    {
        return privateUndoCommand;
    }

    private void setUndoCommand(UICommand value)
    {
        privateUndoCommand = value;
    }

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    private UICommand privateCloneVmCommand;

    public UICommand getCloneVmCommand()
    {
        return privateCloneVmCommand;
    }

    private void setCloneVmCommand(UICommand value)
    {
        privateCloneVmCommand = value;
    }

    private EntityModel privateCanSelectSnapshot;

    public EntityModel getCanSelectSnapshot()
    {
        return privateCanSelectSnapshot;
    }

    private void setCanSelectSnapshot(EntityModel value)
    {
        privateCanSelectSnapshot = value;
    }

    private SystemTreeItemModel systemTreeSelectedItem;

    public SystemTreeItemModel getSystemTreeSelectedItem()
    {
        return systemTreeSelectedItem;
    }

    public void setSystemTreeSelectedItem(SystemTreeItemModel value)
    {
        systemTreeSelectedItem = value;
        onPropertyChanged(new PropertyChangedEventArgs("SystemTreeSelectedItem")); //$NON-NLS-1$
    }

    private HashMap<Guid, SnapshotModel> snapshotsMap;

    public HashMap<Guid, SnapshotModel> getSnapshotsMap()
    {
        return snapshotsMap;
    }

    public void setSnapshotsMap(HashMap<Guid, SnapshotModel> value)
    {
        snapshotsMap = value;
        onPropertyChanged(new PropertyChangedEventArgs("SnapshotsMap")); //$NON-NLS-1$
    }

    private boolean isCloneVmSupported;

    public boolean getIsCloneVmSupported()
    {
        return isCloneVmSupported;
    }

    private void setIsCloneVmSupported(boolean value)
    {
        if (isCloneVmSupported != value)
        {
            isCloneVmSupported = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsCloneVmSupported")); //$NON-NLS-1$
        }
    }

    private boolean memorySnapshotSupported;

    public boolean isMemorySnapshotSupported()
    {
        return memorySnapshotSupported;
    }

    private void setMemorySnapshotSupported(boolean value)
    {
        if (memorySnapshotSupported != value)
        {
            memorySnapshotSupported = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsMemorySnapshotSupported")); //$NON-NLS-1$
        }
    }

    private boolean liveMergeSupported;

    public boolean isLiveMergeSupported()
    {
        return liveMergeSupported;
    }

    private void setLiveMergeSupported(boolean value)
    {
        if (liveMergeSupported != value)
        {
            liveMergeSupported = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsLiveMergeSupported")); //$NON-NLS-1$
        }
    }

    private List<DiskImage> vmDisks;

    public List<DiskImage> getVmDisks() {
        return vmDisks;
    }

    public void setVmDisks(List<DiskImage> value) {
        vmDisks = value;
    }

    public VmSnapshotListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().snapshotsTitle());
        setHelpTag(HelpTag.snapshots);
        setHashName("snapshots"); //$NON-NLS-1$

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setPreviewCommand(new UICommand("Preview", this)); //$NON-NLS-1$
        setCustomPreviewCommand(new UICommand("CustomPreview", this)); //$NON-NLS-1$
        setCommitCommand(new UICommand("Commit", this)); //$NON-NLS-1$
        setUndoCommand(new UICommand("Undo", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setCloneVmCommand(new UICommand("CloneVM", this)); //$NON-NLS-1$

        setCanSelectSnapshot(new EntityModel());
        getCanSelectSnapshot().setEntity(true);

        setSnapshotsMap(new HashMap<Guid, SnapshotModel>());
        setVmDisks(new ArrayList<DiskImage>());
    }

    @Override
    public void setItems(Collection value)
    {
        ArrayList<Snapshot> snapshots =
                value != null ? Linq.<Snapshot> cast(value) : new ArrayList<Snapshot>();

        Collections.sort(snapshots, Collections.reverseOrder(new SnapshotByCreationDateCommparer()));
        ArrayList<Snapshot> sortedSnapshots = new ArrayList<Snapshot>();

        for (Snapshot snapshot : snapshots) {
            SnapshotModel snapshotModel = snapshotsMap.get(snapshot.getId());
            if (snapshotModel == null) {
                snapshotModel = new SnapshotModel();
                snapshotsMap.put(snapshot.getId(), snapshotModel);
            }
            snapshotModel.setEntity(snapshot);

            if ((snapshot.getType() == SnapshotType.ACTIVE && getInType(SnapshotType.PREVIEW, snapshots) == null)
                    || snapshot.getType() == SnapshotType.PREVIEW) {
                sortedSnapshots.add(0, snapshot);
            }
            else if (snapshot.getType() == SnapshotType.REGULAR || snapshot.getType() == SnapshotType.STATELESS) {
                sortedSnapshots.add(snapshot);
            }
        }

        if (getInPreview(sortedSnapshots) != null) {
            updatePreviewedDiskSnapshots(sortedSnapshots);
        }
        else {
            updateItems(sortedSnapshots);
        }
    }

    private void updateItems(List<Snapshot> snapshots) {
        super.setItems(snapshots);

        // Try to select the last created snapshot (fallback to active snapshot)
        if (getSelectedItem() == null) {
            setSelectedItem(snapshots.size() > 1 ? snapshots.get(1) : snapshots.get(0));
        }

        updateActionAvailability();
    }

    @Override
    public void setEntity(Object value) {
        updateIsMemorySnapshotSupported(value);
        updateIsLiveMergeSupported(value);
        super.setEntity(value);
        updateIsCloneVmSupported();
        updateVmActiveDisks();
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();

        if (getEntity() != null)
        {
            getSearchCommand().execute();
        }
    }

    @Override
    protected void syncSearch()
    {
        VM vm = (VM) getEntity();
        if (vm == null)
        {
            return;
        }

        super.syncSearch(VdcQueryType.GetAllVmSnapshotsByVmId, new IdQueryParameters(vm.getId()));
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void remove()
    {
        if (getEntity() != null)
        {
            if (getWindow() != null)
            {
                return;
            }

            Snapshot snapshot = (Snapshot) getSelectedItem();
            ConfirmationModel model = new ConfirmationModel();
            setWindow(model);
            model.setTitle(ConstantsManager.getInstance().getConstants().deleteSnapshotTitle());
            model.setHelpTag(HelpTag.delete_snapshot);
            model.setHashName("delete_snapshot"); //$NON-NLS-1$
            model.setMessage(ConstantsManager.getInstance()
                    .getMessages()
                    .areYouSureYouWantToDeleteSanpshot( DateTimeFormat.getFormat(DATE_FORMAT).format(snapshot.getCreationDate()),
                            snapshot.getDescription()));
            UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
            tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
            tempVar.setIsDefault(true);
            model.getCommands().add(tempVar);
            UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
            tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
            tempVar2.setIsCancel(true);
            model.getCommands().add(tempVar2);
        }
    }

    private void onRemove()
    {
        Snapshot snapshot = (Snapshot) getSelectedItem();
        if (snapshot == null)
        {
            cancel();
            return;
        }

        VM vm = (VM) getEntity();
        if (vm != null)
        {
            Frontend.getInstance().runAction(VdcActionType.RemoveSnapshot,
                    new RemoveSnapshotParameters(snapshot.getId(), vm.getId()), null, null);
        }

        getCanSelectSnapshot().setEntity(false);

        cancel();
    }

    private void undo()
    {
        VM vm = (VM) getEntity();
        if (vm != null)
        {
            Frontend.getInstance().runAction(VdcActionType.RestoreAllSnapshots,
                    new RestoreAllSnapshotsParameters(vm.getId(), SnapshotActionEnum.UNDO),
                    null,
                    null);
        }
    }

    private void commit()
    {
        VM vm = (VM) getEntity();
        if (vm != null)
        {
            Frontend.getInstance().runAction(VdcActionType.RestoreAllSnapshots,
                    new RestoreAllSnapshotsParameters(vm.getId(), SnapshotActionEnum.COMMIT),
                    null,
                    null);
        }
    }

    private void preview() {
        VM vm = (VM) getEntity();
        if (vm == null) {
            return;
        }

        final Snapshot snapshot = (Snapshot) getSelectedItem();
        AsyncDataProvider.getVmConfigurationBySnapshot(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                VM vm = (VM) returnValue;
                ArrayList<DiskImage> snapshotDisks = vm.getDiskList();
                List<DiskImage> disksExcludedFromSnapshot = Linq.imagesSubtract(getVmDisks(), snapshotDisks);

                boolean showMemorySnapshotWarning = isMemorySnapshotSupported() && !snapshot.getMemoryVolume().isEmpty();
                boolean showPartialSnapshotWarning = !disksExcludedFromSnapshot.isEmpty();

                if (showMemorySnapshotWarning || showPartialSnapshotWarning) {
                    SnapshotModel model = new SnapshotModel();
                    model.setVmDisks(getVmDisks());
                    model.setDisks(snapshotDisks);
                    model.setShowMemorySnapshotWarning(showMemorySnapshotWarning);
                    model.setShowPartialSnapshotWarning(showPartialSnapshotWarning);
                    setWindow(model);

                    model.setTitle(showPartialSnapshotWarning ?
                            ConstantsManager.getInstance().getConstants().previewPartialSnapshotTitle() :
                            ConstantsManager.getInstance().getConstants().previewSnapshotTitle());
                    model.setHelpTag(showPartialSnapshotWarning ? HelpTag.preview_partial_snapshot : HelpTag.preview_snapshot);
                    model.setHashName(showPartialSnapshotWarning ? "preview_partial_snapshot" : "preview_snapshot"); //$NON-NLS-1$ //$NON-NLS-2$

                    addCommands(model, "OnPreview"); //$NON-NLS-1$
                } else {
                    runTryBackToAllSnapshotsOfVm(null, vm, snapshot, false, null);
                }
            }
        }), snapshot.getId());
    }

    private void updateVmActiveDisks() {
        VM vm = (VM) getEntity();
        if (vm == null) {
            return;
        }

        AsyncDataProvider.getVmDiskList(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                ArrayList<Disk> disks = (ArrayList<Disk>) returnValue;
                getVmDisks().clear();
                for (Disk disk : disks) {
                    if (disk.getDiskStorageType() == Disk.DiskStorageType.LUN) {
                        continue;
                    }

                    DiskImage diskImage = (DiskImage) disk;
                    getVmDisks().add(diskImage);
                }
            }
        }), vm.getId());
    }

    private void updatePreviewedDiskSnapshots(final List<Snapshot> snapshots) {
        for (DiskImage diskImage : getVmDisks()) {
            if (diskImage.getSnapshots().size() <= 1) {
                continue;
            }

            Guid snapshotId = diskImage.getSnapshots().get(1).getVmSnapshotId();
            getSnapshotsMap().get(snapshotId).getEntity().getDiskImages().add(diskImage);
        }

        updateItems(snapshots);
    }

    private void customPreview()
    {
        VM vm = (VM) getEntity();
        if (vm == null) {
            return;
        }

        PreviewSnapshotModel model = new PreviewSnapshotModel();
        model.setVmId(vm.getId());
        model.initialize();

        // Update according to the selected snapshot
        Snapshot selectedSnapshot = (Snapshot) getSelectedItem();
        if (selectedSnapshot != null) {
            model.setSnapshotModel(getSnapshotsMap().get(selectedSnapshot.getId()));
        }

        setWindow(model);

        model.setTitle(ConstantsManager.getInstance().getConstants().customPreviewSnapshotTitle());
        model.setHelpTag(HelpTag.custom_preview_snapshot);
        model.setHashName("custom_preview_snapshot"); //$NON-NLS-1$

        addCommands(model, "OnCustomPreview"); //$NON-NLS-1$
    }

    private void onPreview() {
        Snapshot snapshot = (Snapshot) getSelectedItem();
        if (snapshot == null) {
            cancel();
            return;
        }

        VM vm = (VM) getEntity();
        SnapshotModel snapshotModel = (SnapshotModel) getWindow();
        boolean memory = false;
        List<DiskImage> disks = null;

        if (snapshotModel.isShowPartialSnapshotWarning()) {
            switch (snapshotModel.getPartialPreviewSnapshotOptions().getSelectedItem()) {
                case preserveActiveDisks:
                    // get snapshot disks
                    disks = snapshotModel.getDisks();
                    // add active disks missed from snapshot
                    disks.addAll(Linq.imagesSubtract(getVmDisks(), disks));
                    break;
                case excludeActiveDisks:
                    // nothing to do - default behaviour
                    break;
                case openCustomPreviewDialog:
                    setWindow(null);
                    getCustomPreviewCommand().execute();
                    return;
            }
        }

        if (snapshotModel.isShowMemorySnapshotWarning()) {
            memory = snapshotModel.getMemory().getEntity();
        }

        runTryBackToAllSnapshotsOfVm(snapshotModel, vm, snapshot, memory, disks);
    }

    private void onCustomPreview() {
        VM vm = (VM) getEntity();
        PreviewSnapshotModel previewSnapshotModel = (PreviewSnapshotModel) getWindow();
        Snapshot snapshot = previewSnapshotModel.getSnapshotModel().getEntity();
        boolean memory = Boolean.TRUE.equals(previewSnapshotModel.getSnapshotModel().getMemory().getEntity());
        List<DiskImage> disks = previewSnapshotModel.getSelectedDisks();

        runTryBackToAllSnapshotsOfVm(previewSnapshotModel, vm, snapshot, memory, disks);
    }

    private void runTryBackToAllSnapshotsOfVm(final Model model, VM vm, Snapshot snapshot, boolean memory, List<DiskImage> disks) {
        if (model != null) {
            model.startProgress(null);
        }

        Frontend.getInstance().runAction(VdcActionType.TryBackToAllSnapshotsOfVm, new TryBackToAllSnapshotsOfVmParameters(
            vm.getId(), snapshot.getId(), memory, disks),
            new IFrontendActionAsyncCallback() {
                @Override
                public void executed(FrontendActionAsyncResult result) {
                    if (model != null) {
                        model.stopProgress();
                    }

                    if (result.getReturnValue().getSucceeded()) {
                        cancel();
                    }
                }
            });
    }

    private void newEntity()
    {
        VM vm = (VM) getEntity();
        if (vm == null || getWindow() != null) {
            return;
        }

        SnapshotModel model = SnapshotModel.createNewSnapshotModel(this);
        setWindow(model);
        model.setVm(vm);
        model.initialize();
    }

    public void postOnNew(List<VdcReturnValueBase> returnValues) {

        SnapshotModel model = (SnapshotModel) getWindow();

        model.stopProgress();

        if (returnValues != null && Linq.all(returnValues, new Linq.CanDoActionSucceedPredicate())) {
            cancel();
        }
    }

    private void addCommands(Model model, String okCommandName) {
        model.getCommands().add(new UICommand(okCommandName, this) //$NON-NLS-1$
                .setTitle(ConstantsManager.getInstance().getConstants().ok())
                .setIsDefault(true));
        model.getCommands().add(new UICommand("Cancel", this) //$NON-NLS-1$
                .setTitle(ConstantsManager.getInstance().getConstants().cancel())
                .setIsCancel(true));
    }

    private void cancel()
    {
        setWindow(null);
    }

    private void cloneVM()
    {
        Snapshot snapshot = (Snapshot) getSelectedItem();
        if (snapshot == null)
        {
            return;
        }

        if (getWindow() != null)
        {
            return;
        }

        VM selectedVm = (VM) getEntity();

        UnitVmModel model = new UnitVmModel(new CloneVmFromSnapshotModelBehavior());
        model.getVmType().setSelectedItem(selectedVm.getVmType());
        model.setIsAdvancedModeLocalStorageKey("wa_snapshot_dialog");  //$NON-NLS-1$
        setWindow(model);

        model.startProgress(null);

        AsyncDataProvider.getVmConfigurationBySnapshot(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                VmSnapshotListModel vmSnapshotListModel = (VmSnapshotListModel) target;
                UnitVmModel model = (UnitVmModel) vmSnapshotListModel.getWindow();

                CloneVmFromSnapshotModelBehavior behavior = (CloneVmFromSnapshotModelBehavior) model.getBehavior();
                VM vm = (VM) returnValue;
                behavior.setVm(vm);

                model.setTitle(ConstantsManager.getInstance().getConstants().cloneVmFromSnapshotTitle());
                model.setHelpTag(HelpTag.clone_vm_from_snapshot);
                model.setHashName("clone_vm_from_snapshot"); //$NON-NLS-1$
                model.setCustomPropertiesKeysList(AsyncDataProvider.getCustomPropertiesList());
                model.initialize(vmSnapshotListModel.getSystemTreeSelectedItem());

                VmBasedWidgetSwitchModeCommand switchModeCommand = new VmBasedWidgetSwitchModeCommand();
                switchModeCommand.init(model);
                model.getCommands().add(switchModeCommand);

                UICommand tempVar = new UICommand("OnCloneVM", vmSnapshotListModel); //$NON-NLS-1$
                tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
                tempVar.setIsDefault(true);
                model.getCommands().add(tempVar);
                UICommand tempVar2 = new UICommand("Cancel", vmSnapshotListModel); //$NON-NLS-1$
                tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                tempVar2.setIsCancel(true);
                model.getCommands().add(tempVar2);

                vmSnapshotListModel.stopProgress();
            }
        }), snapshot.getId());
    }

    private void onCloneVM()
    {
        UnitVmModel model = (UnitVmModel) getWindow();
        CloneVmFromSnapshotModelBehavior behavior = (CloneVmFromSnapshotModelBehavior) model.getBehavior();
        Snapshot snapshot = (Snapshot) getSelectedItem();
        if (snapshot == null)
        {
            cancel();
            return;
        }

        if (!model.validate())
        {
            return;
        }

        VM vm = behavior.getVm();

        // Save changes.
        buildVmOnClone(model, vm);

        vm.setCustomProperties(model.getCustomProperties().getEntity());

        vm.setUseHostCpuFlags(model.getHostCpu().getEntity());
        vm.setDiskMap(behavior.getVm().getDiskMap());

        HashMap<Guid, DiskImage> imageToDestinationDomainMap =
                model.getDisksAllocationModel().getImageToDestinationDomainMap();

        AddVmFromSnapshotParameters parameters =
                new AddVmFromSnapshotParameters(vm.getStaticData(), snapshot.getId());
        parameters.setDiskInfoDestinationMap(imageToDestinationDomainMap);
        setupAddVmFromSnapshotParameters(parameters);
        parameters.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
        parameters.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());
        parameters.setBalloonEnabled(model.getMemoryBalloonDeviceEnabled().getEntity());

        model.startProgress(null);

        Frontend.getInstance().runAction(VdcActionType.AddVmFromSnapshot, parameters,
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {

                        VmSnapshotListModel vmSnapshotListModel = (VmSnapshotListModel) result.getState();
                        vmSnapshotListModel.getWindow().stopProgress();
                        VdcReturnValueBase returnValueBase = result.getReturnValue();
                        if (returnValueBase != null && returnValueBase.getSucceeded())
                        {
                            vmSnapshotListModel.cancel();
                            vmSnapshotListModel.updateActionAvailability();
                        }
                    }
                }, this);
    }

    protected static void buildVmOnClone(UnitVmModel model, VM vm) {
        BuilderExecutor.build(model, vm.getStaticData(), new FullUnitToVmBaseBuilder());
        BuilderExecutor.build(model, vm, new VmSpecificUnitToVmBuilder());
    }

    protected void setupAddVmFromSnapshotParameters(AddVmFromSnapshotParameters parameters) {
        // do nothing - no additional setup needed
    }

    public void updateActionAvailability()
    {
        if (getItems() == null) {
            // no need to update action availability
            return;
        }

        VM vm = (VM) getEntity();
        Snapshot snapshot = (Snapshot) getSelectedItem();
        List<VM> vmList = vm != null ? Collections.singletonList(vm) : Collections.<VM> emptyList();

        boolean isVmDown = vm != null && vm.getStatus() == VMStatus.Down;
        boolean isVmImageLocked = vm != null && vm.getStatus() == VMStatus.ImageLocked;
        boolean isVmQualifiedForSnapshotMerge = vm != null && vm.getStatus().isQualifiedForSnapshotMerge();
        boolean isPreviewing = getIsPreviewing();
        boolean isLocked = getIsLocked();
        boolean isSelected = snapshot != null && snapshot.getType() != SnapshotType.ACTIVE;
        boolean isStateless = getIsStateless();
        boolean isCloneVmSupported = getIsCloneVmSupported();

        getCanSelectSnapshot().setEntity(!isPreviewing && !isLocked && !isStateless
                && VdcActionUtils.canExecute(vmList, VM.class, VdcActionType.CreateAllSnapshotsFromVm));
        getNewCommand().setIsExecutionAllowed(!isPreviewing && !isLocked && !isVmImageLocked && !isStateless);
        getPreviewCommand().setIsExecutionAllowed(isSelected && !isLocked && !isPreviewing && isVmDown && !isStateless);
        getCustomPreviewCommand().setIsExecutionAllowed(getPreviewCommand().getIsExecutionAllowed());
        getCommitCommand().setIsExecutionAllowed(isPreviewing && isVmDown && !isStateless);
        getUndoCommand().setIsExecutionAllowed(isPreviewing && isVmDown && !isStateless);
        getRemoveCommand().setIsExecutionAllowed(isSelected && !isLocked && !isPreviewing && !isStateless
                && (isLiveMergeSupported() ? isVmQualifiedForSnapshotMerge : isVmDown));
        getCloneVmCommand().setIsExecutionAllowed(isSelected && !isLocked && !isPreviewing
                && !isVmImageLocked && !isStateless && isCloneVmSupported);
    }

    public boolean getIsPreviewing() {
        return getInPreview() != null;
    }

    public boolean getIsLocked() {
        return getLocked() != null;
    }

    public boolean getIsStateless() {
        return getInType(SnapshotType.STATELESS, (ArrayList<Snapshot>) getItems()) != null;
    }

    public Snapshot getLocked() {
        for (Snapshot snapshot : (ArrayList<Snapshot>) getItems()) {
            if (snapshot.getStatus() == SnapshotStatus.LOCKED) {
                return snapshot;
            }
        }
        return null;
    }

    public Snapshot getInPreview() {
        return getInPreview(getItems());
    }

    public Snapshot getInPreview(Collection<Snapshot> snapshots) {
        for (Snapshot snapshot : snapshots) {
            if (snapshot.getStatus() == SnapshotStatus.IN_PREVIEW) {
                return snapshot;
            }
        }
        return null;
    }

    public Snapshot getInStatus(SnapshotStatus snapshotStatus, ArrayList<Snapshot> snapshots) {
        for (Snapshot snapshot : snapshots) {
            if (snapshot.getStatus() == snapshotStatus) {
                return snapshot;
            }
        }
        return null;
    }

    public Snapshot getInType(SnapshotType snapshotType, ArrayList<Snapshot> snapshots) {
        for (Snapshot snapshot : snapshots) {
            if (snapshot.getType() == snapshotType) {
                return snapshot;
            }
        }
        return null;
    }

    protected void updateIsCloneVmSupported()
    {
        if (getEntity() == null)
        {
            return;
        }

        VM vm = (VM) getEntity();

        AsyncDataProvider.getDataCenterById(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                VmSnapshotListModel model = (VmSnapshotListModel) target;
                StoragePool dataCenter = (StoragePool) returnValue;
                VM vm = (VM) model.getEntity();

                Version minClusterVersion = vm.getVdsGroupCompatibilityVersion();
                Version minDcVersion = dataCenter.getcompatibility_version();

                AsyncDataProvider.isCommandCompatible(new AsyncQuery(model, new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        VmSnapshotListModel model = (VmSnapshotListModel) target;
                        model.setIsCloneVmSupported((Boolean) returnValue);
                    }
                }), VdcActionType.AddVmFromSnapshot, minClusterVersion, minDcVersion);
            }
        }), vm.getStoragePoolId());
    }

    private void updateIsMemorySnapshotSupported(Object entity) {
        if (entity == null) {
            return;
        }

        VM vm = (VM) entity;

        setMemorySnapshotSupported(AsyncDataProvider.isMemorySnapshotSupported(vm));
    }

    private void updateIsLiveMergeSupported(Object entity) {
        if (entity == null) {
            return;
        }

        VM vm = (VM) entity;

        if (vm.getRunOnVds() == null || !AsyncDataProvider.isLiveMergeSupported(vm)) {
            setLiveMergeSupported(false);
            return;
        }

        AsyncQuery query = new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                VmSnapshotListModel vmSnapshotListModel = (VmSnapshotListModel) model;
                VDS vds = (VDS) returnValue;
                vmSnapshotListModel.setLiveMergeSupported(vds.getLiveMergeSupport());
            }
        });
        AsyncDataProvider.getHostById(query, vm.getRunOnVds());
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getNewCommand())
        {
            newEntity();
        }
        else if (command == getPreviewCommand())
        {
            preview();
        }
        else if (command == getCustomPreviewCommand())
        {
            customPreview();
        }
        else if (command == getCommitCommand())
        {
            commit();
        }
        else if (command == getUndoCommand())
        {
            undo();
        }
        else if (command == getRemoveCommand())
        {
            remove();
        }
        else if (command == getCloneVmCommand())
        {
            cloneVM();
        }
        else if ("OnRemove".equals(command.getName())) //$NON-NLS-1$
        {
            onRemove();
        }
        else if ("Cancel".equals(command.getName())) //$NON-NLS-1$
        {
            cancel();
        }
        else if ("OnCloneVM".equals(command.getName())) //$NON-NLS-1$
        {
            onCloneVM();
        }
        else if ("OnPreview".equals(command.getName())) //$NON-NLS-1$
        {
            onPreview();
        }
        else if ("OnCustomPreview".equals(command.getName())) //$NON-NLS-1$
        {
            onCustomPreview();
        }
    }

    @Override
    protected String getListName() {
        return "VmSnapshotListModel"; //$NON-NLS-1$
    }
}
