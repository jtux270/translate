package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.action.AttachDetachVmDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.UIConstants;

public class AttachDiskModel extends NewDiskModel {
    protected static final UIConstants constants = ConstantsManager.getInstance().getConstants();

    private Map<DiskStorageType, ListModel<EntityModel<DiskModel>>> attachableDisksMap;
    private EntityModel<String> messageLabel;
    private EntityModel<String> warningLabel;

    public AttachDiskModel() {
        attachableDisksMap = new HashMap<DiskStorageType, ListModel<EntityModel<DiskModel>>>();
        attachableDisksMap.put(DiskStorageType.IMAGE, new ListModel<EntityModel<DiskModel>>());
        attachableDisksMap.put(DiskStorageType.LUN, new ListModel<EntityModel<DiskModel>>());
        attachableDisksMap.put(DiskStorageType.CINDER, new ListModel<EntityModel<DiskModel>>());
        setWarningLabel(new EntityModel<String>());
        getWarningLabel().setIsAvailable(false);
        setMessageLabel(new EntityModel<String>());
        getMessageLabel().setIsAvailable(false);
        addListeners();
    }

    public Map<DiskStorageType, ListModel<EntityModel<DiskModel>>> getAttachableDisksMap() {
        return attachableDisksMap;
    }

    @Override
    public void flush() {
        // no need to do any flush
    }

    @Override
    public void initialize() {
        super.initialize();

        getIsPlugged().setIsAvailable(true);

        if (getVm().getId() != null) {
            loadAttachableDisks();
        }
    }

    public void loadAttachableDisks() {
        doLoadAttachableDisks(new GetDisksCallback(DiskStorageType.IMAGE),
                new GetDisksCallback(DiskStorageType.LUN),
                new GetDisksCallback(DiskStorageType.CINDER));
    }

    protected void doLoadAttachableDisks(GetDisksCallback imageCallback, GetDisksCallback lunCallback,
                                         GetDisksCallback cinderCallback) {
        AsyncDataProvider.getInstance().getAllAttachableDisks(
                new AsyncQuery(this, imageCallback
                ), getVm().getStoragePoolId(), getVm().getId());

        AsyncDataProvider.getInstance().getAllAttachableDisks(
                new AsyncQuery(this, lunCallback
                ), null, getVm().getId());

        AsyncDataProvider.getInstance().getAllAttachableDisks(
                new AsyncQuery(this, cinderCallback
                ), null, getVm().getId());
    }

    class GetDisksCallback implements INewAsyncCallback {

        private DiskStorageType diskStorageType;

        GetDisksCallback(DiskStorageType diskStorageType) {
            this.diskStorageType = diskStorageType;
        }

        @Override
        public void onSuccess(Object model, Object returnValue) {
            List<Disk> disks = adjustReturnValue(returnValue);
            Collections.sort(disks, new Linq.DiskByAliasComparer());
            ArrayList<DiskModel> diskModels = Linq.disksToDiskModelList(disks);

            List<EntityModel<DiskModel>> entities = Linq.toEntityModelList(
                    Linq.filterDisksByType(diskModels, diskStorageType));
            initAttachableDisks(entities);
        }

        protected void initAttachableDisks(List<EntityModel<DiskModel>> entities) {
            getAttachableDisksMap().get(diskStorageType).setItems(entities);
        }

        protected List<Disk> adjustReturnValue(Object returnValue) {
            return (List<Disk>) returnValue;
        }
    }

    @Override
    public boolean validate() {
        if (isNoSelection()) {
            getInvalidityReasons().add(constants.noDisksSelected());
            setIsValid(false);
            return false;
        }
        return true;
    }

    @Override
    public void store(IFrontendActionAsyncCallback callback) {
        if (getProgress() != null || !validate()) {
            return;
        }

        ArrayList<VdcActionType> actionTypes = new ArrayList<VdcActionType>();
        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();
        ArrayList<IFrontendActionAsyncCallback> callbacks = new ArrayList<IFrontendActionAsyncCallback>();

        IFrontendActionAsyncCallback onFinishCallback = callback != null ? callback : new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {
                NewDiskModel diskModel = (NewDiskModel) result.getState();
                diskModel.stopProgress();
                diskModel.cancel();
            }
        };

