package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.ovirt.engine.api.model.IscsiBond;
import org.ovirt.engine.core.common.action.AddIscsiBondParameters;
import org.ovirt.engine.core.common.action.RemoveIscsiBondParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendIscsiBondsResourceTest extends AbstractBackendCollectionResourceTest<IscsiBond, org.ovirt.engine.core.common.businessentities.IscsiBond, BackendIscsiBondsResource> {
    protected static final Guid ISCSI_BOND_ID = GUIDS[1];
    protected static final Guid DATA_CENTER_ID = GUIDS[2];
    static Guid PARENT_GUID = GUIDS[2];

    public BackendIscsiBondsResourceTest() {
        super(new BackendIscsiBondsResource(DATA_CENTER_ID.toString()), null, "");
    }

    @Override
    protected List<IscsiBond> getCollection() {
        return collection.list().getIscsiBonds();
    }

    @Override
    protected org.ovirt.engine.core.common.businessentities.IscsiBond getEntity(int index) {
        org.ovirt.engine.core.common.businessentities.IscsiBond iscsiBond =
                new org.ovirt.engine.core.common.businessentities.IscsiBond();
        iscsiBond.setId(GUIDS[index]);
        iscsiBond.setStoragePoolId(DATA_CENTER_ID);
        iscsiBond.setName(NAMES[1]);
        iscsiBond.setDescription(DESCRIPTIONS[1]);
        return iscsiBond;
    }

    @Test
    public void testAddIscsiBond() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpCreationExpectations(VdcActionType.AddIscsiBond,
                AddIscsiBondParameters.class,
                new String[] { "IscsiBond" },
                new Object[] { getIscsiBond() },
                true,
                true,
                getIscsiBond().getId(),
                VdcQueryType.GetIscsiBondById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { ISCSI_BOND_ID },
                getEntity(1));

        Response response = collection.add(getIscsiBondApi());
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof IscsiBond);
        verifyModel((IscsiBond) response.getEntity(), 1);
    }

    private org.ovirt.engine.core.common.businessentities.IscsiBond getIscsiBond() {
        org.ovirt.engine.core.common.businessentities.IscsiBond iscsiBond =
                new org.ovirt.engine.core.common.businessentities.IscsiBond();
        iscsiBond.setId(ISCSI_BOND_ID);
        iscsiBond.setStoragePoolId(DATA_CENTER_ID);
        iscsiBond.setName(NAMES[0]);
        return iscsiBond;
    }

    private IscsiBond getIscsiBondApi() {
        IscsiBond iscsiBond = new IscsiBond();
        iscsiBond.setId(ISCSI_BOND_ID.toString());
        iscsiBond.setName(NAMES[0]);
        return iscsiBond;
    }

    protected void setUpEntityQueryExpectations(int times, Object failure) throws Exception {
        while (times-- > 0) {
            setUpEntityQueryExpectations(VdcQueryType.GetIscsiBondsByStoragePoolId,
                    IdQueryParameters.class,
                    new String[] { "Id" },
                    new Object[] { DATA_CENTER_ID },
                    setUpIscsiBonds(),
                    failure);
        }
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetIscsiBondsByStoragePoolId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { DATA_CENTER_ID },
                setUpIscsiBonds(),
                failure);
        control.replay();
    }

    static List<org.ovirt.engine.core.common.businessentities.IscsiBond> setUpIscsiBonds() {
        List<org.ovirt.engine.core.common.businessentities.IscsiBond> iscsiBonds = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            org.ovirt.engine.core.common.businessentities.IscsiBond iscsiBond =
                    new org.ovirt.engine.core.common.businessentities.IscsiBond();
            iscsiBond.setDescription(DESCRIPTIONS[i]);
            iscsiBond.setName(NAMES[i]);
            iscsiBond.setId(GUIDS[i]);
            iscsiBond.setStoragePoolId(DATA_CENTER_ID);
            iscsiBonds.add(iscsiBond);
        }
        return iscsiBonds;
    }

    @Test
    public void testRemove() throws Exception {
        setUpGetEntityExcpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveIscsiBond,
                RemoveIscsiBondParameters.class,
                new String[] { "IscsiBondId" },
                new Object[] { GUIDS[0] },
                true,
                true));
        verifyRemove(collection.remove(GUIDS[0].toString()));
    }

    @Test
    public void testRemoveNonExistant() throws Exception {
        setUpGetEntityExpectations(VdcQueryType.GetIscsiBondById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { NON_EXISTANT_GUID },
                null);
        control.replay();
        try {
            collection.remove(NON_EXISTANT_GUID.toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(404, wae.getResponse().getStatus());
        }
    }

    private void setUpGetEntityExcpectations() throws Exception {
        setUpGetEntityExpectations(VdcQueryType.GetIscsiBondById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                getEntity(0));
    }
}
