package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

@SuppressWarnings("unused")
public class IscsiStorageModel extends SanStorageModel
{
    @Override
    public StorageType getType()
    {
        return StorageType.ISCSI;
    }

    @Override
    protected String getListName() {
        return "IscsiStorageModel"; //$NON-NLS-1$
    }

    @Override
    public String getLoginButtonLabel() {
        return ConstantsManager.getInstance().getConstants().loginAllButtonLabel();
    }
}