        List<EntityModel<DiskModel>> disksToAttach = getSelectedDisks();
        for (int i = 0; i < disksToAttach.size(); i++) {
            DiskModel disk = disksToAttach.get(i).getEntity();

            /*
            IDE disks can be activated only when the VM is down.
            Other disks can be hot plugged.
             */
            boolean activate = false;
            if (getIsPlugged().getEntity()) {
                activate = disk.getDisk().getDiskInterface() == DiskInterface.IDE ?
                        getVm().getStatus() == VMStatus.Down : true;
            }

            // Disk is attached to VM as read only or not, null is applicable only for floating disks
            // but this is not a case here.
            AttachDetachVmDiskParameters parameters = new AttachDetachVmDiskParameters(
                    getVm().getId(), disk.getDisk().getId(), activate,
                    Boolean.TRUE.equals(disk.getDisk().getReadOnly()));

            actionTypes.add(VdcActionType.AttachDiskToVm);
            paramerterList.add(parameters);
            callbacks.add(i == disksToAttach.size() - 1 ? onFinishCallback : null);
        }

        startProgress(null);

        Frontend.getInstance().runMultipleActions(actionTypes, paramerterList, callbacks, null, this);
    }

    public EntityModel<String> getWarningLabel() {
        return warningLabel;
    }

    public void setWarningLabel(EntityModel<String> value) {
        warningLabel = value;
    }

    public EntityModel<String> getMessageLabel() {
        return messageLabel;
    }

    public void setMessageLabel(EntityModel<String> messageLabel) {
        this.messageLabel = messageLabel;
    }

    private boolean isNoSelection() {
        for (ListModel<EntityModel<DiskModel>> listModel : attachableDisksMap.values()) {
            boolean multipleSelectionSelected = listModel.getSelectedItems() != null && !listModel.getSelectedItems().isEmpty();
            boolean singleSelectionSelected = listModel.getSelectedItem() != null;
            if (multipleSelectionSelected || singleSelectionSelected) {
                return false;
            }
        }
        return true;
    }

    public List<EntityModel<DiskModel>> getSelectedDisks() {
        List<EntityModel<DiskModel>> selectedDisks = new ArrayList<EntityModel<DiskModel>>();
        for (ListModel<EntityModel<DiskModel>> listModel : attachableDisksMap.values()) {
            if (listModel.getSelectedItems() != null && !listModel.getSelectedItems().isEmpty()) {
                selectedDisks.addAll(listModel.getSelectedItems());
            }

            if (listModel.getSelectedItem() != null) {
                selectedDisks.add(listModel.getSelectedItem());
            }
        }
        return selectedDisks;
    }

    private boolean isSelectedDiskInterfaceIDE(List<EntityModel<DiskModel>> selectedDisks) {
        for (EntityModel<DiskModel> selectedDisk : selectedDisks) {
            if (selectedDisk.getEntity().getDisk().getDiskInterface() == DiskInterface.IDE) {
                return true;
            }
        }
        return false;
    }

    private void addListeners() {
        addSelectedItemsChangedListener();
        addIsPluggedEntityChangedListener();
    }

    private void updateWarningLabel() {
        getWarningLabel().setIsAvailable(false);
        if (getIsPlugged().getEntity().equals(Boolean.TRUE) && getVm().getStatus() != VMStatus.Down) {
            List<EntityModel<DiskModel>> selectedDisks = getSelectedDisks();
            if (selectedDisks != null && isSelectedDiskInterfaceIDE(selectedDisks)) {
                getWarningLabel().setEntity(constants.ideDisksWillBeAttachedButNotActivated());
                getWarningLabel().setIsAvailable(true);
            }
        }
    }

    private void addSelectedItemsChangedListener() {
        IEventListener<EventArgs> selectionChangedListener = new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                updateWarningLabel();
            }
        };
        attachableDisksMap.get(DiskStorageType.IMAGE).
                getSelectedItemsChangedEvent().addListener(selectionChangedListener);
        attachableDisksMap.get(DiskStorageType.LUN).
                getSelectedItemsChangedEvent().addListener(selectionChangedListener);
    }

    private void addIsPluggedEntityChangedListener() {
        IEventListener<EventArgs> entityChangedListener = new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                updateWarningLabel();
            }
        };
        getIsPlugged().getEntityChangedEvent().addListener(entityChangedListener);
    }
}
