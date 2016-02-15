package org.ovirt.engine.core.utils.timer;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.utils.ejb.BeanProxyType;
import org.ovirt.engine.core.utils.ejb.BeanType;
import org.ovirt.engine.core.utils.ejb.EjbUtils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

// Here we use a Singleton bean, names Scheduler.
// The @Startup annotation is to make sure the bean is initialized on startup.
// @ConcurrencyManagement - we use bean managed concurrency:
// Singletons that use bean-managed concurrency allow full concurrent access to all the
// business and timeout methods in the singleton.
// The developer of the singleton is responsible for ensuring that the state of the singleton is synchronized across all clients.
@Singleton(name = "Scheduler")
@DependsOn("LockManager")
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SchedulerUtilQuartzImpl implements SchedulerUtil {

    // consts
    public static final String RUNNABLE_INSTANCE = "runnable.instance";
    public static final String RUN_METHOD_NAME = "method.name";
    public static final String RUN_METHOD_PARAM_TYPE = "method.paramType";
    public static final String RUN_METHOD_PARAM = "method.param";
    public static final String FIXED_DELAY_VALUE = "fixedDelayValue";
    public static final String FIXED_DELAY_TIME_UNIT = "fixedDelayTimeUnit";
    public static final String CONFIGURABLE_DELAY_KEY_NAME = "configDelayKeyName";
    private static final String TRIGGER_PREFIX = "trigger";

    // members
    private final Log log = LogFactory.getLog(SchedulerUtilQuartzImpl.class);
    private Scheduler sched;

    private final AtomicLong sequenceNumber = new AtomicLong(Long.MIN_VALUE);

    /**
     * This method is called upon the bean creation as part
     * of the management Service bean lifecycle.
     */
    @PostConstruct
    public void create(){
        setup();
    }

    /*
     * retrieving the quartz scheduler from the factory.
     */
    public void setup() {
        try {
            SchedulerFactory sf = new StdSchedulerFactory();
            sched = sf.getScheduler();
            sched.start();
            sched.getListenerManager().addJobListener(new FixedDelayJobListener(this), jobGroupEquals(Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException se) {
            log.error("there is a problem with the underlying Scheduler.", se);
        }
    }

    @PreDestroy
    public void teardown() {
        try {
            if (sched != null) {
                sched.shutdown();
            }
        } catch (SchedulerException e) {
            log.error("Failed to shutdown Quartz service", e);
        }
    }

    /**
     * Returns the single instance of this Class.
     *
     * @return a SchedulerUtil instance
     */
    public static SchedulerUtil getInstance() {
        return EjbUtils.findBean(BeanType.SCHEDULER, BeanProxyType.LOCAL);
    }

    /**
     * schedules a fixed delay job.
     *
     * @param instance
     *            - the instance to activate the method on timeout
     * @param methodName
     *            - the name of the method to activate on the instance
     * @param inputTypes
     *            - the method input types
     * @param inputParams
     *            - the method input parameters
     * @param initialDelay
     *            - the initial delay before the first activation
     * @param taskDelay
     *            - the delay between jobs
     * @param timeUnit
     *            - the unit of time used for initialDelay and taskDelay.
     * @return the scheduled job id
     */
    @Override
    public String scheduleAFixedDelayJob(Object instance,
            String methodName,
            Class<?>[] inputTypes,
            Object[] inputParams,
            long initialDelay,
            long taskDelay,
            TimeUnit timeUnit) {
        JobDetail job = createJobForDelayJob(instance, methodName, inputTypes, inputParams, taskDelay, timeUnit);
        scheduleJobWithTrigger(initialDelay, timeUnit, instance, job);
        return job.getKey().getName();
    }

    private void scheduleJobWithTrigger(long initialDelay, TimeUnit timeUnit, Object instance, JobDetail job) {
        Trigger trigger = createSimpleTrigger(initialDelay, timeUnit, instance);
        try {
            sched.scheduleJob(job, trigger);
        } catch (SchedulerException se) {
            log.error("failed to schedule job", se);
        }
    }

    private JobDetail createJobForDelayJob(Object instance,
            String methodName,
            Class<?>[] inputTypes,
            Object[] inputParams, long taskDelay, TimeUnit timeUnit) {
        JobDetail job = createJobWithBasicMapValues(instance, methodName, inputTypes, inputParams);
        JobDataMap data = job.getJobDataMap();
        setupDataMapForDelayJob(data, taskDelay, timeUnit);
        return job;
    }

    /**
     * schedules a job with a configurable delay.
     *
     * @param instance
     *            - the instance to activate the method on timeout
     * @param methodName
     *            - the name of the method to activate on the instance
     * @param inputTypes
     *            - the method input types
     * @param inputParams
     *            - the method input parameters
     * @param initialDelay
     *            - the initial delay before the first activation
     * @param taskDelay
     *            - the name of the config value that sets the delay between jobs
     * @param timeUnit
     *            - the unit of time used for initialDelay and taskDelay.
     * @return the scheduled job id
     */
    @Override
    public String scheduleAConfigurableDelayJob(Object instance,
            String methodName,
            Class<?>[] inputTypes,
            Object[] inputParams,
            long initialDelay,
            String configurableDelayKeyName,
            TimeUnit timeUnit) {
        long configurableDelay = getConfigurableDelay(configurableDelayKeyName);
        JobDetail job = createJobForDelayJob(instance, methodName, inputTypes, inputParams, configurableDelay, timeUnit);
        JobDataMap data = job.getJobDataMap();
        data.put(CONFIGURABLE_DELAY_KEY_NAME, configurableDelayKeyName);
        scheduleJobWithTrigger(initialDelay, timeUnit, instance, job);
        return job.getKey().getName();
    }

    /**
     * get the configurable delay value from the DB according to given key
     * @param configurableDelayKeyName
     * @return
     */
    private long getConfigurableDelay(String configurableDelayKeyName) {
        ConfigValues configDelay = ConfigValues.valueOf(configurableDelayKeyName);
        return Config.<Integer> getValue(configDelay).longValue();
    }

    /**
     * setup the values in the data map that are relevant for jobs with delay
     */
    private void setupDataMapForDelayJob(JobDataMap data,
            long taskDelay,
            TimeUnit timeUnit) {
        data.put(FIXED_DELAY_TIME_UNIT, timeUnit);
        data.put(FIXED_DELAY_VALUE, taskDelay);
    }

    private JobDetail createJobWithBasicMapValues(Object instance,
            String methodName,
            Class<?>[] inputTypes,
            Object[] inputParams) {
        String jobName = generateUniqueNameForInstance(instance, methodName);
        JobDetail job = newJob()
            .withIdentity(jobName, Scheduler.DEFAULT_GROUP)
            .ofType(JobWrapper.class)
            .build();
        setBasicMapValues(job.getJobDataMap(), instance, methodName, inputTypes, inputParams);
        return job;
    }

    private Trigger createSimpleTrigger(long initialDelay, TimeUnit timeUnit, Object instance) {
        Date runTime = getFutureDate(initialDelay, timeUnit);
        String triggerName = generateUniqueNameForInstance(instance, TRIGGER_PREFIX);
        Trigger trigger = newTrigger()
            .withIdentity(triggerName, Scheduler.DEFAULT_GROUP)
            .startAt(runTime)
            .build();
        return trigger;
    }

    /**
     * schedules a one time job.
     *
     * @param instance
     *            - the instance to activate the method on timeout
     * @param methodName
     *            - the name of the method to activate on the instance
     * @param inputTypes
     *            - the method input types
     * @param inputParams
     *            - the method input parameters
     * @param initialDelay
     *            - the initial delay before the job activation
     * @param timeUnit
     *            - the unit of time used for initialDelay and taskDelay.
     * @return the scheduled job id
     */
    @Override
    public String scheduleAOneTimeJob(Object instance,
            String methodName,
            Class<?>[] inputTypes,
            Object[] inputParams,
            long initialDelay,
            TimeUnit timeUnit) {
        JobDetail job = createJobWithBasicMapValues(instance, methodName, inputTypes, inputParams);
        scheduleJobWithTrigger(initialDelay, timeUnit, instance, job);
        return job.getKey().getName();
    }

    /**
     * schedules a cron job.
     *
     * @param instance
     *            - the instance to activate the method on timeout
     * @param methodName
     *            - the name of the method to activate on the instance
     * @param inputTypes
     *            - the method input types
     * @param inputParams
     *            - the method input parameters
     * @param cronExpression
     *            - cron expression to run this job
     * @return the scheduled job id
     */
    @Override
    public String scheduleACronJob(Object instance,
            String methodName,
            Class<?>[] inputTypes,
            Object[] inputParams,
            String cronExpression) {
        JobDetail job = createJobWithBasicMapValues(instance, methodName, inputTypes, inputParams);
        try {
            String triggerName = generateUniqueNameForInstance(instance, TRIGGER_PREFIX);
            Trigger trigger = newTrigger()
                .withIdentity(triggerName, Scheduler.DEFAULT_GROUP)
                .withSchedule(cronSchedule(cronExpression))
                .build();
            sched.scheduleJob(job, trigger);
        } catch (Exception se) {
            log.error("failed to schedule job", se);
        }

        return job.getKey().getName();
    }

    private void setBasicMapValues(JobDataMap data,
            Object instance,
            String methodName,
            Class<?>[] inputTypes,
            Object[] inputParams) {
        data.put(RUNNABLE_INSTANCE, instance);
        data.put(RUN_METHOD_NAME, methodName);
        data.put(RUN_METHOD_PARAM, inputParams);
        data.put(RUN_METHOD_PARAM_TYPE, inputTypes);
    }

    /**
     * reschedule the job associated with the given old trigger with the new
     * trigger.
     *
     * @param oldTriggerName
     *            - the name of the trigger to remove.
     * @param oldTriggerGroup
     *            - the group of the trigger to remove.
     * @param newTrigger
     *            - the new Trigger to associate the job with
     */
    public void rescheduleAJob(String oldTriggerName, String oldTriggerGroup, Trigger newTrigger) {
        try {
            sched.rescheduleJob(triggerKey(oldTriggerName, oldTriggerGroup), newTrigger);
        } catch (SchedulerException se) {
            log.error("failed to reschedule the job", se);
        }
    }

    /**
     * pauses a job with the given jobId assuming the job is in the default
     * quartz group
     *
     * @param jobId
     */
    @Override
    public void pauseJob(String jobId) {
        try {
            sched.pauseJob(jobKey(jobId, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException se) {
            log.error("failed to pause a job with id=" + jobId, se);
        }

    }

    /**
     * Delete the identified Job from the Scheduler
     *
     * @param jobId
     *            - the id of the job to delete
     */
    @Override
    public void deleteJob(String jobId) {
        try {
            sched.deleteJob(jobKey(jobId, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException se) {
            log.error("failed to delete a job with id=" + jobId, se);
        }

    }

    /**
     * resume a job with the given jobId assuming the job is in the default
     * quartz group
     *
     * @param jobId
     */
    @Override
    public void resumeJob(String jobId) {
        try {
            sched.resumeJob(jobKey(jobId, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException se) {
            log.error("failed to pause a job with id=" + jobId, se);
        }

    }

    @Override
    public void triggerJob(String jobId) {
        try {
            List<? extends Trigger> existingTriggers = sched.getTriggersOfJob(jobKey(jobId, Scheduler.DEFAULT_GROUP));

            if (!existingTriggers.isEmpty()) {
                // Note: we assume that every job has exactly one trigger
                Trigger oldTrigger = existingTriggers.get(0);
                TriggerKey oldTriggerKey = oldTrigger.getKey();
                Trigger newTrigger = newTrigger()
                    .withIdentity(oldTriggerKey)
                    .startAt(getFutureDate(0, TimeUnit.MILLISECONDS))
                    .build();

                rescheduleAJob(oldTriggerKey.getName(), oldTriggerKey.getGroup(), newTrigger);
            } else {
                log.error("failed to trigger a job with id=" + jobId + ", job has no trigger");
            }
        } catch (SchedulerException se) {
            log.error("failed to trigger a job with id=" + jobId, se);
        }
    }

    /**
     * Halts the Scheduler, and cleans up all resources associated with the
     * Scheduler. The scheduler cannot be re-started.
     *
     * @see org.quartz.Scheduler#shutdown(boolean waitForJobsToComplete)
     */
    @Override
    public void shutDown() {
        try {
            sched.shutdown(true);
        } catch (SchedulerException se) {
            log.error("failed to shut down the scheduler", se);
        }
    }

    /**
     * @return the quartz scheduler wrapped by this SchedulerUtil
     */
    public Scheduler getRawScheduler() {
        return sched;
    }

    /*
     * returns a future date with the given delay. the delay is being calculated
     * according to the given Time units
     */
    public static Date getFutureDate(long delay, TimeUnit timeUnit) {
        if (delay > 0) {
            return new Date(new Date().getTime() + TimeUnit.MILLISECONDS.convert(delay, timeUnit));
        } else {
            return new Date();
        }
    }

    /*
     * generate a unique name for the given instance, using a sequence number.
     */
    private String generateUniqueNameForInstance(Object instance, String nestedName) {
        String name = instance.getClass().getName() + "." + nestedName + "#" + sequenceNumber.incrementAndGet();
        return name;
    }

}
