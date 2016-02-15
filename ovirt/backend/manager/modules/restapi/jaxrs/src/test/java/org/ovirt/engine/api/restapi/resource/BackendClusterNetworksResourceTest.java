package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.ovirt.engine.api.model.Network;
import org.ovirt.engine.core.common.action.AttachNetworkToVdsGroupParameter;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendClusterNetworksResourceTest extends AbstractBackendNetworksResourceTest<BackendClusterNetworksResource> {

    static final Guid CLUSTER_ID = GUIDS[1];

    public BackendClusterNetworksResourceTest() {
        super(new BackendClusterNetworksResource(CLUSTER_ID.toString()));
    }

    @Test
    public void testRemoveNotFound() throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetAllNetworksByClusterId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { CLUSTER_ID },
                                     new ArrayList<org.ovirt.engine.core.common.businessentities.network.Network>());
        control.replay();
        try {
            collection.remove(GUIDS[0].toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testRemove() throws Exception {
        setUpVDSGroupExpectations(CLUSTER_ID);

        setUpEntityQueryExpectations(2);

        setUriInfo(setUpActionExpectations(VdcActionType.DetachNetworkToVdsGroup,
                                           AttachNetworkToVdsGroupParameter.class,
                                           new String[] { "VdsGroupId" },
                                           new Object[] { CLUSTER_ID },
                                           true,
                                           true));
        verifyRemove(collection.remove(GUIDS[0].toString()));
    }


    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    protected void doTestBadRemove(boolean canDo, boolean success, String detail) throws Exception {
        setUpVDSGroupExpectations(CLUSTER_ID);

        setUpEntityQueryExpectations(2);

        setUriInfo(setUpActionExpectations(VdcActionType.DetachNetworkToVdsGroup,
                                           AttachNetworkToVdsGroupParameter.class,
                                           new String[] { "VdsGroupId" },
                                           new Object[] { CLUSTER_ID },
                                           canDo,
                                           success));
        try {
            collection.remove(GUIDS[0].toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddNetwork() throws Exception {
        setUpVDSGroupExpectations(CLUSTER_ID);

        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(1, null);
        setUpGetClusterExpectations(1, null);
        setUpGetNetworksByDataCenterExpectations(1, null);
        setUpActionExpectations(VdcActionType.AttachNetworkToVdsGroup,
                AttachNetworkToVdsGroupParameter.class,
                new String[] { "VdsGroupId" },
                new Object[] { CLUSTER_ID },
                true,
                true);
        Network model = getModel(0);
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Network);
        verifyModel((Network) response.getEntity(), 0);
    }

    @Test
    public void testAddNetworkCantDo() throws Exception {
        doTestBadAddNetwork(false, true, CANT_DO);
    }

    @Test
    public void testAddNetworkFailure() throws Exception {
        doTestBadAddNetwork(true, false, FAILURE);
    }

    private void doTestBadAddNetwork(boolean canDo, boolean success, String detail) throws Exception {
        setUpVDSGroupExpectations(CLUSTER_ID);

        setUriInfo(setUpBasicUriExpectations());
        setUpGetClusterExpectations(1, null);
        setUpGetNetworksByDataCenterExpectations(1, null);
        setUpActionExpectations(VdcActionType.AttachNetworkToVdsGroup,
                AttachNetworkToVdsGroupParameter.class,
                new String[] { "VdsGroupId" },
                new Object[] { CLUSTER_ID },
                canDo,
                success);
        Network model = getModel(0);

        try {
            collection.add(model);
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddNameSuppliedButNoId() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        Network model = new Network();
        model.setName("orcus");
        model.setDescription(DESCRIPTIONS[0]);
        setUpEntityQueryExpectations(1, null);
        setUpGetClusterExpectations(1, null);
        setUpGetNetworksByDataCenterExpectations(1, null);
        setUpVDSGroupExpectations(CLUSTER_ID);
        setUpActionExpectations(VdcActionType.AttachNetworkToVdsGroup,
                AttachNetworkToVdsGroupParameter.class,
                new String[] { "VdsGroupId" },
                new Object[] { CLUSTER_ID },
                true,
                true);
        collection.add(model);
    }

    @Test
    public void testAddIdSuppliedButNoName() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        Network model = new Network();
        model.setId("11111111-1111-1111-1111-111111111111");
        setUpEntityQueryExpectations(1, null);
        setUpGetClusterExpectations(1, null);
        setUpGetNetworksByDataCenterExpectations(1, null);
        setUpVDSGroupExpectations(CLUSTER_ID);
        setUpActionExpectations(VdcActionType.AttachNetworkToVdsGroup,
                AttachNetworkToVdsGroupParameter.class,
                new String[] { "VdsGroupId" },
                new Object[] { CLUSTER_ID },
                true,
                true);
        collection.add(model);
    }

    @Test
    public void testAddIncompleteParameters_noName() throws Exception {
        Network model = new Network();
        model.setDescription(DESCRIPTIONS[0]);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "Network", "add", "id|name");
        }
    }

    @Override
    protected void setUpEntityQueryExpectations(int times, Object failure) throws Exception {
        while (times-- > 0) {
            setUpEntityQueryExpectations(VdcQueryType.GetAllNetworksByClusterId,
                                         IdQueryParameters.class,
                                         new String[] { "Id" },
                                         new Object[] { CLUSTER_ID },
                                         getEntityList(),
                                         failure);
        }
    }

    protected VDSGroup setUpVDSGroupExpectations(Guid id) {
        VDSGroup group = control.createMock(VDSGroup.class);
        expect(group.getId()).andReturn(id).anyTimes();

        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { id },
                                     group);
        return group;
    }

    protected void setUpGetNetworksByDataCenterExpectations(int times, Object failure) throws Exception {
        while (times-- > 0) {
            setUpEntityQueryExpectations(VdcQueryType.GetAllNetworks,
                                         IdQueryParameters.class,
                                         new String[] { "Id" },
                                         new Object[] { GUIDS[2] },
                                         getEntityList(),
                                         failure);
        }
    }

    protected void setUpGetClusterExpectations(int times, Object failure) {
        while (times-- > 0) {
            VDSGroup cluster = new VDSGroup();
            cluster.setStoragePoolId(GUIDS[2]);
            setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupById,
                    IdQueryParameters.class,
                    new String[] { "Id" },
                    new Object[] { CLUSTER_ID },
                    cluster,
                    null);
        }
    }

    @Override
    protected void setUpQueryExpectations(String query) throws Exception {
        setUpEntityQueryExpectations(1);
        control.replay();
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        setUpEntityQueryExpectations(1, failure);
        control.replay();
    }
}
