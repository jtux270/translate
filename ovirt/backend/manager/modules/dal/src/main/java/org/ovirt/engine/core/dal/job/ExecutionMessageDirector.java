package org.ovirt.engine.core.dal.job;

import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;

/**
 * Contains messages by the context of Job or Step, as read from <i>bundles/ExecutionMessages.properties</i>
 */
public class ExecutionMessageDirector {

    public static final String EXECUTION_MESSAGES_FILE_PATH = "bundles/ExecutionMessages";

    /**
     * The prefix of the job message in the properties file
     */
    protected static final String JOB_MESSAGE_PREFIX = "job.";

    /**
     * The prefix of the step message in the properties file
     */
    protected static final String STEP_MESSAGE_PREFIX = "step.";

    /**
     * A single instance of the {@code ExecutionMessageDirector}
     */
    private static ExecutionMessageDirector instance = new ExecutionMessageDirector();

    /**
     * Logger
     */
    private static final Log log = LogFactory.getLog(ExecutionMessageDirector.class);

    /**
     * Stores the job messages
     */
    private Map<VdcActionType, String> jobMessages = new EnumMap<VdcActionType, String>(VdcActionType.class);

    /**
     * Stores the step messages
     */
    private Map<StepEnum, String> stepMessages = new EnumMap<StepEnum, String>(StepEnum.class);

    private ExecutionMessageDirector() {
    }

    /**
     * Load resources files and initialize the messages cache.
     *
     * @param bundleBaseName
     *            The base name of the resource bundle
     */
    public void initialize(String bundleBaseName) {
        log.info("Start initializing " + getClass().getSimpleName());
        ResourceBundle bundle = ResourceBundle.getBundle(bundleBaseName);
        final int jobMessagePrefixLength = JOB_MESSAGE_PREFIX.length();
        final int stepMessagePrefixLength = STEP_MESSAGE_PREFIX.length();

        for (String key : bundle.keySet()) {

            if (key.startsWith(JOB_MESSAGE_PREFIX)) {
                addMessage(key, bundle.getString(key), jobMessages, VdcActionType.class, jobMessagePrefixLength);
            } else if (key.startsWith(STEP_MESSAGE_PREFIX)) {
                addMessage(key, bundle.getString(key), stepMessages, StepEnum.class, stepMessagePrefixLength);
            } else {
                log.errorFormat("The message key {0} cannot be categorized since not started with {1} nor {2}",
                        key,
                        JOB_MESSAGE_PREFIX,
                        STEP_MESSAGE_PREFIX);
            }
        }
        log.info("Finished initializing " + getClass().getSimpleName());
    }

    /**
     * Adds a pair of {@code Enum} and message to the messages map. If the key is not valid, an error message is logged.
     * The key should be resolvable as an {@code Enum}, once its prefix is trimmed and the searched for an {@code Enum}
     * match by name. Possible entries of (key,value) from the resource bundle:
     *
     * <pre>
     * job.ChangeVMCluster=Change VM ${VM} Cluster to ${VdsGroups}
     * step.VALIDATING=Validating
     * </pre>
     *
     * @param key
     *            The key of the pair to be added, by which the enum is searched.
     * @param value
     *            The message of the pair to be added
     * @param enumClass
     *            The enum class search for an instance which match the key
     * @param messagesMap
     *            The map whic the message should be added to
     * @param prefixLength
     *            The length of the key prefix
     */
    private <T extends Enum<T>> void addMessage(String key,
            String value,
            Map<T, String> messagesMap,
            Class<T> enumClass,
            int prefixLength) {

        T enumKey = null;

        try {
            enumKey = T.valueOf(enumClass, key.substring(prefixLength));
        } catch (IllegalArgumentException e) {
            log.errorFormat("Message key {0} is not valid for enum {1}", key, enumClass.getSimpleName());
            return;
        }

        if (!messagesMap.containsKey(key)) {
            messagesMap.put(enumKey, value);
        } else {
            log.warnFormat("Code {0} appears more then once in {1} table.", key, enumClass.getSimpleName());
        }
    }

    public static ExecutionMessageDirector getInstance() {
        return instance;
    }

    /**
     * Retrieves a message by the step name.
     *
     * @param stepName
     *            The name by which the message is retrieved
     * @return A message describing the step, or the step name by {@code StepEnum.name()} if not found.
     */
    public String getStepMessage(StepEnum stepName) {
        return getMessage(stepMessages, stepName);
    }

    /**
     * Retrieves a message by the action type.
     *
     * @param actionType
     *            The type by which the message is retrieved
     * @return A message describing the action type, or the action type name by {@code VdcActionType.name()} if not
     *         found.
     */
    public String getJobMessage(VdcActionType actionType) {
        return getMessage(jobMessages, actionType);
    }

    private <T extends Enum<T>> String getMessage(Map<T, String> map, T type) {
        String message = map.get(type);
        if (message == null) {
            log.warnFormat("The message key {0} is missing from {1}", type.name(), EXECUTION_MESSAGES_FILE_PATH);
            message = type.name();
        }
        return message;
    }

    public static String resolveJobMessage(VdcActionType actionType, Map<String, String> values) {
        String jobMessage = getInstance().getJobMessage(actionType);
        if (jobMessage != null) {
            return AuditLogDirector.resolveMessage(jobMessage, values);
        } else {
            return actionType.name();
        }
    }

    public static String resolveStepMessage(StepEnum stepName, Map<String, String> values) {
        String stepMessage = getInstance().getStepMessage(stepName);
        if (stepMessage != null) {
            return AuditLogDirector.resolveMessage(stepMessage, values);
        } else {
            return stepName.name();
        }
    }
}
