package org.ovirt.engine.core.dao.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.network.InterfaceStatus;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.NetworkQoS;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkStatistics;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAOTestCase;
import org.ovirt.engine.core.dao.FixturesTool;
import org.ovirt.engine.core.utils.RandomUtils;

public class InterfaceDaoTest extends BaseDAOTestCase {
    private static final String IP_ADDR = "10.35.110.10";
    private static final Guid VDS_ID = new Guid("afce7a39-8e8c-4819-ba9c-796d316592e6");
    private static final Guid CLUSTER_ID = new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d1");
    private static final String TARGET_ID = "0cc146e8-e5ed-482c-8814-270bc48c297b";
    private static final String LABEL = "abc";

    private InterfaceDao dao;
    private VdsNetworkInterface existingVdsInterface;
    private VdsNetworkInterface newVdsInterface;
    private VdsNetworkStatistics newVdsStatistics;
    private NetworkQoS newQos;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = dbFacade.getInterfaceDao();
        existingVdsInterface = dao.get(FixturesTool.VDS_NETWORK_INTERFACE);

        newQos = new NetworkQoS();
        newQos.setInboundAverage(30);
        newQos.setInboundPeak(30);
        newQos.setInboundBurst(30);

        newVdsInterface = new VdsNetworkInterface();
        newVdsInterface.setStatistics(new VdsNetworkStatistics());
        newVdsInterface.setId(Guid.newGuid());
        newVdsInterface.setName("eth77");
        newVdsInterface.setNetworkName("enginet");
        newVdsInterface.setAddress("192.168.122.177");
        newVdsInterface.setSubnet("255.255.255.0");
        newVdsInterface.setSpeed(1000);
        newVdsInterface.setType(3);
        newVdsInterface.setBootProtocol(NetworkBootProtocol.STATIC_IP);
        newVdsInterface.setMacAddress("01:C0:81:21:71:17");
        newVdsInterface.setGateway("192.168.122.1");
        newVdsInterface.setMtu(1500);
        newVdsInterface.setQos(newQos);

