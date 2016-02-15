package org.ovirt.engine.core.bll.dwh;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.ovirt.engine.core.common.businessentities.DwhHistoryTimekeeping;
import org.ovirt.engine.core.common.businessentities.DwhHistoryTimekeepingVariable;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.timer.SchedulerUtilQuartzImpl;

/**
 * Job notifies DWH, that engine is up and running
 */
public class DwhHeartBeat {
    /**
     * Name of method to execute periodically
     */
    private static final String DWH_HEART_BEAT_METHOD = "engineIsRunningNotification";

    /**
     * Logger instance
     */
    private static final Log log = LogFactory.getLog(DwhHeartBeat.class);

    /**
     * Instance of heartBeat variable
     */
    private DwhHistoryTimekeeping heartBeatVar;

    /**
     * Update {@code dwh_history_timekeeping} table to notify DWH, that engine is up an running
     */
    @OnTimerMethodAnnotation(DWH_HEART_BEAT_METHOD)
    public void engineIsRunningNotification() {
        try {
            heartBeatVar.setDateTime(new Date());
            DbFacade.getInstance().getDwhHistoryTimekeepingDao().save(heartBeatVar);
        } catch (Exception ex) {
            log.error("Error updating DWH Heart Beat: ", ex);
        }
    }

    /**
     * Starts up DWH Heart Beat as a periodic job
     */
    public void init() {
        log.info("Initializing DWH Heart Beat");
        heartBeatVar = new DwhHistoryTimekeeping();
        heartBeatVar.setVariable(DwhHistoryTimekeepingVariable.HEART_BEAT);

        SchedulerUtilQuartzImpl.getInstance().scheduleAFixedDelayJob(this,
                DWH_HEART_BEAT_METHOD,
                new Class[] {},
                new Object[] {},
                0,
                Config.<Integer> getValue(ConfigValues.DwhHeartBeatInterval),
                TimeUnit.SECONDS);
        log.info("DWH Heart Beat initialized");
    }
}
