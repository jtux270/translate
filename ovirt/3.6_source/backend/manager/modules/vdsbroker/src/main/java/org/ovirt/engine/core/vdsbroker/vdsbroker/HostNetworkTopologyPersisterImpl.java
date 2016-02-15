package org.ovirt.engine.core.vdsbroker.vdsbroker;

import static org.ovirt.engine.core.common.businessentities.network.NetworkStatus.OPERATIONAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.ovirt.engine.core.bll.network.cluster.ManagementNetworkUtil;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface.NetworkImplementationDetails;
import org.ovirt.engine.core.common.vdscommands.UserConfiguredNetworkData;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VmDynamicDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkAttachmentDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.vdsbroker.NetworkImplementationDetailsUtils;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.ovirt.engine.core.vdsbroker.vdsbroker.predicates.DisplayInterfaceEqualityPredicate;
import org.ovirt.engine.core.vdsbroker.vdsbroker.predicates.IsNetworkOnInterfacePredicate;

@Singleton
final class HostNetworkTopologyPersisterImpl implements HostNetworkTopologyPersister {

    private final VmDynamicDao vmDynamicDao;
    private final InterfaceDao interfaceDao;
    private final NetworkDao networkDao;
    private final ResourceManager resourceManager;
    private final ManagementNetworkUtil managementNetworkUtil;
    private final AuditLogDirector auditLogDirector = new AuditLogDirector();
    private final NetworkAttachmentDao networkAttachmentDao;
    private final NetworkImplementationDetailsUtils networkImplementationDetailsUtils;

    @Inject
    HostNetworkTopologyPersisterImpl(VmDynamicDao vmDynamicDao,
                                     InterfaceDao interfaceDao,
                                     NetworkAttachmentDao networkAttachmentDao,
                                     NetworkDao networkDao,
                                     ResourceManager resourceManager,
                                     NetworkImplementationDetailsUtils networkImplementationDetailsUtils,
                                     ManagementNetworkUtil managementNetworkUtil) {
        Validate.notNull(networkDao, "networkAttachmentDao can not be null");
        Validate.notNull(networkDao, "networkDao can not be null");
        Validate.notNull(interfaceDao, "interfaceDao can not be null");
        Validate.notNull(vmDynamicDao, "vmDynamicDao can not be null");
        Validate.notNull(resourceManager, "resourceManager can not be null");
        Validate.notNull(networkImplementationDetailsUtils, "networkImplementationDetailsUtils can not be null");
        Validate.notNull(managementNetworkUtil, "managementNetworkUtil can not be null");

        this.vmDynamicDao = vmDynamicDao;
        this.interfaceDao = interfaceDao;
        this.networkDao = networkDao;
        this.resourceManager = resourceManager;
        this.managementNetworkUtil = managementNetworkUtil;
        this.networkAttachmentDao = networkAttachmentDao;
        this.networkImplementationDetailsUtils = networkImplementationDetailsUtils;
    }

    @Override
    public NonOperationalReason persistAndEnforceNetworkCompliance(VDS host,
                                                                   boolean skipManagementNetwork,
                                                                   UserConfiguredNetworkData userConfiguredData) {

        List<VdsNetworkInterface> dbIfaces = interfaceDao.getAllInterfacesForVds(host.getId());
        List<Network> clusterNetworks = networkDao.getAllForCluster(host.getVdsGroupId());

        persistTopology(host, dbIfaces, clusterNetworks, userConfiguredData);
        NonOperationalReason nonOperationalReason =
                enforceNetworkCompliance(host, skipManagementNetwork, clusterNetworks);
        auditNetworkCompliance(host, dbIfaces, clusterNetworks);
        return nonOperationalReason;
    }

