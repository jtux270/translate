package org.ovirt.engine.core.common.action.gluster;

import javax.validation.constraints.NotNull;

import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.compat.Guid;

public class GlusterHookParameters extends VdcActionParametersBase {

    private static final long serialVersionUID = -8236696198344082891L;

    @NotNull(message = "VALIDATION.GLUSTER.GLUSTER_HOOK_ID.NOT_NULL")
    private Guid hookId;

    public GlusterHookParameters() {
    }

    public GlusterHookParameters(Guid hookId) {
        setHookId(hookId);
    }

    public Guid getHookId() {
        return hookId;
    }

    public void setHookId(Guid hookId) {
        this.hookId = hookId;
    }
}
