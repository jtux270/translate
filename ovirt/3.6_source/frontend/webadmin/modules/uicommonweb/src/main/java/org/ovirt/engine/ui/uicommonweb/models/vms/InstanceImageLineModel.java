package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.UIConstants;
import org.ovirt.engine.ui.uicompat.UIMessages;
import org.ovirt.engine.ui.uicompat.external.StringUtils;

public class InstanceImageLineModel extends EntityModel {

    public static final String CANCEL_DISK = "CancelDisk"; //$NON-NLS-1$

    public static final String DISK = "_Disk"; //$NON-NLS-1$

    private UIMessages messages = ConstantsManager.getInstance().getMessages();

    private UIConstants constants = ConstantsManager.getInstance().getConstants();

    private UICommand attachCommand;

    private UICommand createEditCommand;

    private EntityModel<AbstractDiskModel> diskModel = new EntityModel<>();

    // if the disk already exists in the engine or is just created here but not yet submitted
    private boolean diskExists;

    private EntityModel<String> name = new EntityModel<>();

    private InstanceImagesModel parentModel;

    private VM vm;

    private boolean active = true;

    public InstanceImageLineModel(InstanceImagesModel parentModel) {
        this.parentModel = parentModel;

        attachCommand = new UICommand("attachCommand", this); //$NON-NLS-1$
        createEditCommand = new UICommand("createEditCommand", this); //$NON-NLS-1$
    }

    private void fillData() {
        if (diskModel.getEntity() == null) {
            return;
        }

        if (diskModel.getEntity() instanceof InstanceImagesAttachDiskModel) {
            List<EntityModel<DiskModel>> disks = ((InstanceImagesAttachDiskModel) diskModel.getEntity()).getSelectedDisks();
            if (disks.size() != 0) {
                updateName(disks.get(0).getEntity().getDisk());
            }
        } else {
            updateName(diskModel.getEntity().getDisk());
        }
    }

    private void updateName(Disk disk) {
        if (disk == null) {
            return;
        }

        String diskName = disk.getDiskAlias();
        String size = Long.toString(disk.getSize());

        if (disk.getDiskStorageType() == DiskStorageType.IMAGE) {
            size = Long.toString(((DiskImage) disk).getSizeInGigabytes());
        }

        String type;
        if (diskExists) {
            type = constants.existingDisk();
        } else if (getDiskModel().getEntity() instanceof InstanceImagesAttachDiskModel) {
            type = constants.attachingDisk();
        } else {
            type = constants.creatingDisk();
        }

        String boot = ""; //$NON-NLS-1$
        if (disk.isBoot()) {
            boot = constants.bootDisk();
        }

        name.setEntity(messages.vmDialogDisk(diskName, size, type, boot));
    }


    public void initialize(Disk disk, VM vm) {
        this.vm = vm;
        active = true;
        diskExists = disk != null;

        attachCommand.setIsAvailable(!diskExists);

        if (disk == null) {
            return;
        }

        final AbstractDiskModel model = new EditDiskModel() {
            @Override
            public void onSave() {
                if (validate()) {
                    flush();
                    getDiskModel().setEntity(this);

                    // needed because the "entity" instances are the same so the event is not fired
                    fillData();

                    parentModel.getParentListModel().setWindow(null);
                    parentModel.getParentListModel().setWindow(parentModel.getUnitVmModel());
                }
            }

            @Override
            public void updateInterface(Version clusterVersion) {
                InstanceImageLineModel.this.updateInterface(clusterVersion, this);
            }

            @Override
            protected void updateBootableDiskAvailable() {
                updateBootableFrom(parentModel.getAllCurrentDisks());
            }
        };

        model.setDisk(disk);
        model.setVm(vm);

        setupModelAsDialog(model,
                ConstantsManager.getInstance().getConstants().editVirtualDiskTitle(),
                HelpTag.edit_virtual_disk, "edit_virtual_disk"); //$NON-NLS-1$

        model.initialize();
        diskModel.setEntity(model);
        fillData();
    }

    public EntityModel<AbstractDiskModel> getDiskModel() {
        return diskModel;
    }

    public EntityModel<String> getName() {
        return name;
    }

    public void setName(EntityModel<String> name) {
        this.name = name;
    }

    public boolean isGhost() {
        return diskModel.getEntity() == null;
    }

