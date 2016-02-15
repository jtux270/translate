package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.ovirt.engine.api.model.StorageConnection;
import org.ovirt.engine.core.common.action.EditIscsiBondParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendIscsiBondStorageConnectionsResourceTest
        extends AbstractBackendCollectionResourceTest<StorageConnection, org.ovirt.engine.core.common.businessentities.StorageServerConnections, BackendStorageServerConnectionsResource> {

    protected static final Guid ISCSI_BOND_ID = GUIDS[1];
    protected static final Guid STORAGE_CONNECTION_ID = GUIDS[2];

    public BackendIscsiBondStorageConnectionsResourceTest() {
        super(new BackendIscsiBondStorageConnectionsResource(ISCSI_BOND_ID.toString()), null, "");
    }

    @Override
    protected List<StorageConnection> getCollection() {
        return collection.list().getStorageConnections();
    }

    @Override
    protected StorageServerConnections getEntity(int index) {
        StorageServerConnections cnx = new StorageServerConnections();
        cnx.setid(GUIDS[index].toString());
        cnx.setconnection("10.11.12.13" + ":" + "/1");
        return cnx;
    }

    protected StorageConnection getDummyStorageConnection() {
        StorageConnection cnx = new StorageConnection();
        cnx.setId(STORAGE_CONNECTION_ID.toString());
        return cnx;
    }

    @Test
    public void testAddStorageConnectionToIscsiBond() throws Exception {
        setUriInfo(setUpBasicUriExpectations());

        setUpGetEntityExpectations(VdcQueryType.GetIscsiBondById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { ISCSI_BOND_ID },
                getIscsiBond());

        setUpActionExpectations(VdcActionType.EditIscsiBond,
                EditIscsiBondParameters.class,
                new String[] { "IscsiBond" },
                new Object[] { getIscsiBondContainingStorageConnection() },
                true,
                true,
                null);

        Response response = collection.add(getDummyStorageConnection());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRemove() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        StorageConnection storageConnection = new StorageConnection();
        storageConnection.setId(STORAGE_CONNECTION_ID.toString());

        setUpGetEntityExpectations(VdcQueryType.GetIscsiBondById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { ISCSI_BOND_ID },
                getIscsiBondContainingStorageConnection());

        setUpActionExpectations(VdcActionType.EditIscsiBond,
                EditIscsiBondParameters.class,
                new String[] { "IscsiBond" },
                new Object[] { getIscsiBond() },
                true,
                true,
                null);

        Response response = collection.performRemove(storageConnection.getId());
        assertEquals(200, response.getStatus());
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetStorageServerConnectionByIscsiBondId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { ISCSI_BOND_ID },
                setUpStorageConnections(),
                failure);
        control.replay();
    }

    protected List<StorageServerConnections> setUpStorageConnections() {
        List<StorageServerConnections> storageConnections = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            storageConnections.add(getEntity(i));
        }
        return storageConnections;
    }

    /**
     * There is no name and description in StorageConnection, that is why the method is overridden
     */
    @Override
    protected void verifyModel(StorageConnection model, int index) {
        assertEquals(GUIDS[index].toString(), model.getId());
        verifyLinks(model);
    }

    private org.ovirt.engine.core.common.businessentities.IscsiBond getIscsiBond() {
        org.ovirt.engine.core.common.businessentities.IscsiBond iscsiBond =
                new org.ovirt.engine.core.common.businessentities.IscsiBond();
        iscsiBond.setId(ISCSI_BOND_ID);
        return iscsiBond;
    }

    private org.ovirt.engine.core.common.businessentities.IscsiBond getIscsiBondContainingStorageConnection() {
        org.ovirt.engine.core.common.businessentities.IscsiBond iscsiBond =
                new org.ovirt.engine.core.common.businessentities.IscsiBond();
        iscsiBond.setId(ISCSI_BOND_ID);
        iscsiBond.getStorageConnectionIds().add(STORAGE_CONNECTION_ID.toString());
        return iscsiBond;
    }

}
