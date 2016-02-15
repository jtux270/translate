package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.ovirt.engine.core.common.businessentities.VmBlockJob;
import org.ovirt.engine.core.common.businessentities.VmBlockJobType;
import org.ovirt.engine.core.common.businessentities.VmJob;
import org.ovirt.engine.core.common.businessentities.VmJobState;
import org.ovirt.engine.core.common.businessentities.VmJobType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.MapSqlParameterMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class VmJobDaoDbFacadeImpl extends MassOperationsGenericDaoDbFacade<VmJob, Guid> implements VmJobDao {

    public VmJobDaoDbFacadeImpl() {
        super("VmJobs");
    }

    @Override
    public VmJob get(Guid id) {
        throw new NotImplementedException();
    }

    @Override
    public List<VmJob> getAll() {
        throw new NotImplementedException();
    }

    @Override
    public List<Guid> getAllIds() {
        return getCallsHandler().executeReadList("GetAllVmJobIds",
                createGuidMapper(),
                getCustomMapSqlParameterSource());
    }

    @Override
    public List<VmJob> getAllForVm(Guid vm) {
        return getCallsHandler().executeReadList("GetVmJobsByVmId",
                VmJobRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("vm_id", vm));
    }

    @Override
    public List<VmJob> getAllForVmDisk(Guid vm, Guid image) {
        return getCallsHandler().executeReadList("GetVmJobsByVmAndImage",
                VmJobRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("vm_id", vm).addValue("image_group_id", image));
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("vm_job_id", id);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(VmJob entity) {
        MapSqlParameterSource mapper = createIdParameterMapper(entity.getId());
        mapper.addValue("vm_id", entity.getVmId());
        mapper.addValue("job_state", entity.getJobState().getValue());
        mapper.addValue("job_type", entity.getJobType().getValue());

        if (entity.getJobType() == VmJobType.BLOCK) {
            VmBlockJob blockJob = (VmBlockJob) entity;
            mapper.addValue("block_job_type", blockJob.getBlockJobType().getValue());
            mapper.addValue("bandwidth", blockJob.getBandwidth());
            mapper.addValue("cursor_cur", blockJob.getCursorCur());
            mapper.addValue("cursor_end", blockJob.getCursorEnd());
            mapper.addValue("image_group_id", blockJob.getImageGroupId());
        } else {
            mapper.addValue("block_job_type", null);
            mapper.addValue("bandwidth", null);
            mapper.addValue("cursor_cur", null);
            mapper.addValue("cursor_end", null);
            mapper.addValue("image_group_id", null);
        }
        return mapper;
    }

    @Override
    protected RowMapper<VmJob> createEntityRowMapper() {
        return VmJobRowMapper.instance;
    }

    private static class VmJobRowMapper implements RowMapper<VmJob> {

        public static final VmJobRowMapper instance = new VmJobRowMapper();

        private VmJobRowMapper() {
        }

        @Override
        public VmJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            VmJob entity;
            VmJobType jobType = VmJobType.forValue(rs.getInt("job_type"));

            switch (jobType) {
            case BLOCK:
                VmBlockJob blockJob = new VmBlockJob();
                blockJob.setBlockJobType(VmBlockJobType.forValue(rs.getInt("block_job_type")));
                blockJob.setBandwidth(rs.getLong("bandwidth"));
                blockJob.setCursorCur(rs.getLong("cursor_cur"));
                blockJob.setCursorEnd(rs.getLong("cursor_end"));
                blockJob.setImageGroupId(getGuidDefaultEmpty(rs, "image_group_id"));
                entity = blockJob;
                break;
            default:
                entity = new VmJob();
                break;
            }

            entity.setId(getGuidDefaultEmpty(rs, "vm_job_id"));
            entity.setVmId(getGuidDefaultEmpty(rs, "vm_id"));
            entity.setJobState(VmJobState.forValue(rs.getInt("job_state")));
            entity.setJobType(jobType);
            return entity;
        }
    }

    @Override
    public MapSqlParameterMapper<VmJob> getBatchMapper() {
        return new MapSqlParameterMapper<VmJob>() {
            @Override
            public MapSqlParameterSource map(VmJob entity) {
                return createFullParametersMapper(entity);
            }
        };
    }
}