    public void attachDisk() {
        InstanceImagesAttachDiskModel model = new InstanceImagesAttachDiskModel() {
            @Override
            public void onSave() {

                if (validate()) {
                    flush();
                    List<EntityModel<DiskModel>> selectedDisks = getSelectedDisks();
                    if (selectedDisks.size() == 1) {
                        // only 0 or 1 is allowed
                        setDisk(selectedDisks.iterator().next().getEntity().getDisk());
                    }

                    getDiskModel().setEntity(this);
                    parentModel.getParentListModel().setWindow(null);
                    parentModel.getParentListModel().setWindow(parentModel.getUnitVmModel());
                    // from now on only editing is possible
                    attachCommand.setIsAvailable(false);

                    fillData();
                }
            }

            @Override
            public void updateInterface(Version clusterVersion) {
                InstanceImageLineModel.this.updateInterface(clusterVersion, this);
            }

            @Override
            protected void updateBootableDiskAvailable() {
                updateBootableFrom(parentModel.getAllCurrentDisks());
            }

            @Override
            protected List<Disk> getAttachedNotSubmittedDisks() {
                return parentModel.getNotYetAttachedNotAttachableDisks();
            }
        };

        VM realOrFakeVm = vm;
        Version compatibilityVersion = parentModel.getUnitVmModel().getSelectedCluster().getCompatibilityVersion();
        if (realOrFakeVm == null) {
            realOrFakeVm = new VM();
            realOrFakeVm.setId(null);
            realOrFakeVm.setVdsGroupId(parentModel.getUnitVmModel().getSelectedCluster().getId());
            realOrFakeVm.setStoragePoolId(parentModel.getUnitVmModel().getSelectedDataCenter().getId());
            realOrFakeVm.setVdsGroupCompatibilityVersion(compatibilityVersion);
        }

        model.setVm(realOrFakeVm);

        setupModelAsDialog(model,
                ConstantsManager.getInstance().getConstants().attachVirtualDiskTitle(),
                HelpTag.attach_virtual_disk, "attach_virtual_disk"); //$NON-NLS-1$
        showDialog(model);
        model.initialize(parentModel.getAllCurrentDisks());
        maybeLoadAttachableDisks(model);
    }

    private void maybeLoadAttachableDisks(InstanceImagesAttachDiskModel model) {
        if (model.getVm().getId() == null) {
            Integer osType = parentModel.getUnitVmModel().getOSType().getSelectedItem();
            Version compatibilityVersion = parentModel.getUnitVmModel().getSelectedCluster().getCompatibilityVersion();
            model.loadAttachableDisks(osType, compatibilityVersion, getDisk());
        } else {
            model.loadAttachableDisks(getDisk());
        }
    }

    public void createEditDisk() {
        if (parentModel.getUnitVmModel().getSelectedCluster() == null || parentModel.getUnitVmModel().getSelectedDataCenter() == null) {
            return;
        }

        if (getDiskModel().getEntity() == null) {
            showNewDialog();
        } else {
            showPreviouslyShownDialog();
        }
    }

    private void showPreviouslyShownDialog() {
        getDiskModel().getEntity().updateBootableFrom(parentModel.getAllCurrentDisks());
        if (getDiskModel().getEntity() instanceof InstanceImagesAttachDiskModel) {
            // needed to re-filter in case the OS or the compatibility version changed
            maybeLoadAttachableDisks((InstanceImagesAttachDiskModel) getDiskModel().getEntity());
        }
        showDialog(getDiskModel().getEntity());
    }

