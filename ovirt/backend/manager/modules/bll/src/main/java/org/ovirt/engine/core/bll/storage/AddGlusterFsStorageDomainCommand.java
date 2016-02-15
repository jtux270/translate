package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.compat.Guid;

/**
 * Adds Gluster storage domain
 */
public class AddGlusterFsStorageDomainCommand<T extends StorageDomainManagementParameter> extends AddStorageDomainCommon<T> {

    protected AddGlusterFsStorageDomainCommand(Guid commandId) {
        super(commandId);
    }

    public AddGlusterFsStorageDomainCommand(T parameters) {
        super(parameters);
    }

}
