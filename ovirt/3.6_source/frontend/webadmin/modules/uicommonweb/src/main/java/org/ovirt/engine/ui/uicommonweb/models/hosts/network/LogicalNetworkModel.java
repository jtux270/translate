package org.ovirt.engine.ui.uicommonweb.models.hosts.network;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.network.IPv4Address;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkAttachment;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.network.ReportedConfigurations;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.Vlan;
import org.ovirt.engine.ui.uicommonweb.models.hosts.DcNetworkParams;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostSetupNetworksModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.NetworkParameters;

/**
 * A Model for Logical Networks
 */
public class LogicalNetworkModel extends NetworkItemModel<NetworkStatus> {

    private boolean selected;
    private final boolean management;
    private boolean attachedViaLabel;
    private String errorMessage;
    private NetworkInterfaceModel attachedToNic;
    private NetworkInterfaceModel vlanNicModel;
    private Network network;

    public LogicalNetworkModel(Network network,
            HostSetupNetworksModel setupModel) {
        super(setupModel);
        setNetwork(network);
        management = network.getCluster() != null && network.getCluster().isManagement();
    }

    /**
     * attach a network to a target nic. If the network has VLAN id, it returns the newly created vlan bridge
     *
     * @param targetNic
     * @return
     */
    public VdsNetworkInterface attach(NetworkInterfaceModel targetNic, boolean createBridge) {
        attachedToNic = targetNic;
        List<LogicalNetworkModel> networksOnTarget = targetNic.getItems();
        networksOnTarget.add(this);

        if (!hasVlan()) {
            restoreNicParameters(targetNic.getIface());
        }

        if (isManagement()) {
            // mark the nic as a management nic
            targetNic.getIface().setType(2);
        }
        if (!createBridge) {
            return null;
        }
        VdsNetworkInterface targetNicEntity = targetNic.getIface();

        if (hasVlan()) {
            // create vlan bridge (eth0.1)
            VdsNetworkInterface bridge = new Vlan();
            bridge.setName(targetNic.getName() + "." + getVlanId()); //$NON-NLS-1$
            bridge.setNetworkName(getName());
            bridge.setBaseInterface(targetNic.getName());
            bridge.setVlanId(getVlanId());
            bridge.setMtu(getNetwork().getMtu());
            bridge.setVdsId(targetNicEntity.getVdsId());
            bridge.setVdsName(targetNicEntity.getVdsName());
            bridge.setBridged(getNetwork().isVmNetwork());
            restoreNicParameters(bridge);
            return bridge;
        } else {
            targetNicEntity.setNetworkName(getName());
            targetNicEntity.setMtu(getNetwork().getMtu());
            targetNicEntity.setBridged(getNetwork().isVmNetwork());
            return null;
        }
    }

    private void restoreNicParameters(VdsNetworkInterface nic) {
        NetworkParameters netParams = getSetupModel().getNetworkToLastDetachParams().get(getName());
        if (netParams != null) {
            nic.setBootProtocol(netParams.getBootProtocol());
            nic.setAddress(netParams.getAddress());
            nic.setSubnet(netParams.getSubnet());
            nic.setGateway(netParams.getGateway());
            nic.setQos(netParams.getQos());
        } else if (nic.getBootProtocol() == null) {
            nic.setBootProtocol(isManagement() ? NetworkBootProtocol.DHCP : NetworkBootProtocol.NONE);
        }
    }

    public void restoreNetworkAttachmentParameters(NetworkAttachment attachment) {
        NetworkParameters netParams = getSetupModel().getNetworkToLastDetachParams().get(getName());
        if (netParams != null) {
            if (netParams.isQosOverridden()) {
                attachment.setHostNetworkQos(netParams.getQos());
            }

            attachment.setProperties(netParams.getCustomProperties());
        }
    }

