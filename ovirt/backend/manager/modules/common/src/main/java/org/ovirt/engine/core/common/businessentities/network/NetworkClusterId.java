package org.ovirt.engine.core.common.businessentities.network;

import java.io.Serializable;

import org.ovirt.engine.core.compat.Guid;

public class NetworkClusterId implements Serializable {

    private static final long serialVersionUID = 4662794069699019632L;

    public Guid clusterId;

    public Guid networkId;

    public NetworkClusterId() {
    }

    public NetworkClusterId(Guid clusterId, Guid networkId) {
        this.clusterId = clusterId;
        this.networkId = networkId;
    }

    public Guid getClusterId() {
        return clusterId;
    }

    public void setClusterId(Guid clusterId) {
        this.clusterId = clusterId;
    }

    public Guid getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Guid networkId) {
        this.networkId = networkId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
        result = prime * result + ((networkId == null) ? 0 : networkId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NetworkClusterId)) {
            return false;
        }
        NetworkClusterId other = (NetworkClusterId) obj;
        if (clusterId == null) {
            if (other.clusterId != null) {
                return false;
            }
        } else if (!clusterId.equals(other.clusterId)) {
            return false;
        }
        if (networkId == null) {
            if (other.networkId != null) {
                return false;
            }
        } else if (!networkId.equals(other.networkId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{clusterId=")
                .append(getClusterId())
                .append(", networkId=")
                .append(getNetworkId())
                .append("}");
        return builder.toString();
    }
}
