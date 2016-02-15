package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.ovirt.engine.core.common.businessentities.VdcOption;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * <code>VdcOptionDaoImpl</code> provides a concrete implementation of {@link VdcOptionDao} using code
 * refactored from {@link org.ovirt.engine.core.dal.dbbroker.DbFacade}.
 */
@Named
@Singleton
public class VdcOptionDaoImpl extends BaseDao implements VdcOptionDao {

    private static final class VdcOptionRowMapper implements RowMapper<VdcOption> {
        public static final VdcOptionRowMapper instance = new VdcOptionRowMapper();

        @Override
        public VdcOption mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            VdcOption entity = new VdcOption();
            entity.setoption_name(rs.getString("option_name"));
            entity.setoption_value(rs.getString("option_value"));
            entity.setoption_id(rs.getInt("option_id"));
            entity.setversion(rs.getString("version"));
            return entity;
        }
    }

    @Override
    public VdcOption get(int id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("option_id", id);

        return getCallsHandler().executeRead("GetVdcOptionById", VdcOptionRowMapper.instance, parameterSource);
    }

    @Override
    public VdcOption getByNameAndVersion(String name, String version) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("option_name", name).addValue("version", version);

        return getCallsHandler().executeRead("GetVdcOptionByName", VdcOptionRowMapper.instance, parameterSource);
    }

    @Override
    public List<VdcOption> getAll() {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource();

        return getCallsHandler().executeReadList("GetAllFromVdcOption", VdcOptionRowMapper.instance, parameterSource);
    }

    @Override
    public void save(VdcOption option) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("option_name", option.getoption_name())
                .addValue("option_value", option.getoption_value())
                .addValue("version", option.getversion())
                .addValue("option_id", option.getoption_id());

        getCallsHandler().executeModification("InsertVdcOption", parameterSource);
    }

    @Override
    public void update(VdcOption option) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("option_name", option.getoption_name())
                .addValue("option_value", option.getoption_value())
                .addValue("option_id", option.getoption_id())
                .addValue("version", option.getversion());

        getCallsHandler().executeModification("UpdateVdcOption", parameterSource);
    }

    @Override
    public void remove(int id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("option_id", id);

        getCallsHandler().executeModification("DeleteVdcOption", parameterSource);
    }
}
