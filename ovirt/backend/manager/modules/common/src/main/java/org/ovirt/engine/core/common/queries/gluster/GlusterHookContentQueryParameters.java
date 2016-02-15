package org.ovirt.engine.core.common.queries.gluster;

import javax.validation.constraints.NotNull;

import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.compat.Guid;

/**
 * Parameter class with Gluster Hook Id and Host Id as parameters. <br>
 * This will be used by Gluster Hook Content query. <br>
 */
public class GlusterHookContentQueryParameters extends VdcQueryParametersBase {
    private static final long serialVersionUID = 4564573475511998657L;

    @NotNull(message = "VALIDATION.GLUSTER.GLUSTER_HOOK_ID.NOT_NULL")
    private Guid glusterHookId;

    private Guid glusterServerId;

    public GlusterHookContentQueryParameters() {
    }

    public GlusterHookContentQueryParameters(Guid glusterHookId) {
        setGlusterHookId(glusterHookId);
    }

    public Guid getGlusterHookId() {
        return glusterHookId;
    }

    public void setGlusterHookId(Guid glusterHookId) {
        this.glusterHookId = glusterHookId;
    }

    public Guid getGlusterServerId() {
        return glusterServerId;
    }

    public void setGlusterServerId(Guid glusterServerId) {
        this.glusterServerId = glusterServerId;
    }

}
