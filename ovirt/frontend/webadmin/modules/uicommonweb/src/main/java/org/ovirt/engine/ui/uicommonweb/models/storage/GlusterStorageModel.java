package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

@SuppressWarnings("unused")
public class GlusterStorageModel extends Model implements IStorageModel {

    private UICommand updateCommand;

    @Override
    public UICommand getUpdateCommand() {
        return updateCommand;
    }

    private void setUpdateCommand(UICommand value) {
        updateCommand = value;
    }

    private StorageModel container;

    @Override
    public StorageModel getContainer() {
        return container;
    }

    @Override
    public void setContainer(StorageModel value) {
        container = value;
    }

    private StorageDomainType privateRole = StorageDomainType.values()[0];

    @Override
    public StorageDomainType getRole() {
        return privateRole;
    }

    @Override
    public void setRole(StorageDomainType value) {
        privateRole = value;
    }

    private EntityModel<String> path;

    public EntityModel<String> getPath() {
        return path;
    }

    private void setPath(EntityModel<String> value) {
        path = value;
    }

    private EntityModel<String> vfsType;

    public EntityModel<String> getVfsType() {
        return vfsType;
    }

    private void setVfsType(EntityModel<String> value) {
        vfsType = value;
    }

    private EntityModel<String> mountOptions;

    public EntityModel<String> getMountOptions() {
        return mountOptions;
    }

    private void setMountOptions(EntityModel<String> value) {
        mountOptions = value;
    }

    public String getConfigurationMessage() {
        return ConstantsManager.getInstance().getConstants().glusterDomainConfigurationMessage();
    }


    public GlusterStorageModel() {

        setUpdateCommand(new UICommand("Update", this)); //$NON-NLS-1$

        setPath(new EntityModel<String>());
        setVfsType(new EntityModel<String>());
        setMountOptions(new EntityModel<String>());

        getVfsType().setEntity("glusterfs"); //$NON-NLS-1$
        getVfsType().setIsChangable(false);
    }

    @Override
    public boolean validate() {

        getPath().validateEntity(
            new IValidation[] {
                new NotEmptyValidation(),
            }
        );

        getVfsType().validateEntity(
            new IValidation[] {
                new NotEmptyValidation(),
            }
        );


        return getPath().getIsValid()
            && getVfsType().getIsValid();
    }

    @Override
    public StorageType getType() {
        return StorageType.GLUSTERFS;
    }
}
