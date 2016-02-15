package org.ovirt.engine.core.bll.network.host;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.businessentities.BusinessEntityMap;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkAttachment;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.utils.NetworkUtils;
import org.ovirt.engine.core.utils.ReplacementUtils;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

public class NetworkMtuValidator {

    public static final String VAR_NETWORK_MTU_DIFFERENCES_LIST = "NETWORK_MTU_DIFFERENCES_LIST";
    private final BusinessEntityMap<Network> networkBusinessEntityMap;

    public NetworkMtuValidator(BusinessEntityMap<Network> networkBusinessEntityMap) {

        this.networkBusinessEntityMap = networkBusinessEntityMap;
    }

    /**
     * Validates there is no differences on MTU value between non-VM network to Vlans over the same interface/bond
     */
    public ValidationResult validateMtu(Collection<NetworkAttachment> attachmentsToConfigure) {
        return validateMtu(getNetworksOnNics(attachmentsToConfigure));
    }

    ValidationResult validateMtu(Map<String, List<Network>> nicsToNetworks) {
        for (List<Network> networksOnNic : nicsToNetworks.values()) {
            if (!networksOnNicMatchMtu(networksOnNic)) {
                ValidationResult validationResult = reportMtuDifferences(networksOnNic);
                if (!validationResult.isValid()) {
                    return validationResult;
                }
            }
        }

        return ValidationResult.VALID;
    }

    Map<String, List<Network>> getNetworksOnNics(Collection<NetworkAttachment> attachmentsToConfigure) {
        Map<String, List<Network>> nicsToNetworks = new HashMap<>();
        for (NetworkAttachment attachment : attachmentsToConfigure) {
            String nicName = attachment.getNicName();
            if (!nicsToNetworks.containsKey(nicName)) {
                nicsToNetworks.put(nicName, new ArrayList<Network>());
            }

            Network networkToConfigure = networkBusinessEntityMap.get(attachment.getNetworkId());
            nicsToNetworks.get(nicName).add(networkToConfigure);
        }
        return nicsToNetworks;
    }

    private ValidationResult reportMtuDifferences(List<Network> ifaceNetworks) {
        List<String> mtuDiffNetworks = new ArrayList<>();
        for (Network net : ifaceNetworks) {
            mtuDiffNetworks.add(String.format("%s(%s)",
                net.getName(),
                net.getMtu() == 0 ? "default" : String.valueOf(net.getMtu())));
        }

        return new ValidationResult(EngineMessage.NETWORK_MTU_DIFFERENCES,
            ReplacementUtils.replaceWith(VAR_NETWORK_MTU_DIFFERENCES_LIST, mtuDiffNetworks));
    }

    private boolean networksOnNicMatchMtu(List<Network> networksOnNic) {

        final Network nonVlanNetwork = LinqUtils.firstOrNull(networksOnNic, new Predicate<Network>() {
            @Override
            public boolean eval(Network network) {
                return !NetworkUtils.isVlan(network);
            }
        });

        if (nonVlanNetwork == null) {
            return true;
        }

        final Network mismatchMtuNetwork = LinqUtils.firstOrNull(networksOnNic, new Predicate<Network>() {
            @Override
            public boolean eval(Network network) {
                return network.getMtu() != nonVlanNetwork.getMtu();
            }
        });

        return mismatchMtuNetwork == null;
    }

}
