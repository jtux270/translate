package org.ovirt.engine.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkQoS;
import org.ovirt.engine.core.common.businessentities.network.Nic;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface.NetworkImplementationDetails;
import org.ovirt.engine.core.common.businessentities.network.Vlan;
import org.ovirt.engine.core.common.config.ConfigValues;

public class NetworkUtilsTest {

    private static final String IFACE_NAME = "eth1";

    private static final String MANAGEMENT_NETWORK = "mgmt";

    private static final int DEFAULT_MTU = 1500;

    @Rule
    public RandomUtilsSeedingRule rusr = new RandomUtilsSeedingRule();

    @Rule
    public MockConfigRule mcr = new MockConfigRule(
            MockConfigRule.mockConfig(ConfigValues.ManagementNetwork, MANAGEMENT_NETWORK),
            MockConfigRule.mockConfig(ConfigValues.DefaultMTU, DEFAULT_MTU));

    @Test
    public void calculateNetworkImplementationDetailsNoNetworkName() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        iface.setNetworkName(null);

        assertNull("Network implementation details should not be filled.",
                NetworkUtils.calculateNetworkImplementationDetails(null, null, iface));
    }

    @Test
    public void calculateNetworkImplementationDetailsEmptyNetworkName() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        iface.setNetworkName("");

        assertNull("Network implementation details should not be filled.",
                NetworkUtils.calculateNetworkImplementationDetails(null, null, iface));
    }

    @Test
    public void calculateNetworkImplementationDetailsUnmanagedNetwork() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertManaged(iface, false, null);
    }

    @Test
    public void calculateNetworkImplementationDetailsManagedNetwork() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertManaged(iface,
                true,
                createNetwork(iface.getNetworkName(), iface.isBridged(), iface.getMtu(), iface.getVlanId()));
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkIsSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                true,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId(),
                createQos());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkDefaultMtuOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                0,
                iface.getVlanId(),
                createQos());
    }

    /**
     * Cover a case when MTU is unset & other network parameters out of sync, which is not covered by other tests.
     */
    @Test
    public void calculateNetworkImplementationDetailsNetworkDefaultMtuAndVmNetworkOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                !iface.isBridged(),
                0,
                RandomUtils.instance().nextInt(),
                createQos());
    }

    @Test
    public void caluculateNetworkImplementationDetailsNetworkInSyncWithoutQos() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        iface.setQos(null);
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                true,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId(),
                null);
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkMtuOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu() + 1,
                iface.getVlanId(),
                createQos());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkVmNetworkOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                !iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId(),
                createQos());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkVlanOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId() + 1,
                createQos());
    }

    @Test
    public void calculateNetworkImplementationDetailsInterfaceQosMissing() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        iface.setQos(null);
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId(),
                createQos());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkQosMissing() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId(),
                null);
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkQosOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        NetworkQoS qos = createQos();
        qos.setOutboundAverage(30);
        qos.setOutboundPeak(30);
        qos.setOutboundBurst(30);
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId(),
                qos);
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkQosOverridden() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        iface.setQosOverridden(true);
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                true,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId(),
                new NetworkQoS());
    }

    @Test
    public void interfaceBasedOn() {
        assertTrue(NetworkUtils.interfaceBasedOn(createVlan(IFACE_NAME), IFACE_NAME));
    }

    @Test
    public void interfaceBasedOnSameName() {
        assertTrue(NetworkUtils.interfaceBasedOn(createNic(IFACE_NAME), IFACE_NAME));
    }

    @Test
    public void interfaceBasedOnNotAVlanOfIface() {
        assertFalse(NetworkUtils.interfaceBasedOn(createVlan(IFACE_NAME + "1"), IFACE_NAME));
    }

    @Test
    public void interfaceBasedOnNotAVlanAtAll() {
        assertFalse(NetworkUtils.interfaceBasedOn(createNic(IFACE_NAME + "1"), IFACE_NAME));
    }

    @Test
    public void interfaceBasedOnNullIface() {
        assertFalse(NetworkUtils.interfaceBasedOn(createVlan(IFACE_NAME), null));
    }

    @Test
    public void interfaceBasedOnNullProposedVlan() {
        assertFalse(NetworkUtils.interfaceBasedOn(null, IFACE_NAME));
    }

    @Test
    public void managementNetwork() throws Exception {
        Network net = new Network();
        net.setName(MANAGEMENT_NETWORK);

        assertTrue(NetworkUtils.isManagementNetwork(net));
    }

    @Test
    public void notManagementNetworkDifferentCase() throws Exception {
        Network net = new Network();
        net.setName(MANAGEMENT_NETWORK.toUpperCase());

        assertFalse(NetworkUtils.isManagementNetwork(net));
    }

    @Test
    public void notManagementNetwork() throws Exception {
        Network net = new Network();
        net.setName(MANAGEMENT_NETWORK + "1");

        assertFalse(NetworkUtils.isManagementNetwork(net));
    }

    @Test
    public void nullNotManagementNetwork() throws Exception {
        Network net = new Network();

        assertFalse(NetworkUtils.isManagementNetwork(net));
    }

    private VdsNetworkInterface createVlan(String baseIfaceName) {
        VdsNetworkInterface iface = new Vlan(RandomUtils.instance().nextInt(100), baseIfaceName);
        return iface;
    }

    private VdsNetworkInterface createNic(String ifaceName) {
        VdsNetworkInterface iface = new Nic();
        iface.setName(ifaceName);
        return iface;
    }

    private void calculateNetworkImplementationDetailsAndAssertManaged(VdsNetworkInterface iface,
            boolean expectManaged,
            Network network) {
        NetworkImplementationDetails networkImplementationDetails =
                NetworkUtils.calculateNetworkImplementationDetails(network, null, iface);

        assertNotNull("Network implementation details should be filled.", networkImplementationDetails);
        assertEquals("Network implementation details should be " + (expectManaged ? "" : "un") + "managed.",
                expectManaged,
                networkImplementationDetails.isManaged());
    }

    private void calculateNetworkImplementationDetailsAndAssertSync(VdsNetworkInterface iface,
            boolean expectSync,
            String networkName,
            boolean vmNet,
            int mtu,
            int vlanId,
            NetworkQoS qos) {
        Network network = createNetwork(networkName, vmNet, mtu, vlanId);

        NetworkImplementationDetails networkImplementationDetails =
                NetworkUtils.calculateNetworkImplementationDetails(network, qos, iface);

        assertNotNull("Network implementation details should be filled.", networkImplementationDetails);
        assertEquals("Network implementation details should be " + (expectSync ? "in" : "out of") + " sync.",
                expectSync,
                networkImplementationDetails.isInSync());
    }

    private Network createNetwork(String networkName,
            boolean vmNetwork,
            int mtu,
            Integer vlanId) {
        Network network = new Network();
        network.setVmNetwork(vmNetwork);
        network.setMtu(mtu);
        network.setVlanId(vlanId);

        return network;
    }

    private VdsNetworkInterface createNetworkDevice() {
        VdsNetworkInterface iface = new VdsNetworkInterface();
        iface.setNetworkName(RandomUtils.instance().nextString(10));
        iface.setBridged(RandomUtils.instance().nextBoolean());
        iface.setMtu(100);
        iface.setVlanId(100);
        iface.setQos(createQos());
        return iface;
    }

    private NetworkQoS createQos() {
        NetworkQoS qos = new NetworkQoS();
        qos.setInboundAverage(30);
        qos.setInboundPeak(30);
        qos.setInboundBurst(30);
        return qos;
    }
}
