package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.ui.uicommonweb.UICommand;

@SuppressWarnings("unused")
public interface IStorageModel
{
    StorageModel getContainer();

    void setContainer(StorageModel value);

    StorageType getType();

    StorageDomainType getRole();

    void setRole(StorageDomainType value);

    UICommand getUpdateCommand();

    boolean validate();
}
