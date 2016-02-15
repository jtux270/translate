package org.ovirt.engine.core.dao.gluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServer;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAOTestCase;
import org.ovirt.engine.core.dao.FixturesTool;


public class GlusterServerDaoTest extends BaseDAOTestCase {

    private static final Guid SERVER_ID1 = new Guid("23f6d691-5dfb-472b-86dc-9e1d2d3c18f3");
    private static final Guid SERVER_ID2 = new Guid("2001751e-549b-4e7a-aff6-32d36856c125");

    private GlusterServerDao dao;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dao = dbFacade.getGlusterServerDao();
    }

    @Test
    public void testSave() {
        GlusterServer newEntity = new GlusterServer();
        newEntity.setId(SERVER_ID2);
        newEntity.setGlusterServerUuid(FixturesTool.GLUSTER_SERVER_UUID2);

        dao.save(newEntity);
        GlusterServer entity = dao.getByServerId(newEntity.getId());
        assertEquals(newEntity, entity);
    }

    @Test
    public void testGetById() {
        GlusterServer entity = dao.getByServerId(SERVER_ID1);
        assertNotNull(entity);
        assertEquals(SERVER_ID1, entity.getId());
        assertEquals(FixturesTool.GLUSTER_SERVER_UUID1, entity.getGlusterServerUuid());
    }

    @Test
    public void testGetByGlusterServerUuid() {
        GlusterServer entity = dao.getByGlusterServerUuid(FixturesTool.GLUSTER_SERVER_UUID1);
        assertNotNull(entity);
        assertEquals(SERVER_ID1, entity.getId());
        assertEquals(FixturesTool.GLUSTER_SERVER_UUID1, entity.getGlusterServerUuid());
    }

    @Test
    public void testRemove() {
        dao.remove(SERVER_ID1);
        GlusterServer entity = dao.getByServerId(SERVER_ID1);
        assertNull(entity);
    }

    @Test
    public void testRemoveByGlusterServerUuid() {
        dao.removeByGlusterServerUuid(FixturesTool.GLUSTER_SERVER_UUID1);
        GlusterServer entity = dao.getByGlusterServerUuid(FixturesTool.GLUSTER_SERVER_UUID1);
        assertNull(entity);
    }

    @Test
    public void testUpdateGlusterServerUuid() {
        GlusterServer entityToUpdate = new GlusterServer(SERVER_ID1, FixturesTool.GLUSTER_SERVER_UUID_NEW);
        dao.update(entityToUpdate);
        GlusterServer entity = dao.getByServerId(SERVER_ID1);
        assertNotNull(entity);
        assertEquals(FixturesTool.GLUSTER_SERVER_UUID_NEW, entity.getGlusterServerUuid());
    }
}
