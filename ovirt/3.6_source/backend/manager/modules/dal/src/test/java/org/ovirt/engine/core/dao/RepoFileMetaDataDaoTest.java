package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.storage.ImageFileType;
import org.ovirt.engine.core.common.businessentities.storage.RepoImage;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.dao.DuplicateKeyException;

public class RepoFileMetaDataDaoTest extends BaseDaoTestCase {

    private RepoFileMetaDataDao repoFileMetaDataDao;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        repoFileMetaDataDao = dbFacade.getRepoFileMetaDataDao();
    }

    /**
     * Ensures that saving a domain works as expected.
     */
    @Test
    public void testSave() {
        // Fetch the file from cache table
        List<RepoImage> listOfRepoFiles = repoFileMetaDataDao
                .getRepoListForStorageDomain(FixturesTool.STORAGE_DOAMIN_NFS_ISO,
                        ImageFileType.ISO);
        assertNotNull(listOfRepoFiles);
        assertTrue(listOfRepoFiles.isEmpty());

        RepoImage newRepoFileMap = getNewIsoRepoFile();
        repoFileMetaDataDao.addRepoFileMap(newRepoFileMap);

        listOfRepoFiles = repoFileMetaDataDao
                .getRepoListForStorageDomain(FixturesTool.STORAGE_DOAMIN_NFS_ISO,
                        ImageFileType.ISO);
        assertFalse(listOfRepoFiles.isEmpty());
    }

    /**
     * Test remove of repo file from storage domain.
     */
    @Test
    public void testRemove() {
        // Should get one iso file
        List<RepoImage> listOfRepoFiles = repoFileMetaDataDao
                .getRepoListForStorageDomain(FixturesTool.SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3,
                        ImageFileType.ISO);

        assertNotNull(listOfRepoFiles);
        assertFalse(listOfRepoFiles.isEmpty());

        // Remove the file from cache table
        repoFileMetaDataDao.removeRepoDomainFileList(FixturesTool.SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3,
                ImageFileType.ISO);
        listOfRepoFiles = getActiveIsoDomain();

        assertNotNull(listOfRepoFiles);
        assertTrue(listOfRepoFiles.isEmpty());
    }

    /**
     * Test foreign key when remove storage domain Iso.
     */
    @Test
    public void testRemoveByRemoveIsoDomain() {
        // Should get one iso file
        List<RepoImage> listOfRepoFiles = repoFileMetaDataDao
                .getRepoListForStorageDomain(FixturesTool.SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3,
                        ImageFileType.ISO);

        assertNotNull(listOfRepoFiles);
        assertFalse(listOfRepoFiles.isEmpty());

        // Test remove Iso
        StorageDomainDao storageDomainDao = dbFacade.getStorageDomainDao();
        storageDomainDao.remove(FixturesTool.SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3);
        listOfRepoFiles = getActiveIsoDomain();

        assertNotNull(listOfRepoFiles);
        assertTrue(listOfRepoFiles.isEmpty());
    }

    /**
     * Test fetch of all storage domains for all the repository files,
     * The fetch should fetch 4 rows, the first one is an empty storage domain,
     * The empty storage domain, should not have files, but should be fetched, since we want to refresh it.
     * The other three are from the same storage domain with three different types.
     */
    @Test
    public void testFetchAllIsoDomainInSystemNoDuplicate() {
        // Should get one iso file
        List<RepoImage> listOfAllIsoFiles = repoFileMetaDataDao
                .getAllRepoFilesForAllStoragePools(StorageDomainType.ISO,
                        StoragePoolStatus.Up,
                        StorageDomainStatus.Active,
                        VDSStatus.Up);

        // Should get only 4 files, 3 file types from one shared storage domain.
        // plus one empty file of the storage pool with no Iso at all.
        assertEquals(4, listOfAllIsoFiles.size());
    }

    /**
     * Test fetch of all storage domains for all the repository files,
     * The fetch should fetch 4 rows, the first one is an empty storage domain,
     * The empty storage domain, should not have files, but should be fetched, since we want to refresh it.
     * The other three are from the same storage domain with three different types.
     * In this test, we test the file types, to check if all were fetched.
     */
    @Test
    public void testFileTypeWhenFetchAllIsoDomainInSystem() {
        // Should get one iso file
        List<RepoImage> listOfAllIsoFiles = repoFileMetaDataDao
                .getAllRepoFilesForAllStoragePools(StorageDomainType.ISO,
                        StoragePoolStatus.Up,
                        StorageDomainStatus.Active,
                        VDSStatus.Up);

        List<ImageFileType> SharedStorageDomainFileType = new ArrayList<>();
        List<ImageFileType> EmptyStorageDomainFileType = new ArrayList<>();
        for (RepoImage fileMD : listOfAllIsoFiles) {
            Guid repoDomainId = fileMD.getRepoDomainId();
            if (repoDomainId.equals(FixturesTool.SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3)) {
                // Should have three types of files.
                SharedStorageDomainFileType.add(fileMD.getFileType());
            } else if (repoDomainId.equals(FixturesTool.STORAGE_DOAMIN_NFS_ISO)) {
                // Should have only one type (UNKNOWN)
                EmptyStorageDomainFileType.add(fileMD.getFileType());
            }
        }

        // Start the check
        // the shared storage domain, should have three types of files.
        assertEquals(3, SharedStorageDomainFileType.size());
        assertTrue(SharedStorageDomainFileType.contains(ImageFileType.Unknown));
        assertTrue(SharedStorageDomainFileType.contains(ImageFileType.ISO));
        assertTrue(SharedStorageDomainFileType.contains(ImageFileType.Floppy));

        // The empty storage domain, should not have files, but should be fetched, since we want to refresh it.
        assertEquals(1, EmptyStorageDomainFileType.size());
        assertTrue(EmptyStorageDomainFileType.contains(ImageFileType.Unknown));
    }

    /**
     * Test fetch of all storage pools and check if fetched the oldest file,
     * when fetching all the repository files.
     */
    @Test
    public void testFetchAllIsoDomainOldestFile() {
        List<RepoImage> listOfIsoFiles = repoFileMetaDataDao
                .getAllRepoFilesForAllStoragePools(StorageDomainType.ISO,
                        StoragePoolStatus.Up,
                        StorageDomainStatus.Active,
                        VDSStatus.Up);

        List<RepoImage> listOfFloppyFiles =
                repoFileMetaDataDao
                        .getRepoListForStorageDomain(FixturesTool.SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3,
                                ImageFileType.Floppy);

        long minLastRefreshed = 9999999999999L;
        for (RepoImage fileMD : listOfFloppyFiles) {
            long fileLastRefreshed = fileMD.getLastRefreshed();
            if (fileLastRefreshed < minLastRefreshed) {
                minLastRefreshed = fileLastRefreshed;
            }
        }

        // Check if fetched the oldest file when fetching all repository files.
        boolean isValid = true;
        for (RepoImage fileMetaData : listOfIsoFiles) {
            if (fileMetaData.getFileType() == ImageFileType.Floppy) {
                if (fileMetaData.getLastRefreshed() > minLastRefreshed) {
                    isValid = false;
                }
            }
        }
        assertTrue(isValid);
    }

    /**
     * Test when insert row and fetching it later.
     */
    @Test
    public void testInsertRepoFileAndFetchItAgain() {
        RepoImage newRepoFileMap = getNewIsoRepoFile();
        repoFileMetaDataDao.addRepoFileMap(newRepoFileMap);

        List<RepoImage> listOfRepoFiles = repoFileMetaDataDao
                .getRepoListForStorageDomain(FixturesTool.STORAGE_DOAMIN_NFS_ISO,
                        ImageFileType.ISO);

        assertNotNull(listOfRepoFiles);
        assertFalse(listOfRepoFiles.isEmpty());
        assertEquals(listOfRepoFiles.get(0).getRepoImageId(), newRepoFileMap.getRepoImageId());
        assertEquals(listOfRepoFiles.get(0).getLastRefreshed(), newRepoFileMap.getLastRefreshed());
        assertEquals(listOfRepoFiles.get(0).getSize(), newRepoFileMap.getSize());
        assertEquals(listOfRepoFiles.get(0).getRepoDomainId(), newRepoFileMap.getRepoDomainId());
    }

    /**
     * Test update of Iso file. The test demonstrate the refresh procedure. It first deletes the Iso file from the
     * repo_file_meta_data table, and then insert the new files fetched again from VDSM.
     */
    @Test
    public void testUpdateRepoFileByRemoveAndInsert() {
        RepoImage newRepoFileMap = getNewIsoRepoFile();
        repoFileMetaDataDao.addRepoFileMap(newRepoFileMap);

        // Fetch the file from cache table
        List<RepoImage> listOfRepoFiles = getActiveIsoDomain();

        // Get first file and update its String
        assertNotNull(listOfRepoFiles);
        assertFalse(listOfRepoFiles.isEmpty());
        RepoImage repoFile = listOfRepoFiles.get(0);
        assertNotNull(repoFile);
        String oldRepoImageId = repoFile.getRepoImageId();
        newRepoFileMap.setRepoImageId("updatedFileName"
                + newRepoFileMap.getRepoImageId());

        // Remove the file from cache table
        repoFileMetaDataDao.removeRepoDomainFileList(FixturesTool.STORAGE_DOAMIN_NFS_ISO, ImageFileType.ISO);

        // Add the new updated file into the cache table.
        repoFileMetaDataDao.addRepoFileMap(newRepoFileMap);

        // Fetch the updated File.
        listOfRepoFiles = getActiveIsoDomain();

        assertNotNull(listOfRepoFiles);
        assertFalse(listOfRepoFiles.isEmpty());
        RepoImage newRepoFile = listOfRepoFiles.get(0);
        assertNotNull(repoFile);

        // Check if not same file name as in the old file.
        assertNotSame(oldRepoImageId, newRepoFile.getRepoImageId());
    }

    /**
     * Test that the list returns is not null.
     */
    @Test
    public void testFetchExistingRepoFileListById() {
        List<RepoImage> listOfRepoFiles = getActiveIsoDomain();
        assertNotNull(listOfRepoFiles);
    }

    /**
     * Test primary key validity.
     */
    @Test (expected = DuplicateKeyException.class)
    public void testPrimaryKeyValidation() {
        RepoImage newRepoFileMap = getNewIsoRepoFile();
        repoFileMetaDataDao.addRepoFileMap(newRepoFileMap);
        assertTrue("Able to insert new file once", true);

        // Should enter here since its a violation of primary key
        repoFileMetaDataDao.addRepoFileMap(newRepoFileMap);
    }

    /**
     * Test that the list returns is not null, but is empty.
     */
    @Test
    public void testFetchNotExistingRepoFileListById() {
        Guid falseGuid = new Guid("11111111-1111-1111-1111-111111111111");
        List<RepoImage> listOfRepoFiles = repoFileMetaDataDao
                .getRepoListForStorageDomain(falseGuid,
                        ImageFileType.ISO);

        assertNotNull(listOfRepoFiles);
        assertTrue(listOfRepoFiles.isEmpty());
    }

    private static RepoImage getNewIsoRepoFile() {
        RepoImage newRepoFileMap = new RepoImage();
        newRepoFileMap.setFileType(ImageFileType.ISO);
        newRepoFileMap.setRepoImageId("isoDomain.iso");
        newRepoFileMap.setLastRefreshed(System.currentTimeMillis());
        newRepoFileMap.setSize(null);
        newRepoFileMap.setDateCreated(null);
        newRepoFileMap.setRepoDomainId(FixturesTool.STORAGE_DOAMIN_NFS_ISO);
        return newRepoFileMap;
    }

    private List<RepoImage> getActiveIsoDomain() {
        return repoFileMetaDataDao
                .getRepoListForStorageDomain(FixturesTool.SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3,
                        ImageFileType.ISO);
    }

}
