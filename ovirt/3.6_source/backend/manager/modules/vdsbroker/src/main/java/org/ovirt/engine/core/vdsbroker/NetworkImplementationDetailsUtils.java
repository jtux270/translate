package org.ovirt.engine.core.vdsbroker;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.network.HostNetworkQos;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkAttachment;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface.NetworkImplementationDetails;
import org.ovirt.engine.core.dao.network.NetworkAttachmentDao;
import org.ovirt.engine.core.utils.NetworkInSyncWithVdsNetworkInterface;

@Singleton
public class NetworkImplementationDetailsUtils {
    private final NetworkAttachmentDao networkAttachmentDao;
    private final EffectiveHostNetworkQos effectiveHostNetworkQos;
    private CalculateBaseNic calculateBaseNic;

    @Inject
    public NetworkImplementationDetailsUtils(EffectiveHostNetworkQos effectiveHostNetworkQos,
        NetworkAttachmentDao networkAttachmentDao,
        CalculateBaseNic calculateBaseNic) {
        this.effectiveHostNetworkQos = effectiveHostNetworkQos;
        this.calculateBaseNic = calculateBaseNic;
        this.networkAttachmentDao = networkAttachmentDao;
    }

    /**
     * Fill network details for the given network devices from the given networks.<br>
     * {@link NetworkImplementationDetails#isInSync()} will be <code>true</code> IFF the logical network
     * properties are exactly the same as those defined on the network device.
     * @param network
     *            The network definition to fill the details from.
     * @param iface
     *            The network device to update.
     */
    public NetworkImplementationDetails calculateNetworkImplementationDetails(VdsNetworkInterface iface,
        Network network) {

        if (iface == null || StringUtils.isEmpty(iface.getNetworkName())) {
            return null;
        }

        if (network != null) {
            boolean networkInSync = build(iface, network).isNetworkInSync();
            return new NetworkImplementationDetails(networkInSync, true);
        } else {
            return new NetworkImplementationDetails();
        }
    }

    private NetworkInSyncWithVdsNetworkInterface build(NetworkAttachment networkAttachment,
        VdsNetworkInterface vdsNetworkInterface,
        Network network) {

        HostNetworkQos hostNetworkQos = effectiveHostNetworkQos.getQos(networkAttachment, network);
        return new NetworkInSyncWithVdsNetworkInterface(vdsNetworkInterface,
                network,
                hostNetworkQos,
                networkAttachment == null ? null : networkAttachment.getIpConfiguration());
    }

    private NetworkInSyncWithVdsNetworkInterface build(VdsNetworkInterface nic, Network network) {
        NetworkAttachment networkAttachment = getNetworkAttachmentForNicAndNetwork(nic, network);
        return build(networkAttachment, nic, network);
    }

    private NetworkAttachment getNetworkAttachmentForNicAndNetwork(VdsNetworkInterface nic, Network network) {
        VdsNetworkInterface baseNic = calculateBaseNic.getBaseNic(nic);
        return networkAttachmentDao.getNetworkAttachmentByNicIdAndNetworkId(baseNic.getId(), network.getId());
    }
}
