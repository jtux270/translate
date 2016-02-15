package org.ovirt.engine.core.bll.gluster.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.utils.ClusterUtils;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterAsyncTask;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.job.ExternalSystemType;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class GlusterTasksService {
    private static final Log log = LogFactory.getLog(GlusterTasksService.class);

    public Map<Guid, GlusterAsyncTask> getTaskListForCluster(Guid id) {
        VDS upServer = ClusterUtils.getInstance().getRandomUpServer(id);
        if (upServer == null) {
            log.info("No up server in cluster");
            return null;
        }
        VDSReturnValue returnValue =runVdsCommand(VDSCommandType.GlusterTasksList,
                new VdsIdVDSCommandParametersBase(upServer.getId()));
        if (returnValue.getSucceeded()) {
            List<GlusterAsyncTask> tasks = (List<GlusterAsyncTask>)returnValue.getReturnValue();
            Map<Guid, GlusterAsyncTask> tasksMap = new HashMap<>();
            for (GlusterAsyncTask task: tasks) {
                tasksMap.put(task.getTaskId(), task);
            }
            return tasksMap;
        } else {
            log.error(returnValue.getVdsError());
            throw new VdcBLLException(VdcBllErrors.GlusterVolumeStatusAllFailedException, returnValue.getVdsError().getMessage());
        }
    }

    public GlusterAsyncTask getTask(Guid taskId) {
        //Get the cluster associated with task and see if host is UP
        return null;
    }

    /**
     * Gets the list of stored tasks in database where the job is not ended
     *
     * @return
     */
    public List<Guid> getMonitoredTaskIDsInDB() {
      List<Guid> externalIds = DbFacade.getInstance().getStepDao().
                getExternalIdsForRunningSteps(ExternalSystemType.GLUSTER);
        return externalIds;
    }

    private VDSReturnValue runVdsCommand(VDSCommandType commandType, VDSParametersBase params) {
        return Backend.getInstance().getResourceManager().RunVdsCommand(commandType, params);
    }

}
