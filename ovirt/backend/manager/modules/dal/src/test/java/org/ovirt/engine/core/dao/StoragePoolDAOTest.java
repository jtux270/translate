package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;

public class StoragePoolDAOTest extends BaseDAOTestCase {
    private static final int NUMBER_OF_POOLS_FOR_PRIVELEGED_USER = 1;

    private StoragePoolDAO dao;
    private StoragePool existingPool;
    private Guid vds;
    private Guid vdsGroup;
    private Guid storageDomain;
    private StoragePool newPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = dbFacade.getStoragePoolDao();
        existingPool = dao
                .get(new Guid("6d849ebf-755f-4552-ad09-9a090cda105d"));
        existingPool.setStatus(StoragePoolStatus.Up);
        vds = new Guid("afce7a39-8e8c-4819-ba9c-796d316592e6");
        vdsGroup = new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d1");
        storageDomain = new Guid("72e3a666-89e1-4005-a7ca-f7548004a9ab");

        newPool = new StoragePool();
        newPool.setName("newPoolDude");
        newPool.setcompatibility_version(new Version("3.0"));

    }

    /**
     * Ensures that an invalid id results in a null pool.
     */
    @Test
    public void testGetWithInvalidId() {
        StoragePool result = dao.get(Guid.newGuid());

        assertNull(result);
    }

    /**
     * Ensures the right object is returned by id.
     */
    @Test
    public void testGet() {
        StoragePool result = dao.get(existingPool.getId());

        assertGetResult(result);
    }

    @Test
    public void testGetFilteredWithPermissions() {
        StoragePool result = dao.get(existingPool.getId(), PRIVILEGED_USER_ID, true);

        assertGetResult(result);
    }

    @Test
    public void testGetFilteredWithPermissionsNoPermissions() {
        StoragePool result = dao.get(existingPool.getId(), UNPRIVILEGED_USER_ID, true);

        assertNull(result);
    }

    @Test
    public void testGetFilteredWithPermissionsNoPermissionsAndNoFilter() {
        StoragePool result = dao.get(existingPool.getId(), UNPRIVILEGED_USER_ID, false);

        assertGetResult(result);
    }

    /**
     * Ensures an invalid name returns null.
     */
    @Test
    public void testGetByNameWithInvalidName() {
        StoragePool result = dao.getByName("farkle");

        assertNull(result);
    }

    /**
     * Ensures retrieving by name works as expected.
     */
    @Test
    public void testGetByName() {
        StoragePool result = dao.getByName(existingPool.getName());

        assertGetResult(result);
    }

    /**
     * Asserts the result of {@link StoragePoolDAO#get(Guid)} is correct
     * @param result The result to check
     */
    private void assertGetResult(StoragePool result) {
        assertNotNull(result);
        assertEquals(existingPool, result);
    }

    /**
     * Ensures the right pool is retrieves for the given VDS.
     */
    @Test
    public void testGetForVds() {
        StoragePool result = dao.getForVds(vds);

        assertNotNull(result);
    }

    /**
     * Ensures the right pool is returned.
     */
    @Test
    public void testGetForVdsGroup() {
        StoragePool result = dao.getForVdsGroup(vdsGroup);

        assertNotNull(result);
    }

    /**
     * Ensures that a collection of pools are returned.
     */
    @Test
    public void testGetAll() {
        List<StoragePool> result = dao.getAll();

        assertCorrectGetAllResult(result);
    }

    @Test
    public void testGetAllByStatus() {
        List<StoragePool> result = dao.getAllByStatus(StoragePoolStatus.Up);
        assertNotNull("list of returned pools in status up shouldn't be null", result);
        assertEquals("wrong number of storage pools returned for up status", 7, result.size());

        result = dao.getAllByStatus(StoragePoolStatus.Maintenance);
        assertNotNull("list of returned pools in maintenance status shouldn't be null", result);
        assertEquals("wrong number of storage pool returned for maintenance status", 0, result.size());
    }

    /**
     * Ensures that retrieving storage pools works as expected for a privileged user.
     */
    @Test
    public void testGetAllWithPermissionsPrivilegedUser() {
        List<StoragePool> result = dao.getAll(PRIVILEGED_USER_ID, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(NUMBER_OF_POOLS_FOR_PRIVELEGED_USER, result.size());
        assertEquals(result.iterator().next(), existingPool);
    }

    /**
     * Ensures that retrieving storage pools works as expected with filtering disabled for an unprivileged user.
     */
    @Test
    public void testGetAllWithPermissionsDisabledUnprivilegedUser() {
        List<StoragePool> result = dao.getAll(UNPRIVILEGED_USER_ID, false);

        assertCorrectGetAllResult(result);
    }

    /**
     * Ensures that no storage pool retrieved for an unprivileged user with filtering enabled.
     */
    @Test
    public void testGetAllWithPermissionsUnprivilegedUser() {
        List<StoragePool> result = dao.getAll(UNPRIVILEGED_USER_ID, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures that all storage pools for the given domain are returned.
     */
    @Test
    public void testGetAllForStorageDomain() {
        List<StoragePool> result = dao.getAllForStorageDomain(storageDomain);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Ensures that multiple data centers are returned if an external network had been imported to more than one.
     */
    @Test
    public void testDataCentersByExternalNetworkId() {
        List<Guid> result = dao.getDcIdByExternalNetworkId(FixturesTool.EXTERNAL_NETWORK_ID);

        assertNotNull(result);
        assertTrue(result.size() > 1);
        assertTrue(!result.get(0).equals(result.get(1)));
    }

    /**
     * Ensures that no data centers are returned for an external network that hadn't been imported.
     */
    @Test
    public void testNoDataCentersByExternalNetworkId() {
        List<Guid> result = dao.getDcIdByExternalNetworkId("foo");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSave() {
        dao.save(newPool);

        StoragePool result = dao.getByName(newPool.getName());

        assertNotNull(result);
        assertEquals(newPool, result);
    }

    /**
     * Ensures that updating a storage pool works as expected.
     */
    @Test
    public void testUpdate() {
        existingPool.setdescription("Farkle");
        existingPool.setStoragePoolFormatType(StorageFormatType.V1);

        dao.update(existingPool);

        StoragePool result = dao.get(existingPool.getId());

        assertGetResult(result);
    }

    /**
     * Ensures that partial updating a storage pool works as expected.
     */
    @Test
    public void testPartialUpdate() {
        existingPool.setdescription("NewFarkle");
        existingPool.setQuotaEnforcementType(QuotaEnforcementTypeEnum.HARD_ENFORCEMENT);

        dao.updatePartial(existingPool);

        StoragePool result = dao.get(existingPool.getId());

        assertGetResult(result);
    }

    /**
     * Ensures that updating a storage pool status works as expected.
     */
    @Test
    public void testUpdateStatus() {
        dao.updateStatus(existingPool.getId(), StoragePoolStatus.NotOperational);
        existingPool.setStatus(StoragePoolStatus.NotOperational);

        StoragePool result = dao.get(existingPool.getId());

        assertGetResult(result);
    }

    @Test
    public void testIncreaseStoragePoolMasterVersion() {
        int result = dao.increaseStoragePoolMasterVersion(existingPool.getId());
        StoragePool dbPool = dao.get(existingPool.getId());
        assertEquals(result, existingPool.getmaster_domain_version() + 1);
        assertEquals(result, dbPool.getmaster_domain_version());
    }

    /**
     * Ensures that removing a storage pool works as expected.
     */
    @Test
    public void testRemove() {
        Guid poolId = existingPool.getId();
        VmAndTemplatesGenerationsDAO vmAndTemplatesGenerationsDAO = dbFacade.getVmAndTemplatesGenerationsDao();
        VmStaticDAO vmStaticDao = dbFacade.getVmStaticDao();

        assertFalse(vmStaticDao.getAllByStoragePoolId(poolId).isEmpty());
        vmStaticDao.incrementDbGenerationForAllInStoragePool(poolId);
        assertFalse(vmAndTemplatesGenerationsDAO.getVmsIdsForOvfUpdate(poolId).isEmpty());
        assertFalse(vmAndTemplatesGenerationsDAO.getVmTemplatesIdsForOvfUpdate(poolId).isEmpty());

        dao.remove(existingPool.getId());


        assertTrue(vmStaticDao.getAllByStoragePoolId(poolId).isEmpty());
        assertTrue(vmAndTemplatesGenerationsDAO.getVmsIdsForOvfUpdate(poolId).isEmpty());
        assertTrue(vmAndTemplatesGenerationsDAO.getVmTemplatesIdsForOvfUpdate(poolId).isEmpty());
        assertNull(dao.get(poolId));
    }

    /**
     * Asserts the result from {@link StoragePoolDAO#getAll()} is correct without filtering
     *
     * @param result A list of storage pools to assert
     */
    private static void assertCorrectGetAllResult(List<StoragePool> result) {
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGetStorageTypesInPoolForMixedTypes() {
        Guid poolId = FixturesTool.STORAGE_POOL_MIXED_TYPES;
        List<StorageType> storageTypes = dao.getStorageTypesInPool(poolId);
        assertStorageTypes(storageTypes, StorageType.POSIXFS, StorageType.NFS, StorageType.ISCSI);
    }

    @Test
    public void testGetStorageTypesInPoolForSingleType() {
        Guid poolId = FixturesTool.STORAGE_POOL_NFS;
        List<StorageType> storageTypes = dao.getStorageTypesInPool(poolId);
        assertStorageTypes(storageTypes, StorageType.NFS);
    }

    @Test
    public void testGetStorageTypesInPoolForNonExistingPool() {
        Guid poolId = Guid.newGuid();
        List<StorageType> storageTypes = dao.getStorageTypesInPool(poolId);
        assertTrue("Number of storage types in a non existing pool returned results", storageTypes.isEmpty());
    }

    @Test
    public void testGetStorageTypesInPoolForEmptyPool() {
        Guid poolId = FixturesTool.STORAGE_POOL_NO_DOMAINS;
        List<StorageType> storageTypes = dao.getStorageTypesInPool(poolId);
        assertTrue("Number of storage types in a non existing pool returned results", storageTypes.isEmpty());
    }

    private void assertStorageTypes(List<StorageType> typesList, StorageType... expectedTypes) {
        assertEquals("Number of storage types different than expected storage type in the pool", typesList.size(), expectedTypes.length);

        List<StorageType> expectedTypesList = new ArrayList<>(Arrays.asList(expectedTypes));

        expectedTypesList.removeAll(typesList);

        assertTrue(String.format("The following storage types were expected but were missing: %s", expectedTypesList), expectedTypesList.isEmpty());

    }
}
