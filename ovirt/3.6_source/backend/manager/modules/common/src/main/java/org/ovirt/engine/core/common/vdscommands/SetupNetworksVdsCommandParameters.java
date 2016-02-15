package org.ovirt.engine.core.common.vdscommands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.common.action.CustomPropertiesForVdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.utils.ToStringBuilder;

/**
 * Replaced by {@link org.ovirt.engine.core.common.vdscommands.HostSetupNetworksVdsCommandParameters}
 */
@Deprecated
public class SetupNetworksVdsCommandParameters extends VdsIdVDSCommandParametersBase {
    private VDS vds;
    private List<Network> networks;
    private List<String> removedNetworks;
    private List<VdsNetworkInterface> bonds;
    private Set<String> removedBonds;
    private List<VdsNetworkInterface> interfaces;
    private CustomPropertiesForVdsNetworkInterface customProperties;

    private boolean force;
    private boolean checkConnectivity;
    private int conectivityTimeout;

    /**
     * @param vds
     *            Host for which the command is sent
     * @param networks
     *            Added networks only
     * @param removedNetworks
     *            Removed networks only
     * @param bonds
     *            Added bonds only
     * @param removedBonds
     *            Removed networks only
     * @param interfaces
     *            Interfaces that are connected to a network or bond
     */
    public SetupNetworksVdsCommandParameters(VDS vds,
        List<Network> networks,
        List<String> removedNetworks,
        List<VdsNetworkInterface> bonds,
        Set<String> removedBonds,
        List<VdsNetworkInterface> interfaces,
        CustomPropertiesForVdsNetworkInterface customProperties) {
        super(vds.getId());

        this.vds = vds;
        this.networks = (networks == null) ? new ArrayList<Network>() : networks;
        this.removedNetworks = (removedNetworks == null) ? new ArrayList<String>() : removedNetworks;
        this.bonds = (bonds == null) ? new ArrayList<VdsNetworkInterface>() : bonds;
        this.removedBonds = (removedBonds == null) ? new HashSet<String>() : removedBonds;
        this.interfaces = (interfaces == null) ? new ArrayList<VdsNetworkInterface>() : interfaces;
        this.customProperties = customProperties == null
            ? new CustomPropertiesForVdsNetworkInterface()
            : customProperties;
    }

    public SetupNetworksVdsCommandParameters() {
    }

    public VDS getVds() {
        return vds;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public void setNetworks(List<Network> networks) {
        this.networks = networks;
    }

    public List<VdsNetworkInterface> getBonds() {
        return bonds;
    }

    public void setBonds(List<VdsNetworkInterface> bonds) {
        this.bonds = bonds;
    }

    public List<VdsNetworkInterface> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<VdsNetworkInterface> interfaces) {
        this.interfaces = interfaces;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isCheckConnectivity() {
        return checkConnectivity;
    }

    public int getConectivityTimeout() {
        return conectivityTimeout;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void setCheckConnectivity(boolean checkConnectivity) {
        this.checkConnectivity = checkConnectivity;
    }

    public void setConectivityTimeout(int conectivityTimeout) {
        this.conectivityTimeout = conectivityTimeout;
    }

    public List<String> getRemovedNetworks() {
        return removedNetworks;
    }

    public void setRemovedNetworks(List<String> removedNetworks) {
        this.removedNetworks = removedNetworks;
    }

    public Set<String> getRemovedBonds() {
        return removedBonds;
    }

    public void setRemovedBonds(Set<String> removedBonds) {
        this.removedBonds = removedBonds;
    }

    public CustomPropertiesForVdsNetworkInterface getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(CustomPropertiesForVdsNetworkInterface customProperties) {
        this.customProperties = customProperties;
    }

    @Override
    protected ToStringBuilder appendAttributes(ToStringBuilder tsb) {
        return super.appendAttributes(tsb)
                .append("force", isForce())
                .append("checkConnectivity", isCheckConnectivity())
                .append("connectivityTimeout", getConectivityTimeout())
                .append("networks", getNetworks())
                .append("bonds", getBonds())
                .append("interfaces", getInterfaces())
                .append("removedNetworks", getRemovedNetworks())
                .append("removedBonds", getRemovedBonds());
    }
}
