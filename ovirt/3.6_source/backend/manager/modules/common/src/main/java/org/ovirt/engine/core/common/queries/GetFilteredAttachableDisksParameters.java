package org.ovirt.engine.core.common.queries;

import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;

public class GetFilteredAttachableDisksParameters extends GetAllAttachableDisksForVmQueryParameters {

    private static final long serialVersionUID = 4092810692277463140L;

    private int os;

    private Version vdsGroupCompatibilityVersion;

    public GetFilteredAttachableDisksParameters() {
        super();
    }

    public GetFilteredAttachableDisksParameters(Guid storagePoolId) {
        super(storagePoolId);
    }

    public int getOs() {
        return os;
    }

    public void setOs(int os) {
        this.os = os;
    }

    public Version getVdsGroupCompatibilityVersion() {
        return vdsGroupCompatibilityVersion;
    }

    public void setVdsGroupCompatibilityVersion(Version vdsGroupCompatibilityVersion) {
        this.vdsGroupCompatibilityVersion = vdsGroupCompatibilityVersion;
    }
}
