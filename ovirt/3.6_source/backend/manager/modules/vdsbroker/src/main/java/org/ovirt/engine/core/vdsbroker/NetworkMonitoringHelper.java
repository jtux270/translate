package org.ovirt.engine.core.vdsbroker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.network.InterfaceStatus;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.utils.NetworkUtils;

public class NetworkMonitoringHelper {
    /**
     * Determine which of the given NICs is problematic - a problematic NIC is considered to be a NIC which it's state
     * is down and it is the underlying interface of a required network.
     *
     * @param interfaces
     *            The NICs to check.
     * @param clusterNetworks
     *            The cluster's networks.
     * @return A pair of a list of the names of the NICs which are problematic, and a list of the names of the networks
     *         which are required by these NICs.
     */
    public static Map<String, Set<String>> determineProblematicNics(List<VdsNetworkInterface> interfaces,
            List<Network> clusterNetworks) {
        Map<String, Set<String>> brokenNicsToNetworks = new HashMap<>();

        Map<String, Network> networksByName = NetworkUtils.networksByName(clusterNetworks);

        for (VdsNetworkInterface iface : interfaces) {
            if (isRequiredInterfaceDown(networksByName, interfaces, iface)) {
                String baseNicName = NetworkUtils.stripVlan(iface);
                Set<String> networks = brokenNicsToNetworks.get(baseNicName);

                if (networks == null) {
                    networks = new HashSet<String>();
                    brokenNicsToNetworks.put(baseNicName, networks);
                }

                networks.add(iface.getNetworkName());
            }
        }

        return brokenNicsToNetworks;
    }

    /**
     * check if an interface implementing a required cluster network is down
     *
     * @param networksByName
     * @param iface
     */
    private static boolean isRequiredInterfaceDown(Map<String, Network> networksByName,
            List<VdsNetworkInterface> interfaces,
            VdsNetworkInterface iface) {
        if (iface.getNetworkName() != null &&
                isBaseInterfaceDown(iface, interfaces)) {

            Network net = networksByName.get(iface.getNetworkName());
            if (net != null && net.getCluster().getStatus() == NetworkStatus.OPERATIONAL
                    && net.getCluster().isRequired()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBaseInterfaceDown(VdsNetworkInterface iface, List<VdsNetworkInterface> interfaces) {
        if (!NetworkUtils.isVlan(iface)) {
            return iface.getStatistics().getStatus() != InterfaceStatus.UP;
        }

        String baseIfaceName = iface.getBaseInterface();
        for (VdsNetworkInterface tmpIface : interfaces) {
            if (tmpIface.getName().equals(baseIfaceName)) {
                return tmpIface.getStatistics().getStatus() != InterfaceStatus.UP;
            }
        }

        // not suppose to get here
        return false;
    }
}
