package org.ovirt.engine.core.dao.network;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.network.NetworkView;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.BaseDAODbFacade;
import org.ovirt.engine.core.dao.network.NetworkDaoDbFacadeImpl.NetworkRowMapperBase;

public class NetworkViewDaoDbFacadeImpl extends BaseDAODbFacade implements NetworkViewDao {

    @Override
    public List<NetworkView> getAllWithQuery(String query) {
        return jdbcTemplate.query(query, NetworkViewRowMapper.instance);
    }

    @Override
    public List<NetworkView> getAllForProvider(Guid id) {
        return getCallsHandler().executeReadList("GetAllNetworkViewsByNetworkProviderId",
                NetworkViewRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("id", id));
    }

    private static class NetworkViewRowMapper extends NetworkRowMapperBase<NetworkView> {
        public final static NetworkViewRowMapper instance = new NetworkViewRowMapper();

        @Override
        public NetworkView mapRow(ResultSet rs, int rowNum) throws SQLException {
            NetworkView entity = super.mapRow(rs, rowNum);
            entity.setStoragePoolName(rs.getString("storage_pool_name"));
            entity.setCompatibilityVersion(new Version(rs.getString("compatibility_version")));
            entity.setProviderName(rs.getString("provider_name"));
            return entity;
        }

        protected NetworkView createNetworkEntity() {
            return new NetworkView();
        }
    }
}
