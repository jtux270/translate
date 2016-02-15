package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;
import static org.ovirt.engine.api.restapi.resource.BackendStorageDomainsResourceTest.getModel;
import static org.ovirt.engine.api.restapi.resource.BackendStorageDomainsResourceTest.setUpEntityExpectations;
import static org.ovirt.engine.api.restapi.resource.BackendStorageDomainsResourceTest.setUpStorageServerConnection;
import static org.ovirt.engine.api.restapi.resource.BackendStorageDomainsResourceTest.verifyModelSpecific;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Host;
import org.ovirt.engine.api.model.LogicalUnit;
import org.ovirt.engine.api.model.LogicalUnits;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageDomainType;
import org.ovirt.engine.api.model.StorageType;
import org.ovirt.engine.core.common.action.ExtendSANStorageDomainParameters;
import org.ovirt.engine.core.common.action.RemoveStorageDomainParameters;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.action.StorageDomainParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.queries.GetLunsByVgIdParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.StorageServerConnectionQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendStorageDomainResourceTest
        extends AbstractBackendSubResourceTest<StorageDomain, org.ovirt.engine.core.common.businessentities.StorageDomain, BackendStorageDomainResource> {

    public BackendStorageDomainResourceTest() {
        super(new BackendStorageDomainResource(GUIDS[0].toString(), new BackendStorageDomainsResource()));
    }

    @Override
    protected void init() {
        super.init();
        initResource(resource.getParent());
    }

    @Test
    public void testBadGuid() throws Exception {
        control.replay();
        try {
            new BackendStorageDomainResource("foo", null);
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testGetNotFound() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(1, true, getEntity(0));
        control.replay();
        try {
            resource.get();
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testGet() throws Exception {
        setUpGetEntityExpectations(1, getEntity(0));
        setUpGetStorageServerConnectionExpectations(1);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();

        verifyModel(resource.get(), 0);
    }

    @Test
    public void testGetFcp() throws Exception {
        setUpGetEntityExpectations(1, getFcpEntity());
        setUpGetEntityExpectations(VdcQueryType.GetLunsByVgId,
                GetLunsByVgIdParameters.class,
                new String[] { "VgId" },
                new Object[] { GUIDS[0].toString() },
                setUpLuns());
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        verifyGetFcp(resource.get());
    }

    private void verifyGetFcp(StorageDomain model) {
        assertEquals(GUIDS[0].toString(), model.getId());
        assertEquals(NAMES[0], model.getName());
        assertEquals(StorageDomainType.DATA.value(), model.getType());
        assertNotNull(model.getStorage());
        assertEquals(StorageType.FCP.value(), model.getStorage().getType());
        assertNotNull(model.getLinks().get(0).getHref());
    }

    protected List<LUNs> setUpLuns() {
        LUNs lun = new LUNs();
        lun.setLUN_id(GUIDS[2].toString());
        List<LUNs> luns = new ArrayList<LUNs>();
        luns.add(lun);
        return luns;
    }

    private org.ovirt.engine.core.common.businessentities.StorageDomain getFcpEntity() {
        org.ovirt.engine.core.common.businessentities.StorageDomain entity = control.createMock(org.ovirt.engine.core.common.businessentities.StorageDomain.class);
        expect(entity.getId()).andReturn(GUIDS[0]).anyTimes();
        expect(entity.getStorageName()).andReturn(NAMES[0]).anyTimes();
        expect(entity.getStorageDomainType()).andReturn(org.ovirt.engine.core.common.businessentities.StorageDomainType.Data).anyTimes();
        expect(entity.getStorageType()).andReturn(org.ovirt.engine.core.common.businessentities.storage.StorageType.FCP).anyTimes();
        expect(entity.getStorage()).andReturn(GUIDS[0].toString()).anyTimes();
        return entity;
    }

    private org.ovirt.engine.core.common.businessentities.StorageDomain getIscsiEntity() {
        org.ovirt.engine.core.common.businessentities.StorageDomain entity =
                control.createMock(org.ovirt.engine.core.common.businessentities.StorageDomain.class);
        expect(entity.getId()).andReturn(GUIDS[0]).anyTimes();
        expect(entity.getStorageName()).andReturn(NAMES[0]).anyTimes();
        expect(entity.getStorageDomainType()).andReturn(org.ovirt.engine.core.common.businessentities.StorageDomainType.Data)
                .anyTimes();
        expect(entity.getStorageType()).andReturn(org.ovirt.engine.core.common.businessentities.storage.StorageType.ISCSI)
                .anyTimes();
        expect(entity.getStorage()).andReturn(GUIDS[0].toString()).anyTimes();
        return entity;
    }

    @Test
    public void testUpdateNotFound() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(1, true, getEntity(0));
        control.replay();
        try {
            resource.update(getModel(0));
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        setUpGetEntityExpectations(2, getEntity(0));
        setUpGetStorageServerConnectionExpectations(2);

        setUriInfo(setUpActionExpectations(VdcActionType.UpdateStorageDomain,
                                           StorageDomainManagementParameter.class,
                                           new String[] {},
                                           new Object[] {},
                                           true,
                                           true));

        verifyModel(resource.update(getModel(0)), 0);
    }

    @Test
    public void testUpdateCantDo() throws Exception {
        doTestBadUpdate(false, true, CANT_DO);
    }

    @Test
    public void testUpdateFailed() throws Exception {
        doTestBadUpdate(true, false, FAILURE);
    }

    private void doTestBadUpdate(boolean canDo, boolean success, String detail) throws Exception {
        setUpGetEntityExpectations(1, getEntity(0));
        setUpGetStorageServerConnectionExpectations(1);

        setUriInfo(setUpActionExpectations(VdcActionType.UpdateStorageDomain,
                                           StorageDomainManagementParameter.class,
                                           new String[] {},
                                           new Object[] {},
                                           canDo,
                                           success));

        try {
            resource.update(getModel(0));
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testConflictedUpdate() throws Exception {
        setUpGetEntityExpectations(1, getEntity(0));
        setUpGetStorageServerConnectionExpectations(1);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();

        StorageDomain model = getModel(1);
        model.setId(GUIDS[1].toString());
        try {
            resource.update(model);
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyImmutabilityConstraint(wae);
        }
    }

    @Test
    public void testRemoveStorageDomainNull() throws Exception {
        control.replay();
        try {
            resource.remove(null); // GUIDS[0].toString()
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            assertEquals(400, wae.getResponse().getStatus());
        }
    }

    public void testRemoveWithHostId() throws Exception {
        setUpGetEntityExpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveStorageDomain,
                RemoveStorageDomainParameters.class,
                new String[] { "StorageDomainId", "VdsId", "DoFormat" },
                new Object[] { GUIDS[0], GUIDS[1], Boolean.FALSE },
                true,
                true));

        StorageDomain storageDomain = new StorageDomain();
        storageDomain.setHost(new Host());
        storageDomain.getHost().setId(GUIDS[1].toString());
        verifyRemove(resource.remove(storageDomain));// GUIDS[0].toString()
    }

    @Test
    public void testRemoveWithFormat() throws Exception {
        setUpGetEntityExpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveStorageDomain,
                RemoveStorageDomainParameters.class,
                new String[] { "StorageDomainId", "VdsId", "DoFormat" },
                new Object[] { GUIDS[0], GUIDS[1], Boolean.TRUE },
                true,
                true));

        StorageDomain storageDomain = new StorageDomain();
        storageDomain.setHost(new Host());
        storageDomain.getHost().setId(GUIDS[1].toString());
        storageDomain.setFormat(true);
        verifyRemove(resource.remove(storageDomain));// GUIDS[0].toString()
    }

    @Test
    public void testRemoveWithDestroy() throws Exception {
        setUpGetEntityExpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.ForceRemoveStorageDomain,
                StorageDomainParametersBase.class,
                new String[] { "StorageDomainId", "VdsId" },
                new Object[] { GUIDS[0], GUIDS[1] },
                true,
                true));

        StorageDomain storageDomain = new StorageDomain();
        storageDomain.setHost(new Host());
        storageDomain.getHost().setId(GUIDS[1].toString());
        storageDomain.setDestroy(true);
        verifyRemove(resource.remove(storageDomain));// GUIDS[0].toString()
    }

    @Test
    public void testRemoveWithHostName() throws Exception {
        setUpGetEntityExpectations();

        setUpGetEntityExpectations(VdcQueryType.GetVdsStaticByName,
                NameQueryParameters.class,
                new String[] { "Name" },
                new Object[] { NAMES[1] },
                setUpVDStatic(1));

        setUriInfo(setUpActionExpectations(VdcActionType.RemoveStorageDomain,
                RemoveStorageDomainParameters.class,
                new String[] { "StorageDomainId", "VdsId", "DoFormat" },
                new Object[] { GUIDS[0], GUIDS[1], Boolean.FALSE },
                true,
                true));

        StorageDomain storageDomain = new StorageDomain();
        storageDomain.setHost(new Host());
        storageDomain.getHost().setName(NAMES[1]);
        verifyRemove(resource.remove(storageDomain));// GUIDS[0].toString()
    }

    @Test
    public void testIncompleteRemove() throws Exception {
        control.replay();
        try {
            resource.remove(new StorageDomain());// GUIDS[0].toString()
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "StorageDomain", "remove", "host.id|name");
        }
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
        setUpGetEntityExpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveStorageDomain,
                RemoveStorageDomainParameters.class,
                new String[] { "StorageDomainId", "VdsId", "DoFormat" },
                new Object[] { GUIDS[0], GUIDS[1], Boolean.FALSE },
                canDo,
                success));

        try {
            StorageDomain storageDomain = new StorageDomain();
            storageDomain.setHost(new Host());
            storageDomain.getHost().setId(GUIDS[1].toString());
            resource.remove(storageDomain);// GUIDS[0].toString()
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }
    protected void setUpGetEntityExpectations(int times, org.ovirt.engine.core.common.businessentities.StorageDomain entity) throws Exception {
        setUpGetEntityExpectations(times, false, entity);
    }

    protected void setUpGetEntityExpectations(int times, boolean notFound, org.ovirt.engine.core.common.businessentities.StorageDomain entity) throws Exception {
        while (times-- > 0) {
            setUpGetEntityExpectations(VdcQueryType.GetStorageDomainById,
                                       IdQueryParameters.class,
                                       new String[] { "Id" },
                                       new Object[] { GUIDS[0] },
                                       notFound ? null : entity);
        }
    }

    protected void setUpGetStorageServerConnectionExpectations(int times) throws Exception {
        while (times-- > 0) {
            setUpGetEntityExpectations(VdcQueryType.GetStorageServerConnectionById,
                                       StorageServerConnectionQueryParametersBase.class,
                                       new String[] { "ServerConnectionId" },
                                       new Object[] { GUIDS[0].toString() },
                                       setUpStorageServerConnection(0));
        }
    }

    private void setUpGetEntityExpectations() throws Exception {
        setUpGetEntityExpectations(VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                new org.ovirt.engine.core.common.businessentities.StorageDomain());
    }

    @Override
    protected org.ovirt.engine.core.common.businessentities.StorageDomain getEntity(int index) {
        return setUpEntityExpectations(control.createMock(org.ovirt.engine.core.common.businessentities.StorageDomain.class), index);
    }

    @Override
    protected void verifyModel(StorageDomain model, int index) {
        verifyModelSpecific(model, index);
        verifyLinks(model);
    }

    protected VdsStatic setUpVDStatic(int index) {
        VdsStatic vds = new VdsStatic();
        vds.setId(GUIDS[index]);
        vds.setName(NAMES[index]);
        return vds;
    }

    @Test
    public void testRefreshLunsSize() throws Exception {
        List<String> lunsArray = new ArrayList();
        lunsArray.add(GUIDS[2].toString());
        setUriInfo(setUpActionExpectations(VdcActionType.RefreshLunsSize,
                ExtendSANStorageDomainParameters.class,
                new String[]{"LunIds"},
                new Object[]{lunsArray},
                true,
                true));
        Action action = new Action();
        LogicalUnits luns= new LogicalUnits();
        LogicalUnit lun = new LogicalUnit();
        lun.setId(GUIDS[2].toString());
        luns.getLogicalUnits().add(lun);
        action.setLogicalUnits(luns);
        verifyActionResponse(resource.refreshLuns(action));
    }

    private void verifyActionResponse(Response response) throws Exception {
        verifyActionResponse(response, "storagedomains/" + GUIDS[0], false);
    }

    protected void verifyModelResponse(StorageDomain model, int index) {
        assertNotNull(model);
        assertEquals(GUIDS[index].toString(), model.getId());
        verifyLinks(model);
    }
}
