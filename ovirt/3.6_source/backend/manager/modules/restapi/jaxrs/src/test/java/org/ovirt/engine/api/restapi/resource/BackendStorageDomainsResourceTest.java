package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.ovirt.engine.api.model.Host;
import org.ovirt.engine.api.model.LogicalUnit;
import org.ovirt.engine.api.model.Storage;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageDomainType;
import org.ovirt.engine.api.model.StorageType;
import org.ovirt.engine.api.model.VolumeGroup;
import org.ovirt.engine.core.common.action.AddSANStorageDomainParameters;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.GetDeviceListQueryParameters;
import org.ovirt.engine.core.common.queries.GetExistingStorageDomainListParameters;
import org.ovirt.engine.core.common.queries.GetLunsByVgIdParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.StorageServerConnectionQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendStorageDomainsResourceTest
        extends AbstractBackendCollectionResourceTest<StorageDomain, org.ovirt.engine.core.common.businessentities.StorageDomain, BackendStorageDomainsResource> {

    protected static final StorageDomainType[] TYPES = { StorageDomainType.DATA,
            StorageDomainType.ISO, StorageDomainType.EXPORT };
    protected static final StorageType[] STORAGE_TYPES = { StorageType.NFS, StorageType.NFS,
            StorageType.LOCALFS, StorageType.POSIXFS };

    protected static final int LOCAL_IDX = 2;
    protected static final int POSIX_IDX = 3;

    protected static final String[] ADDRESSES = { "10.11.12.13", "13.12.11.10", "10.01.10.01", "10.01.10.14" };
    protected static final String[] PATHS = { "/1", "/2", "/3", "/4" };
    protected static final String[] MOUNT_OPTIONS = { "", "", "", "rw" };
    protected static final String[] VFS_TYPES = { "", "", "", "nfs" };
    protected static final String LUN = "1IET_00010001";
    protected static final String TARGET = "iqn.2009-08.org.fubar.engine:markmc.test1";
    protected static final Integer PORT = 3260;

    protected static final org.ovirt.engine.core.common.businessentities.StorageDomainType TYPES_MAPPED[] = {
            org.ovirt.engine.core.common.businessentities.StorageDomainType.Data,
            org.ovirt.engine.core.common.businessentities.StorageDomainType.ISO,
            org.ovirt.engine.core.common.businessentities.StorageDomainType.ImportExport };

    protected static final org.ovirt.engine.core.common.businessentities.storage.StorageType STORAGE_TYPES_MAPPED[] = {
            org.ovirt.engine.core.common.businessentities.storage.StorageType.NFS,
            org.ovirt.engine.core.common.businessentities.storage.StorageType.NFS,
            org.ovirt.engine.core.common.businessentities.storage.StorageType.LOCALFS,
            org.ovirt.engine.core.common.businessentities.storage.StorageType.POSIXFS };

    public BackendStorageDomainsResourceTest() {
        super(new BackendStorageDomainsResource(), SearchType.StorageDomain, "Storage : ");
    }

    @Test
    public void testAddStorageDomain() throws Exception {
        Host host = new Host();
        host.setId(GUIDS[0].toString());
        doTestAddStorageDomain(0, host, false);
    }

    @Test
    public void testAddStorageDomainWithExistingConnectionId() throws Exception {
        Host host = new Host();
        host.setId(GUIDS[0].toString());
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(VdcQueryType.GetStorageServerConnectionById,
                StorageServerConnectionQueryParametersBase.class,
                new String[] { "ServerConnectionId" },
                new Object[] { GUIDS[POSIX_IDX].toString() },
                setUpPosixStorageServerConnection(POSIX_IDX));
        setUpGetEntityExpectations(VdcQueryType.GetStorageServerConnectionById,
                StorageServerConnectionQueryParametersBase.class,
                new String[] { "ServerConnectionId" },
                new Object[] { GUIDS[POSIX_IDX].toString() },
                setUpPosixStorageServerConnection(POSIX_IDX));
        setUpGetEntityExpectations(VdcQueryType.GetExistingStorageDomainList,
                GetExistingStorageDomainListParameters.class,
                new String[] { "Id", "StorageType", "StorageDomainType", "Path" },
                new Object[] { GUIDS[0], STORAGE_TYPES_MAPPED[POSIX_IDX], TYPES_MAPPED[0], ADDRESSES[POSIX_IDX] + ":" + PATHS[POSIX_IDX] },
                new ArrayList<StorageDomainStatic>());

        setUpCreationExpectations(VdcActionType.AddPosixFsStorageDomain,
                StorageDomainManagementParameter.class,
                new String[] { "VdsId" },
                new Object[] { GUIDS[0] },
                true,
                true,
                GUIDS[POSIX_IDX],
                VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[POSIX_IDX] },
                getEntity(POSIX_IDX));

        StorageDomain model = new StorageDomain();
        model.setName(getSafeEntry(POSIX_IDX, NAMES));
        model.setDescription(getSafeEntry(POSIX_IDX, DESCRIPTIONS));
        model.setType(getSafeEntry(POSIX_IDX, TYPES).value());
        model.setStorage(new Storage());
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());
        model.getStorage().setId(GUIDS[POSIX_IDX].toString());

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof StorageDomain);
        verifyModel((StorageDomain) response.getEntity(), POSIX_IDX);
    }

    @Test
    public void testAddStorageDomainWithNoStorageObject() throws Exception {
        Host host = new Host();
        host.setId(GUIDS[0].toString());
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        StorageDomain model = new StorageDomain();
        model.setName(getSafeEntry(POSIX_IDX, NAMES));
        model.setDescription(getSafeEntry(POSIX_IDX, DESCRIPTIONS));
        model.setType(getSafeEntry(POSIX_IDX, TYPES).value());
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());

        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "StorageDomain", "add", "storage");
        }
    }

    @Test
    public void testAddStorageDomainWithHostName() throws Exception {
        Host host = new Host();
        host.setName(NAMES[0]);

        setUpGetEntityExpectations(VdcQueryType.GetVdsStaticByName,
                NameQueryParameters.class,
                new String[] { "Name" },
                new Object[] { NAMES[0] },
                setUpVDStatic(0));

        doTestAddStorageDomain(0, host, false);
    }

    @Test
    public void testAddExistingStorageDomain() throws Exception {
        Host host = new Host();
        host.setId(GUIDS[0].toString());
        doTestAddStorageDomain(1, host, true);
    }

    public void doTestAddStorageDomain(int idx, Host host, boolean existing) throws Exception {
        setUriInfo(setUpActionExpectations(VdcActionType.AddStorageServerConnection,
                StorageServerConnectionParametersBase.class,
                new String[] { "StorageServerConnection.connection", "StorageServerConnection.storage_type", "VdsId" },
                new Object[] { ADDRESSES[idx] + ":" + PATHS[idx], STORAGE_TYPES_MAPPED[idx], GUIDS[0] },
                true,
                true,
                GUIDS[idx].toString(),
                false));

        setUpGetEntityExpectations(VdcQueryType.GetStorageServerConnectionById,
                StorageServerConnectionQueryParametersBase.class,
                new String[] { "ServerConnectionId" },
                new Object[] { GUIDS[idx].toString() },
                setUpStorageServerConnection(idx));

        setUpGetEntityExpectations(VdcQueryType.GetExistingStorageDomainList,
                GetExistingStorageDomainListParameters.class,
                new String[] { "Id", "StorageType", "StorageDomainType", "Path" },
                new Object[] { GUIDS[0], STORAGE_TYPES_MAPPED[idx], TYPES_MAPPED[idx],
                        ADDRESSES[idx] + ":" + PATHS[idx] },
                getExistingStorageDomains(existing));

        setUpCreationExpectations(!existing ? VdcActionType.AddNFSStorageDomain
                : VdcActionType.AddExistingFileStorageDomain,
                StorageDomainManagementParameter.class,
                new String[] {},
                new Object[] {},
                true,
                true,
                GUIDS[idx],
                VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[idx] },
                getEntity(idx));

        StorageDomain model = getModel(idx);
        model.setHost(host);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof StorageDomain);
        verifyModel((StorageDomain) response.getEntity(), idx);
    }

    @Test
    public void testAddLocalStorageDomain() throws Exception {
        setUriInfo(setUpActionExpectations(VdcActionType.AddStorageServerConnection,
                StorageServerConnectionParametersBase.class,
                new String[] { "StorageServerConnection.connection", "StorageServerConnection.storage_type", "VdsId" },
                new Object[] { PATHS[LOCAL_IDX], STORAGE_TYPES_MAPPED[LOCAL_IDX], GUIDS[0] },
                true,
                true,
                GUIDS[LOCAL_IDX].toString(),
                false));

        setUpGetEntityExpectations(VdcQueryType.GetStorageServerConnectionById,
                StorageServerConnectionQueryParametersBase.class,
                new String[] { "ServerConnectionId" },
                new Object[] { GUIDS[LOCAL_IDX].toString() },
                setUpLocalStorageServerConnection(LOCAL_IDX));

        setUpCreationExpectations(VdcActionType.AddLocalStorageDomain,
                StorageDomainManagementParameter.class,
                new String[] { "VdsId" },
                new Object[] { GUIDS[0] },
                true,
                true,
                GUIDS[LOCAL_IDX],
                VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[LOCAL_IDX] },
                getEntity(LOCAL_IDX));

        StorageDomain model = getModel(LOCAL_IDX);
        model.getStorage().setAddress(null);
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof StorageDomain);
        verifyModel((StorageDomain) response.getEntity(), LOCAL_IDX);
    }

    @Test
    public void testAddPosixStorageDomain() throws Exception {
        setUriInfo(setUpActionExpectations(VdcActionType.AddStorageServerConnection,
                StorageServerConnectionParametersBase.class,
                new String[] { "StorageServerConnection.connection",
                        "StorageServerConnection.storage_type",
                        "StorageServerConnection.MountOptions",
                        "StorageServerConnection.VfsType",
                        "VdsId" },
                new Object[] { ADDRESSES[POSIX_IDX] + ":" + PATHS[POSIX_IDX],
                        STORAGE_TYPES_MAPPED[POSIX_IDX],
                        MOUNT_OPTIONS[POSIX_IDX], VFS_TYPES[POSIX_IDX], GUIDS[0] },
                true,
                true,
                GUIDS[POSIX_IDX].toString(),
                false));

        setUpGetEntityExpectations(VdcQueryType.GetStorageServerConnectionById,
                StorageServerConnectionQueryParametersBase.class,
                new String[] { "ServerConnectionId" },
                new Object[] { GUIDS[POSIX_IDX].toString() },
                setUpPosixStorageServerConnection(POSIX_IDX));

        setUpGetEntityExpectations(VdcQueryType.GetExistingStorageDomainList,
                GetExistingStorageDomainListParameters.class,
                new String[] { "Id", "StorageType", "StorageDomainType", "Path" },
                new Object[] { GUIDS[0], STORAGE_TYPES_MAPPED[POSIX_IDX], TYPES_MAPPED[0], ADDRESSES[POSIX_IDX] + ":" + PATHS[POSIX_IDX] },
                new ArrayList<StorageDomainStatic>());

        setUpCreationExpectations(VdcActionType.AddPosixFsStorageDomain,
                StorageDomainManagementParameter.class,
                new String[] { "VdsId" },
                new Object[] { GUIDS[0] },
                true,
                true,
                GUIDS[POSIX_IDX],
                VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[POSIX_IDX] },
                getEntity(POSIX_IDX));

        StorageDomain model = getModel(POSIX_IDX);
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof StorageDomain);
        verifyModel((StorageDomain) response.getEntity(), POSIX_IDX);
    }

    @Test
    public void testAddIscsiStorageDomain() throws Exception {
        StorageDomain model = getIscsi();

        Host host = new Host();
        host.setId(GUIDS[0].toString());
        model.setHost(host);

        setUriInfo(setUpActionExpectations(VdcActionType.ConnectStorageToVds,
                StorageServerConnectionParametersBase.class,
                new String[] { "StorageServerConnection.connection", "VdsId" },
                new Object[] { ADDRESSES[0], GUIDS[0] },
                true,
                true,
                GUIDS[0].toString(),
                false));

        setUpGetEntityExpectations(VdcQueryType.GetDeviceList,
                GetDeviceListQueryParameters.class,
                new String[] { "Id", "StorageType" },
                new Object[] { GUIDS[0], org.ovirt.engine.core.common.businessentities.storage.StorageType.ISCSI },
                "this return value isn't used");

        setUpGetEntityExpectations(VdcQueryType.GetLunsByVgId,
                GetLunsByVgIdParameters.class,
                new String[] { "VgId" },
                new Object[] { GUIDS[GUIDS.length - 1].toString() },
                setUpLuns());

        setUpCreationExpectations(VdcActionType.AddSANStorageDomain,
                AddSANStorageDomainParameters.class,
                new String[] { "VdsId" },
                new Object[] { GUIDS[0] },
                true,
                true,
                GUIDS[0],
                VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                getIscsiEntity());

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof StorageDomain);
        verifyIscsi((StorageDomain) response.getEntity());
    }

    @Test
    public void testAddIscsiStorageDomainAssumingConnection() throws Exception {
        StorageDomain model = getIscsi();

        Host host = new Host();
        host.setId(GUIDS[0].toString());
        model.setHost(host);
        for (LogicalUnit lun : model.getStorage().getVolumeGroup().getLogicalUnits()) {
            lun.setAddress(null);
            lun.setTarget(null);
        }
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(VdcQueryType.GetDeviceList,
                GetDeviceListQueryParameters.class,
                new String[] { "Id", "StorageType" },
                new Object[] { GUIDS[0], org.ovirt.engine.core.common.businessentities.storage.StorageType.ISCSI },
                "this return value isn't used");

        List<LUNs> luns = setUpLuns();
        setUpGetEntityExpectations(VdcQueryType.GetLunsByVgId,
                GetLunsByVgIdParameters.class,
                new String[] { "VgId" },
                new Object[] { GUIDS[GUIDS.length - 1].toString() },
                luns);

        setUpCreationExpectations(VdcActionType.AddSANStorageDomain,
                AddSANStorageDomainParameters.class,
                new String[] { "VdsId" },
                new Object[] { GUIDS[0] },
                true,
                true,
                GUIDS[0],
                VdcQueryType.GetStorageDomainById,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                getIscsiEntity());

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof StorageDomain);
        verifyIscsi((StorageDomain) response.getEntity());
    }

    @Test
    public void testAddStorageDomainNoHost() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        StorageDomain model = getModel(0);
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "StorageDomain", "add", "host.id|name");
        }
    }

    @Test
    public void testAddStorageDomainCantDo() throws Exception {
        doTestBadAddStorageDomain(false, true, CANT_DO);
    }

    @Test
    public void testAddStorageDomainFailure() throws Exception {
        doTestBadAddStorageDomain(true, false, FAILURE);
    }

    private void doTestBadAddStorageDomain(boolean canDo, boolean success, String detail)
            throws Exception {
        setUriInfo(setUpActionExpectations(VdcActionType.AddStorageServerConnection,
                StorageServerConnectionParametersBase.class,
                new String[] { "StorageServerConnection.connection", "StorageServerConnection.storage_type", "VdsId" },
                new Object[] { ADDRESSES[0] + ":" + PATHS[0], STORAGE_TYPES_MAPPED[0], GUIDS[0] },
                true,
                true,
                GUIDS[0].toString(),
                false));

        setUpActionExpectations(VdcActionType.RemoveStorageServerConnection,
                StorageServerConnectionParametersBase.class,
                new String[] {},
                new Object[] {},
                true,
                true, null, false);

        setUpGetEntityExpectations(VdcQueryType.GetExistingStorageDomainList,
                GetExistingStorageDomainListParameters.class,
                new String[] { "Id", "StorageType", "StorageDomainType", "Path" },
                new Object[] { GUIDS[0], STORAGE_TYPES_MAPPED[0], TYPES_MAPPED[0], ADDRESSES[0] + ":" + PATHS[0] },
                new ArrayList<StorageDomainStatic>());

        setUpActionExpectations(VdcActionType.AddNFSStorageDomain,
                StorageDomainManagementParameter.class,
                new String[] {},
                new Object[] {},
                canDo,
                success);

        StorageDomain model = getModel(0);
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());

        try {
            collection.add(model);
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddStorageDomainCantDoCnxAdd() throws Exception {
        doTestBadCnxAdd(false, true, CANT_DO);
    }

    @Test
    public void testAddStorageDomainCnxAddFailure() throws Exception {
        doTestBadCnxAdd(true, false, FAILURE);
    }

    private void doTestBadCnxAdd(boolean canDo, boolean success, String detail) throws Exception {
        setUriInfo(setUpActionExpectations(VdcActionType.AddStorageServerConnection,
                StorageServerConnectionParametersBase.class,
                new String[] { "StorageServerConnection.connection", "StorageServerConnection.storage_type", "VdsId" },
                new Object[] { ADDRESSES[0] + ":" + PATHS[0], STORAGE_TYPES_MAPPED[0], GUIDS[0] },
                canDo,
                success,
                GUIDS[0].toString(),
                true));

        StorageDomain model = getModel(0);
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());

        try {
            collection.add(model);
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddIncompleteDomainParameters() throws Exception {
        StorageDomain model = getModel(0);
        model.setName(NAMES[0]);
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());
        model.setStorage(new Storage());
        model.getStorage().setAddress(ADDRESSES[0]);
        model.getStorage().setPath(PATHS[0]);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "StorageDomain", "add", "storage.type");
        }
    }

    @Test
    public void testAddIncompleteNfsStorageParameters() throws Exception {
        StorageDomain model = getModel(0);
        model.setName(NAMES[0]);
        model.setHost(new Host());
        model.getHost().setId(GUIDS[0].toString());
        model.setStorage(new Storage());
        model.getStorage().setType(StorageType.NFS.value());
        model.getStorage().setPath(PATHS[0]);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "Storage", "add", "address");
        }
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        if (failure == null) {
            for (int i = 0; i < NAMES.length; i++) {
                setUpGetEntityExpectations(VdcQueryType.GetStorageServerConnectionById,
                        StorageServerConnectionQueryParametersBase.class,
                        new String[] { "ServerConnectionId" },
                        new Object[] { GUIDS[i].toString() }, setUpStorageServerConnection(i));
            }
        }
        super.setUpQueryExpectations(query, failure);
    }

    static StorageServerConnections setUpLocalStorageServerConnection(int index) {
        return setUpStorageServerConnection(index, index, StorageType.LOCALFS);
    }

    static StorageServerConnections setUpPosixStorageServerConnection(int index) {
        return setUpStorageServerConnection(index, index, StorageType.POSIXFS);
    }

    static StorageServerConnections setUpStorageServerConnection(int index) {
        return setUpStorageServerConnection(index, index, StorageType.NFS);
    }

    static StorageServerConnections setUpStorageServerConnection(int idIndex, int index, StorageType storageType) {
        StorageServerConnections cnx = new StorageServerConnections();
        if (idIndex != -1) {
            cnx.setid(GUIDS[idIndex].toString());
        }
        if (storageType == StorageType.LOCALFS) {
            cnx.setconnection(PATHS[index]);
        } else if (storageType == StorageType.POSIXFS) {
            cnx.setconnection(ADDRESSES[index] + ":" + PATHS[index]);
            cnx.setMountOptions(MOUNT_OPTIONS[index]);
            cnx.setVfsType(VFS_TYPES[index]);
        } else {
            cnx.setconnection(ADDRESSES[index] + ":" + PATHS[index]);
        }
        cnx.setstorage_type(STORAGE_TYPES_MAPPED[index]);
        return cnx;
    }

    protected VDS setUpVDS(int index) {
        VDS vds = new VDS();
        vds.setId(GUIDS[index]);
        vds.setVdsName(NAMES[index]);
        return vds;
    }

    protected VdsStatic setUpVDStatic(int index) {
        VdsStatic vds = new VdsStatic();
        vds.setId(GUIDS[index]);
        vds.setName(NAMES[index]);
        return vds;
    }

    @Override
    protected org.ovirt.engine.core.common.businessentities.StorageDomain getEntity(int index) {
        return setUpEntityExpectations(control.createMock(org.ovirt.engine.core.common.businessentities.StorageDomain.class),
                index);
    }

    static org.ovirt.engine.core.common.businessentities.StorageDomain setUpEntityExpectations(org.ovirt.engine.core.common.businessentities.StorageDomain entity,
            int index) {
        expect(entity.getId()).andReturn(getSafeEntry(index, GUIDS)).anyTimes();
        expect(entity.getStorageName()).andReturn(getSafeEntry(index, NAMES)).anyTimes();
        // REVIST No descriptions for storage domains
        // expect(entity.getDescription()).andReturn(DESCRIPTIONS[index]).anyTimes();
        expect(entity.getStorageDomainType()).andReturn(getSafeEntry(index, TYPES_MAPPED)).anyTimes();
        expect(entity.getStorageType()).andReturn(getSafeEntry(index, STORAGE_TYPES_MAPPED)).anyTimes();
        expect(entity.getStorage()).andReturn(getSafeEntry(index, GUIDS).toString()).anyTimes();
        return entity;
    }

    private static <T> T getSafeEntry(int index, T[] arr) {
        return arr[index % arr.length];
    }

    protected List<LUNs> setUpLuns() {
        StorageServerConnections cnx = new StorageServerConnections();
        cnx.setconnection(ADDRESSES[0]);
        cnx.setiqn(TARGET);
        cnx.setport(Integer.toString(PORT));

        LUNs lun = new LUNs();
        lun.setLUN_id(LUN);
        lun.setLunConnections(new ArrayList<StorageServerConnections>());
        lun.getLunConnections().add(cnx);

        List<LUNs> luns = new ArrayList<LUNs>();
        luns.add(lun);
        return luns;
    }

    protected org.ovirt.engine.core.common.businessentities.StorageDomain getIscsiEntity() {
        org.ovirt.engine.core.common.businessentities.StorageDomain entity =
                control.createMock(org.ovirt.engine.core.common.businessentities.StorageDomain.class);
        expect(entity.getId()).andReturn(GUIDS[0]).anyTimes();
        expect(entity.getStorageName()).andReturn(NAMES[0]).anyTimes();
        expect(entity.getStorageDomainType()).andReturn(TYPES_MAPPED[0]).anyTimes();
        expect(entity.getStorageType()).andReturn(org.ovirt.engine.core.common.businessentities.storage.StorageType.ISCSI)
                .anyTimes();
        expect(entity.getStorage()).andReturn(GUIDS[GUIDS.length - 1].toString()).anyTimes();
        return entity;
    }

    static StorageDomain getModel(int index) {
        StorageDomain model = new StorageDomain();
        model.setName(getSafeEntry(index, NAMES));
        model.setDescription(getSafeEntry(index, DESCRIPTIONS));
        model.setType(getSafeEntry(index, TYPES).value());
        model.setStorage(new Storage());
        model.getStorage().setType(getSafeEntry(index, STORAGE_TYPES).value());
        model.getStorage().setAddress(getSafeEntry(index, ADDRESSES));
        model.getStorage().setPath(getSafeEntry(index, PATHS));
        model.getStorage().setMountOptions(getSafeEntry(index, MOUNT_OPTIONS));
        model.getStorage().setVfsType(getSafeEntry(index, VFS_TYPES));
        return model;
    }

    protected List<org.ovirt.engine.core.common.businessentities.StorageDomain> getExistingStorageDomains(boolean existing) {
        List<org.ovirt.engine.core.common.businessentities.StorageDomain> ret =
                new ArrayList<org.ovirt.engine.core.common.businessentities.StorageDomain>();
        if (existing) {
            ret.add(new org.ovirt.engine.core.common.businessentities.StorageDomain());
        }
        return ret;
    }

    @Override
    protected List<StorageDomain> getCollection() {
        return collection.list().getStorageDomains();
    }

    @Override
    protected void verifyModel(StorageDomain model, int index) {
        verifyModelSpecific(model, index);
        verifyLinks(model);
    }

    static void verifyModelSpecific(StorageDomain model, int index) {
        assertEquals(getSafeEntry(index, GUIDS).toString(), model.getId());
        assertEquals(getSafeEntry(index, NAMES), model.getName());
        // REVIST No descriptions for storage domains
        // assertEquals(DESCRIPTIONS[index], model.getDescription());
        assertEquals(getSafeEntry(index, TYPES).value(), model.getType());
        assertNotNull(model.getStorage());
        assertEquals(getSafeEntry(index, STORAGE_TYPES).value(), model.getStorage().getType());
        if (index != LOCAL_IDX && index != POSIX_IDX) {
            assertEquals(getSafeEntry(index, ADDRESSES), model.getStorage().getAddress());
        }
        assertEquals(PATHS[index], model.getStorage().getPath());
        assertEquals("permissions", model.getLinks().get(0).getRel());
        if (StorageDomainType.fromValue(model.getType()) == StorageDomainType.ISO) {
            assertEquals(5, model.getLinks().size());
            assertEquals("files", model.getLinks().get(1).getRel());

        } else if (model.getType().equals(TYPES[2].value())) {
            assertEquals(7, model.getLinks().size());
            assertEquals("templates", model.getLinks().get(1).getRel());
            assertEquals("vms", model.getLinks().get(2).getRel());
        }
        assertNotNull(model.getLinks().get(0).getHref());
    }

    protected StorageDomain getIscsi() {
        StorageDomain model = getModel(0);
        model.getStorage().setType(StorageType.ISCSI.value());
        model.getStorage().setAddress(null);
        model.getStorage().setPath(null);
        model.getStorage().setVolumeGroup(new VolumeGroup());
        model.getStorage().getVolumeGroup().getLogicalUnits().add(new LogicalUnit());
        model.getStorage().getVolumeGroup().getLogicalUnits().get(0).setId(LUN);
        model.getStorage().getVolumeGroup().getLogicalUnits().get(0).setTarget(TARGET);
        model.getStorage().getVolumeGroup().getLogicalUnits().get(0).setAddress(ADDRESSES[0]);
        model.getStorage().getVolumeGroup().getLogicalUnits().get(0).setPort(PORT);
        model.getStorage().setOverrideLuns(false);
        return model;
    }

    protected void verifyIscsi(StorageDomain model) {
        assertEquals(GUIDS[0].toString(), model.getId());
        assertEquals(NAMES[0], model.getName());
        assertEquals(TYPES[0].value(), model.getType());
        assertNotNull(model.getStorage());
        assertEquals(StorageType.ISCSI.value(), model.getStorage().getType());
        assertNotNull(model.getStorage().getVolumeGroup());
        assertEquals(GUIDS[GUIDS.length - 1].toString(), model.getStorage().getVolumeGroup().getId());
        assertTrue(model.getStorage().getVolumeGroup().isSetLogicalUnits());
        assertNotNull(model.getStorage().getVolumeGroup().getLogicalUnits().get(0));
        assertEquals(LUN, model.getStorage().getVolumeGroup().getLogicalUnits().get(0).getId());
        assertEquals(TARGET, model.getStorage().getVolumeGroup().getLogicalUnits().get(0).getTarget());
        assertEquals(ADDRESSES[0], model.getStorage().getVolumeGroup().getLogicalUnits().get(0).getAddress());
        assertEquals(PORT, model.getStorage().getVolumeGroup().getLogicalUnits().get(0).getPort());
        assertEquals(7, model.getLinks().size());
        assertEquals("permissions", model.getLinks().get(0).getRel());
        assertNotNull(model.getLinks().get(0).getHref());
        verifyLinks(model);
    }
}
