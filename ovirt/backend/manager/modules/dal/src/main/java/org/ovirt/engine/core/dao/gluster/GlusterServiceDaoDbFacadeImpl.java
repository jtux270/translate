package org.ovirt.engine.core.dao.gluster;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterService;
import org.ovirt.engine.core.common.businessentities.gluster.ServiceType;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DefaultReadDaoDbFacade;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class GlusterServiceDaoDbFacadeImpl extends DefaultReadDaoDbFacade<GlusterService, Guid> implements GlusterServiceDao {

    private static final ParameterizedRowMapper<GlusterService> serviceRowMapper = new GlusterServiceRowMapper();

    public GlusterServiceDaoDbFacadeImpl() {
        super("GlusterService");
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("id", id);
    }

    @Override
    protected RowMapper<GlusterService> createEntityRowMapper() {
        return serviceRowMapper;
    }

    @Override
    public List<GlusterService> getByServiceType(ServiceType type) {
        return getCallsHandler().executeReadList("GetGlusterServicesByType",
                serviceRowMapper, getCustomMapSqlParameterSource().addValue("service_type", EnumUtils.nameOrNull(type)));
    }

    @Override
    public GlusterService getByServiceTypeAndName(ServiceType type, String name) {
        return getCallsHandler().executeRead("GetGlusterServiceByTypeAndName",
                serviceRowMapper,
                getCustomMapSqlParameterSource()
                        .addValue("service_type", EnumUtils.nameOrNull(type))
                        .addValue("service_name", name));
    }

    public static class GlusterServiceRowMapper implements ParameterizedRowMapper<GlusterService> {

        @Override
        public GlusterService mapRow(ResultSet rs, int rowNum) throws SQLException {
            GlusterService entity = new GlusterService();
            entity.setId(getGuidDefaultEmpty(rs, "id"));
            entity.setServiceType(ServiceType.valueOf(rs.getString("service_type")));
            entity.setServiceName(rs.getString("service_name"));
            return entity;
        }
    }
}
