package org.ovirt.engine.core.dao.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.network.VnicProfile;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAOTestCase;
import org.ovirt.engine.core.dao.FixturesTool;


public class VnicProfileDaoTest extends BaseDAOTestCase {

    private VnicProfile vnicProfile;
    private VnicProfileDao dao;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = dbFacade.getVnicProfileDao();
        vnicProfile = new VnicProfile();
        vnicProfile.setId(Guid.newGuid());
        vnicProfile.setName("new_profile");
        vnicProfile.setNetworkId(FixturesTool.NETWORK_ENGINE);
        vnicProfile.setNetworkQosId(FixturesTool.NETWORK_QOS);
        vnicProfile.setPortMirroring(false);
    }

    /**
     * Ensures null is returned.
     */
    @Test
    public void testGetWithNonExistingId() {
        VnicProfile result = dao.get(Guid.newGuid());

        assertNull(result);
    }

    /**
     * Ensures that the network interface profile is returned.
     */
    @Test
    public void testGet() {
        VnicProfile result = dao.get(FixturesTool.VM_NETWORK_INTERFACE_PROFILE);

        assertNotNull(result);
        assertEquals(FixturesTool.VM_NETWORK_INTERFACE_PROFILE, result.getId());
        assertEquals(false, result.isPortMirroring());
    }

    /**
     * Ensures that the network interface profile is returned.
     */
    @Test
    public void testGetWithPm() {
        VnicProfile result = dao.get(FixturesTool.VM_NETWORK_INTERFACE_PM_PROFILE);

        assertNotNull(result);
        assertEquals(FixturesTool.VM_NETWORK_INTERFACE_PM_PROFILE, result.getId());
        assertEquals(true, result.isPortMirroring());
    }

    /**
     * Ensures that an empty collection is returned.
     */
    @Test
    public void testGetAllForNetworkEmpty() {
        List<VnicProfile> result = dao.getAllForNetwork(Guid.newGuid());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures that profiles are returned.
     */
    @Test
    public void testGetAllForNetworkFull() {
        List<VnicProfile> result = dao.getAllForNetwork(FixturesTool.NETWORK_ENGINE);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    /**
     * Ensures that a single profile is returned.
     */
    @Test
    public void testGetAllForNetwork() {
        List<VnicProfile> result = dao.getAllForNetwork(FixturesTool.NETWORK_ENGINE);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    /**
     * Ensures that an empty collection is returned.
     */
    @Test
    public void testGetAll() {
        List<VnicProfile> result = dao.getAll();

        assertNotNull(result);
        assertEquals(5, result.size());
    }

    /**
     * Ensures that the save is working correctly
     */
    @Test
    public void testSave() {
        dao.save(vnicProfile);
        VnicProfile result = dao.get(vnicProfile.getId());
        assertNotNull(result);
        assertEquals(vnicProfile.getId(), result.getId());
        assertEquals(false, result.isPortMirroring());
    }

    /**
     * Ensures that the update is working correctly
     */
    @Test
    public void testUpdate() {
        dao.save(vnicProfile);
        vnicProfile.setPortMirroring(true);
        dao.update(vnicProfile);
        VnicProfile result = dao.get(vnicProfile.getId());
        assertNotNull(result);
        assertEquals(vnicProfile.getId(), result.getId());
        assertEquals(true, result.isPortMirroring());
    }

    /**
     * Ensures that the remove is working correctly
     */
    @Test
    public void testRemove() {
        dao.save(vnicProfile);
        VnicProfile result = dao.get(vnicProfile.getId());
        assertNotNull(result);
        dao.remove(vnicProfile.getId());
        assertNull(dao.get(vnicProfile.getId()));
    }

}
