package org.ovirt.engine.core.bll.network.cluster;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkDao;

public class AddClusterNetworkClusterValidator extends NetworkClusterValidatorBase {

    public AddClusterNetworkClusterValidator(InterfaceDao interfaceDao,
            NetworkDao networkDao,
            NetworkCluster networkCluster,
            Version version) {
        super(interfaceDao, networkDao, networkCluster, version);
    }

    @Override
    protected boolean isManagementNetworkChangeInvalid() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isManagementNetworkChanged() {
        return false;
    }

    @Override
    public ValidationResult roleNetworkHasIp() {
        return ValidationResult.VALID;
    }
}