    private NonOperationalReason enforceNetworkCompliance(VDS host,
                                                          boolean skipManagementNetwork,
                                                          List<Network> clusterNetworks) {
        if (host.getStatus() != VDSStatus.Maintenance) {
            if (skipManagementNetwork) {
                skipManagementNetworkCheck(host.getInterfaces(), clusterNetworks, host.getVdsGroupId());
            }

            Map<String, String> customLogValues;

            // here we check if the host networks match it's cluster networks
            String networks = getMissingOperationalClusterNetworks(host, clusterNetworks);
            if (networks.length() > 0) {
                customLogValues = new HashMap<>();
                customLogValues.put("Networks", networks);

                setNonOperational(host, NonOperationalReason.NETWORK_UNREACHABLE, customLogValues);
                return NonOperationalReason.NETWORK_UNREACHABLE;
            }

            // Check that VM networks are implemented above a bridge.
            networks = getVmNetworksImplementedAsBridgeless(host, clusterNetworks);
            if (networks.length() > 0) {
                customLogValues = new HashMap<>();
                customLogValues.put("Networks", networks);

                setNonOperational(host, NonOperationalReason.VM_NETWORK_IS_BRIDGELESS, customLogValues);
                return NonOperationalReason.VM_NETWORK_IS_BRIDGELESS;
            }
        }

        return NonOperationalReason.NONE;
    }

    private void auditNetworkCompliance(VDS host,
            List<VdsNetworkInterface> dbIfaces,
            List<Network> clusterNetworks) {
        if (host.getStatus() == VDSStatus.Maintenance) {
            return;
        }

        final Map<String, Network> clusterNetworksByName = Entities.entitiesByName(clusterNetworks);
        final Collection<Network> dbHostNetworks = findNetworksOnInterfaces(dbIfaces, clusterNetworksByName);
        logChangedDisplayNetwork(host, dbHostNetworks, dbIfaces);
        logUnsynchronizedNetworks(host, clusterNetworksByName);
    }

    @Override
    public NonOperationalReason persistAndEnforceNetworkCompliance(VDS host) {
        return persistAndEnforceNetworkCompliance(host, false, new UserConfiguredNetworkData());
    }

    private void skipManagementNetworkCheck(List<VdsNetworkInterface> ifaces, List<Network> clusterNetworks, Guid clusterId) {
        final Network managementNetwork = managementNetworkUtil.getManagementNetwork(clusterId);
        final String managementNetworkName = managementNetwork.getName();
        for (VdsNetworkInterface iface : ifaces) {
            if (managementNetworkName.equals(iface.getNetworkName())) {
                return;
            }
        }

        for (Iterator<Network> iterator = clusterNetworks.iterator(); iterator.hasNext();) {
            Network network = iterator.next();
            if (managementNetworkName.equals(network.getName())) {
                iterator.remove();
                break;
            }
        }
    }

    private void logChangedDisplayNetwork(VDS host,
                                          Collection<Network> engineHostNetworks,
                                          Collection<VdsNetworkInterface> engineInterfaces) {

        if (isVmRunningOnHost(host.getId())) {
            final Network engineDisplayNetwork = findDisplayNetwork(host.getVdsGroupId(), engineHostNetworks);

            if (engineDisplayNetwork == null) {
                return;
            }

            final IsNetworkOnInterfacePredicate isNetworkOnInterfacePredicate =
                    new IsNetworkOnInterfacePredicate(engineDisplayNetwork.getName());
            final VdsNetworkInterface vdsmDisplayInterface = LinqUtils.firstOrNull(
                    host.getInterfaces(),
                    isNetworkOnInterfacePredicate);
            final VdsNetworkInterface engineDisplayInterface = LinqUtils.firstOrNull(
                    engineInterfaces,
                    isNetworkOnInterfacePredicate);
            final DisplayInterfaceEqualityPredicate displayIneterfaceEqualityPredicate =
                    new DisplayInterfaceEqualityPredicate(engineDisplayInterface);
            if (vdsmDisplayInterface == null // the display interface is't on host anymore
                || !displayIneterfaceEqualityPredicate.eval(vdsmDisplayInterface)) {
                final AuditLogableBase loggable = new AuditLogableBase(host.getId());
                auditLogDirector.log(loggable, AuditLogType.NETWORK_UPDATE_DISPLAY_FOR_HOST_WITH_ACTIVE_VM);
            }
        }
    }

    private boolean isVmRunningOnHost(Guid hostId) {
        return !vmDynamicDao.getAllRunningForVds(hostId).isEmpty();
    }

    private Collection<Network> findNetworksOnInterfaces(Collection<VdsNetworkInterface> ifaces,
                                                         Map<String, Network> clusterNetworksByName) {
        final Collection<Network> networks = new ArrayList<>();
        for (VdsNetworkInterface iface : ifaces) {
            final String interfaceNetworkName = iface.getNetworkName();
            if (clusterNetworksByName.containsKey(interfaceNetworkName)) {
                final Network network = clusterNetworksByName.get(interfaceNetworkName);
                networks.add(network);
            }
        }
        return networks;
    }

