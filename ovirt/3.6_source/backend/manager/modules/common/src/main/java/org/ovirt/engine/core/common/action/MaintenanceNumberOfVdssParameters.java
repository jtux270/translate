package org.ovirt.engine.core.common.action;

import java.util.LinkedList;
import java.util.List;

import org.ovirt.engine.core.compat.Guid;

public class MaintenanceNumberOfVdssParameters extends VdcActionParametersBase {
    private static final long serialVersionUID = 8806810521151850069L;

    private List<Guid> _vdsIdList;

    private boolean _isInternal;

    /*
     * If the power management policy is responsible for this action
     * pass true so we keep the powerManagementControlledByPolicy flag set.
     *
     * If the user triggered this action, clear the flag.
     */
    private boolean keepPolicyPMEnabled = false;

    private String reason;

    public MaintenanceNumberOfVdssParameters(List<Guid> vdsIdList, boolean isInternal) {
        this(vdsIdList, isInternal, "");
    }

    public MaintenanceNumberOfVdssParameters(List<Guid> vdsIdList, boolean isInternal, String reason) {
        _vdsIdList = vdsIdList;
        _isInternal = isInternal;
        this.reason = reason;
    }

    public MaintenanceNumberOfVdssParameters(List<Guid> vdsIdList, boolean isInternal, boolean keepPolicyPMEnabled) {
        this(vdsIdList, isInternal);
        this.keepPolicyPMEnabled = keepPolicyPMEnabled;
    }

    public Iterable<Guid> getVdsIdList() {
        return _vdsIdList == null ? new LinkedList<Guid>() : _vdsIdList;
    }

    public void setVdsIdList(List<Guid> value) {
        _vdsIdList = value;
    }

    public boolean getIsInternal() {
        return _isInternal;
    }

    public boolean getKeepPolicyPMEnabled() {
        return keepPolicyPMEnabled;
    }

    public void setKeepPolicyPMEnabled(boolean _keepPolicyPMEnabled) {
        this.keepPolicyPMEnabled = _keepPolicyPMEnabled;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        reason = value;
    }

    public MaintenanceNumberOfVdssParameters() {
    }
}
