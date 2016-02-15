package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.compat.Guid;

public class AddInternalJobParameters extends AddJobParameters {

    private static final long serialVersionUID = -7824725232199970355L;
    private VdcActionType actionType;
    private VdcObjectType jobEntityType;
    private Guid jobEntityId;

    public AddInternalJobParameters() {
    }

    public AddInternalJobParameters(VdcActionType actionType, boolean isAutoCleared) {
        super();
        this.actionType = actionType;
        this.isAutoCleared = isAutoCleared;
    }

    public AddInternalJobParameters(String description, VdcActionType actionType, boolean isAutoCleared) {
        super();
        this.description = description;
        this.actionType = actionType;
        this.isAutoCleared = isAutoCleared;
    }

    public AddInternalJobParameters(String description, VdcActionType actionType, boolean isAutoCleared,
                                    VdcObjectType jobEntityType, Guid jobEntityId) {
        this(description, actionType, isAutoCleared);
        this.jobEntityType = jobEntityType;
        this.jobEntityId = jobEntityId;
    }

    public VdcActionType getActionType() {
        return actionType;
    }

    public void setActionType(VdcActionType actionType) {
        this.actionType = actionType;
    }

    public VdcObjectType getJobEntityType() {
        return jobEntityType;
    }

    public void setJobEntityType(VdcObjectType jobEntityType) {
        this.jobEntityType = jobEntityType;
    }

    public Guid getJobEntityId() {
        return jobEntityId;
    }

    public void setJobEntityId(Guid jobEntityId) {
        this.jobEntityId = jobEntityId;
    }

}
