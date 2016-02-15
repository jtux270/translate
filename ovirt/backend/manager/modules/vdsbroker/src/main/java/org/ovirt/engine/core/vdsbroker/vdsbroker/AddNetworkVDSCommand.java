package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.NetworkVdsmVDSCommandParameters;
import org.ovirt.engine.core.utils.NetworkUtils;
import org.ovirt.engine.core.utils.transaction.RollbackHandler;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public class AddNetworkVDSCommand<P extends NetworkVdsmVDSCommandParameters> extends VdsBrokerCommand<P> implements RollbackHandler {
    public AddNetworkVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        if (TransactionSupport.current() != null) {
            TransactionSupport.registerRollbackHandler(this);
        }
        String networkName = (getParameters().getNetworkName() == null) ? "" : getParameters()
                .getNetworkName();
        String vlanId = (getParameters().getVlanId() != null) ? getParameters().getVlanId().toString()
                : "";
        String bond = (getParameters().getBondName() == null) ? "" : getParameters().getBondName();
        String[] nics = (getParameters().getNics() == null) ? new String[] {} : getParameters().getNics();
        Map<String, String> options = new HashMap<String, String>();

        switch (getParameters().getBootProtocol()) {
        case DHCP:
            options.put(VdsProperties.BOOT_PROTOCOL, VdsProperties.DHCP);
            break;
        case STATIC_IP:
            if (!StringUtils.isEmpty(getParameters().getInetAddr())) {
                options.put(VdsProperties.IP_ADDRESS, getParameters().getInetAddr());
            }
            if (!StringUtils.isEmpty(getParameters().getNetworkMask())) {
                options.put(VdsProperties.NETMASK, getParameters().getNetworkMask());
            }
            if (!StringUtils.isEmpty(getParameters().getGateway())) {
                options.put(VdsProperties.GATEWAY, getParameters().getGateway());
            }
            break;
        default:
            break;
        }

        options.put(VdsProperties.STP, (getParameters().getStp()) ? "yes" : "no");

        if (!StringUtils.isEmpty(getParameters().getBondingOptions())) {
            options.put(VdsProperties.BONDING_OPTIONS, getParameters().getBondingOptions());
        }

        options.put(VdsProperties.STP, (getParameters().getStp()) ? "yes" : "no");
        // options[VdsProperties.force] = "true";

        options.put("bridged", Boolean.toString(getParameters().isVmNetwork()));

        Network network = getParameters().getNetwork();
        if (network != null) {
            if (network.getMtu() == 0) {
                options.put("mtu", NetworkUtils.getDefaultMtu().toString());
            } else {
                options.put("mtu", String.valueOf(network.getMtu()));
            }
        }

        status = getBroker().addNetwork(networkName, vlanId, bond, nics, options);
        proceedProxyReturnValue();
    }

    @Override
    public void rollback() {
        try {
            // We check for "Done" status because we want to be sure that we made the net change, or in case of empty
            // response (which means the call to VDSM failed on timeout).
            // 1. If we failed VDSM revert the change so we don't need to do anything.
            // 2. If we are in transaction first command was AddNetworkCommand (end successfully), second command fails,
            // we want to revert the network change (that is why we check for Done).
            // 3. If the call to VDSM timeout out we assume it had succeeded and try to remove the network.
            // 3.1. If the timeout was a failure to call the VDSM in the first place, then probably the call to delete
            // the network will timeout also.
            if (getReturnValueFromBroker() == null ||
                    VdcBllErrors.Done == getReturnValueFromStatus(getReturnStatus())) {
                String network = (getParameters().getNetworkName() == null) ? "" : getParameters()
                        .getNetworkName();
                String vlanId = (getParameters().getVlanId() != null) ? getParameters().getVlanId()
                        .toString() : "";
                String bond = (getParameters().getBondName() == null) ? "" : getParameters()
                        .getBondName();
                String[] nics = (getParameters().getNics() == null) ? new String[] {} : getParameters()
                        .getNics();
                status = getBroker().delNetwork(network, vlanId, bond, nics);
            }
        } catch (RuntimeException ex) {
            log.error("Exception in Rollback executeVdsBrokerCommand", ex);
        }
    }
}
