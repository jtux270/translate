package org.ovirt.engine.core.common.businessentities;

import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;

public class VmJob implements BusinessEntity<Guid> {

    private static final long serialVersionUID = -1748312497527481706L;
    private Guid id;
    private Guid vmId;
    private VmJobState jobState;
    private VmJobType jobType;

    public VmJob() {
        id = Guid.Empty;
        vmId = Guid.Empty;
        jobState = VmJobState.UNKNOWN;
        jobType = VmJobType.UNKNOWN;
    }

    @Override
    public Guid getId() {
        return id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }

    public Guid getVmId() {
        return vmId;
    }

    public void setVmId(Guid id) {
        this.vmId = id;
    }

    public VmJobState getJobState() {
        return jobState;
    }

    public void setJobState(VmJobState jobState) {
        this.jobState = jobState;
    }

    public VmJobType getJobType() {
        return jobType;
    }

    public void setJobType(VmJobType jobType) {
        this.jobType = jobType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        VmJob other = (VmJob) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && ObjectUtils.objectsEqual(vmId, other.vmId)
                && jobType == other.jobType);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + (vmId == null ? 0 : vmId.hashCode());
        result = prime * result + (jobType == null ? 0 : jobType.hashCode());
        return result;
    }
}