    private void showNewDialog() {
        final AbstractDiskModel model = new NewDiskModel() {
            @Override
            public void onSave() {
                if (validate()) {
                    flush();
                    getDiskModel().setEntity(this);
                    parentModel.getParentListModel().setWindow(null);
                    parentModel.getParentListModel().setWindow(parentModel.getUnitVmModel());
                    // the "new" turns into "edit" - no need for attach anymore
                    attachCommand.setIsAvailable(false);

                    fillData();

                    Disk disk = super.getDisk();
                    if (disk.getDiskStorageType() == DiskStorageType.IMAGE || disk.getDiskStorageType() == DiskStorageType.CINDER) {
                        ((DiskImage) disk).setActive(true);
                    }
                }
            }

            @Override
            public void updateInterface(Version clusterVersion) {
                InstanceImageLineModel.this.updateInterface(clusterVersion, this);
            }

            @Override
            protected void updateBootableDiskAvailable() {
                updateBootableFrom(parentModel.getAllCurrentDisks());
            }
        };

        VM vm = new VM();
        vm.setVdsGroupId(parentModel.getUnitVmModel().getSelectedCluster().getId());
        vm.setStoragePoolId(parentModel.getUnitVmModel().getSelectedDataCenter().getId());
        vm.setVdsGroupCompatibilityVersion(parentModel.getUnitVmModel().getSelectedCluster().getCompatibilityVersion());
        model.setVm(vm);
        model.getSizeExtend().setIsAvailable(false);

        setupModelAsDialog(model,
                ConstantsManager.getInstance().getConstants().newVirtualDiskTitle(),
                HelpTag.new_virtual_disk, "new_virtual_disk"); //$NON-NLS-1$

        showDialog(model);

        model.initialize(parentModel.getAllCurrentDisks());

        if (getVm() != null) {
            model.setVm(getVm());
            ((NewDiskModel)model).updateSuggestedDiskAliasFromServer();
        } else {
            String currentVmName = parentModel.getUnitVmModel().getName().getEntity();
            if (!StringUtils.isEmpty(currentVmName)) {
                // if already set the VM name on the new VM dialog, suggest the name according to the name
                model.getAlias().setEntity(suggestAliasForNewVm(currentVmName));
            }
        }
    }

    private String suggestAliasForNewVm(String currentVmName) {
        Set<String> aliases = createDiskAliasesList();
        String suggestedAlias;
        int i = 0;
        do {
            i++;
            suggestedAlias = currentVmName + DISK + i;
        } while (aliases.contains(suggestedAlias));

        return suggestedAlias;
    }

    private Set<String> createDiskAliasesList() {
        Set<String> res = new HashSet<>();
        for (Disk disk : parentModel.getAllCurrentDisks()) {
            res.add(disk.getDiskAlias());
        }

        return res;
    }

    private void setupModelAsDialog(AbstractDiskModel model, String title, HelpTag helpTag, String hashName) {
        model.setTitle(title);
        model.setHelpTag(helpTag);
        model.setHashName(hashName);

        UICommand cancelCommand = new UICommand(CANCEL_DISK, this);
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        model.setCancelCommand(cancelCommand);
    }

    private void showDialog(AbstractDiskModel model) {
        parentModel.getParentListModel().setWindow(null);
        parentModel.getParentListModel().setWindow(model);
    }

    @Override
    public void executeCommand(UICommand command) {
        if (!active) {
            // don't listen to this commands anymore - no need to show any more windows
            return;
        }
        if (CANCEL_DISK.equals(command.getName())) {
            parentModel.getParentListModel().setWindow(null);
            parentModel.getParentListModel().setWindow(parentModel.getUnitVmModel());
        } else if (command == createEditCommand) {
            createEditDisk();
        } else if (command == attachCommand) {
            attachDisk();
        } else {
            super.executeCommand(command);
        }
    }

    public boolean isBootable() {
        if (isGhost()) {
            return false;
        }

        return diskModel.getEntity().getIsBootable().getEntity();
    }

    public Disk getDisk() {
        AbstractDiskModel diskModel = getDiskModel().getEntity();

        if (diskModel == null) {
            return null;
        }

        if (diskModel.getDisk() != null) {
            return diskModel.getDisk();
        }

        DiskStorageType diskStorageType = diskModel.getDiskStorageType().getEntity();

        if (diskStorageType == DiskStorageType.IMAGE) {
            return diskModel.getDiskImage();
        }

        if (diskStorageType == DiskStorageType.LUN) {
            return diskModel.getLunDisk();
        }

        return null;
    }

    public boolean isDiskExists() {
        return diskExists;
    }

    public VM getVm() {
        return vm;
    }

    public UICommand getAttachCommand() {
        return attachCommand;
    }

    public UICommand getCreateEditCommand() {
        return createEditCommand;
    }


    public void setEnabled(boolean enabled) {
        attachCommand.setIsExecutionAllowed(enabled);
        createEditCommand.setIsExecutionAllowed(enabled);
    }

    public void deactivate() {
        active = false;
    }

    public void updateInterface(Version clusterVersion, AbstractDiskModel model) {
        model.getIsVirtioScsiEnabled().setEntity(Boolean.TRUE.equals(parentModel.getUnitVmModel().getIsVirtioScsiEnabled().getEntity()));
        model.updateInterfaceList(clusterVersion);
    }
}
