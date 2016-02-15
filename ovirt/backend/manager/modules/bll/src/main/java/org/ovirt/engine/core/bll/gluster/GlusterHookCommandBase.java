package org.ovirt.engine.core.bll.gluster;

import java.util.List;

import org.ovirt.engine.core.common.action.gluster.GlusterHookParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterHookStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerHook;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

/**
 * Base class for all Gluster Hook commands
 */
public abstract class GlusterHookCommandBase<T extends GlusterHookParameters> extends GlusterCommandBase<T> {
    protected GlusterHookEntity entity;

    public GlusterHookCommandBase(T params) {
        super(params);
    }

    @Override
    public VDSGroup getVdsGroup() {
        if (getGlusterHook() != null) {
            setVdsGroupId(getGlusterHook().getClusterId());
        }
        return super.getVdsGroup();
    }

    protected GlusterHookEntity getGlusterHook() {
        if (entity == null) {
            entity = getGlusterHooksDao().getById(getParameters().getHookId(), true);
        }
        return entity;
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        if (Guid.isNullOrEmpty(getParameters().getHookId())) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_HOOK_ID_IS_REQUIRED);
            return false;
        }

        if (getGlusterHooksDao().getById(getParameters().getHookId()) == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_HOOK_DOES_NOT_EXIST);
            return false;
        }

        return true;
    }

    protected List<VDS> getAllUpServers(Guid clusterId) {
        return getClusterUtils().getAllUpServers(clusterId);
    }

    protected void updateServerHookStatusInDb(Guid hookId, Guid serverId, GlusterHookStatus status) {
        getGlusterHooksDao().updateGlusterServerHookStatus(hookId, serverId, status);
    }

    protected void updateHookInDb(GlusterHookEntity hook) {
        getGlusterHooksDao().updateGlusterHook(hook);
    }

    protected void addServerHookInDb(GlusterServerHook serverHook) {
        getGlusterHooksDao().saveGlusterServerHook(serverHook);
    }

    protected void updateGlusterHook(GlusterHookEntity entity) {
        if (entity.getConflictStatus() == 0) {
            getGlusterHooksDao().removeGlusterServerHooks(entity.getId());
        }
        getGlusterHooksDao().updateGlusterHook(entity);

    }

}
