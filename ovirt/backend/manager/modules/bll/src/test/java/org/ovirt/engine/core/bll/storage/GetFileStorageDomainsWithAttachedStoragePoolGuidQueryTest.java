package org.ovirt.engine.core.bll.storage;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.ovirt.engine.core.bll.AbstractQueryTest;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.queries.StorageDomainsAndStoragePoolIdQueryParameters;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.HSMGetStorageDomainInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.StoragePoolDAO;
import org.ovirt.engine.core.dao.VdsDAO;

public class GetFileStorageDomainsWithAttachedStoragePoolGuidQueryTest extends
        AbstractQueryTest<StorageDomainsAndStoragePoolIdQueryParameters, GetFileStorageDomainsWithAttachedStoragePoolGuidQuery<StorageDomainsAndStoragePoolIdQueryParameters>> {
    @Mock
    private VDSBrokerFrontend vdsBrokerFrontendMock;

    private StorageDomain storageDomain;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        storageDomain = new StorageDomain();
        storageDomain.setStorageName("Name of Storage");
        storageDomain.setStorageType(StorageType.NFS);

        VDS vds = new VDS();
        vds.setId(Guid.newGuid());
        VdsDAO vdsDAOMock = mock(VdsDAO.class);
        List<VDS> listVds = new ArrayList<>();
        listVds.add(vds);
        when(vdsDAOMock.getAllForStoragePoolAndStatus(any(Guid.class), eq(VDSStatus.Up))).thenReturn(listVds);
        when(getDbFacadeMockInstance().getVdsDao()).thenReturn(vdsDAOMock);
    }

    @Test
    public void testAttachedStorageDomainWithStorageDomainsParameterQuery() {
        mockVdsCommand();
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(StoragePoolStatus.Up);
        mockStoragePoolDao(storagePool);

        // Create parameters
        List<StorageDomain> storageDomainList = new ArrayList<>();
        storageDomainList.add(storageDomain);
        StorageDomainsAndStoragePoolIdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getStorageDomainList()).thenReturn(storageDomainList);

        // Run 'HSMGetStorageDomainInfo' command
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setSucceeded(true);

        Pair<StorageDomainStatic, Guid> storageDomainToPoolId =
                new Pair<>(storageDomain.getStorageStaticData(), Guid.newGuid());
        returnValue.setReturnValue(storageDomainToPoolId);
        when(vdsBrokerFrontendMock.RunVdsCommand(eq(VDSCommandType.HSMGetStorageDomainInfo),
                any(HSMGetStorageDomainInfoVDSCommandParameters.class))).thenReturn(returnValue);

        // Execute command
        getQuery().executeQueryCommand();

        // Assert the query's results
        List<StorageDomainStatic> returnedStorageDomainList = new ArrayList<>();
        returnedStorageDomainList.add(storageDomain.getStorageStaticData());
        assertEquals(returnedStorageDomainList, getQuery().getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testUnattachedStorageDomainWithStorageDomainsParameterQuery() {
        mockVdsCommand();
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(StoragePoolStatus.Up);
        mockStoragePoolDao(storagePool);

        // Create parameters
        List<StorageDomain> storageDomainList = new ArrayList<>();
        storageDomainList.add(storageDomain);
        StorageDomainsAndStoragePoolIdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getStorageDomainList()).thenReturn(storageDomainList);

        // Run 'HSMGetStorageDomainInfo' command
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setSucceeded(true);

        Pair<StorageDomainStatic, Guid> storageDomainToPoolId = new Pair<>(storageDomain.getStorageStaticData(), null);
        returnValue.setReturnValue(storageDomainToPoolId);
        when(vdsBrokerFrontendMock.RunVdsCommand(eq(VDSCommandType.HSMGetStorageDomainInfo),
                any(HSMGetStorageDomainInfoVDSCommandParameters.class))).thenReturn(returnValue);

        // Execute command
        getQuery().executeQueryCommand();

        // Assert the query's results
        List<StorageDomainStatic> returnedStorageDomainList = new ArrayList<>();
        assertEquals(returnedStorageDomainList, getQuery().getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testEmptyStorageDomainListQuery() {
        mockVdsCommand();
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(StoragePoolStatus.Up);
        mockStoragePoolDao(storagePool);

        // Create parameters
        List<StorageDomain> storageDomainList = new ArrayList<>();
        StorageDomainsAndStoragePoolIdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getStorageDomainList()).thenReturn(storageDomainList);

        // Run 'HSMGetStorageDomainInfo' command
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setSucceeded(true);

        Pair<StorageDomainStatic, Guid> storageDomainToPoolId =
                new Pair<>(storageDomain.getStorageStaticData(), Guid.newGuid());
        returnValue.setReturnValue(storageDomainToPoolId);
        when(vdsBrokerFrontendMock.RunVdsCommand(eq(VDSCommandType.HSMGetStorageDomainInfo),
                any(HSMGetStorageDomainInfoVDSCommandParameters.class))).thenReturn(returnValue);

        // Execute command
        getQuery().executeQueryCommand();

        // Assert the query's results
        List<StorageDomainStatic> returnedStorageDomainList = new ArrayList<>();
        assertEquals(returnedStorageDomainList, getQuery().getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testNullStorageDomainListQuery() {
        mockVdsCommand();
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(StoragePoolStatus.Up);
        mockStoragePoolDao(storagePool);

        // Create parameters
        StorageDomainsAndStoragePoolIdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getStorageDomainList()).thenReturn(null);

        // Run 'HSMGetStorageDomainInfo' command
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setSucceeded(true);

        Pair<StorageDomainStatic, Guid> storageDomainToPoolId =
                new Pair<>(storageDomain.getStorageStaticData(), Guid.newGuid());
        returnValue.setReturnValue(storageDomainToPoolId);
        when(vdsBrokerFrontendMock.RunVdsCommand(eq(VDSCommandType.HSMGetStorageDomainInfo),
                any(HSMGetStorageDomainInfoVDSCommandParameters.class))).thenReturn(returnValue);

        // Execute command
        getQuery().executeQueryCommand();

        // Assert the query's results
        List<StorageDomainStatic> returnedStorageDomainList = new ArrayList<>();
        assertEquals(returnedStorageDomainList, getQuery().getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testFetchUsingStorageServerConnectionWithEmptyListRetrieved() {
        mockVdsCommand();
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(StoragePoolStatus.Up);
        mockStoragePoolDao(storagePool);

        // Create parameters
        StorageDomainsAndStoragePoolIdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getStorageDomainList()).thenReturn(null);
        StorageServerConnections storageServerConnections = new StorageServerConnections();
        when(paramsMock.getStorageServerConnection()).thenReturn(storageServerConnections);

        List<StorageDomain> storageDomains = new ArrayList<>();
        doReturn(storageDomains).when(getQuery()).getExistingStorageDomainList(eq(storageServerConnections));

        // Execute command
        getQuery().executeQueryCommand();

        // Assert the query's results
        List<StorageDomainStatic> returnedStorageDomainList = new ArrayList<>();
        assertEquals(returnedStorageDomainList, getQuery().getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testFetchUsingStorageServerConnectionWithAttachedStorageDomain() {
        mockVdsCommand();
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(StoragePoolStatus.Up);
        mockStoragePoolDao(storagePool);

        // Create parameters
        StorageDomainsAndStoragePoolIdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getStorageDomainList()).thenReturn(null);
        StorageServerConnections storageServerConnection = new StorageServerConnections();
        when(paramsMock.getStorageServerConnection()).thenReturn(storageServerConnection);

        StorageDomain mockedStorageDomain = new StorageDomain();
        mockedStorageDomain.setStorageStaticData(new StorageDomainStatic());
        mockedStorageDomain.setStoragePoolId(Guid.newGuid());

        List<StorageDomain> storageDomains = new ArrayList<>();
        storageDomains.add(mockedStorageDomain);
        doReturn(storageDomains).when(getQuery()).getExistingStorageDomainList(eq(storageServerConnection));

        // Execute command
        getQuery().executeQueryCommand();

        // Assert the query's results
        List<StorageDomainStatic> returnedStorageDomainList = new ArrayList<>();
        returnedStorageDomainList.add(mockedStorageDomain.getStorageStaticData());
        assertEquals(returnedStorageDomainList, getQuery().getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testFetchUsingStorageServerConnectionWithUnattachedStorageDomain() {
        mockVdsCommand();
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(StoragePoolStatus.Up);
        mockStoragePoolDao(storagePool);

        // Create parameters
        StorageDomainsAndStoragePoolIdQueryParameters paramsMock = getQueryParameters();
        when(paramsMock.getStorageDomainList()).thenReturn(null);
        StorageServerConnections storageServerConnections = new StorageServerConnections();
        when(paramsMock.getStorageServerConnection()).thenReturn(storageServerConnections);

        StorageDomain mockedStorageDomain = new StorageDomain();
        mockedStorageDomain.setStorageStaticData(new StorageDomainStatic());

        List<StorageDomain> storageDomains = new ArrayList<>();
        storageDomains.add(mockedStorageDomain);
        doReturn(storageDomains).when(getQuery()).getExistingStorageDomainList(eq(storageServerConnections));

        // Execute command
        getQuery().executeQueryCommand();

        // Assert the query's results
        List<StorageDomainStatic> returnedStorageDomainList = new ArrayList<>();
        assertEquals(returnedStorageDomainList, getQuery().getQueryReturnValue().getReturnValue());
    }

    private void mockVdsCommand() {
        vdsBrokerFrontendMock = mock(VDSBrokerFrontend.class);
        doReturn(vdsBrokerFrontendMock).when(getQuery()).getVdsBroker();
    }

    private void mockStoragePoolDao(StoragePool storagePool) {
        StoragePoolDAO storagePoolDaoMock = mock(StoragePoolDAO.class);
        when(getDbFacadeMockInstance().getStoragePoolDao()).thenReturn(storagePoolDaoMock);
        when(storagePoolDaoMock.get(any(Guid.class))).thenReturn(storagePool);
    }

}
