package org.ovirt.engine.core.bll.network.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.network.VmInterfaceManager;
import org.ovirt.engine.core.bll.network.cluster.ManagementNetworkUtil;
import org.ovirt.engine.core.bll.validator.network.NetworkExclusivenessValidator;
import org.ovirt.engine.core.bll.validator.network.NetworkExclusivenessValidatorResolver;
import org.ovirt.engine.core.bll.validator.network.NetworkType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.action.SetupNetworksParameters;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.network.HostNetworkQos;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkAttachment;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface.NetworkImplementationDetails;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.utils.customprop.SimpleCustomPropertiesUtil;
import org.ovirt.engine.core.common.utils.customprop.ValidationError;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.gluster.GlusterBrickDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkAttachmentDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.di.Injector;
import org.ovirt.engine.core.utils.NetworkUtils;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.violation.DetailedViolation;
import org.ovirt.engine.core.utils.violation.Violation;
import org.ovirt.engine.core.utils.violation.ViolationRenderer;
import org.ovirt.engine.core.vdsbroker.CalculateBaseNic;
import org.ovirt.engine.core.vdsbroker.EffectiveHostNetworkQos;
import org.ovirt.engine.core.vdsbroker.NetworkImplementationDetailsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupNetworksHelper {
    private static final Logger log = LoggerFactory.getLogger(SetupNetworksHelper.class);
    private final NetworkDao networkDao;
    private final NetworkAttachmentDao networkAttachmentDao;
    private final InterfaceDao interfaceDao;
    private final GlusterBrickDao glusterBrickDao;
    private final CalculateBaseNic calculateBaseNic;
    private SetupNetworksParameters params;
    private VDS vds;
    private Map<EngineMessage, ViolationRenderer> violations = new HashMap<>();
    private Map<String, VdsNetworkInterface> existingIfaces;
    private Map<String, Network> existingClusterNetworks;

    private List<Network> modifiedNetworks = new ArrayList<>();
    private List<String> removedNetworks = new ArrayList<>();
    private Map<String, VdsNetworkInterface> modifiedBonds = new HashMap<>();
    private Map<String, VdsNetworkInterface> removedBonds = new HashMap<>();
    private List<VdsNetworkInterface> modifiedInterfaces = new ArrayList<>();

    /** All interfaces that were processed by the helper. */
    private Map<String, VdsNetworkInterface> ifaceByNames = new HashMap<>();

    /** Map of all bonds which were processed by the helper. Key = bond name, Value = list of slave NICs. */
    private Map<String, List<VdsNetworkInterface>> bonds = new HashMap<>();

    /** All network`s names that are attached to some sort of interface. */
    private Set<String> attachedNetworksNames = new HashSet<>();

    private Map<String, List<NetworkType>> ifacesWithExclusiveNetwork = new HashMap<>();

    private boolean hostNetworkQosSupported;
    private boolean networkCustomPropertiesSupported;

    private final ManagementNetworkUtil managementNetworkUtil;
    private EffectiveHostNetworkQos effectiveHostNetworkQos;
    private final NetworkImplementationDetailsUtils networkImplementationDetailsUtils;
    private final NetworkExclusivenessValidator networkExclusivenessValidator;

    public SetupNetworksHelper(SetupNetworksParameters parameters,
                               VDS vds,
                               ManagementNetworkUtil managementNetworkUtil,
                               NetworkExclusivenessValidatorResolver networkExclusivenessValidatorResolver) {
        Validate.notNull(managementNetworkUtil, "managementNetworkUtil can not be null");
        Validate.notNull(networkExclusivenessValidatorResolver, "networkExclusivenessValidatorResolver can not be null");

        this.managementNetworkUtil = managementNetworkUtil;
        this.params = parameters;
        this.vds = vds;

        networkExclusivenessValidator =
                networkExclusivenessValidatorResolver.resolveNetworkExclusivenessValidator(vds.getSupportedClusterVersionsSet());

        setSupportedFeatures();
        networkDao = Injector.get(NetworkDao.class);
        networkAttachmentDao = Injector.get(NetworkAttachmentDao.class);
        interfaceDao = Injector.get(InterfaceDao.class);
        glusterBrickDao = Injector.get(GlusterBrickDao.class);
        effectiveHostNetworkQos = Injector.get(EffectiveHostNetworkQos.class);
        networkImplementationDetailsUtils = Injector.get(NetworkImplementationDetailsUtils.class);
        calculateBaseNic = Injector.get(CalculateBaseNic.class);
    }

    private void setSupportedFeatures() {
        hostNetworkQosSupported = FeatureSupported.hostNetworkQos(vds.getVdsGroupCompatibilityVersion());
        networkCustomPropertiesSupported =
                FeatureSupported.networkCustomProperties(vds.getVdsGroupCompatibilityVersion());
    }

    protected List<String> translateErrorMessages(List<String> messages) {
        return Backend.getInstance().getErrorsTranslator().TranslateErrorText(messages);
    }

    /**
     * validate and extract data from the list of interfaces sent. The general flow is:
     * <ul>
     * <li>create mapping of existing the current topology - interfaces and logical networks.
     * <li>create maps for networks bonds and bonds-slaves.
     * <li>iterate over the interfaces and extract network/bond/slave info as we go.
     * <li>validate the extracted information by using the pre-build mappings of the current topology.
     * <li>store and encapsulate the extracted lists to later be fetched by the calling command.
     * <li>error messages are aggregated
     * </ul>
     * TODO add fail-fast to exist on the first validation error.
     *
     * @return List of violations encountered (if none, list is empty).
     */
    public List<String> validate() {
        for (VdsNetworkInterface iface : params.getInterfaces()) {
            String name = iface.getName();
            if (addInterfaceToProcessedList(iface)) {
                if (iface.isBond()) {
                    extractBondIfModified(iface, name);
                } else if (StringUtils.isNotBlank(iface.getBondName())) {
                    extractBondSlave(iface);
                }

                // validate and extract to network map
                if (violations.isEmpty() && StringUtils.isNotBlank(iface.getNetworkName())) {
                    extractNetwork(iface);
                    validateGateway(iface);
                }
            }
        }

        validateInterfacesExist();
        validateBondSlavesCount();
        extractRemovedNetworks();
        extractRemovedBonds();
        extractModifiedInterfaces();
        detectSlaveChanges();
        validateMTU();
        validateNetworkQos();
        validateNotRemovingLabeledNetworks();
        validateCustomProperties();
        validateNotRemovingGlusterBrickNetworks();

        return translateViolations();
    }

    private void validateNotRemovingLabeledNetworks() {
        Map<String, VdsNetworkInterface> existingIfaces = getExistingIfaces();
        Map<String, VdsNetworkInterface> hostInterfacesByNetworkName =
                Entities.hostInterfacesByNetworkName(existingIfaces.values());

        for (String network : removedNetworks) {
            VdsNetworkInterface nic = hostInterfacesByNetworkName.get(network);
            final String baseInterfaceName = NetworkUtils.stripVlan(nic);

            if (!removedBonds.containsKey(baseInterfaceName)) {
                if (NetworkUtils.isVlan(nic)) {
                    final VdsNetworkInterface baseInterface = existingIfaces.get(baseInterfaceName);
                    validateNicForNotRemovingLabeledNetworks(network, baseInterface);
                } else {
                    validateNicForNotRemovingLabeledNetworks(network, nic);
                }
            }
        }
    }

    private void validateNotRemovingGlusterBrickNetworks() {
        for (String network : removedNetworks) {
            Network removedNetwork = getExistingClusterNetworks().get(network);
            if (removedNetwork == null || !removedNetwork.getCluster().isGluster()) {
                continue;
            }
            List<GlusterBrickEntity> bricks =
                    glusterBrickDao.getAllByClusterAndNetworkId(vds.getVdsGroupId(),
                        removedNetwork.getId());
            if (!bricks.isEmpty()) {
                addViolation(EngineMessage.ACTION_TYPE_FAILED_CANNOT_REMOVE_NETWORK_FROM_BRICK, network);
            }
        }
    }

    private void validateNicForNotRemovingLabeledNetworks(String network, VdsNetworkInterface nic) {
        Network removedNetwork = getExistingClusterNetworks().get(network);
        if (NetworkUtils.isLabeled(nic) && removedNetwork != null
                && nic.getLabels().contains(removedNetwork.getLabel())) {
            addViolation(EngineMessage.ACTION_TYPE_FAILED_CANNOT_REMOVE_LABELED_NETWORK_FROM_NIC, network);
        }
    }

    private void extractModifiedInterfaces() {
        for (VdsNetworkInterface nic : params.getInterfaces()) {
            VdsNetworkInterface existingNic = getExistingIfaces().get(nic.getName());
            if (existingNic != null) {
                Set<String> newLabels = NetworkUtils.isLabeled(nic) ? nic.getLabels() : Collections.<String> emptySet();
                Set<String> existingLabels =
                        NetworkUtils.isLabeled(existingNic) ? existingNic.getLabels() : Collections.<String> emptySet();
                if (!CollectionUtils.isEqualCollection(newLabels, existingLabels)) {
                    existingNic.setLabels(newLabels);
                    modifiedInterfaces.add(existingNic);
                }
            }
        }
    }

    /**
     * Validates that all interfaces exist on the host, except bonds that may be created.
     */
    private void validateInterfacesExist() {

        for (VdsNetworkInterface iface : params.getInterfaces()) {
            updateBaseInterface(iface);

            String nameWithoutVlanId = NetworkUtils.stripVlan(iface);

            if (!getExistingIfaces().containsKey(nameWithoutVlanId) && !bonds.containsKey(nameWithoutVlanId)) {
                addViolation(EngineMessage.NETWORK_INTERFACES_DONT_EXIST, nameWithoutVlanId);
            }
        }
    }

    private void updateBaseInterface(VdsNetworkInterface nic) {
        if (StringUtils.isNotEmpty(nic.getBaseInterface())) {
            return;
        }

        if (NetworkUtils.isVlan(nic)) {
            String[] tokens = nic.getName().split("[.]", -1);
            if (tokens.length == 1) {
                nic.setBaseInterface(nic.getName());
                return;
            }

            nic.setBaseInterface(StringUtils.join(tokens, '.', 0, tokens.length -1));
        }
    }

    /**
     * Validates there is no differences on MTU value between non-VM network to Vlans over the same interface/bond
     */
    private void validateMTU() {
        Map<String, VdsNetworkInterface> ifacesByNetworkName =
                Entities.hostInterfacesByNetworkName(params.getInterfaces());
        Set<String> checkedNetworks = new HashSet<>(getNetworks().size());

        for (Network network : getNetworks()) {
            if (!checkedNetworks.contains(network.getName())) {
                List<Network> networksOnInterface = findNetworksOnInterface(ifacesByNetworkName.get(network.getName()));
                boolean mtuMismatched = false;
                final Network nonVlanNetwork = LinqUtils.firstOrNull(networksOnInterface, new Predicate<Network>() {
                    @Override
                    public boolean eval(Network network) {
                        return !NetworkUtils.isVlan(network);
                    }
                });

                if (nonVlanNetwork != null) {
                    final Network mismatchMtuNetwork =
                            LinqUtils.firstOrNull(networksOnInterface, new Predicate<Network>() {
                                @Override
                                public boolean eval(Network network) {
                                    return network.getMtu() != nonVlanNetwork.getMtu();
                                }
                            });
                    mtuMismatched = mismatchMtuNetwork != null;
                }

                if (mtuMismatched) {
                    reportMTUDifferences(networksOnInterface);
                }
            }
        }
    }

    private void reportMTUDifferences(List<Network> ifaceNetworks) {
        List<String> mtuDiffNetworks = new ArrayList<>();
        for (Network net : ifaceNetworks) {
            mtuDiffNetworks.add(String.format("%s(%s)",
                    net.getName(),
                    net.getMtu() == 0 ? "default" : String.valueOf(net.getMtu())));
        }
        addViolation(EngineMessage.NETWORK_MTU_DIFFERENCES,
            String.format("[%s]", StringUtils.join(mtuDiffNetworks, ", ")));
    }

    private void validateNetworkQos() {
        validateQosNotPartiallyConfigured();
    }

    /**
     * Ensure that either none or all of the networks on a single interface have QoS configured on them.
     */
    private void validateQosNotPartiallyConfigured() {
        Set<String> someSubInterfacesHaveQos = new HashSet<>();
        Set<String> notAllSubInterfacesHaveQos = new HashSet<>();

        // first map which interfaces have some QoS configured on them, and which interfaces lack some QoS configuration
        for (VdsNetworkInterface iface : params.getInterfaces()) {
            String networkName = iface.getNetworkName();
            Network network = getExistingClusterNetworks().get(networkName);

            if (network != null) {
                NetworkAttachment networkAttachment = getNetworkAttachment(iface, network);

                String baseIfaceName = NetworkUtils.stripVlan(iface);

                if (NetworkUtils.qosConfiguredOnInterface(networkAttachment, network)) {
                    someSubInterfacesHaveQos.add(baseIfaceName);
                } else {
                    notAllSubInterfacesHaveQos.add(baseIfaceName);
                }
            }
        }

        // if any base interface has some sub-interfaces with QoS and some without - this is a partial configuration
        for (String ifaceName : someSubInterfacesHaveQos) {
            if (notAllSubInterfacesHaveQos.contains(ifaceName)) {
                addViolation(EngineMessage.ACTION_TYPE_FAILED_HOST_NETWORK_QOS_INTERFACES_WITHOUT_QOS, ifaceName);
            }
        }
    }

    private NetworkAttachment getNetworkAttachment(VdsNetworkInterface iface, Network network) {
        Map<String, VdsNetworkInterface> existingIfaces = getExistingIfaces();
        VdsNetworkInterface existingNic = existingIfaces.get(iface.getName());
        iface.setId(existingNic == null ? null : existingNic.getId());
        VdsNetworkInterface baseNic =
                existingNic == null ? null : calculateBaseNic.getBaseNic(existingNic, existingIfaces);
        return baseNic == null || network == null ? null :
                networkAttachmentDao.getNetworkAttachmentByNicIdAndNetworkId(baseNic.getId(), network.getId());
    }

    private void validateCustomProperties() {
        String version = vds.getVdsGroupCompatibilityVersion().getValue();
        SimpleCustomPropertiesUtil util = SimpleCustomPropertiesUtil.getInstance();
        Map<String, String> validProperties =
                util.convertProperties(Config.<String> getValue(ConfigValues.PreDefinedNetworkCustomProperties,
                    version));
        validProperties.putAll(util.convertProperties(Config.<String> getValue(ConfigValues.UserDefinedNetworkCustomProperties,
            version)));
        Map<String, String> validPropertiesNonVm = new HashMap<>(validProperties);
        validPropertiesNonVm.remove("bridge_opts");
        for (VdsNetworkInterface iface : params.getInterfaces()) {
            String networkName = iface.getNetworkName();
            if (params.getCustomProperties().hasCustomPropertiesFor(iface) && StringUtils.isNotEmpty(networkName)) {
                if (!networkCustomPropertiesSupported) {
                    addViolation(EngineMessage.ACTION_TYPE_FAILED_NETWORK_CUSTOM_PROPERTIES_NOT_SUPPORTED, networkName);
                }

                Network network = existingClusterNetworks.get(networkName);
                boolean isVmOrEmptyNetwork = network == null || network.isVmNetwork();
                Map<String, String> regExMap = isVmOrEmptyNetwork
                    ? validProperties
                    : validPropertiesNonVm;

                List<ValidationError> errors =
                    util.validateProperties(regExMap, params.getCustomProperties().getCustomPropertiesFor(iface));
                if (!errors.isEmpty()) {
                    addViolation(EngineMessage.ACTION_TYPE_FAILED_NETWORK_CUSTOM_PROPERTIES_BAD_INPUT, networkName);
                    List<String> messages = new ArrayList<>();
                    util.handleCustomPropertiesError(errors, messages);
                    log.error(StringUtils.join(translateErrorMessages(messages), ','));
                }
            }
        }
    }

    /**
     * Finds all the networks on a specific network interface, directly on the interface or over a vlan.
     *
     * @param iface
     *            the underlying interface
     * @return a list of attached networks to the given underlying interface
     */
    private List<Network> findNetworksOnInterface(VdsNetworkInterface iface) {
        String nameWithoutVlanId = NetworkUtils.stripVlan(iface);
        List<Network> networks = new ArrayList<>();
        for (VdsNetworkInterface tmp : params.getInterfaces()) {
            if (NetworkUtils.stripVlan(tmp).equals(nameWithoutVlanId) && tmp.getNetworkName() != null) {
                if (getExistingClusterNetworks().containsKey(tmp.getNetworkName())) {
                    networks.add(getExistingClusterNetworks().get(tmp.getNetworkName()));
                }
            }
        }
        return networks;
    }

    private void addViolation(EngineMessage violationMsg, String violatingEntity) {

        final Violation violation = (Violation) violations.get(violationMsg);
        if (violation == null) {
            violations.put(violationMsg, new Violation(violationMsg.name(), violatingEntity));
        } else {
            violation.add(violatingEntity);
        }
    }

    private void addDetailedViolation(
            EngineMessage violationMsg,
            String violatingEntity,
            Map<String, String> violationDetails) {

        DetailedViolation detailedViolation = (DetailedViolation) violations.get(violationMsg);
        if (detailedViolation == null) {
            detailedViolation = new DetailedViolation(violationMsg.name());
            violations.put(violationMsg, detailedViolation);
        }

        for (Entry<String, String> violationDetailsEntry : violationDetails.entrySet()) {
            detailedViolation.add(violatingEntity, violationDetailsEntry.getKey(), violationDetailsEntry.getValue());
        }
    }

    private List<String> translateViolations() {
        List<String> violationMessages = new ArrayList<>(violations.size() * 2);
        for (Entry<EngineMessage, ViolationRenderer> violationEntry : violations.entrySet()) {
            final List<String> renderedViolationMessages = violationEntry.getValue().render();
            violationMessages.addAll(renderedViolationMessages);
        }

        return violationMessages;
    }

    /**
     * Add the given interface to the list of processed interfaces, failing if it already existed.
     *
     * @param iface
     *            The interface to add.
     * @return <code>true</code> if interface wasn't in the list and was added to it, otherwise <code>false</code>.
     */
    private boolean addInterfaceToProcessedList(VdsNetworkInterface iface) {
        if (ifaceByNames.containsKey(iface.getName())) {
            addViolation(EngineMessage.NETWORK_INTERFACES_ALREADY_SPECIFIED, iface.getName());
            return false;
        }

        ifaceByNames.put(iface.getName(), iface);
        return true;
    }

    /**
     * Detect a bond that it's slaves have changed, to add to the modified bonds list.<br>
     * Make sure not to add bond that was removed entirely.
     */
    private void detectSlaveChanges() {
        for (VdsNetworkInterface newIface : params.getInterfaces()) {
            VdsNetworkInterface existingIface = getExistingIfaces().get(newIface.getName());
            if (existingIface != null && !existingIface.isBond() && existingIface.getVlanId() == null) {
                String bondNameInNewIface = newIface.getBondName();
                String bondNameInOldIface = existingIface.getBondName();

                if (!StringUtils.equals(bondNameInNewIface, bondNameInOldIface)) {
                    if (bondNameInNewIface != null && !modifiedBonds.containsKey(bondNameInNewIface)) {
                        modifiedBonds.put(bondNameInNewIface, getExistingIfaces().get(bondNameInNewIface));
                    }

                    if (bondNameInOldIface != null && !modifiedBonds.containsKey(bondNameInNewIface)
                            && !removedBonds.containsKey(bondNameInOldIface)) {
                        modifiedBonds.put(bondNameInOldIface, getExistingIfaces().get(bondNameInOldIface));
                    }
                }
            }
        }
    }

    private Map<String, Network> getExistingClusterNetworks() {
        if (existingClusterNetworks == null) {
            existingClusterNetworks = Entities.entitiesByName(
                networkDao.getAllForCluster(vds.getVdsGroupId()));
        }

        return existingClusterNetworks;
    }

    private Map<String, VdsNetworkInterface> getExistingIfaces() {
        if (existingIfaces == null) {
            List<VdsNetworkInterface> ifaces = interfaceDao.getAllInterfacesForVds(params.getVdsId());

            for (VdsNetworkInterface iface : ifaces) {
                Network network = getExistingClusterNetworks().get(iface.getNetworkName());

                NetworkImplementationDetails networkImplementationDetails =
                    networkImplementationDetailsUtils.calculateNetworkImplementationDetails(iface, network);
                iface.setNetworkImplementationDetails(networkImplementationDetails);
            }

            existingIfaces = Entities.entitiesByName(ifaces);
        }

        return existingIfaces;
    }

    private VdsNetworkInterface getExistingIfaceByNetwork(String network) {
        for (VdsNetworkInterface iface : getExistingIfaces().values()) {
            if (network.equals(iface.getNetworkName())) {
                return iface;
            }
        }

        return null;
    }

    protected DbFacade getDbFacade() {
        return DbFacade.getInstance();
    }

    /**
     * extracting a network is done by matching the desired network name with the network details from db on
     * clusterNetworksMap. The desired network is just a key and actual network configuration is taken from the db
     * entity.
     *
     * @param iface
     *            current iterated interface
     */
    private void extractNetwork(VdsNetworkInterface iface) {
        String networkName = iface.getNetworkName();

        // prevent attaching 2 interfaces to 1 network
        if (attachedNetworksNames.contains(networkName)) {
            addViolation(EngineMessage.NETWORKS_ALREADY_ATTACHED_TO_IFACES, networkName);
        } else {
            attachedNetworksNames.add(networkName);

            // check if network exists on cluster
            if (getExistingClusterNetworks().containsKey(networkName)) {
                Network network = getExistingClusterNetworks().get(networkName);
                iface.setVlanId(network.getVlanId());
                validateNetworkInternal(network);
                validateNetworkExclusiveOnIface(iface,
                        determineNetworkType(network.getVlanId(), network.isVmNetwork()));

                VdsNetworkInterface existingIface = getExistingIfaces().get(iface.getName());
                if (existingIface != null && !networkName.equals(existingIface.getNetworkName())) {
                    existingIface = getExistingIfaceByNetwork(networkName);
                }

                if (existingIface != null && existingIface.getNetworkImplementationDetails() != null
                        && !existingIface.getNetworkImplementationDetails().isInSync()) {
                    iface.setVlanId(existingIface.getVlanId());
                    if (networkShouldBeSynced(networkName)) {
                        modifiedNetworks.add(network);

                        if (unableToApplyQos(iface, network)) {
                            addViolation(EngineMessage.ACTION_TYPE_FAILED_HOST_NETWORK_QOS_NOT_SUPPORTED, networkName);
                        }
                    } else if (networkWasModified(iface)) {
                        addViolation(EngineMessage.NETWORKS_NOT_IN_SYNC, networkName);
                    }
                } else if (networkWasModified(iface)) {
                    if (networkIpAddressWasSameAsHostnameAndChanged(iface)) {
                        addViolation(EngineMessage.ACTION_TYPE_FAILED_NETWORK_ADDRESS_CANNOT_BE_CHANGED, networkName);
                    }
                    if (networkIpAddressUsedByBrickChanged(iface, network)) {
                        addViolation(EngineMessage.ACTION_TYPE_FAILED_NETWORK_ADDRESS_BRICK_IN_USE, networkName);
                    }
                    modifiedNetworks.add(network);
                }
            } else {
                VdsNetworkInterface existingIface = getExistingIfaces().get(iface.getName());
                existingIface = (existingIface == null ? iface : existingIface);
                iface.setVlanId(existingIface.getVlanId());
                validateNetworkExclusiveOnIface(iface,
                        determineNetworkType(existingIface.getVlanId(), existingIface.isBridged()));

                if (unmanagedNetworkChanged(iface)) {
                    addViolation(EngineMessage.NETWORKS_DONT_EXIST_IN_CLUSTER, networkName);
                }
            }
        }
    }

    private boolean unableToApplyQos(VdsNetworkInterface iface, Network network) {
        return !hostNetworkQosSupported && qosShouldBeApplied(iface, network);
    }

    private boolean qosShouldBeApplied(VdsNetworkInterface iface, Network network) {
        NetworkAttachment networkAttachment = getNetworkAttachment(iface, network);

        HostNetworkQos qos = effectiveHostNetworkQos.getQos(networkAttachment, network);
        return qos != null && !qos.isEmpty();
    }

    /**
     * Checks if a network is configured incorrectly:
     * <ul>
     * <li>If the host was added to the system using its IP address as the computer name for the certification creation,
     * it is forbidden to modify the IP address without reinstalling the host.</li>
     * </ul>
     *
     * @param iface
     *            The network interface which carries the network
     * @return <code>true</code> if the network was reconfigured improperly
     */
    private boolean networkIpAddressWasSameAsHostnameAndChanged(VdsNetworkInterface iface) {
        if (iface.getBootProtocol() == NetworkBootProtocol.STATIC_IP) {
            VdsNetworkInterface existingIface = getExistingIfaceByNetwork(iface.getNetworkName());
            if (existingIface != null) {
                String oldAddress = existingIface.getAddress();
                String hostName = vds.getHostName();
                return StringUtils.equals(oldAddress, hostName) && !StringUtils.equals(oldAddress, iface.getAddress());
            }
        }

        return false;
    }

    /**
     * Checks if a network is configured incorrectly:
     * <ul>
     * <li>If the network is configured to use static IP address and the interface is used by gluster bricks, then it is
     * forbidden to modify the IP address without replacing the bricks.</li>
     * </ul>
     *
     * @param iface
     *            The network interface which carries the network
     * @return <code>true</code> if the network was reconfigured improperly
     */

    private boolean networkIpAddressUsedByBrickChanged(VdsNetworkInterface iface, Network network) {
        if (iface.getBootProtocol() == NetworkBootProtocol.STATIC_IP) {
            List<GlusterBrickEntity> bricks =
                    glusterBrickDao.getAllByClusterAndNetworkId(vds.getVdsGroupId(), network.getId());
            if (bricks.isEmpty()) {
                return false;
            }
            VdsNetworkInterface existingIface = getExistingIfaceByNetwork(iface.getNetworkName());
            if (existingIface != null) {
                String oldAddress = existingIface.getAddress();
                return StringUtils.isNotEmpty(oldAddress) && !StringUtils.equals(oldAddress, iface.getAddress());
            }
        }
        return false;
    }

    private NetworkType determineNetworkType(Integer vlanId, boolean vmNetwork) {
        return vlanId != null ? NetworkType.VLAN : vmNetwork ? NetworkType.VM : NetworkType.NON_VM;
    }

    /**
     * Make sure that if the given interface has a VM network on it then there is nothing else on the interface, or if
     * the given interface is a VLAN network, than there is no VM network on the interface.<br>
     * Other combinations are either legal or illegal but are not a concern of this method.
     *
     * @param iface
     *            The interface to check.
     * @param networkType
     *            The type of the network.
     */
    private void validateNetworkExclusiveOnIface(VdsNetworkInterface iface, NetworkType networkType) {
        String ifaceName = NetworkUtils.stripVlan(iface);
        List<NetworkType> networksOnIface = ifacesWithExclusiveNetwork.get(ifaceName);

        if (networksOnIface == null) {
            networksOnIface = new ArrayList<>();
            ifacesWithExclusiveNetwork.put(ifaceName, networksOnIface);
        }

        networksOnIface.add(networkType);
        if (!networkExclusivenessValidator.isNetworkExclusive(networksOnIface)) {
            addViolation(networkExclusivenessValidator.getViolationMessage(), ifaceName);
        }
    }

    /**
     * Make sure the network is not an external network, which we can't set up.
     *
     * @param network
     *            The network to check.
     */
    private void validateNetworkInternal(Network network) {
        if (network.getProvidedBy() != null) {
            addViolation(EngineMessage.ACTION_TYPE_FAILED_EXTERNAL_NETWORKS_CANNOT_BE_PROVISIONED, network.getName());
        }
    }

    /**
     * Checks if an unmanaged network changed.<br>
     * This can be either if there is no existing interface for this network, i.e. it is a unmanaged VLAN which was
     * moved to a different interface, or if the network name on the existing interface is not the same as it was
     * before.
     *
     * @param iface
     *            The interface on which the unmanaged network is now defined.
     * @return <code>true</code> if the network changed, or <code>false</code> otherwise.
     */
    private boolean unmanagedNetworkChanged(VdsNetworkInterface iface) {
        VdsNetworkInterface existingIface = getExistingIfaces().get(iface.getName());
        return existingIface == null || !iface.getNetworkName().equals(existingIface.getNetworkName());
    }

    /**
     * Check if the network parameters on the given interface were modified (or network was added).
     *
     * @param iface
     *            The interface to check.
     * @return <code>true</code> if the network parameters were changed, or the network wan't on the given interface.
     *         <code>false</code> if it existed and didn't change.
     */
    private boolean networkWasModified(VdsNetworkInterface iface) {
        VdsNetworkInterface existingIface = getExistingIfaces().get(iface.getName());

        if (existingIface == null) {
            return true;
        }

        return !ObjectUtils.equals(iface.getNetworkName(), existingIface.getNetworkName())
                || iface.getBootProtocol() != existingIface.getBootProtocol()
                || staticBootProtoPropertiesChanged(iface, existingIface)
                || !ObjectUtils.equals(iface.getQos(), existingIface.getQos())
                || customPropertiesChanged(iface, existingIface);
    }

    /**
     * Check if network's logical configuration should be synchronized (as sent in parameters).
     *
     * @param network
     *            The network to check if synchronized.
     * @return <code>true</code> in case network should be sunchronized, <code>false</code> otherwise.
     */
    private boolean networkShouldBeSynced(String network) {
        return params.getNetworksToSync() != null && params.getNetworksToSync().contains(network);
    }

    /**
     * @param iface
     *            New interface definition.
     * @param existingIface
     *            Existing interface definition.
     * @return <code>true</code> if the boot protocol is static, and one of the properties has changed.
     *         <code>false</code> otherwise.
     */
    private boolean staticBootProtoPropertiesChanged(VdsNetworkInterface iface, VdsNetworkInterface existingIface) {
        return iface.getBootProtocol() == NetworkBootProtocol.STATIC_IP
                && (!ObjectUtils.equals(iface.getAddress(), existingIface.getAddress())
                        || !ObjectUtils.equals(iface.getGateway(), existingIface.getGateway())
                        || !ObjectUtils.equals(iface.getSubnet(), existingIface.getSubnet()));
    }

    /**
     * @param iface
     *            New interface definition.
     * @param existingIface
     *            Existing interface definition.
     * @return <code>true</code> iff the custom properties have changed (null and empty map are considered equal).
     */
    private boolean customPropertiesChanged(VdsNetworkInterface iface, VdsNetworkInterface existingIface) {
        String networkName = iface.getNetworkName();
        Network network = getExistingClusterNetworks().get(networkName);
        VdsNetworkInterface baseNic = calculateBaseNic.getBaseNic(iface, getExistingIfaces());
        NetworkAttachment networkAttachment = baseNic == null || network == null ? null :
            networkAttachmentDao.getNetworkAttachmentByNicIdAndNetworkId(baseNic.getId(), network.getId());


        Map<String, String> newCustomProperties = params.getCustomProperties().getCustomPropertiesFor(iface);
        Map<String, String> existingCustomProperties = networkAttachment == null ? Collections.<String, String>emptyMap() : networkAttachment.getProperties();



        return !Objects.equals(getEmptyMapIfNull(newCustomProperties), getEmptyMapIfNull(existingCustomProperties));
    }

    private Object getEmptyMapIfNull(Map<String, String> customProperties) {
        return customProperties == null ? Collections.emptyMap() : customProperties;
    }

    /**
     * build mapping of the bond name - > list of slaves. slaves are interfaces with a pointer to the master bond by
     * bondName.
     *
     * @param iface
     */
    private void extractBondSlave(VdsNetworkInterface iface) {
        List<VdsNetworkInterface> slaves = bonds.get(iface.getBondName());
        if (slaves == null) {
            slaves = new ArrayList<>();
            bonds.put(iface.getBondName(), slaves);
        }

        slaves.add(iface);
    }

    /**
     * Extract the bond to the modified bonds list if it was added or the bond interface config has changed.
     *
     * @param iface
     *            The interface of the bond.
     * @param bondName
     *            The bond name.
     */
    private void extractBondIfModified(VdsNetworkInterface iface, String bondName) {
        if (!bonds.containsKey(bondName)) {
            bonds.put(bondName, new ArrayList<VdsNetworkInterface>());
        }

        if (bondWasModified(iface)) {
            modifiedBonds.put(bondName, iface);
        }
    }

    /**
     * Check if the given bond was modified (or added).<br>
     * Currently changes that are recognized are if bonding options changed, or the bond was added.
     *
     * @param iface
     *            The bond to check.
     * @return <code>true</code> if the bond was changed, or is a new one. <code>false</code> if it existed and didn't
     *         change.
     */
    private boolean bondWasModified(VdsNetworkInterface iface) {
        VdsNetworkInterface existingIface = getExistingIfaces().get(iface.getName());

        if (existingIface == null) {
            return true;
        }

        return !ObjectUtils.equals(iface.getBondOptions(), existingIface.getBondOptions());
    }

    /**
     * Extract the bonds to be removed. If a bond was attached to slaves but it's not attached to anything then it
     * should be removed. Otherwise, no point in removing it: Either it is still a bond, or it isn't attached to any
     * slaves either way so no need to touch it. If a bond is removed, its labels are also cleared.
     */
    private void extractRemovedBonds() {
        for (VdsNetworkInterface iface : getExistingIfaces().values()) {
            String bondName = iface.getBondName();
            if (StringUtils.isNotBlank(bondName) && !bonds.containsKey(bondName)) {
                VdsNetworkInterface existingBond = getExistingIfaces().get(bondName);
                existingBond.setLabels(null);
                removedBonds.put(bondName, existingBond);
            }
        }
    }

    private boolean validateBondSlavesCount() {
        boolean returnValue = true;
        for (Map.Entry<String, List<VdsNetworkInterface>> bondEntry : bonds.entrySet()) {
            if (bondEntry.getValue().size() < 2) {
                returnValue = false;
                addViolation(EngineMessage.NETWORK_BONDS_INVALID_SLAVE_COUNT, bondEntry.getKey());
            }
        }

        return returnValue;
    }

    /**
     * Calculate the networks that should be removed - If the network was attached to a NIC and is no longer attached to
     * it, then it will be removed.
     */
    private void extractRemovedNetworks() {
        for (VdsNetworkInterface iface : getExistingIfaces().values()) {
            String networkName = iface.getNetworkName();
            if (StringUtils.isNotBlank(networkName) && !attachedNetworksNames.contains(networkName)) {
                removedNetworks.add(networkName);

                final List<String> vmNames = getVmInterfaceManager().findActiveVmsUsingNetworks(
                        params.getVdsId(),
                        Collections.singleton(networkName));

                for (String vmName : vmNames) {
                    addDetailedViolation(
                            EngineMessage.NETWORK_CANNOT_DETACH_NETWORK_USED_BY_VMS,
                            vmName,
                            Collections.singletonMap("networkNames", networkName));
                }
            }
        }
    }

    /**
     * Validates that gateway is set not on management network just if multiple gateways feature is supported
     */
    private void validateGateway(VdsNetworkInterface iface) {
        if (StringUtils.isNotEmpty(iface.getGateway())
                && !managementNetworkUtil.isManagementNetwork(iface.getNetworkName(), vds.getVdsGroupId())
                && !FeatureSupported.multipleGatewaysSupported(vds.getVdsGroupCompatibilityVersion())) {

            addViolation(EngineMessage.NETWORK_ATTACH_ILLEGAL_GATEWAY, iface.getNetworkName());
        }
    }

    public List<Network> getNetworks() {
        return modifiedNetworks;
    }

    public List<String> getRemoveNetworks() {
        return removedNetworks;
    }

    public List<VdsNetworkInterface> getBonds() {
        return new ArrayList<>(modifiedBonds.values());
    }

    public Map<String, VdsNetworkInterface> getRemovedBonds() {
        return removedBonds;
    }

    public List<VdsNetworkInterface> getModifiedInterfaces() {
        return modifiedInterfaces;
    }

    public VmInterfaceManager getVmInterfaceManager() {
        return new VmInterfaceManager();
    }

}