        newVdsStatistics = newVdsInterface.getStatistics();
    }

    /**
     * Ensures an empty collection is returned.
     */
    @Test
    public void testGetAllInterfacesForVdsWithInvalidVds() {
        List<VdsNetworkInterface> result = dao.getAllInterfacesForVds(Guid.newGuid());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures the right set of interfaces are returned.
     */
    @Test
    public void testGetAllInterfacesForVds() {
        List<VdsNetworkInterface> result = dao.getAllInterfacesForVds(VDS_ID);

        assertGetAllForVdsCorrectResult(result);
    }

    /**
     * Ensures that saving an interface for a VDS works as expected.
     */
    @Test
    public void testSaveInterfaceForVds() {
        newVdsInterface.setVdsId(VDS_ID);

        dao.saveInterfaceForVds(newVdsInterface);
        dao.saveStatisticsForVds(newVdsStatistics);

        List<VdsNetworkInterface> result = dao.getAllInterfacesForVds(VDS_ID);
        boolean found = false;

        for (VdsNetworkInterface iface : result) {
            found |=
                    iface.getName().equals(newVdsInterface.getName())
                            && iface.getQos().equals(newVdsInterface.getQos());
        }

        assertTrue(found);
    }

    /**
     * Ensures that the specified VDS's interfaces are deleted.
     */
    @Test
    public void testRemoveInterfacesForVds() {
        List<VdsNetworkInterface> before = dao.getAllInterfacesForVds(VDS_ID);

        // ensure we have records before the test
        boolean found = false;
        for (VdsNetworkInterface iface : before) {
            found |= (FixturesTool.VDS_NETWORK_INTERFACE.equals(iface.getId()));
        }
        assertTrue(found);
        assertNotNull(dbFacade.getNetworkQosDao().get(FixturesTool.VDS_NETWORK_INTERFACE));

        dao.removeInterfaceFromVds(FixturesTool.VDS_NETWORK_INTERFACE);

        List<VdsNetworkInterface> after = dao.getAllInterfacesForVds(VDS_ID);

        for (VdsNetworkInterface iface : after) {
            assertNotSame(FixturesTool.VDS_NETWORK_INTERFACE, iface.getId());
        }
        assertNull(dbFacade.getNetworkQosDao().get(FixturesTool.VDS_NETWORK_INTERFACE));
    }

    /**
     * Ensures that all statistics are removed for the specified VDS.
     */
    @Test
    public void testRemoveStatisticsForVds() {
        List<VdsNetworkInterface> before = dao.getAllInterfacesForVds(VDS_ID);

        for (VdsNetworkInterface iface : before) {
            assertNotSame(0.0, iface.getStatistics().getTransmitRate());
            assertNotSame(0.0, iface.getStatistics().getReceiveRate());
            assertNotSame(0.0, iface.getStatistics().getReceiveDropRate());
            assertNotSame(0.0, iface.getStatistics().getReceiveDropRate());
        }
        dao.removeStatisticsForVds(FixturesTool.VDS_NETWORK_INTERFACE);

        List<VdsNetworkInterface> after = dao.getAllInterfacesForVds(VDS_ID);

        for (VdsNetworkInterface iface : after) {
            assertEquals(0.0, iface.getStatistics().getTransmitRate(), 0.0001);
            assertEquals(0.0, iface.getStatistics().getReceiveRate(), 0.0001);
            assertEquals(0.0, iface.getStatistics().getReceiveDropRate(), 0.0001);
            assertEquals(0.0, iface.getStatistics().getReceiveDropRate(), 0.0001);
        }
    }

    private void testUpdateInterface(Guid interface_id) {
        VdsNetworkInterface iface = dao.get(interface_id);

        iface.setName(iface.getName().toUpperCase());
        iface.setQos(newQos);

        dao.updateInterfaceForVds(iface);

        VdsNetworkInterface ifaced = dao.get(interface_id);
        assertEquals(iface.getName(), ifaced.getName());
        assertEquals(iface.getQos(), ifaced.getQos());
    }

    /**
     * Ensures updating an interface works, while also updating its previous QoS configuration.
     */
    @Test
    public void testUpdateInterfaceWithQos() {
        testUpdateInterface(FixturesTool.VDS_NETWORK_INTERFACE);
    }

    /**
     * Ensures updating an interface works, including a newly-reported QoS configuration.
     */
    @Test
    public void testUpdateInterfaceWithoutQos() {
        testUpdateInterface(FixturesTool.VDS_NETWORK_INTERFACE_WITHOUT_QOS);
    }

    /**
     * Ensures that updating statistics for an interface works as expected.
     */
    @Test
    public void testUpdateStatisticsForVds() {
        List<VdsNetworkInterface> before = dao.getAllInterfacesForVds(VDS_ID);
        VdsNetworkStatistics stats = before.get(0).getStatistics();

        stats.setReceiveDropRate(999.0);

        dao.updateStatisticsForVds(stats);

        List<VdsNetworkInterface> after = dao.getAllInterfacesForVds(VDS_ID);
        boolean found = false;

        for (VdsNetworkInterface ifaced : after) {
            if (ifaced.getStatistics().getId().equals(stats.getId())) {
                found = true;
                assertEquals(stats.getReceiveDropRate(), ifaced.getStatistics().getReceiveDropRate());
            }
        }

        if (!found)
            fail("Did not find statistics which is bad.");
    }

    @Test
    public void testMasshUpdateStatisticsForVds() throws Exception {
        List<VdsNetworkInterface> interfaces = dao.getAllInterfacesForVds(VDS_ID);
        List<VdsNetworkStatistics> statistics = new ArrayList<VdsNetworkStatistics>(interfaces.size());
        for (VdsNetworkInterface iface : interfaces) {
            VdsNetworkStatistics stats = iface.getStatistics();
            stats.setReceiveDropRate(RandomUtils.instance().nextInt() * 1.0);
            stats.setStatus(RandomUtils.instance().nextEnum(InterfaceStatus.class));
            statistics.add(stats);
        }

        dao.massUpdateStatisticsForVds(statistics);

        List<VdsNetworkInterface> after = dao.getAllInterfacesForVds(VDS_ID);
        for (VdsNetworkInterface iface : after) {
            boolean found = false;
            for (VdsNetworkStatistics stats : statistics) {
                if (iface.getId().equals(stats.getId())) {
                    found = true;
                    assertEquals(stats.getReceiveDropRate(), iface.getStatistics().getReceiveDropRate());
                    assertEquals(stats.getStatus(), iface.getStatistics().getStatus());
                }
            }
            assertTrue(found);
        }
    }

    /**
     * Asserts that the right collection containing network interfaces is returned for a privileged user with filtering enabled
     */
    @Test
    public void testGetAllInterfacesForVdsWithPermissionsForPriviligedUser() {
        List<VdsNetworkInterface> result = dao.getAllInterfacesForVds(VDS_ID, PRIVILEGED_USER_ID, true);
        assertGetAllForVdsCorrectResult(result);
    }

    /**
     * Asserts that an empty collection is returned for an non privileged user with filtering enabled
     */
    @Test
    public void testGetAllInterfacesForVdsWithPermissionsForUnpriviligedUser() {
        List<VdsNetworkInterface> result = dao.getAllInterfacesForVds(VDS_ID, UNPRIVILEGED_USER_ID, true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Asserts that the right collection containing network interfaces is returned for a non privileged user with filtering disabled
     */
    @Test
    public void testGetAllInterfacesForVdsWithPermissionsDisabledForUnpriviligedUser() {
        List<VdsNetworkInterface> result = dao.getAllInterfacesForVds(VDS_ID, UNPRIVILEGED_USER_ID, false);
        assertGetAllForVdsCorrectResult(result);
    }

    private void assertGetAllForVdsCorrectResult(List<VdsNetworkInterface> result) {
        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (VdsNetworkInterface iface : result) {
            assertEquals(VDS_ID, iface.getVdsId());
        }
    }

    /**
     * Asserts that a null result is returned for a non privileged user with filtering enabled
     */
    @Test
    public void testGetManagedInterfaceForVdsFilteredForUnpriviligedUser() {
        VdsNetworkInterface result = dao.getManagedInterfaceForVds(VDS_ID, UNPRIVILEGED_USER_ID, true);
        assertNull(result);
    }

    /**
     * Asserts that the management network interface of a VDS is returned for a privileged user with filtering enabled
     */
    @Test
    public void testGetManagedInterfaceForVdsFilteredForPriviligedUser() {
        VdsNetworkInterface result = dao.getManagedInterfaceForVds(VDS_ID, PRIVILEGED_USER_ID, true);
        assertCorrectGetManagedInterfaceForVdsResult(result);
    }

    /**
     * Asserts that the management network interface of a VDS is returned for a non privileged user with filtering disabled
     */
    @Test
    public void testGetManagedInterfaceForVdsFilteringDisabledForUnpriviligedUser() {
        VdsNetworkInterface result = dao.getManagedInterfaceForVds(VDS_ID, UNPRIVILEGED_USER_ID, false);
        assertCorrectGetManagedInterfaceForVdsResult(result);
    }

    /**
     * Ensures that get works as expected.
     */
    @Test
    public void testGet() {
        newVdsInterface.setVdsId(VDS_ID);
        dao.saveInterfaceForVds(newVdsInterface);
        dao.saveStatisticsForVds(newVdsInterface.getStatistics());
        VdsNetworkInterface result = dao.get(newVdsInterface.getId());
        assertEquals(newVdsInterface, result);
    }

    /**
     * Asserts that the correct VdsNetworkInterface is returned for the given network.
     */
    @Test
    public void testGetVdsInterfacesByNetworkId() throws Exception {
        List<VdsNetworkInterface> result = dao.getVdsInterfacesByNetworkId(FixturesTool.NETWORK_ENGINE);
        assertEquals(existingVdsInterface, result.get(0));
    }

    static private void assertCorrectGetManagedInterfaceForVdsResult(VdsNetworkInterface result) {
        assertNotNull(result);
        assertTrue(result.getIsManagement());
    }

    @Test
    public void testGetAllInterfacesWithIpAddress() {
        List<VdsNetworkInterface> interfaces = dao.getAllInterfacesWithIpAddress(CLUSTER_ID, IP_ADDR);
        assertNotNull(interfaces);
        assertEquals(1, interfaces.size());
        assertGetAllForVdsCorrectResult(interfaces);
    }

    @Test
    public void testgetAllInterfacesByClusterId() {
        List<VdsNetworkInterface> interfaces = dao.getAllInterfacesByClusterId(CLUSTER_ID);
        assertNotNull(interfaces);
        assertFalse(interfaces.isEmpty());
    }

    @Test
    public void testGetAllInterfacesByLabelForCluster() {
        List<VdsNetworkInterface> interfaces = dao.getAllInterfacesByLabelForCluster(CLUSTER_ID, LABEL);
        assertNotNull(interfaces);
        assertFalse(interfaces.isEmpty());

        for (VdsNetworkInterface nic : interfaces) {
            assertTrue(nic.getLabels().contains(LABEL));
        }
    }

    @Test
    public void testGetAllNetworkLabelsForDataCenter() {
        Set<String> result = dao.getAllNetworkLabelsForDataCenter(FixturesTool.DATA_CENTER);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGetHostNetworksByCluster() {
        Map<Guid, List<String>> map = dao.getHostNetworksByCluster(CLUSTER_ID);
        assertNotNull(map);
        assertFalse(map.isEmpty());
        assertNotNull(map.get(VDS_ID));
        assertFalse(map.get(VDS_ID).isEmpty());
    }

    public void testGetIscsiIfacesByHostIdAndStorageTargetId() {
        List<VdsNetworkInterface> interfaces =
                dao.getIscsiIfacesByHostIdAndStorageTargetId(VDS_ID, TARGET_ID);

        assertNotNull(interfaces);
        assertFalse(interfaces.isEmpty());

        for (VdsNetworkInterface nic : interfaces) {
            assertEquals(VDS_ID, nic.getVdsId());
        }
    }
}
