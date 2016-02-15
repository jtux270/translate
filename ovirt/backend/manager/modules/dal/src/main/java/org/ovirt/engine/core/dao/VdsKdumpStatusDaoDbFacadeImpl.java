package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.KdumpFlowStatus;
import org.ovirt.engine.core.common.businessentities.VdsKdumpStatus;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.RowMapper;

public class VdsKdumpStatusDaoDbFacadeImpl  extends BaseDAODbFacade implements VdsKdumpStatusDao {
    private static class VdsKdumpStatusMapper implements RowMapper<VdsKdumpStatus> {
        private static final VdsKdumpStatusMapper instance = new VdsKdumpStatusMapper();

        @Override
        public VdsKdumpStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
            VdsKdumpStatus entity = new VdsKdumpStatus();
            entity.setVdsId(
                    Guid.createGuidFromStringDefaultEmpty(rs.getString("vds_id"))
            );
            entity.setStatus(KdumpFlowStatus.forString(rs.getString("status")));
            entity.setAddress(rs.getString("address"));
            return entity;
        }
    }

    @Override
    public void update(VdsKdumpStatus vdsKdumpStatus){
        getCallsHandler().executeModification(
                "UpsertKdumpStatus",
                getCustomMapSqlParameterSource()
                        .addValue("vds_id", vdsKdumpStatus.getVdsId())
                        .addValue("status", vdsKdumpStatus.getStatus().getAsString())
                        .addValue("address", vdsKdumpStatus.getAddress())
        );
    }

    /**
     * Updates kdump status record for specified VDS
     *
     * @param ip
     *            IP address of host to update status for
     * @param vdsKdumpStatus
     *            updated kdump status
     */
    public void updateForIp(String ip, VdsKdumpStatus vdsKdumpStatus){
        getCallsHandler().executeModification(
                "UpsertKdumpStatusForIp",
                getCustomMapSqlParameterSource()
                        .addValue("ip", ip)
                        .addValue("status", vdsKdumpStatus.getStatus().getAsString())
                        .addValue("address", vdsKdumpStatus.getAddress())
        );
    }



    @Override
    public void remove(Guid vdsId) {
        getCallsHandler().executeModification(
                "RemoveFinishedKdumpStatusForVds",
                getCustomMapSqlParameterSource().addValue("vds_id", vdsId)
        );
    }

    @Override
    public VdsKdumpStatus get(Guid vdsId) {
        return getCallsHandler().executeRead(
                "GetKdumpStatusForVds",
                VdsKdumpStatusMapper.instance,
                getCustomMapSqlParameterSource().addValue("vds_id", vdsId)
        );
    }

    @Override
    public List<VdsKdumpStatus> getAllUnfinishedVdsKdumpStatus(){
        return getCallsHandler().executeReadList(
                "GetAllUnfinishedVdsKdumpStatus",
                VdsKdumpStatusMapper.instance,
                getCustomMapSqlParameterSource()
        );
    }
}
