package org.ovirt.engine.core.bll.network.cluster.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.ManageLabeledNetworksParametersBuilder;
import org.ovirt.engine.core.bll.network.ManageLabeledNetworksParametersBuilderFactory;
import org.ovirt.engine.core.common.action.PersistentSetupNetworksParameters;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkDao;

final class NetworkClustersToSetupNetworksParametersTransformerImpl
        implements NetworkClustersToSetupNetworksParametersTransformer {

    private final NetworkDao networkDao;
    private final InterfaceDao interfaceDao;
    private final ManageLabeledNetworksParametersBuilderFactory manageLabeledNetworksParametersBuilderFactory;
    private final CommandContext commandContext;

    NetworkClustersToSetupNetworksParametersTransformerImpl(
            NetworkDao networkDao,
            InterfaceDao interfaceDao,
            ManageLabeledNetworksParametersBuilderFactory manageLabeledNetworksParametersBuilderFactory,
            CommandContext commandContext) {
        Objects.requireNonNull(networkDao, "networkDao cannot be null");
        Objects.requireNonNull(interfaceDao, "interfaceDao cannot be null");
        Objects.requireNonNull(manageLabeledNetworksParametersBuilderFactory,
                "manageLabeledNetworksParametersBuilderFactory cannot be null");

        this.networkDao = networkDao;
        this.interfaceDao = interfaceDao;
        this.manageLabeledNetworksParametersBuilderFactory = manageLabeledNetworksParametersBuilderFactory;
        this.commandContext = commandContext;
    }

    @Override
    public List<PersistentSetupNetworksParameters> transform(
            Collection<NetworkCluster> attachments,
            Collection<NetworkCluster> detachments) {

        final Map<Guid, List<Network>> attachNetworksByHost = new HashMap<>();
        final Map<Guid, Map<String, VdsNetworkInterface>> labelsToNicsByHost = new HashMap<>();

        for (NetworkCluster networkCluster : attachments) {
            final Network network = networkDao.get(networkCluster.getNetworkId());
            final List<VdsNetworkInterface> nics = interfaceDao.getAllInterfacesByLabelForCluster(
                    networkCluster.getClusterId(),
                    network.getLabel());

            for (VdsNetworkInterface nic : nics) {
                if (!attachNetworksByHost.containsKey(nic.getVdsId())) {
                    attachNetworksByHost.put(nic.getVdsId(), new ArrayList<Network>());
                    labelsToNicsByHost.put(nic.getVdsId(), new HashMap<String, VdsNetworkInterface>());
                }

                labelsToNicsByHost.get(nic.getVdsId()).put(network.getLabel(), nic);
                attachNetworksByHost.get(nic.getVdsId()).add(network);
            }
        }

        Map<Guid, List<Network>> detachNetworksByHost = new HashMap<>();
        Map<Guid, List<VdsNetworkInterface>> detachNicsByHost = new HashMap<>();

        for (NetworkCluster networkCluster : detachments) {
            final Network network = networkDao.get(networkCluster.getNetworkId());
            final List<VdsNetworkInterface> nics = interfaceDao.getAllInterfacesByLabelForCluster(
                    networkCluster.getClusterId(),
                    network.getLabel());

            for (VdsNetworkInterface nic : nics) {
                if (!detachNetworksByHost.containsKey(nic.getVdsId())) {
                    detachNetworksByHost.put(nic.getVdsId(), new ArrayList<Network>());
                    detachNicsByHost.put(nic.getVdsId(), new ArrayList<VdsNetworkInterface>());
                }

                detachNicsByHost.get(nic.getVdsId()).add(nic);
                detachNetworksByHost.get(nic.getVdsId()).add(network);
            }
        }

        return createSetupNetworksParameters(attachNetworksByHost,
                labelsToNicsByHost,
                detachNetworksByHost,
                detachNicsByHost);
    }

    private List<PersistentSetupNetworksParameters> createSetupNetworksParameters(
            Map<Guid, List<Network>> attachNetworksByHost,
            Map<Guid, Map<String, VdsNetworkInterface>> labelsToNicsByHost,
            Map<Guid, List<Network>> detachNetworksByHost,
            Map<Guid, List<VdsNetworkInterface>> detachNicsByHost) {

        final List<PersistentSetupNetworksParameters> parameters = new ArrayList<>(attachNetworksByHost.size());
        final ManageLabeledNetworksParametersBuilder builder =
                manageLabeledNetworksParametersBuilderFactory.create(commandContext);
        for (Entry<Guid, List<Network>> entry : attachNetworksByHost.entrySet()) {
            final Guid hostId = entry.getKey();
            final List<Network> attachHostNetworks = entry.getValue();
            final List<Network> detachHostNetworks;
            final List<VdsNetworkInterface> labeledNicsToBeRemoved;
            if (detachNetworksByHost.containsKey(hostId)) {
                detachHostNetworks = detachNetworksByHost.get(hostId);
                labeledNicsToBeRemoved = detachNicsByHost.get(hostId);
            } else {
                detachHostNetworks = Collections.emptyList();
                labeledNicsToBeRemoved = Collections.emptyList();
            }
            final Map<String, VdsNetworkInterface> nicsByLabel = labelsToNicsByHost.get(hostId);
            parameters.add(builder.buildParameters(
                    hostId,
                    attachHostNetworks,
                    detachHostNetworks,
                    nicsByLabel,
                    labeledNicsToBeRemoved));
        }

        for (Entry<Guid, List<Network>> entry : detachNetworksByHost.entrySet()) {
            final Guid hostId = entry.getKey();
            if (!attachNetworksByHost.containsKey(hostId)) {
                final List<Network> detachHostNetworks = entry.getValue();
                final List<VdsNetworkInterface> labeledNicsToBeRemoved = detachNicsByHost.get(hostId);
                parameters.add(builder.buildParameters(
                        hostId,
                        Collections.<Network>emptyList(),
                        detachHostNetworks,
                        Collections.<String, VdsNetworkInterface>emptyMap(),
                        labeledNicsToBeRemoved));
            }
        }
        return parameters;
    }
}