    public void detach() {
        boolean syncNetworkValues = false;
        if (!isInSync() && isManaged()) {
            getSetupModel().getNetworksToSync().add(getName());
            syncNetworkValues = true;
        }

        assert attachedToNic != null;
        NetworkInterfaceModel attachingNic = attachedToNic;
        // this needs to be null before the NIC items are changed, because they trigger an event
        attachedToNic = null;
        attachedViaLabel = false;
        List<LogicalNetworkModel> nicNetworks = attachingNic.getItems();
        nicNetworks.remove(this);
        // clear network name
        VdsNetworkInterface nicEntity = attachingNic.getIface();

        storeAttachmentParamsBeforeDetach();

        if (!hasVlan()) {
            nicEntity.setNetworkName(null);
            nicEntity.setBootProtocol(null);
            nicEntity.setAddress(null);
            nicEntity.setSubnet(null);
            nicEntity.setGateway(null);
            nicEntity.setQos(null);
            nicEntity.setNetworkImplementationDetails(null);
        }
        setVlanNicModel(null);
        // is this a management nic?
        if (nicEntity.getIsManagement()) {
            nicEntity.setType(0);
        }

        if (syncNetworkValues) {
            syncNetworkValues();
        }
    }
    private void storeAttachmentParamsBeforeDetach() {
        NetworkAttachment networkAttachment = getNetworkAttachment();
        if (networkAttachment == null) {
            return;
        }
        NetworkParameters netParams = new NetworkParameters();

        IPv4Address ipAdrdress =
                networkAttachment.getIpConfiguration() != null
                        && networkAttachment.getIpConfiguration().hasPrimaryAddressSet() ?
                        networkAttachment.getIpConfiguration().getPrimaryAddress()
                        : null;

        if (ipAdrdress != null) {
            netParams.setBootProtocol(ipAdrdress.getBootProtocol());
            netParams.setAddress(ipAdrdress.getAddress());
            netParams.setSubnet(ipAdrdress.getNetmask());
            netParams.setGateway(ipAdrdress.getGateway());
        }

        netParams.setQos(networkAttachment.getHostNetworkQos());

        netParams.setQosOverridden(networkAttachment.isQosOverridden());
        netParams.setCustomProperties(networkAttachment.getProperties());

        getSetupModel().getNetworkToLastDetachParams().put(getName(), netParams);
    }

    private void syncNetworkValues() {
        DcNetworkParams dcNetParams = getSetupModel().getNetDcParams(getName());

        if (dcNetParams != null) {
            getNetwork().setVlanId(dcNetParams.getVlanId());
            getNetwork().setMtu(dcNetParams.getMtu());
            getNetwork().setVmNetwork(dcNetParams.isVmNetwork());
        }

    }

    public NetworkInterfaceModel getAttachedToNic() {
        return attachedToNic;
    }

    public NetworkInterfaceModel getVlanNicModel() {
        return vlanNicModel;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public String getName() {
        return getNetwork().getName();
    }

    @Override
    public NetworkStatus getStatus() {
        return (getNetwork().getCluster() == null ? null : getNetwork().getCluster().getStatus());
    }

    public int getVlanId() {
        Integer vlanId = getNetwork().getVlanId();
        return vlanId == null ? -1 : vlanId;
    }

    public boolean hasVlan() {
        return getVlanId() >= 0;
    }

    public boolean isAttached() {
        return attachedToNic != null;
    }

    public boolean isAttachedViaLabel() {
        return attachedViaLabel;
    }

    public void attachViaLabel() {
        attachedViaLabel = true;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isManagement() {
        return management;
    }

    public Boolean isSelected() {
        return selected;
    }

    public void setVlanNicModel(NetworkInterfaceModel vlanNicmodel) {
        this.vlanNicModel = vlanNicmodel;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public boolean isInSync() {
        ReportedConfigurations reportedConfigurations = getReportedConfigurations();
        return reportedConfigurations == null || reportedConfigurations.isNetworkInSync();
    }

    public boolean isManaged() {
        return !(isAttached() && getNetworkAttachment() == null);
    }

    public ReportedConfigurations getReportedConfigurations() {
        NetworkAttachment networkAttachment = getNetworkAttachment();
        return networkAttachment == null ? null : networkAttachment.getReportedConfigurations();
    }

    @Override
    public String getType() {
        return HostSetupNetworksModel.NETWORK;
    }

    public NetworkAttachment getNetworkAttachment() {
        return getSetupModel().getNetworkAttachmentForNetwork(getNetwork().getId());
    }
}