    private Network findDisplayNetwork(Guid clusterId, Collection<Network> networks) {
        Network managementNetwork = null;
        for (Network network : networks) {
            if (network.getCluster().isDisplay()) {
                return network;
            }
            if (managementNetworkUtil.isManagementNetwork(network.getId(), clusterId)) {
                managementNetwork = network;
            }
        }

        return managementNetwork;
    }

    private void logUnsynchronizedNetworks(VDS host, Map<String, Network> networks) {
        List<String> networkNames = new ArrayList<>();

        for (VdsNetworkInterface iface : host.getInterfaces()) {
            Network network = networks.get(iface.getNetworkName());

            NetworkImplementationDetails networkImplementationDetails =
                networkImplementationDetailsUtils.calculateNetworkImplementationDetails(iface, network);

            if (networkImplementationDetails != null
                && !networkImplementationDetails.isInSync()
                && networkImplementationDetails.isManaged()) {
                networkNames.add(iface.getNetworkName());
            }
        }

        if (!networkNames.isEmpty()) {
            AuditLogableBase logable = new AuditLogableBase(host.getId());
            logable.addCustomValue("Networks", StringUtils.join(networkNames, ","));
            auditLogDirector.log(logable, AuditLogType.VDS_NETWORKS_OUT_OF_SYNC);
        }
    }

    /**
     * Persists the network topology as reported by vdsm, with respect to the following:
     * <ul>
     * <li>Pre-configured provided interfaces will maintain their settings (labels, properties and QoS)</li>
     * <li>Network attachments will be created for nics without it and on which a known network is configured</li>
     * <li>A nic which existed on db and wasn't reported will be removed with its network attachment</li>
     * </ul>
     *
     * @param host
     *            the host for which the network topology should be persisted and contains the list of the reported nics
     * @param dbNics
     *            network interfaces from the database prior to vdsm report
     * @param clusterNetworks
     *            the networks which assigned to the host's cluster
     * @param userConfiguredData
     *            The network configuration as provided by the user, for which engine managed data will be preserved.
     */
    private void persistTopology(VDS host,
            List<VdsNetworkInterface> dbNics,
            List<Network> clusterNetworks,
            UserConfiguredNetworkData userConfiguredData) {

        final HostNetworkInterfacesPersister networkInterfacesPersister =
                new HostNetworkInterfacesPersisterImpl(interfaceDao, host.getInterfaces(), dbNics, userConfiguredData);

        networkInterfacesPersister.persistTopology();

        final HostNetworkAttachmentsPersister networkAttachmentPersister =
                new HostNetworkAttachmentsPersister(networkAttachmentDao,
                        host.getId(),
                        host.getInterfaces(),
                        userConfiguredData.getCustomProperties(),
                        userConfiguredData.getNetworkAttachments(),
                        clusterNetworks);

        networkAttachmentPersister.persistNetworkAttachments();
    }

    private String getVmNetworksImplementedAsBridgeless(VDS host, List<Network> clusterNetworks) {
        Map<String, VdsNetworkInterface> interfacesByNetworkName =
                Entities.hostInterfacesByNetworkName(host.getInterfaces());
        List<String> networkNames = new ArrayList<>();

        for (Network net : clusterNetworks) {
            if (net.isVmNetwork()
                && interfacesByNetworkName.containsKey(net.getName())
                && !interfacesByNetworkName.get(net.getName()).isBridged()) {
                networkNames.add(net.getName());
            }
        }

        return StringUtils.join(networkNames, ",");
    }

    private String getMissingOperationalClusterNetworks(VDS host, List<Network> clusterNetworks) {
        List<String> networkNames = new ArrayList<>();

        for (Network net : clusterNetworks) {
            if (net.getCluster().getStatus() == OPERATIONAL &&
                net.getCluster().isRequired() &&
                !host.getNetworkNames().contains(net.getName())) {
                networkNames.add(net.getName());
            }
        }
        return StringUtils.join(networkNames, ",");
    }

    private void setNonOperational(VDS host, NonOperationalReason reason, Map<String, String> customLogValues) {
        resourceManager.getEventListener().vdsNonOperational(host.getId(), reason, true, Guid.Empty, customLogValues);
    }
}
