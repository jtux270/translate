package org.ovirt.engine.core.common.businessentities.gluster;

import static org.ovirt.engine.core.common.businessentities.gluster.GlusterHookConflictFlags.CONTENT_CONFLICT;
import static org.ovirt.engine.core.common.businessentities.gluster.GlusterHookConflictFlags.MISSING_HOOK;
import static org.ovirt.engine.core.common.businessentities.gluster.GlusterHookConflictFlags.STATUS_CONFLICT;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;

/**
 * The gluster hook entity.
 *
 * @see GlusterHookStage
 * @see GlusterHookStatus
 */
public class GlusterHookEntity extends IVdcQueryable implements BusinessEntity<Guid> {

    private static final long serialVersionUID = -1139174348695506810L;

    private Guid id;

    private Guid clusterId;

    private String glusterCommand;

    private GlusterHookStage stage;

    private String name;

    private GlusterHookStatus status;

    private GlusterHookContentType contentType;

    private String checksum;

    private String content;

    private Integer conflictStatus;

    private List<GlusterServerHook> serverHooks;

    public GlusterHookEntity() {
        super();
        id = Guid.newGuid();
        conflictStatus = 0;
        serverHooks = new ArrayList<GlusterServerHook>();
    }

    @Override
    public Guid getId() {
        return id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }

    public Guid getClusterId() {
        return clusterId;
    }

    public void setClusterId(Guid clusterId) {
        this.clusterId = clusterId;
    }

    public String getGlusterCommand() {
        return this.glusterCommand;
    }

    public void setGlusterCommand(String glusterCommand) {
        this.glusterCommand = glusterCommand;
    }

    public GlusterHookStage getStage() {
        return stage;
    }

    public void setStage(GlusterHookStage stage) {
        this.stage = stage;
    }

    public void setStage(String stage) {
        this.stage = GlusterHookStage.valueOf(stage);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String hookName) {
        this.name = hookName;
    }

    public Integer getConflictStatus() {
        return conflictStatus;
    }

    public void setConflictStatus(Integer conflictStatus) {
        this.conflictStatus = conflictStatus;
    }

    public GlusterHookContentType getContentType() {
        return contentType;
    }

    public void setContentType(GlusterHookContentType contentType) {
        this.contentType = contentType;
    }

    public void setContentType(String contentType) {
        if (contentType != null) {
            if (contentType.toLowerCase().contains("binary")) {
                this.contentType = GlusterHookContentType.BINARY;
            } else {
                this.contentType = GlusterHookContentType.TEXT;
            }
        }
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public GlusterHookStatus getStatus() {
        return status;
    }

    public void setStatus(GlusterHookStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        setStatus(GlusterHookStatus.valueOf(status));
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<GlusterServerHook> getServerHooks() {
        return serverHooks;
    }

    public void setServerHooks(List<GlusterServerHook> serverHooks) {
        this.serverHooks = serverHooks;
    }

    public boolean isMissingHookConflict() {
        return (conflictStatus & MISSING_HOOK.getValue()) == MISSING_HOOK.getValue();
    }

    public boolean isStatusConflict() {
        return (conflictStatus & STATUS_CONFLICT.getValue()) == STATUS_CONFLICT.getValue();
    }

    public boolean isContentConflict() {
        return (conflictStatus & CONTENT_CONFLICT.getValue()) == CONTENT_CONFLICT.getValue();
    }

    public void setConflictValue(boolean missingHook, boolean statusConflict, boolean contentConflict) {
        conflictStatus = 0;
        if (missingHook) {
            conflictStatus = conflictStatus | MISSING_HOOK.getValue();
        }
        if (statusConflict) {
            conflictStatus = conflictStatus | STATUS_CONFLICT.getValue();
        }
        if (contentConflict) {
            conflictStatus = conflictStatus | CONTENT_CONFLICT.getValue();
        }
    }

    public void addStatusConflict() {
        conflictStatus = conflictStatus | STATUS_CONFLICT.getValue();
    }

    public void addMissingConflict() {
        conflictStatus = conflictStatus | MISSING_HOOK.getValue();
    }

    public void addContentConflict() {
        conflictStatus = conflictStatus | CONTENT_CONFLICT.getValue();
    }

    public boolean hasConflicts() {
        return conflictStatus != null && conflictStatus > 0;
    }

    public void removeStatusConflict() {
        conflictStatus &= ~STATUS_CONFLICT.getValue();
    }

    public void removeMissingConflict() {
        conflictStatus &= ~MISSING_HOOK.getValue();
    }

    public void removeContentConflict() {
        conflictStatus &= ~CONTENT_CONFLICT.getValue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getId().hashCode();
        result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
        result = prime * result + ((glusterCommand == null) ? 0 : glusterCommand.hashCode());
        result = prime * result + ((stage == null) ? 0 : stage.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
        result = prime * result + ((conflictStatus == null) ? 0 : conflictStatus.hashCode());
        result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GlusterHookEntity)) {
            return false;
        }
        GlusterHookEntity hook = (GlusterHookEntity) obj;

        if (!(ObjectUtils.objectsEqual(getId(), hook.getId())
                && ObjectUtils.objectsEqual(clusterId, hook.getClusterId())
                && ObjectUtils.objectsEqual(glusterCommand, hook.getGlusterCommand())
                && stage == hook.getStage()
                && ObjectUtils.objectsEqual(name, hook.getName())
                && contentType == hook.getContentType()
                && ObjectUtils.objectsEqual(conflictStatus, hook.getConflictStatus())
                && ObjectUtils.objectsEqual(checksum, hook.getChecksum())
                && status == hook.getStatus())) {
            return false;
        }
        return true;
    }

    @Override
    public Object getQueryableId() {
        return getId();
    }

    public String getHookKey() {
        return new StringBuilder().append(glusterCommand).append("-")
                .append(stage).append("-").append(name).toString();
    }
}
