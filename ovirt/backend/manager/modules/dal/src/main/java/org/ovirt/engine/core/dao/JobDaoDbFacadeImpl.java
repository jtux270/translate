package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.job.Job;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class JobDaoDbFacadeImpl extends DefaultGenericDaoDbFacade<Job, Guid> implements JobDao {

    private static JobRowMapper jobRowMapper = new JobRowMapper();

    public JobDaoDbFacadeImpl() {
        super("Job");
        setProcedureNameForGetAll("GetAllJobs");
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("job_id", id);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(Job entity) {
        return createIdParameterMapper(entity.getId())
                .addValue("action_type", EnumUtils.nameOrNull(entity.getActionType()))
                .addValue("description", entity.getDescription())
                .addValue("status", EnumUtils.nameOrNull(entity.getStatus()))
                .addValue("owner_id", entity.getOwnerId())
                .addValue("visible", entity.isVisible())
                .addValue("start_time", entity.getStartTime())
                .addValue("end_time", entity.getEndTime())
                .addValue("last_update_time", entity.getLastUpdateTime())
                .addValue("correlation_id", entity.getCorrelationId())
                .addValue("is_external", entity.isExternal())
                .addValue("is_auto_cleared", entity.isAutoCleared());
    }

    @Override
    protected RowMapper<Job> createEntityRowMapper() {
        return jobRowMapper;
    }

    @Override
    public boolean exists(Guid id) {
        return get(id) != null;
    }

    @Override
    public List<Job> getJobsByOffsetAndPageSize(int offset, int pageSize) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("position", offset)
                .addValue("page_size", pageSize);

        return getCallsHandler().executeReadList("GetJobsByOffsetAndPageSize", createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<Job> getJobsByCorrelationId(String correlationId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("correlation_id", correlationId);

        return getCallsHandler().executeReadList("GetJobsByCorrelationId", createEntityRowMapper(), parameterSource);
    }

    @Override
    public void updateJobLastUpdateTime(Guid jobId, Date lastUpdateTime) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("job_id", jobId)
                .addValue("last_update_time", lastUpdateTime);
        getCallsHandler().executeModification("UpdateJobLastUpdateTime", parameterSource);
    }

    @Override
    public void deleteJobOlderThanDateWithStatus(Date sinceDate, List<JobExecutionStatus> statusesList) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("end_time", sinceDate)
                .addValue("status", StringUtils.join(statusesList, ","));
        getCallsHandler().executeModification("DeleteJobOlderThanDateWithStatus", parameterSource);
    }

    @Override
    public void updateStartedExecutionEntitiesToUnknown(Date updateTime) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource().addValue("end_time", updateTime);
        getCallsHandler().executeModification("UpdateStartedExecutionEntitiesToUnknown", parameterSource);
    }

    @Override
    public void deleteRunningJobsOfTasklessCommands() {
        getCallsHandler().executeModification("DeleteRunningJobsOfTasklessCommands",
                getCustomMapSqlParameterSource());
    }

    @Override
    public void deleteCompletedJobs(Date succeededJobs, Date failedJobs) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("succeeded_end_time", succeededJobs)
                .addValue("failed_end_time", failedJobs);
        getCallsHandler().executeModification("DeleteCompletedJobsOlderThanDate", parameterSource);

    }

    @Override
    public boolean checkIfJobHasTasks(Guid jobId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource().addValue("job_id", jobId);
        return getCallsHandler().executeRead("CheckIfJobHasTasks", createBooleanMapper(), parameterSource);
    }

    private static class JobRowMapper implements RowMapper<Job> {

        @Override
        public Job mapRow(ResultSet rs, int rowNum) throws SQLException {
            Job job = new Job();

            job.setId(getGuidDefaultEmpty(rs, "job_id"));
            job.setActionType(VdcActionType.valueOf(rs.getString("action_type")));
            job.setDescription(rs.getString("description"));
            job.setStatus(JobExecutionStatus.valueOf(rs.getString("status")));
            job.setOwnerId(getGuid(rs, "owner_id"));
            job.setVisible(rs.getBoolean("visible"));
            job.setStartTime(DbFacadeUtils.fromDate(rs.getTimestamp("start_time")));
            job.setEndTime(DbFacadeUtils.fromDate(rs.getTimestamp("end_time")));
            job.setLastUpdateTime(DbFacadeUtils.fromDate(rs.getTimestamp("last_update_time")));
            job.setCorrelationId(rs.getString("correlation_id"));
            job.setExternal(rs.getBoolean("is_external"));
            job.setAutoCleared(rs.getBoolean("is_auto_cleared"));
            return job;
        }
    }

}
