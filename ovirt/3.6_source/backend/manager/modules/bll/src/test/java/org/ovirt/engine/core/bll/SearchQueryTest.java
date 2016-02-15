package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.ovirt.engine.core.bll.quota.QuotaManager;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.network.NetworkView;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.utils.CommonConstants;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.QuotaDao;
import org.ovirt.engine.core.dao.StoragePoolDao;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.VdsGroupDao;
import org.ovirt.engine.core.dao.VmDao;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;
import org.ovirt.engine.core.dao.network.NetworkViewDao;
import org.ovirt.engine.core.searchbackend.SearchObjectAutoCompleter;
import org.ovirt.engine.core.searchbackend.SearchObjects;
import org.ovirt.engine.core.utils.MockConfigRule;

public class SearchQueryTest extends DbDependentTestBase {

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.SearchResultsLimit, 100),
            mockConfig(ConfigValues.DBSearchTemplate,
                    "SELECT * FROM (SELECT *, ROW_NUMBER() OVER(%1$s) as RowNum FROM (%2$s)) as T1 ) as T2 %3$s"),
            mockConfig(ConfigValues.MinimumPercentageToUpdateQuotaCache, 60)
            );

    List<Disk> diskImageResultList = new ArrayList<Disk>();
    List<Quota> quotaResultList = new ArrayList<Quota>();
    List<VM> vmResultList = new ArrayList<VM>();
    List<VDS> vdsResultList = new ArrayList<VDS>();
    List<VDSGroup> vdsGroupResultList = new ArrayList<VDSGroup>();
    List<StoragePool> storagePoolResultList = new ArrayList<StoragePool>();
    List<GlusterVolumeEntity> glusterVolumeList = new ArrayList<GlusterVolumeEntity>();
    List<NetworkView> networkResultList = new ArrayList<NetworkView>();
    private DbFacade facadeMock;

    @Before
    public void setup() {
        facadeMock = DbFacade.getInstance();
        final DiskDao diskDao = Mockito.mock(DiskDao.class);
        final QuotaDao quotaDao = Mockito.mock(QuotaDao.class);
        final VmDao vmDao = Mockito.mock(VmDao.class);
        final VdsDao vdsDao = Mockito.mock(VdsDao.class);
        final VdsGroupDao vdsGroupDao = Mockito.mock(VdsGroupDao.class);
        final StoragePoolDao storagePoolDao = Mockito.mock(StoragePoolDao.class);
        final GlusterVolumeDao glusterVolumeDao = Mockito.mock(GlusterVolumeDao.class);
        final NetworkViewDao networkViewDao = Mockito.mock(NetworkViewDao.class);

        Mockito.when(facadeMock.getDiskDao()).thenReturn(diskDao);
        Mockito.when(facadeMock.getQuotaDao()).thenReturn(quotaDao);
        Mockito.when(facadeMock.getVmDao()).thenReturn(vmDao);
        Mockito.when(facadeMock.getVdsDao()).thenReturn(vdsDao);
        Mockito.when(facadeMock.getVdsGroupDao()).thenReturn(vdsGroupDao);
        Mockito.when(facadeMock.getStoragePoolDao()).thenReturn(storagePoolDao);
        Mockito.when(facadeMock.getGlusterVolumeDao()).thenReturn(glusterVolumeDao);
        Mockito.when(facadeMock.getNetworkViewDao()).thenReturn(networkViewDao);
        // mock Daos
        mockDiskSao(diskDao);
        mockQuotaDao(quotaDao);
        mockVMDao(vmDao);
        mockVdsDao(vdsDao);
        mockVdsGroupDao(vdsGroupDao);
        mockStoragePoolDao(storagePoolDao);
        mockGlusterVolumeDao(glusterVolumeDao);
        mockNetworkDao(networkViewDao);
    }

    /**
     * Mock disk Dao so that when getAllWithQuery will be called with the appropriate query string, a unique list will
     * be returned. <BR/>
     * This returned list will indicate, if the correct string has been passed as an argument to the getAllWithQuery
     * API.
     * @param diskDao
     *            - The dao to be used
     */
    private void mockDiskSao(final DiskDao diskDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(diskDao.getAllWithQuery(Matchers.matches(getDiskImageRegexString(search))))
                .thenReturn(diskImageResultList);
    }

    private void mockQuotaDao(final QuotaDao quotaDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(quotaDao.getAllWithQuery(Matchers.matches(getQuotaRegexString(search))))
                .thenReturn(quotaResultList);
    }

    private static String getQuotaRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();

        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.QUOTA_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.QUOTA_OBJ_NAME, false))
                .append(".* ");
        return query.toString();

    }

    /**
     * Mock vds group Dao so that when getAllWithQuery will be called with the appropriate query string, a unique list
     * will be returned. <BR/>
     * This returned list will indicate, if the correct string has been passed as an argument to the getAllWithQuery
     * API.
     * @param vdsGroupDao
     *            - The dao to be used
     */
    private void mockVdsGroupDao(final VdsGroupDao vdsGroupDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(vdsGroupDao.getAllWithQuery(Matchers.matches(getVdsGroupRegexString(search))))
                .thenReturn(vdsGroupResultList);
    }

    /**
     * Mock storage pool Dao so that when getAllWithQuery will be called with the appropriate query string, a unique
     * list will be returned. <BR/>
     * This returned list will indicate, if the correct string has been passed as an argument to the getAllWithQuery
     * API.
     * @param storagePoolDao
     *            - The dao to be used
     */
    private void mockStoragePoolDao(final StoragePoolDao storagePoolDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(storagePoolDao.getAllWithQuery(Matchers.matches(getStoragePoolRegexString(search))))
                .thenReturn(storagePoolResultList);
    }

    /**
     * Mock Gluster Volume Dao so that when getAllWithQuery will be called with the appropriate query string, a unique
     * list will be returned. <BR/>
     * This returned list will indicate, if the correct string has been passed as an argument to the getAllWithQuery
     * API.
     * @param glusterVolumeDao
     *            - The dao to be used
     */
    private void mockGlusterVolumeDao(final GlusterVolumeDao glusterVolumeDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(glusterVolumeDao.getAllWithQuery(Matchers.matches(getGlusterVolumeRegexString(search))))
                .thenReturn(glusterVolumeList);
    }

    private void mockNetworkDao(final NetworkViewDao networkViewDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(networkViewDao.getAllWithQuery(Matchers.matches(getNetworkRegexString(search))))
                .thenReturn(networkResultList);
    }

    /**
     * Regex string which contains all of the disk image properties.
     * @param search
     */
    private static String getDiskImageRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();

        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.DISK_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.DISK_OBJ_NAME, false))
                .append(".* ");
        return query.toString();
    }

    /**
     * Mock Vds Dao so that when getAllWithQuery will be called with the appropriate query string, a unique list will be
     * returned. <BR/>
     * This returned list will indicate, if the correct string has been passed as an argument to the getAllWithQuery
     * API.
     * @param diskImageDao
     *            - The dao to be used
     */
    private void mockVdsDao(final VdsDao vdsDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(vdsDao.getAllWithQuery(Matchers.matches(getVdsRegexString(search))))
                .thenReturn(vdsResultList);
    }

    /**
     * Mock VM Dao so that when getAllWithQuery will be called with the appropriate query string, a unique list will be
     * returned. <BR/>
     * This returned list will indicate, if the correct string has been passed as an argument to the getAllWithQuery
     * API.
     * @param vmDao
     *            - The dao to be used
     */
    private void mockVMDao(final VmDao vmDao) {
        SearchObjectAutoCompleter search = new SearchObjectAutoCompleter();
        Mockito.when(vmDao.getAllUsingQuery(Matchers.matches(getVMRegexString(search))))
                .thenReturn(vmResultList);
    }

    /**
     * Regex string which contains all of the VM properties.
     * @param search
     */
    private static String getVMRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();
        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.VM_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.VM_OBJ_NAME, false))
                .append(".* ");
        return query.toString();
    }

    /**
     * Regex string which contains all of the VDS properties.
     * @param search
     */
    private static String getVdsRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();

        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.VDS_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.VDS_OBJ_NAME, false))
                .append(".* ");
        return query.toString();
    }

    /**
     * Regex string which contains all of the Vds group properties.
     * @param search
     */
    private static String getVdsGroupRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();

        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.VDC_CLUSTER_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.VDC_CLUSTER_OBJ_NAME, false))
                .append(".* ");
        return query.toString();
    }

    /**
     * Regex string which contains all of the storage pool properties.
     * @param search
     */
    private static String getStoragePoolRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();

        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME, false))
                .append(".* ");
        return query.toString();
    }

    /**
     * Regex string which contains all of the Gluster Volume properties.
     * @param search
     */
    private static String getGlusterVolumeRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();

        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.GLUSTER_VOLUME_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.GLUSTER_VOLUME_OBJ_NAME, false))
                .append(".* ");
        return query.toString();
    }

    /**
     * Regex string which contains all of the Network properties.
     * @param search
     */
    private static String getNetworkRegexString(SearchObjectAutoCompleter search) {
        StringBuilder query = new StringBuilder();
        query.append(".*")
                .append(search.getDefaultSort(SearchObjects.NETWORK_OBJ_NAME))
                .append(".*")
                .append(search.getRelatedTableName(SearchObjects.NETWORK_OBJ_NAME, false))
                .append(".* ");
        return query.toString();
    }

    private void mockInjections(SearchQuery<SearchParameters> searchQuery) {
        QuotaManager quotaManager = mock(QuotaManager.class);
        doNothing().when(quotaManager).updateUsage(anyListOf(Quota.class));
        when(searchQuery.getQuotaManager()).thenReturn(quotaManager);
    }

    private SearchQuery<SearchParameters> spySearchQuery(SearchParameters searchParam) {
        SearchQuery<SearchParameters> searchQuery = spy(new SearchQuery<SearchParameters>(searchParam));
        mockInjections(searchQuery);
        return searchQuery;
    }

    @Test
    public void testGetAllMultiDiskImageSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Disks" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.Disk);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(diskImageResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllDiskImageSearch() throws Exception {
        // The query Should be used is : "SELECT * FROM (SELECT *, ROW_NUMBER() OVER( ORDER BY disk_name ASC ) as RowNum
        // FROM (SELECT * FROM vm_images_view WHERE ( image_guid IN (SELECT vm_images_view.image_guid FROM
        // vm_images_view ))) as T1 ) as T2"
        SearchParameters searchParam = new SearchParameters("Disk" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.Disk);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(diskImageResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllVMSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("VM" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.VM);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(vmResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllMultiVmSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("VMs" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.VM);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(vmResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllVdsSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Host" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.VDS);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(vdsResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllMultiVdsSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Hosts" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.VDS);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(vdsResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllClusterSearch() throws Exception {
        // The original query should be : SELECT * FROM (SELECT *, ROW_NUMBER() OVER( ORDER BY name ASC ) as RowNum FROM
        // (SELECT * FROM vds_groups WHERE ( vds_group_id IN (SELECT vds_groups_storage_domain.vds_group_id FROM
        // vds_groups_storage_domain ))) as T1 ) as T2
        SearchParameters searchParam = new SearchParameters("Cluster" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.Cluster);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(vdsGroupResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllMultiClusterSearch() throws Exception {
        // The original query should be : SELECT * FROM (SELECT *, ROW_NUMBER() OVER( ORDER BY name ASC ) as RowNum FROM
        // (SELECT * FROM vds_groups WHERE ( vds_group_id IN (SELECT vds_groups_storage_domain.vds_group_id FROM
        // vds_groups_storage_domain ))) as T1 ) as T2
        SearchParameters searchParam = new SearchParameters("Clusters" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.Cluster);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(vdsGroupResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllStoragePoolSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Datacenter" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.StoragePool);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(storagePoolResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    // TODO: Search using search text "Datacenters:" is not supported.
    @Ignore
    @Test
    public void testGetAllMultiStoragePoolSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Datacenters" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.StoragePool);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(storagePoolResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllGlusterVolumesSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Volumes" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.GlusterVolume);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(glusterVolumeList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllQuotaSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Quota" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.Quota);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(quotaResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }

    @Test
    public void testGetAllNetworkSearch() throws Exception {
        SearchParameters searchParam = new SearchParameters("Network" + CommonConstants.QUERY_RETURN_TYPE_SEPARATOR, SearchType.Network);
        SearchQuery<SearchParameters> searchQuery = spySearchQuery(searchParam);
        searchQuery.executeQueryCommand();
        assertTrue(networkResultList == searchQuery.getQueryReturnValue().getReturnValue());
    }
}
