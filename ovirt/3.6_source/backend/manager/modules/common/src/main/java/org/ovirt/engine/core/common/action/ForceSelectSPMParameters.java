package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.Guid;

public class ForceSelectSPMParameters extends VdcActionParametersBase {

    private static final long serialVersionUID = -755083459086386402L;

    private Guid preferredSPMId;

    public ForceSelectSPMParameters() {
    }

    public ForceSelectSPMParameters(Guid prefferedSPMId) {
        setPreferredSPMId(prefferedSPMId);
    }

    public Guid getPreferredSPMId() {
        return preferredSPMId;
    }

    public void setPreferredSPMId(Guid preferredSPMId) {
        this.preferredSPMId = preferredSPMId;
    }
}
