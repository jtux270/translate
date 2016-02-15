package org.ovirt.engine.core.dao.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.network.InterfaceStatus;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkStatistics;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAOTestCase;

public class VmNetworkStatisticsDaoTest extends BaseDAOTestCase {
    private static final Guid INTERFACE_ID = new Guid("e2817b12-f873-4046-b0da-0098293c14fd");
    private static final Guid NEW_INTERFACE_ID = new Guid("14550e82-1e1f-47b5-ae41-b009348dabfa");
    private static final Guid VM_ID = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4355");

    private VmNetworkStatisticsDao dao;

    private VmNetworkStatistics newVmStatistics;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = dbFacade.getVmNetworkStatisticsDao();

        newVmStatistics = new VmNetworkStatistics();
        newVmStatistics.setId(NEW_INTERFACE_ID);
        newVmStatistics.setVmId(VM_ID);
        newVmStatistics.setStatus(InterfaceStatus.DOWN);
        newVmStatistics.setReceiveDropRate(0.0);
        newVmStatistics.setReceiveRate(0.0);
        newVmStatistics.setTransmitDropRate(0.0);
        newVmStatistics.setTransmitRate(0.0);
    }

    /**
     * Ensures null is returned.
     */
    @Test
    public void testGetWithNonExistingId() {
        VmNetworkStatistics result = dao.get(Guid.newGuid());

        assertNull(result);
    }

    /**
     * Ensures that the network interface statistics entity is returned.
     */
    @Test
    public void testGet() {
        VmNetworkStatistics result = dao.get(INTERFACE_ID);

        assertNotNull(result);
        assertEquals(INTERFACE_ID, result.getId());
    }

    /**
     * Ensures that saving an interface for a VM works as expected.
     */
    @Test
    public void testSave() {
        dao.save(newVmStatistics);

        VmNetworkStatistics savedStatistics = dao.get(NEW_INTERFACE_ID);

        assertNotNull(savedStatistics);
        assertEquals(newVmStatistics.getStatus(), savedStatistics.getStatus());
    }

    /**
     * Ensures that updating statistics for an interface works as expected.
     */
    @Test
    public void testUpdate() {
        List<VmNetworkInterface> before = dbFacade.getVmNetworkInterfaceDao().getAllForVm(VM_ID);
        VmNetworkStatistics stats = before.get(0).getStatistics();

        stats.setReceiveDropRate(999.0);

        dao.update(stats);

        List<VmNetworkInterface> after = dbFacade.getVmNetworkInterfaceDao().getAllForVm(VM_ID);
        boolean found = false;

        for (VmNetworkInterface ifaced : after) {
            if (ifaced.getStatistics().getId().equals(stats.getId())) {
                found = true;
                assertEquals(stats.getReceiveDropRate(), ifaced.getStatistics().getReceiveDropRate());
            }
        }

        if (!found)
            fail("Did not find statistics which is bad.");
    }

    /**
     * Ensures that the specified VM's interfaces are deleted.
     */
    @Test
    public void testRemove() {
        assertNotNull(dao.get(INTERFACE_ID));

        dao.remove(INTERFACE_ID);

        assertNull(dao.get(INTERFACE_ID));
    }

    @Test(expected = NotImplementedException.class)
    public void testGetAll() throws Exception {
        dao.getAll();
    }

    @Test
    public void testUpdateAll() throws Exception {
        VmNetworkStatistics existingStats = dao.get(INTERFACE_ID);
        VmNetworkStatistics existingStats2 = dao.get(new Guid("e2817b12-f873-4046-b0da-0098293c14fe"));
        existingStats.setReceiveDropRate(10.0);
        existingStats2.setStatus(InterfaceStatus.DOWN);

        dao.updateAll(Arrays.asList(new VmNetworkStatistics[] { existingStats, existingStats2 }));

        assertEquals(existingStats.getReceiveDropRate(), dao.get(existingStats.getId()).getReceiveDropRate());
        assertEquals(existingStats2.getStatus(), dao.get(existingStats2.getId()).getStatus());
    }
}
