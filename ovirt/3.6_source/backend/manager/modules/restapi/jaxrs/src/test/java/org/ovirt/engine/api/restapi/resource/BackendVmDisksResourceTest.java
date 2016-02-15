package org.ovirt.engine.api.restapi.resource;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.ovirt.engine.api.model.CreationStatus;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.LogicalUnit;
import org.ovirt.engine.api.model.Snapshot;
import org.ovirt.engine.api.model.Storage;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageDomains;
import org.ovirt.engine.api.resource.VmDiskResource;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.AttachDetachVmDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatusEnum;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmDisksResourceTest
        extends AbstractBackendDisksResourceTest<BackendVmDisksResource> {

    private static final String ISCSI_SERVER_ADDRESS = "1.1.1.1";
    private static final Guid DISK_ID = GUIDS[0];
    private static final int ISCSI_SERVER_CONNECTION_PORT = 4567;
    private static final String ISCSI_SERVER_TARGET = "iqn.1986-03.com.sun:02:ori01";

    public BackendVmDisksResourceTest() {
        super(new BackendVmDisksResource(PARENT_ID,
                                       VdcQueryType.GetAllDisksByVmId,
                                       new IdQueryParameters(PARENT_ID)),
              VdcQueryType.GetAllDisksByVmId,
                new IdQueryParameters(PARENT_ID),
              "Id");
    }

    @Test
    public void testListIncludeStatistics() throws Exception {
        try {
            accepts.add("application/xml; detail=statistics");
            setUriInfo(setUpUriExpectations(null));
            setUpQueryExpectations("");

            List<Disk> disks = getCollection();
            assertTrue(disks.get(0).isSetStatistics());
            verifyCollection(disks);
        } finally {
            accepts.clear();
        }
    }

    private void setUpGetEntityExpectations() {
        setUpEntityQueryExpectations(VdcQueryType.GetAllDisksByVmId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { PARENT_ID },
                getEntityList());
    }

    @Test
    public void testAddAsyncPending() throws Exception {
        doTestAddAsync(AsyncTaskStatusEnum.init, CreationStatus.PENDING);
    }

    @Test
    public void testAddAsyncInProgress() throws Exception {
        doTestAddAsync(AsyncTaskStatusEnum.running, CreationStatus.IN_PROGRESS);
    }

    @Test
    public void testAddAsyncFinished() throws Exception {
        doTestAddAsync(AsyncTaskStatusEnum.finished, CreationStatus.COMPLETE);
    }

    @Test
    public void testAttachDisk() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(VdcQueryType.GetAllDisksByVmId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { PARENT_ID },
                getEntityList());
        setUpActionExpectations (VdcActionType.AttachDiskToVm,
                AttachDetachVmDiskParameters.class,
                new String[] { "VmId", "EntityInfo" },
                new Object[] { PARENT_ID, new EntityInfo(VdcObjectType.Disk, DISK_ID) },
                true,
                true);
        Disk model = getModel(0);
        model.setId(DISK_ID.toString()); //means this is an existing disk --> attach
        model.setSize(1024 * 1024L);
        Response response = collection.add(model);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testAttachDiskSnapshot() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        Guid snapshotId = Guid.newGuid();
        Disk model = getModel(0);
        model.setSnapshot(new Snapshot());
        model.getSnapshot().setId(snapshotId.toString());
        model.setId(DISK_ID.toString()); //means this is an existing disk --> attach
        setUpEntityQueryExpectations(VdcQueryType.GetAllDisksByVmId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { PARENT_ID },
                getEntityList());
        setUpActionExpectations (VdcActionType.AttachDiskToVm,
                AttachDetachVmDiskParameters.class,
                new String[] { "VmId", "EntityInfo", "SnapshotId" },
                new Object[] { PARENT_ID, new EntityInfo(VdcObjectType.Disk, DISK_ID), snapshotId },
                true,
                true);
        Response response = collection.add(model);
        assertEquals(200, response.getStatus());
    }

    private void doTestAddAsync(AsyncTaskStatusEnum asyncStatus, CreationStatus creationStatus) throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[2] },
                getStorageDomain(GUIDS[2]));
        setUpCreationExpectations(VdcActionType.AddDisk,
                                  AddDiskParameters.class,
                                  new String[] { "VmId" },
                                  new Object[] { PARENT_ID },
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[3]),
                                  asList(new AsyncTaskStatus(asyncStatus)),
                                  VdcQueryType.GetAllDisksByVmId,
                                  IdQueryParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { PARENT_ID },
                                  asList(getEntity(0)));
        Disk model = getModel(0);
        model.setSize(1024 * 1024L);

        Response response = collection.add(model);
        assertEquals(202, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk) response.getEntity(), 0);
        Disk created = (Disk)response.getEntity();
        assertNotNull(created.getCreationStatus());
        assertEquals(creationStatus.value(), created.getCreationStatus().getState());
    }

    @Test
    public void testAddDiskWithJobId() throws Exception {

        Disk model = getModel(0);

        setUriInfo(setUpBasicUriExpectations());

        setUriInfo(setUpGetMatrixConstraintsExpectations(
                BackendResource.JOB_ID_CONSTRAINT,
                true,
                GUIDS[1].toString(),
                collection.getUriInfo(),
                false));

        setCommonExpectations(model);
        Response response = collection.add(getModel(0));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk)response.getEntity(), 0);
        assertNull(((Disk)response.getEntity()).getCreationStatus());
    }

    @Test
    public void testAddDiskWithStepId() throws Exception {

        Disk model = getModel(0);

        setUriInfo(setUpBasicUriExpectations());

        setUriInfo(setUpGetMatrixConstraintsExpectations(
                BackendResource.STEP_ID_CONSTRAINT,
                true,
                GUIDS[1].toString(),
                collection.getUriInfo(),
                false));

        setCommonExpectations(model);
        Response response = collection.add(getModel(0));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk)response.getEntity(), 0);
        assertNull(((Disk)response.getEntity()).getCreationStatus());
    }

    private void setCommonExpectations(Disk model) {
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpEntityQueryExpectations(VdcQueryType.GetAllDisksByVmId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { PARENT_ID },
                                     asList(getEntity(0)));
        setUpEntityQueryExpectations(VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[2] },
                getStorageDomain(GUIDS[2]));
        setUpCreationExpectations(VdcActionType.AddDisk,
                                  AddDiskParameters.class,
                                  new String[] { "VmId", "StorageDomainId" },
                                  new Object[] { PARENT_ID, GUIDS[2] },
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[3]),
                                  asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                                  VdcQueryType.GetAllDisksByVmId,
                                  IdQueryParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { PARENT_ID },
                asList(getEntity(0)));
        model.setSize(1024 * 1024L);
    }

    @Test
    public void testAddDisk() throws Exception {
        testAddDiskImpl(getModel(0));
    }

    private void testAddDiskImpl(Disk model) {
        setUriInfo(setUpBasicUriExpectations());
        setCommonExpectations(model);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk)response.getEntity(), 0);
        assertNull(((Disk)response.getEntity()).getCreationStatus());
    }

    @Test
    public void testAddDiskIdentifyStorageDomainByName() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpEntityQueryExpectations(VdcQueryType.GetAllDisksByVmId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { PARENT_ID },
                                     asList(getEntity(0)));
        int times = 2;
        while (times-- > 0) {
            setUpEntityQueryExpectations(VdcQueryType.GetAllStorageDomains,
                    VdcQueryParametersBase.class,
                    new String[] {},
                    new Object[] {},
                    getStorageDomains());
        }
        setUpEntityQueryExpectations(VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[2] },
                getStorageDomain(GUIDS[2]));
        setUpCreationExpectations(VdcActionType.AddDisk,
                                  AddDiskParameters.class,
                                  new String[] { "VmId", "StorageDomainId" },
                                  new Object[] { PARENT_ID, GUIDS[2] },
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[3]),
                                  asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                                  VdcQueryType.GetAllDisksByVmId,
                                  IdQueryParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { PARENT_ID },
                                  asList(getEntity(0)));
        Disk model = getModel(0);
        model.getStorageDomains().getStorageDomains().get(0).setId(null);
        model.getStorageDomains().getStorageDomains().get(0).setName("Storage_Domain_1");
        model.setSize(1024 * 1024L);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk)response.getEntity(), 0);
        assertNull(((Disk)response.getEntity()).getCreationStatus());
    }

    private List getStorageDomains() {
        List<org.ovirt.engine.core.common.businessentities.StorageDomain> sds = new LinkedList<>();
        sds.add(getStorageDomain(GUIDS[2]));
        return sds;
    }

    private org.ovirt.engine.core.common.businessentities.StorageDomain getStorageDomain(Guid guid) {
        org.ovirt.engine.core.common.businessentities.StorageDomain sd = new org.ovirt.engine.core.common.businessentities.StorageDomain();
        sd.setStorageName("Storage_Domain_1");
        sd.setId(guid);
        return sd;
    }

    @Test
    public void testAddDiskWithinStorageDomain() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpEntityQueryExpectations(VdcQueryType.GetAllDisksByVmId,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { PARENT_ID },
                                     asList(getEntity(0)));
        setUpEntityQueryExpectations(VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[3] },
                getStorageDomain(GUIDS[3]));
        setUpCreationExpectations(VdcActionType.AddDisk,
                                  AddDiskParameters.class,
                                  new String[] { "VmId", "StorageDomainId" },
                                  new Object[] { PARENT_ID, GUIDS[3] },
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[3]),
                                  asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                                  VdcQueryType.GetAllDisksByVmId,
                                  IdQueryParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { PARENT_ID },
                                  asList(getEntity(0)));
        Disk model = getModel(0);
        model.setStorageDomains(new StorageDomains());
        StorageDomain storageDomain = new StorageDomain();
        storageDomain.setId(GUIDS[3].toString());
        model.getStorageDomains().getStorageDomains().add(storageDomain);
        model.setSize(1024 * 1024L);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk)response.getEntity(), 0);
        assertNull(((Disk)response.getEntity()).getCreationStatus());
    }

    @Test
    public void testAddDiskCantDo() throws Exception {
        doTestBadAddDisk(false, true, CANT_DO);
    }

    @Test
    public void testAddDiskFailure() throws Exception {
        doTestBadAddDisk(true, false, FAILURE);
    }

    private void doTestBadAddDisk(boolean canDo, boolean success, String detail) throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[2] },
                getStorageDomain(GUIDS[2]));
        setUriInfo(setUpActionExpectations(VdcActionType.AddDisk,
                                           AddDiskParameters.class,
                                           new String[] { "VmId" },
                                           new Object[] { PARENT_ID },
                                           canDo,
                                           success));
        Disk model = getModel(0);
        model.setSize(1024 * 1024L);

        try {
            collection.add(model);
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddIncompleteParameters() throws Exception {
        Disk model = new Disk();
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            // Because of extra frame offset used current method name in test, while in real world used "add" method name
            verifyIncompleteException(wae, "Disk", "testAddIncompleteParameters", "interface");
        }
    }

    @Test
    public void testAddIncompleteParameters_2() throws Exception {
        Disk model = getModel(0);
        model.setSize(null);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            // Because of extra frame offset used current method name in test, while in real world used "add" method name
            verifyIncompleteException(wae, "Disk", "testAddIncompleteParameters_2", "provisionedSize|size");
        }
    }

    @Test
    public void testAddLunDisk_MissingType() {
        Disk model = createIscsiLunDisk();
        model.getLunStorage().setType(null);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            // Because of extra frame offset used current method name in test, while in real world used "add" method name
            verifyIncompleteException(wae, "Storage", "testAddLunDisk_MissingType", "type");
        }
    }

    @Test
    public void testAddLunDisk_MissingId() {
        Disk model = createIscsiLunDisk();
        model.getLunStorage().getLogicalUnits().get(0).setId(null);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            // Because of extra frame offset used current method name in test, while in real world used "add" method name
            verifyIncompleteException(wae, "LogicalUnit", "testAddLunDisk_MissingId", "id");
        }
    }

    @Test
    public void testAddIscsiLunDisk_IncompleteParameters_ConnectionAddress() {
        Disk model = createIscsiLunDisk();
        model.getLunStorage().getLogicalUnits().get(0).setAddress(null);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            // Because of extra frame offset used current method name in test, while in real world used "add" method name
            verifyIncompleteException(wae, "LogicalUnit", "testAddIscsiLunDisk_IncompleteParameters_ConnectionAddress", "address");
        }
    }

    @Test
    public void testAddIscsiLunDisk_IncompleteParameters_ConnectionTarget() {
        Disk model = createIscsiLunDisk();
        model.getLunStorage().getLogicalUnits().get(0).setTarget(null);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            // Because of extra frame offset used current method name in test, while in real world used "add" method name
            verifyIncompleteException(wae, "LogicalUnit", "testAddIscsiLunDisk_IncompleteParameters_ConnectionTarget", "target");
        }
    }

    @Test
    public void testAddIscsiLunDisk_IncompleteParameters_ConnectionPort() {
        Disk model = createIscsiLunDisk();
        model.getLunStorage().getLogicalUnits().get(0).setPort(null);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            // Because of extra frame offset used current method name in test, while in real world used "add" method name
            verifyIncompleteException(wae, "LogicalUnit", "testAddIscsiLunDisk_IncompleteParameters_ConnectionPort", "port");
        }
    }

    private Disk createIscsiLunDisk() {
        Disk model = getModel(0);
        model.setLunStorage(new Storage());
        model.getLunStorage().setType("iscsi");
        model.getLunStorage().getLogicalUnits().add(new LogicalUnit());
        model.getLunStorage().getLogicalUnits().get(0).setId(GUIDS[0].toString());
        model.getLunStorage().getLogicalUnits().get(0).setAddress(ISCSI_SERVER_ADDRESS);
        model.getLunStorage().getLogicalUnits().get(0).setTarget(ISCSI_SERVER_TARGET);
        model.getLunStorage().getLogicalUnits().get(0).setPort(ISCSI_SERVER_CONNECTION_PORT);
        model.setSize(null);
        model.setProvisionedSize(null);
        return model;
    }

    @Test
    /**
     * This test checks that addition of LUN-Disk is successful. There is no real difference in the
     * implementation of adding regular disk and adding a lun-disk; in both cases the disk entity
     * is mapped and passed to the Backend, and Backend infers the type of disk from the entity and creates it.
     *
     * So what this test actually checks is that it's OK for the user not to specify size|provisionedSize
     * when creating a LUN-Disk
     */
    public void testAddLunDisk() {
        Disk model = createIscsiLunDisk();
        testAddDiskImpl(model);
    }

    @Test
    public void testSubResourceLocator() throws Exception {
        control.replay();
        assertTrue(collection.getDeviceSubResource(GUIDS[0].toString()) instanceof VmDiskResource);
    }

    @Test
    public void testSubResourceLocatorBadGuid() throws Exception {
        control.replay();
        try {
            collection.getDeviceSubResource("foo");
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }
}
