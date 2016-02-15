package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.compat.Guid;

public class VdsIdAndVdsVDSCommandParametersBase extends VdsIdVDSCommandParametersBase {
    private VDS privateVds;

    public VDS getVds() {
        return privateVds;
    }

    public void setVds(VDS value) {
        privateVds = value;
    }

    public VdsIdAndVdsVDSCommandParametersBase(VDS vds) {
        super(vds.getId());
        setVds(vds);
    }

    public VdsIdAndVdsVDSCommandParametersBase(Guid vdsId) {
        super(vdsId);
    }

    public VdsIdAndVdsVDSCommandParametersBase() {
    }

    @Override
    public String toString() {
        return String.format("%s, vds=%s", super.toString(), getVds());
    }
}
