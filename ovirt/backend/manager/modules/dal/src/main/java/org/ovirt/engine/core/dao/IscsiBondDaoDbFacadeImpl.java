package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.IscsiBond;
import org.ovirt.engine.core.compat.Guid;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class IscsiBondDaoDbFacadeImpl extends DefaultGenericDaoDbFacade<IscsiBond, Guid> implements IscsiBondDao {

    public IscsiBondDaoDbFacadeImpl() {
        super("IscsiBond");
    }

    @Override
    public List<IscsiBond> getAllByStoragePoolId(Guid storagePoolId) {
        return getCallsHandler().executeReadList("GetIscsiBondsByStoragePoolId",
                IscsiBondRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("storage_pool_id", storagePoolId));
    }

    @Override
    public List<Guid> getNetworkIdsByIscsiBondId(Guid iscsiBondId) {
        return getCallsHandler().executeReadList("GetNetworksByIscsiBondId",
                createGuidMapper(), getCustomMapSqlParameterSource().addValue("iscsi_bond_id", iscsiBondId));
    }

    @Override
    public List<IscsiBond> getIscsiBondsByNetworkId(Guid netowrkId) {
        return getCallsHandler().executeReadList("GetIscsiBondsByNetworkId",
                IscsiBondRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("network_id", netowrkId));
    }

    @Override
    public void addNetworkToIscsiBond(Guid iscsiBondId, Guid networkId) {
        getCallsHandler().executeModification("AddNetworkToIscsiBond",
                getCustomMapSqlParameterSource()
                .addValue("iscsi_bond_id", iscsiBondId)
                .addValue("network_id", networkId));
    }

    @Override
    public void removeNetworkFromIscsiBond(Guid iscsiBondId, Guid networkId) {
        getCallsHandler().executeModification("RemoveNetworkFromIscsiBond",
                getCustomMapSqlParameterSource()
                .addValue("iscsi_bond_id", iscsiBondId)
                .addValue("network_id", networkId));
    }

    @Override
    public List<String> getStorageConnectionIdsByIscsiBondId(Guid iscsiBondId) {
        return getCallsHandler().executeReadList("GetConnectionsByIscsiBondId",
                getStringMapper(), getCustomMapSqlParameterSource().addValue("iscsi_bond_id", iscsiBondId));
    }

    @Override
    public void addStorageConnectionToIscsiBond(Guid iscsiBondId, String connectionId) {
        getCallsHandler().executeModification("AddConnectionToIscsiBond",
                getCustomMapSqlParameterSource()
                        .addValue("iscsi_bond_id", iscsiBondId)
                        .addValue("connection_id", connectionId));
    }

    @Override
    public void removeStorageConnectionFromIscsiBond(Guid iscsiBondId, String connectionId) {
        getCallsHandler().executeModification("RemoveConnectionFromIscsiBond",
                getCustomMapSqlParameterSource()
                        .addValue("iscsi_bond_id", iscsiBondId)
                        .addValue("connection_id", connectionId));
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("id", id);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(IscsiBond iscsiBond) {
        return createIdParameterMapper(iscsiBond.getId())
                .addValue("name", iscsiBond.getName())
                .addValue("description", iscsiBond.getDescription())
                .addValue("storage_pool_id", iscsiBond.getStoragePoolId());
    }

    @Override
    protected RowMapper<IscsiBond> createEntityRowMapper() {
        return IscsiBondRowMapper.instance;
    }

    private static class IscsiBondRowMapper implements RowMapper<IscsiBond> {
        public static final IscsiBondRowMapper instance = new IscsiBondRowMapper();

        @Override
        public IscsiBond mapRow(ResultSet rs, int rowNum) throws SQLException {
            IscsiBond entity = new IscsiBond();

            entity.setId(getGuid(rs, "id"));
            entity.setDescription(rs.getString("description"));
            entity.setName(rs.getString("name"));
            entity.setStoragePoolId(getGuidDefaultNewGuid(rs, "storage_pool_id"));
            return entity;
        }
    }
}
