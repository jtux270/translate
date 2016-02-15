package org.ovirt.engine.core.dao.network;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.NetworkClusterId;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DAO;

public interface NetworkClusterDao extends DAO {

    /**
     * Retrieves the entity with the given id.
     *
     * @param id
     *            The id to look by (can't be <code>null</code>).
     * @return The entity instance, or <code>null</code> if not found.
     */
    public NetworkCluster get(NetworkClusterId id);

    /**
     * Retrieves all network clusters.
     *
     * @return the list of network clusters
     */
    List<NetworkCluster> getAll();

    /**
     * Retrieves all network clusters for the specified cluster.
     *
     * @param the
     *            network cluster
     * @return the list of clusters
     */
    List<NetworkCluster> getAllForCluster(Guid cluster);

    /**
     * Retrieves all network clusters for the specified network.
     *
     * @param network
     *            the network
     * @return
     */
    List<NetworkCluster> getAllForNetwork(Guid network);

    /**
     * Saves the new network cluster.
     *
     * @param cluster
     *            the network cluster
     */
    void save(NetworkCluster cluster);

    /**
     * Updates the network cluster.
     *
     * @param cluster
     *            the network cluster
     */
    void update(NetworkCluster cluster);

    /**
     * Updates the network cluster status.
     *
     * @param cluster
     *            the network cluster
     */
    void updateStatus(NetworkCluster cluster);
    /**
     * Removes the specified network from the specified cluster.
     *
     * @param clusterid
     *            the cluster
     * @param networkid
     *            the network
     */
    void remove(Guid clusterid, Guid networkid);

    /**
     * Sets this cluster network as the only display network on the cluster.
     *
     * @param clusterId
     * @param networkId
     */
    void setNetworkExclusivelyAsDisplay(Guid clusterId, Guid networkId);

    /**
     * Sets this cluster network as the only migration network on the cluster.
     *
     * @param clusterId
     * @param networkId
     */
    void setNetworkExclusivelyAsMigration(Guid clusterId, Guid networkId);
}
