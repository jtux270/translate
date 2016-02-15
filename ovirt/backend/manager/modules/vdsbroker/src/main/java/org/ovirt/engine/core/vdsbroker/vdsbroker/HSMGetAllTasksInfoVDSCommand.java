package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.ArrayList;
import java.util.Map;

import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.compat.Guid;

public class HSMGetAllTasksInfoVDSCommand<P extends VdsIdVDSCommandParametersBase> extends VdsBrokerCommand<P> {
    private TaskInfoListReturnForXmlRpc _result;
    private static final String VERB_KEY = "verb";

    public HSMGetAllTasksInfoVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        _result = getBroker().getAllTasksInfo();
        proceedProxyReturnValue();
        setReturnValue(parseTaskInfoList(_result.taskInfoList));
    }

    protected ArrayList<AsyncTaskCreationInfo> parseTaskInfoList(Map<String, Map<String, String>> taskInfoList) {
        try {
            ArrayList<AsyncTaskCreationInfo> result = new ArrayList<AsyncTaskCreationInfo>(
                    taskInfoList.size());
            for (Map.Entry<String, Map<String, String>> entry : taskInfoList.entrySet()) {
                Guid taskID = new Guid(entry.getKey());
                Map<String, String> taskInfo = entry.getValue();
                AsyncTaskCreationInfo task = parseTaskInfo(taskInfo, taskID);
                if (task != null) {
                    result.add(task);
                }
            }
            return result;
        } catch (RuntimeException exp) {
            log.errorFormat("Could not parse task info list: '{0}'", taskInfoList.toString());
            throw exp;
        }
    }

    protected AsyncTaskCreationInfo parseTaskInfo(Map<String, String> taskInfo, Guid taskID) {
        try {
            String deTaskType = taskInfo.get(VERB_KEY);
            AsyncTaskType taskType;
            try {
                taskType = AsyncTaskType.valueOf(deTaskType);
            } catch (Exception e) {
                taskType = AsyncTaskType.unknown;
                log.warn("The task type in the vdsm response is " + deTaskType
                        + " and does not appear in the AsyncTaskType enum");
            }

            AsyncTaskCreationInfo tempVar = new AsyncTaskCreationInfo();
            tempVar.setVdsmTaskId(taskID);
            tempVar.setTaskType(taskType);
            AsyncTaskCreationInfo task = tempVar;

            return task;
        }

        catch (RuntimeException e) {
            log.error(String.format(
                    "Could not parse single task info: '%1$s'  (possibly specific task should not be monitored).",
                    taskInfo), e);
            return null;
        }
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return _result.mStatus;
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return _result;
    }
}
