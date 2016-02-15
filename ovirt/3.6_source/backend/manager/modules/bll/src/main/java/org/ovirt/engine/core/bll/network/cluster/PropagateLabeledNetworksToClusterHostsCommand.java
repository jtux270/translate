package org.ovirt.engine.core.bll.network.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.NetworkParametersBuilder;
import org.ovirt.engine.core.bll.network.cluster.transformer.NetworkClustersToSetupNetworksParametersTransformer;
import org.ovirt.engine.core.bll.network.cluster.transformer.NetworkClustersToSetupNetworksParametersTransformerFactory;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.action.ManageNetworkClustersParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Mapper;

@InternalCommandAttribute
@NonTransactiveCommandAttribute
public class PropagateLabeledNetworksToClusterHostsCommand extends CommandBase<ManageNetworkClustersParameters> {

    public static final NetworkClusterMapper NETWORK_CLUSTER_MAPPER = new NetworkClusterMapper();

    @Inject
    private NetworkClustersToSetupNetworksParametersTransformerFactory
            networkClustersToSetupNetworksParametersTransformerFactory;

    public PropagateLabeledNetworksToClusterHostsCommand(ManageNetworkClustersParameters parameters) {
        super(parameters);
    }

    public PropagateLabeledNetworksToClusterHostsCommand(ManageNetworkClustersParameters parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void executeCommand() {

        final Map<Guid, ManageNetworkClustersParameters> paramsByClusterId = mapParametersByClusterId();

        for (Entry<Guid, ManageNetworkClustersParameters> singleClusterInput : paramsByClusterId.entrySet()) {
            final Guid clusterId = singleClusterInput.getKey();
            if (isSetupNetworkSupported(clusterId)) {
                final ManageNetworkClustersParameters param = singleClusterInput.getValue();
                processSingleClusterChanges(param);
            }
        }

        setSucceeded(true);
    }

    private boolean isSetupNetworkSupported(Guid clusterId) {
        final VDSGroup cluster = getVdsGroupDao().get(clusterId);
        return NetworkHelper.setupNetworkSupported(cluster.getCompatibilityVersion());
    }

    private void processSingleClusterChanges(ManageNetworkClustersParameters param) {
        final NetworkClustersToSetupNetworksParametersTransformer
                networkClustersToSetupNetworksParametersTransformer =
                networkClustersToSetupNetworksParametersTransformerFactory.
                        createNetworkClustersToSetupNetworksParametersTransformer(getContext());
        final ArrayList<VdcActionParametersBase> setupNetworksParams = new ArrayList<>();
        setupNetworksParams.addAll(networkClustersToSetupNetworksParametersTransformer.transform(
                param.getAttachments(),
                param.getDetachments()));

        NetworkParametersBuilder.updateParametersSequencing(setupNetworksParams);
        runInternalMultipleActions(VdcActionType.PersistentSetupNetworks, setupNetworksParams);
    }

    private Map<Guid, ManageNetworkClustersParameters> mapParametersByClusterId() {
        final Map<Guid, ManageNetworkClustersParameters> paramsByClusterId = new HashMap<>();
        final Map<Guid, List<NetworkCluster>> attachmentByClusterId = LinqUtils.toMultiMap(
                getParameters().getAttachments(),
                NETWORK_CLUSTER_MAPPER);
        final Map<Guid, List<NetworkCluster>> detachmentByClusterId = LinqUtils.toMultiMap(
                getParameters().getDetachments(),
                NETWORK_CLUSTER_MAPPER);
        for (Entry<Guid, List<NetworkCluster>> singleClusterAttachments: attachmentByClusterId.entrySet()) {
            final Guid clusterId = singleClusterAttachments.getKey();
            final List<NetworkCluster> networkAttachments = singleClusterAttachments.getValue();
            final List<NetworkCluster> networkDetachments;
            if (detachmentByClusterId.containsKey(clusterId)) {
                networkDetachments = detachmentByClusterId.get(clusterId);
            } else {
                networkDetachments = Collections.emptyList();
            }
            paramsByClusterId.put(clusterId, new ManageNetworkClustersParameters(
                    networkAttachments,
                    networkDetachments,
                    Collections.<NetworkCluster>emptyList()));
        }

        for (Entry<Guid, List<NetworkCluster>> singleClusterAttachments: detachmentByClusterId.entrySet()) {
            final Guid clusterId = singleClusterAttachments.getKey();
            final List<NetworkCluster> networkDetachments = singleClusterAttachments.getValue();
            if (!attachmentByClusterId.containsKey(clusterId)) {
                paramsByClusterId.put(
                        clusterId,
                        new ManageNetworkClustersParameters(
                                Collections.<NetworkCluster>emptyList(),
                                networkDetachments,
                                Collections.<NetworkCluster>emptyList()));
            }
        }

        return paramsByClusterId;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.emptyList();
    }

    private static class NetworkClusterMapper implements Mapper<NetworkCluster, Guid, NetworkCluster> {
        @Override
        public Guid createKey(NetworkCluster networkCluster) {
            return networkCluster.getClusterId();
        }

        @Override
        public NetworkCluster createValue(NetworkCluster networkCluster) {
            return networkCluster;
        }
    }

}
