package org.ovirt.engine.core.dao.gluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServiceStatus;
import org.ovirt.engine.core.common.businessentities.gluster.ServiceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAOTestCase;

/**
 * Tests for Services DAO
 */
public class GlusterServerServiceDaoTest extends BaseDAOTestCase {
    private static final Guid NEW_SERVICE_ID = new Guid("d1745ef8-8369-43e5-b55a-b4fceea63877");
    private static final Guid SERVER1_ID = new Guid("23f6d691-5dfb-472b-86dc-9e1d2d3c18f3");
    private static final Guid SERVER2_ID = new Guid("2001751e-549b-4e7a-aff6-32d36856c125");
    private static final Guid CLUSTER_ID = new Guid("ae956031-6be2-43d6-bb8f-5191c9253314");
    private static final Guid SERVICE1_ID = new Guid("c83c9ee3-b7d8-4709-ae4b-5d86a152e6b1");
    private static final String SERVICE1_NAME = "gluster-swift-test-1";
    private static final String VDS_NAME1 = "gluster-server1";
    private static final ServiceType SERVICE1_TYPE = ServiceType.GLUSTER_SWIFT;
    private static final Guid SERVICE2_ID = new Guid("fc00df54-4fcd-4495-8756-b217780bdad7");
    private static final String SERVICE2_NAME = "gluster-test";
    private static final ServiceType SERVICE2_TYPE = ServiceType.GLUSTER;
    private static final Guid SERVER1_SERVICE_ID = new Guid("c77b4d6a-a2c9-4c9f-a873-3dbff8a34720");
    private static final String VDS_NAME2 = "gluster-server2";

    private static final Integer PID_1 = 11111;
    private static final Integer PID_2 = 22222;
    private static final Integer NEW_PID = 33333;
    private GlusterServerServiceDao dao;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dao = dbFacade.getGlusterServerServiceDao();
    }

    @Test
    public void testGetByClusterId() {
        List<GlusterServerService> services = dao.getByClusterId(CLUSTER_ID);
        assertNotNull(services);
        assertEquals(2, services.size());
        for (GlusterServerService service : services) {
            Guid serverId = service.getServerId();
            if (SERVER1_ID.equals(serverId)) {
                verifyServiceOnServer1(service);
            } else if (SERVER2_ID.equals(serverId)) {
                verifyServiceOnServer2(service);
            } else {
                fail("Unexpected server id: " + service.getServerId());
            }
        }
    }

    @Test
    public void testGetByServerId() {
        List<GlusterServerService> services = dao.getByServerId(SERVER1_ID);
        assertNotNull(services);
        assertEquals(1, services.size());
        verifyServiceOnServer1(services.get(0));
    }

    @Test
    public void testGetByClusterIdAndServiceType() {
        List<GlusterServerService> services = dao.getByClusterIdAndServiceType(CLUSTER_ID, SERVICE1_TYPE);
        assertNotNull(services);
        assertEquals(1, services.size());
        verifyServiceOnServer1(services.get(0));
    }

    @Test
    public void testGetByServerIdAndServiceType() {
        List<GlusterServerService> services = dao.getByServerIdAndServiceType(SERVER1_ID, SERVICE1_TYPE);
        assertNotNull(services);
        assertEquals(1, services.size());
        verifyServiceOnServer1(services.get(0));
    }

    @Test
    public void testGetAllWithQuery() {
        List<GlusterServerService> services =
                dao.getAllWithQuery("select * from gluster_server_services_view where status = '"
                        + GlusterServiceStatus.STOPPED.name() + "'");

        assertTrue(services != null);
        assertTrue(services.size() == 1);
        verifyServiceOnServer2(services.get(0));
    }

    @Test
    public void testSaveAndGet() {
        GlusterServerService service = dao.get(NEW_SERVICE_ID);
        assertNull(service);

        GlusterServerService newService = insertTestService();
        service = dao.get(NEW_SERVICE_ID);

        assertNotNull(service);
        assertEquals(newService, service);
    }

    @Test
    public void testUpdate() {
        GlusterServerService existingService = dao.get(SERVER1_SERVICE_ID);
        assertNotNull(existingService);
        assertEquals(GlusterServiceStatus.RUNNING, existingService.getStatus());

        GlusterServerService serviceToModify = dao.get(SERVER1_SERVICE_ID);
        serviceToModify.setStatus(GlusterServiceStatus.STOPPED);
        dao.update(serviceToModify);

        GlusterServerService modifiedService = dao.get(SERVER1_SERVICE_ID);
        assertNotNull(modifiedService);
        assertEquals(GlusterServiceStatus.STOPPED, modifiedService.getStatus());
        assertEquals(GlusterServiceStatus.RUNNING, existingService.getStatus());
        assertFalse(existingService.equals(modifiedService));
    }

    @Test
    public void testUpdateByServerIdAndServiceType() {
        GlusterServerService existingService = dao.get(SERVER1_SERVICE_ID);
        assertNotNull(existingService);
        assertEquals(GlusterServiceStatus.RUNNING, existingService.getStatus());

        GlusterServerService serviceToModify = dao.get(SERVER1_SERVICE_ID);
        serviceToModify.setStatus(GlusterServiceStatus.STOPPED);
        dao.updateByServerIdAndServiceType(serviceToModify);

        GlusterServerService modifiedService = dao.get(SERVER1_SERVICE_ID);
        assertNotNull(modifiedService);
        assertEquals(GlusterServiceStatus.STOPPED, modifiedService.getStatus());
        assertEquals(GlusterServiceStatus.RUNNING, existingService.getStatus());
        assertFalse(existingService.equals(modifiedService));
    }

    private GlusterServerService insertTestService() {
        GlusterServerService service = new GlusterServerService();
        service.setId(NEW_SERVICE_ID);
        service.setServerId(SERVER1_ID);
        service.setServiceId(SERVICE2_ID);
        service.setServiceType(SERVICE2_TYPE);
        service.setStatus(GlusterServiceStatus.UNKNOWN);
        service.setMessage("test message");
        service.setPid(NEW_PID);
        service.setHostName(VDS_NAME1);

        dao.save(service);
        return service;
    }

    private void verifyServiceOnServer2(GlusterServerService service) {
        assertEquals(SERVICE2_ID, service.getServiceId());
        assertEquals(SERVICE2_NAME, service.getServiceName());
        assertEquals(SERVICE2_TYPE, service.getServiceType());
        assertEquals(PID_2, service.getPid());
        assertEquals(GlusterServiceStatus.STOPPED, service.getStatus());
        assertEquals(VDS_NAME2, service.getHostName());
    }

    private void verifyServiceOnServer1(GlusterServerService service) {
        assertEquals(SERVICE1_ID, service.getServiceId());
        assertEquals(SERVICE1_NAME, service.getServiceName());
        assertEquals(SERVICE1_TYPE, service.getServiceType());
        assertEquals(PID_1, service.getPid());
        assertEquals(GlusterServiceStatus.RUNNING, service.getStatus());
        assertEquals(VDS_NAME1, service.getHostName());
    }
}
