package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.common.businessentities.AuditLog;

public class AddExternalEventParameters extends VdcActionParametersBase {
    private static final long serialVersionUID = 7023971593753015624L;

    private AuditLog event;

    public AuditLog getEvent() {
        return event;
    }

    public AddExternalEventParameters() {
    }

    public AddExternalEventParameters(AuditLog event) {
        super();
        this.event = event;
    }
}
