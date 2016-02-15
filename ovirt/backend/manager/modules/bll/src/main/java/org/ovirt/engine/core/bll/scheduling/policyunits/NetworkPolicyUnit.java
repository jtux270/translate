package org.ovirt.engine.core.bll.scheduling.policyunits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.scheduling.PerHostMessages;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.dao.network.VmNetworkInterfaceDao;
import org.ovirt.engine.core.utils.NetworkUtils;

public class NetworkPolicyUnit extends PolicyUnitImpl {
    public NetworkPolicyUnit(PolicyUnit policyUnit) {
        super(policyUnit);
    }

    @Override
    public List<VDS> filter(List<VDS> hosts, VM vm, Map<String, String> parameters, PerHostMessages messages) {
        if (hosts == null || hosts.isEmpty()) {
            return null;
        }

        List<VDS> toRemoveHostList = new ArrayList<VDS>();
        List<VmNetworkInterface> vmNICs = getVmNetworkInterfaceDao().getAllForVm(vm.getId());
        Guid clusterId = hosts.get(0).getVdsGroupId();
        List<Network> clusterNetworks = getNetworkDAO().getAllForCluster(clusterId);
        Map<String, Network> networksByName = Entities.entitiesByName(clusterNetworks);
        Map<Guid, List<String>> hostNics = getInterfaceDAO().getHostNetworksByCluster(clusterId);
        Network displayNetwork = NetworkUtils.getDisplayNetwork(clusterNetworks);
        Map<Guid, VdsNetworkInterface> hostDisplayNics = getDisplayNics(displayNetwork);

        for (VDS host : hosts) {
            List<String> missingIfs = new ArrayList<>();
            ValidationResult result =
                    validateRequiredNetworksAvailable(host,
                            vm,
                            vmNICs,
                            displayNetwork,
                            networksByName,
                            hostNics.get(host.getId()),
                            hostDisplayNics.get(host.getId()),
                            missingIfs);

            if (!result.isValid()) {
                toRemoveHostList.add(host);
                String nics = StringUtils.join(missingIfs, ", ");
                messages.addMessage(host.getId(), String.format("$networkNames %1$s", nics));
                messages.addMessage(host.getId(), VdcBllMessages.VAR__DETAIL__NETWORK_MISSING.name());
            }
        }
        hosts.removeAll(toRemoveHostList);
        return hosts;
    }

    public Map<Guid, VdsNetworkInterface> getDisplayNics(Network displayNetwork) {
        Map<Guid, VdsNetworkInterface> displayNics = new HashMap<>();
        if (displayNetwork != null) {
            List<VdsNetworkInterface> nics = getInterfaceDAO().getVdsInterfacesByNetworkId(displayNetwork.getId());
            for (VdsNetworkInterface nic : nics) {
                displayNics.put(nic.getVdsId(), nic);
            }
        }

        return displayNics;
    }

    /**
     * Determine whether all required Networks are attached to the Host's Nics. A required Network, depending on
     * ConfigValue.OnlyRequiredNetworksMandatoryForVdsSelection, is defined as: 1. false: any network that is defined on
     * an Active vNic of the VM or the cluster's display network. 2. true: a Cluster-Required Network that is defined on
     * an Active vNic of the VM.
     *
     * @param vds
     *            the Host
     * @param vm
     *            the VM
     * @param vmNICs
     * @param displayNetwork
     * @param networksByName
     * @param hostNetworks
     *            the Host network names
     * @param displayNic
     *            the interface on top the display network is configured
     * @return the result of network compatibility check
     */
    private ValidationResult validateRequiredNetworksAvailable(VDS vds,
            VM vm,
            List<VmNetworkInterface> vmNICs,
            Network displayNetwork,
            Map<String, Network> networksByName,
            List<String> hostNetworks,
            VdsNetworkInterface displayNic,
            List<String> missingNetworks) {

        boolean onlyRequiredNetworks =
                Config.<Boolean> getValue(ConfigValues.OnlyRequiredNetworksMandatoryForVdsSelection);
        for (final VmNetworkInterface vmIf : vmNICs) {
            boolean found = false;

            if (vmIf.getNetworkName() == null) {
                found = true;
            } else {
                for (String networkName : hostNetworks) {
                    if (!networkRequiredOnVds(vmIf, networksByName, onlyRequiredNetworks)
                            || StringUtils.equals(vmIf.getNetworkName(), networkName)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                if (missingNetworks != null) {
                    missingNetworks.add(vmIf.getNetworkName());
                }
                StringBuilder sbBuilder = new StringBuilder();
                sbBuilder.append(Entities.vmInterfacesByNetworkName(vmNICs).keySet());
                log.debugFormat("host {0} is missing networks required by VM nics {1}",
                        vds.getName(),
                        sbBuilder.toString());
                return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_VDS_VM_NETWORKS);
            }
        }

        return validateDisplayNetworkAvailability(vds, onlyRequiredNetworks, displayNic, displayNetwork);
    }

    private boolean networkRequiredOnVds(VmNetworkInterface vmIface,
            Map<String, Network> networksByName,
            boolean onlyRequiredNetworks) {
        if (!vmIface.isPlugged()) {
            return false;
        }

        Network network = networksByName.get(vmIface.getNetworkName());
        if (onlyRequiredNetworks) {
            return network.getCluster().isRequired();
        }

        return !network.isExternal();
    }

    /**
     * Determines whether the cluster's display network is defined on the host.
     *
     * @param host
     *            the host
     * @param onlyRequiredNetworks
     *            should be false, in order the method to be non-trivial.
     * @param displayNic
     *            the interface on top the display network is configured
     * @param displayNetwork
     *            the cluster's display network
     * @return the result of the display network validity check on the given host
     */
    private ValidationResult validateDisplayNetworkAvailability(VDS host,
            boolean onlyRequiredNetworks,
            VdsNetworkInterface displayNic,
            Network displayNetwork) {
        if (onlyRequiredNetworks) {
            return ValidationResult.VALID;
        }

        if (displayNetwork == null) {
            return ValidationResult.VALID;
        }

        // Check if display network attached to host and has a proper boot protocol
        if (displayNic == null) {
            log.debugFormat("host {0} is missing the cluster's display network", host.getName());
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_MISSING_DISPLAY_NETWORK);
        }

        if (displayNic.getBootProtocol() == NetworkBootProtocol.NONE) {
            log.debugFormat("Host {0} has the display network {1} configured with improper boot protocol on interface {2}.",
                    host.getName(),
                    displayNetwork.getName(),
                    displayNic.getName());
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_DISPLAY_NETWORK_HAS_NO_BOOT_PROTOCOL,
                    String.format("$DisplayNetwork %s", displayNetwork.getName()));
        }

        return ValidationResult.VALID;
    }

    private VmNetworkInterfaceDao getVmNetworkInterfaceDao() {
        return DbFacade.getInstance().getVmNetworkInterfaceDao();
    }

    private InterfaceDao getInterfaceDAO() {
        return DbFacade.getInstance().getInterfaceDao();
    }

    private NetworkDao getNetworkDAO() {
        return DbFacade.getInstance().getNetworkDao();
    }
}
