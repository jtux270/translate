package org.ovirt.engine.core.bll.gluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.utils.ClusterUtils;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.gluster.GlusterVolumeValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.dao.VdsStaticDAO;
import org.ovirt.engine.core.dao.gluster.GlusterBrickDao;

/**
 * Base class for all Gluster commands
 */
public abstract class GlusterCommandBase<T extends VdcActionParametersBase> extends CommandBase<T> {
    protected AuditLogType errorType;
    protected VDS upServer;

    public GlusterCommandBase(T params) {
        this(params, null);
    }

    public GlusterCommandBase(T params, CommandContext commandContext) {
        super(params, commandContext);
    }

    @Override
    protected BackendInternal getBackend() {
        return super.getBackend();
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        if (!isInternalExecution()) {
            return Collections.singletonMap(getVdsGroupId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.GLUSTER, VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED));
        }
        return super.getExclusiveLocks();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ovirt.engine.core.bll.CommandBase#getPermissionCheckSubjects()
     */
    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        // By default, check permissions at cluster level. Commands that need
        // more granular permissions can override this method.
        return Collections.singletonList(new PermissionSubject(getVdsGroupId(),
                VdcObjectType.VdsGroups,
                getActionType().getActionGroup()));
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = new HashMap<String, String>();
            VDSGroup vdsGroup = getVdsGroup();
            jobProperties.put(GlusterConstants.CLUSTER, vdsGroup == null ? null : vdsGroup.getName());
        }

        return jobProperties;
    }

    /**
     * This server is chosen as random from all the Up servers.
     *
     * @return One of the servers in up status
     */
    protected VDS getUpServer() {
        return getClusterUtils().getRandomUpServer(getVdsGroupId());
    }

    protected ClusterUtils getClusterUtils() {
        return ClusterUtils.getInstance();
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        upServer = getUpServer();
        if (upServer == null) {
            addCanDoActionMessageVariable("clusterName", getVdsGroup().getName());
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NO_UP_SERVER_FOUND);
            return false;
        }
        return true;
    }

    protected void handleVdsErrors(AuditLogType errType, List<String> errors) {
        errorType = errType;
        getReturnValue().getExecuteFailedMessages().addAll(errors);
        getReturnValue().getFault().setMessage(StringUtils.join(errors, SystemUtils.LINE_SEPARATOR));
        // Setting Error to null to make the FrontendErrorHandler to use the Message which is being set above instead of
        // the VdcBllErrors.ENGINE(which will get translated to "Internal engine error")
        getReturnValue().getFault().setError(null);
    }

    protected void handleVdsError(AuditLogType errType, String error) {
        errorType = errType;
        getReturnValue().getExecuteFailedMessages().add(error);
        getReturnValue().getFault().setMessage(error);
        // Setting Error to null to make the FrontendErrorHandler to use the Message which is being set above instead of
        // the VdcBllErrors.ENGINE(which will get translated to "Internal engine error")
        getReturnValue().getFault().setError(null);
    }

    protected boolean updateBrickServerNames(List<GlusterBrickEntity> bricks, boolean addCanDoActionMessage) {
        for (GlusterBrickEntity brick : bricks) {
            if (!updateBrickServerName(brick, addCanDoActionMessage)) {
                return false;
            }
        }
        return true;
    }

    protected boolean updateBrickServerName(GlusterBrickEntity brick, boolean addCanDoActionMessage) {
        VdsStatic server = getVdsStaticDao().get(brick.getServerId());
        if ((server == null || !server.getVdsGroupId().equals(getVdsGroupId()))) {
            if (addCanDoActionMessage) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_INVALID_BRICK_SERVER_ID);
            }
            return false;
        }
        brick.setServerName(server.getHostName());
        return true;
    }

    protected boolean validateDuplicateBricks(List<GlusterBrickEntity> newBricks) {
        Set<String> bricks = new HashSet<String>();
        for (GlusterBrickEntity brick : newBricks) {
            if (bricks.contains(brick.getQualifiedName())) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DUPLICATE_BRICKS);
                addCanDoActionMessageVariable("brick", brick.getQualifiedName());
                return false;
            }
            bricks.add(brick.getQualifiedName());

            GlusterBrickEntity existingBrick =
                    getGlusterBrickDao().getBrickByServerIdAndDirectory(brick.getServerId(), brick.getBrickDirectory());
            if (existingBrick != null) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_BRICK_ALREADY_EXISTS_IN_VOLUME);
                addCanDoActionMessageVariable("brick", brick.getQualifiedName());
                addCanDoActionMessageVariable("volumeName",
                        getGlusterVolumeDao().getById(existingBrick.getVolumeId()).getName());
                return false;
            }
        }
        return true;
    }

    protected GlusterVolumeValidator createVolumeValidator() {
        return new GlusterVolumeValidator();
    }

    public VdsStaticDAO getVdsStaticDao() {
        return getDbFacade().getVdsStaticDao();
    }

    protected GlusterBrickDao getGlusterBrickDao() {
        return getDbFacade().getGlusterBrickDao();
    }
}
