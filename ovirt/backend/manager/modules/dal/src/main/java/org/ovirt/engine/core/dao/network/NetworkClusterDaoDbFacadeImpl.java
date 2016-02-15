package org.ovirt.engine.core.dao.network;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.NetworkClusterId;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAODbFacade;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class NetworkClusterDaoDbFacadeImpl extends BaseDAODbFacade implements NetworkClusterDao {

    private static final RowMapper<NetworkCluster> mapper =
            new RowMapper<NetworkCluster>() {
                @Override
                public NetworkCluster mapRow(ResultSet rs, int rowNum)
                        throws SQLException {
                    NetworkCluster entity = new NetworkCluster();
                    entity.setClusterId(getGuidDefaultEmpty(rs, "cluster_id"));
                    entity.setNetworkId(getGuidDefaultEmpty(rs, "network_id"));
                    entity.setStatus(NetworkStatus.forValue(rs.getInt("status")));
                    entity.setDisplay(rs.getBoolean("is_display"));
                    entity.setRequired(rs.getBoolean("required"));
                    entity.setMigration(rs.getBoolean("migration"));
                    return entity;
                }
            };

    @Override
    public NetworkCluster get(NetworkClusterId id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", id.getClusterId())
                .addValue("network_id", id.getNetworkId());

        return getCallsHandler().executeRead("Getnetwork_clusterBycluster_idAndBynetwork_id", mapper, parameterSource);
    }

    @Override
    public List<NetworkCluster> getAll() {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource();

        return getCallsHandler().executeReadList("GetAllFromnetwork_cluster", mapper, parameterSource);
    }

    @Override
    public List<NetworkCluster> getAllForCluster(Guid clusterid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", clusterid);

        return getCallsHandler().executeReadList("GetAllFromnetwork_clusterByClusterId", mapper,
                parameterSource);
    }

    @Override
    public List<NetworkCluster> getAllForNetwork(Guid network) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("network_id", network);

        return getCallsHandler().executeReadList("GetAllFromnetwork_clusterByNetworkId", mapper,
                parameterSource);
    }

    @Override
    public void save(NetworkCluster cluster) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", cluster.getClusterId())
                .addValue("network_id", cluster.getNetworkId())
                .addValue("status", cluster.getStatus())
                .addValue("is_display", cluster.isDisplay())
                .addValue("required", cluster.isRequired())
                .addValue("migration", cluster.isMigration());

        getCallsHandler().executeModification("Insertnetwork_cluster", parameterSource);
    }

    @Override
    public void update(NetworkCluster cluster) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", cluster.getClusterId())
                .addValue("network_id", cluster.getNetworkId())
                .addValue("status", cluster.getStatus())
                .addValue("is_display", cluster.isDisplay())
                .addValue("required", cluster.isRequired())
                .addValue("migration", cluster.isMigration());

        getCallsHandler().executeModification("Updatenetwork_cluster", parameterSource);
    }

    @Override
    public void updateStatus(NetworkCluster cluster) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", cluster.getClusterId())
                .addValue("network_id", cluster.getNetworkId())
                .addValue("status", cluster.getStatus());

        getCallsHandler().executeModification("Updatenetwork_cluster_status", parameterSource);
    }

    @Override
    public void remove(Guid clusterid, Guid networkid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", clusterid).addValue("network_id",
                        networkid);

        getCallsHandler().executeModification("Deletenetwork_cluster", parameterSource);
    }

    @Override
    public void setNetworkExclusivelyAsDisplay(Guid clusterId, Guid networkId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", clusterId).addValue("network_id", networkId);

        getCallsHandler().executeModification("set_network_exclusively_as_display", parameterSource);
    }

    @Override
    public void setNetworkExclusivelyAsMigration(Guid clusterId, Guid networkId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("cluster_id", clusterId).addValue("network_id", networkId);

        getCallsHandler().executeModification("set_network_exclusively_as_migration", parameterSource);
    }
}
