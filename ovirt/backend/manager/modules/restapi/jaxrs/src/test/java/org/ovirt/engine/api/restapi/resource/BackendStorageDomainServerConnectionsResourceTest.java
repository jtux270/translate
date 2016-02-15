package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.ovirt.engine.api.model.Host;
import org.ovirt.engine.api.model.StorageConnection;
import org.ovirt.engine.core.common.action.AttachDetachStorageConnectionParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.queries.StorageServerConnectionQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class BackendStorageDomainServerConnectionsResourceTest extends AbstractBackendCollectionResourceTest<StorageConnection, StorageServerConnections, BackendStorageDomainServerConnectionsResource> {
    protected static final org.ovirt.engine.core.common.businessentities.StorageType STORAGE_TYPES_MAPPED[] = {
            org.ovirt.engine.core.common.businessentities.StorageType.NFS,
            org.ovirt.engine.core.common.businessentities.StorageType.LOCALFS,
            org.ovirt.engine.core.common.businessentities.StorageType.POSIXFS,
            org.ovirt.engine.core.common.businessentities.StorageType.ISCSI };

    public BackendStorageDomainServerConnectionsResourceTest() {
        super(new BackendStorageDomainServerConnectionsResource(GUIDS[3]), null, "");
    }

    @Test
    @Ignore
    @Override
    public void testQuery() throws Exception {
    }

    @Test
    public void testAttachSuccess() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpActionExpectations(VdcActionType.AttachStorageConnectionToStorageDomain,
                AttachDetachStorageConnectionParameters.class,
                new String[] { },
                new Object[] { },
                true,
                true);
        StorageConnection connection = new StorageConnection();
        connection.setId(GUIDS[3].toString());
        Response response = collection.add(connection);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testAttachFailure() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpActionExpectations(VdcActionType.AttachStorageConnectionToStorageDomain,
                AttachDetachStorageConnectionParameters.class,
                new String[] { },
                new Object[] { },
                false,
                false);
        StorageConnection connection = new StorageConnection();
        connection.setId(GUIDS[3].toString());
       try {
            Response response = collection.add(connection);
        } catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(400, wae.getResponse().getStatus());
        }
    }

    @Test
    public void testDetachSuccess() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations();
        setUpActionExpectations(VdcActionType.DetachStorageConnectionFromStorageDomain,
                AttachDetachStorageConnectionParameters.class,
                new String[] {},
                new Object[] {},
                true,
                true);
        Response response = collection.remove(GUIDS[3].toString());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDetachFailure() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations();
        setUpActionExpectations(VdcActionType.DetachStorageConnectionFromStorageDomain,
                AttachDetachStorageConnectionParameters.class,
                new String[] {},
                new Object[] {},
                false,
                false);
        try {
            Response response = collection.remove(GUIDS[3].toString());
        } catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(400, wae.getResponse().getStatus());
        }
    }

    @Override
    protected List<StorageConnection> getCollection() {
        return collection.list().getStorageConnections();
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        assertEquals("", query);

        setUpEntityQueryExpectations(VdcQueryType.GetStorageServerConnectionsForDomain,
                VdcQueryParametersBase.class,
                new String[] {},
                new Object[] {},
                setUpStorageConnections(),
                failure);

        control.replay();
    }

    protected List<StorageServerConnections> setUpStorageConnections() {
        List<StorageServerConnections> storageConnections = new ArrayList<>();
        storageConnections.add(getEntity(3));
        return storageConnections;
    }

    @Override
    protected void verifyCollection(List<StorageConnection> collection) throws Exception {
        assertNotNull(collection);
        assertEquals(1, collection.size());
    }

    @Override
    protected StorageServerConnections getEntity(int index) {
        return setUpEntityExpectations(control.createMock(StorageServerConnections.class), index);
    }

    static StorageServerConnections setUpEntityExpectations(StorageServerConnections entity, int index) {
        expect(entity.getid()).andReturn(GUIDS[index].toString()).anyTimes();
        expect(entity.getstorage_type()).andReturn(STORAGE_TYPES_MAPPED[index]).anyTimes();
        expect(entity.getconnection()).andReturn("1.1.1.255").anyTimes();
        if (STORAGE_TYPES_MAPPED[index].equals(StorageType.ISCSI)) {
            expect(entity.getport()).andReturn("3260").anyTimes();
        }

        return entity;
    }

     StorageConnection getModel(int index) {
        StorageConnection model = new StorageConnection();
        model.setType(STORAGE_TYPES_MAPPED[index].toString());
        if ( index == 0 || index == 3 ) {
            model.setAddress("1.1.1.1");
        }
        Host host = new Host();
        host.setId(GUIDS[1].toString());
        model.setHost(host);
        if (index == 0 || index == 1) {
            model.setPath("/data1");
        }
        return model;
    }

    private void setUpGetEntityExpectations() throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetStorageServerConnectionById,
                StorageServerConnectionQueryParametersBase.class,
                new String[] { "ServerConnectionId" },
                new Object[] { GUIDS[3].toString() },
                getEntity(3));
    }
}
