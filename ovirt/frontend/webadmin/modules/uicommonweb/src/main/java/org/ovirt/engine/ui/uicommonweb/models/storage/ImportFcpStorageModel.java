package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

import java.util.ArrayList;
import java.util.List;

public class ImportFcpStorageModel extends ImportSanStorageModel {
    @Override
    public StorageType getType() {
        return StorageType.FCP;
    }

    public ImportFcpStorageModel() {
        setStorageDomains(new ListModel<StorageDomain>());
        getStorageDomains().setItems(new ArrayList<StorageDomain>());
    }

    @Override
    protected void update() {
        setMessage(null);
        getStorageDomains().setItems(new ArrayList<StorageDomain>());
        getUnregisteredStorageDomains(null);
    }

    @Override
    protected void postGetUnregisteredStorageDomains(List<StorageDomain> storageDomains, List<StorageServerConnections> connections) {
        setMessage(storageDomains == null || storageDomains.isEmpty() ?
                ConstantsManager.getInstance().getConstants().noStorageDomainsFound() : null);
    }
}
