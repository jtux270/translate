package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.Guid;

public class ProcessOvfUpdateForStorageDomainCommandParameters extends StorageDomainParametersBase {
    private boolean skipDomainChecks;

    public ProcessOvfUpdateForStorageDomainCommandParameters() {
        super();
    }

    public ProcessOvfUpdateForStorageDomainCommandParameters(Guid storagePoolId, Guid storageDomainId) {
        super(storagePoolId, storageDomainId);
    }

    public boolean isSkipDomainChecks() {
        return skipDomainChecks;
    }

    public void setSkipDomainChecks(boolean skipDomainChecks) {
        this.skipDomainChecks = skipDomainChecks;
    }
}
