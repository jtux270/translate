package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.RowMapper;

/**
 * <code>StorageDomainDAODbFacadeImpl</code> provides an implementation of {@link StorageDomainDAO} based on code from
 * {@link org.ovirt.engine.core.dal.dbbroker.DbFacade}.
 */
@SuppressWarnings("synthetic-access")
public class StorageDomainDAODbFacadeImpl extends BaseDAODbFacade implements StorageDomainDAO {

    @Override
    public Guid getMasterStorageDomainIdForPool(Guid poolId) {
        return getStorageDomainId(poolId, StorageDomainType.Master);
    }

    @Override
    public StorageDomain getStorageDomain(Guid poolId, StorageDomainType type) {
        return getStorageDomain(poolId, type, null);
    }

    public StorageDomain getStorageDomain(Guid poolId, StorageDomainType type, StorageDomainStatus status) {
        Integer statusNum = status == null ? null : status.getValue();
        return getCallsHandler().executeRead("Getstorage_domain_by_type_storagePoolId_and_status",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("storage_pool_id", poolId)
                        .addValue("storage_domain_type", type.getValue())
                        .addValue("status", statusNum));
    }

    @Override
    public Guid getIsoStorageDomainIdForPool(Guid poolId, StorageDomainStatus status) {
        return getStorageDomainId(poolId, StorageDomainType.ISO, status);
    }

    @Override
    public StorageDomain get(Guid id) {
        return get(id, null, false);
    }

    @Override
    public StorageDomain get(Guid id, Guid userID, boolean isFiltered) {
        return getCallsHandler().executeRead("Getstorage_domains_By_id",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("user_id", userID)
                        .addValue("is_filtered", isFiltered));
    }

    @Override
    public StorageDomain getForStoragePool(Guid id, Guid storagepool) {
        return getCallsHandler().executeRead("Getstorage_domains_By_id_and_by_storage_pool_id",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("id", id).addValue("storage_pool_id", storagepool));
    }

    @Override
    public List<StorageDomain> getAllForConnection(String connection) {
        return getCallsHandler().executeReadList("Getstorage_domains_By_connection", StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("connection", connection));
    }

    @Override
    public List<StorageDomain> getAllByConnectionId(Guid connectionId) {
        return getCallsHandler().executeReadList("GetAllFromStorageDomainsByConnectionId",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("connection_id", connectionId));
    }

    @Override
    public List<StorageDomain> getAllForStoragePool(Guid pool) {
        return getAllForStoragePool(pool, null, false);
    }

    @Override
    public List<StorageDomain> getAllForStoragePool(Guid pool, Guid userID, boolean isFiltered) {
        return getCallsHandler().executeReadList("Getstorage_domains_By_storagePoolId",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("storage_pool_id", pool)
                        .addValue("user_id", userID)
                        .addValue("is_filtered", isFiltered));
    }

    @Override
    public List<StorageDomain> getAllForStorageDomain(Guid id) {
        return getCallsHandler().executeReadList("Getstorage_domains_List_By_storageDomainId",
                StorageDomainRowMapper.instance, getCustomMapSqlParameterSource()
                        .addValue("storage_domain_id", id));
    }

    @Override
    public List<StorageDomain> getAllWithQuery(String query) {
        return jdbcTemplate.query(query, StorageDomainRowMapper.instance);
    }

    @Override
    public List<StorageDomain> getAll() {
        return getAll(null, false);
    }

    @Override
    public List<StorageDomain> getAll(Guid userID, boolean isFiltered) {
        return getCallsHandler().executeReadList("GetAllFromstorage_domains",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("user_id", userID).addValue("is_filtered", isFiltered));
    }

    @Override
    public void remove(Guid id) {
        getCallsHandler().executeModification("Force_Delete_storage_domain", getCustomMapSqlParameterSource()
                .addValue("storage_domain_id", id));
    }

    @Override
    public void removeEntitesFromStorageDomain(Guid id) {
        getCallsHandler().executeModification("Remove_Entities_From_storage_domain", getCustomMapSqlParameterSource()
                .addValue("storage_domain_id", id));
    }

    @Override
    public List<StorageDomain> getAllStorageDomainsByImageId(Guid imageId) {
        return getCallsHandler().executeReadList("Getstorage_domains_List_By_ImageId",
                StorageDomainRowMapper.instance, getCustomMapSqlParameterSource()
                .addValue("image_id", imageId));
    }

    /**
     * Row mapper to map a returned row to a {@link StorageDomain} object.
     */
    private static final class StorageDomainRowMapper implements RowMapper<StorageDomain> {
        // single instance
        public static final StorageDomainRowMapper instance = new StorageDomainRowMapper();

        @Override
        public StorageDomain mapRow(final ResultSet rs, final int rowNum)
                throws SQLException {
            final StorageDomain entity = new StorageDomain();
            entity.setId(getGuidDefaultEmpty(rs, "id"));
            entity.setStorage(rs.getString("storage"));
            entity.setStorageName(rs.getString("storage_name"));
            entity.setDescription(rs.getString("storage_description"));
            entity.setStoragePoolId(getGuid(rs, "storage_pool_id"));
            entity.setComment(rs.getString("storage_comment"));
            entity.setStorageType(StorageType.forValue(rs.getInt("storage_type")));
            entity.setStoragePoolName(rs.getString("storage_pool_name"));
            entity.setStorageDomainType(StorageDomainType.forValue(rs.getInt("storage_domain_type")));
            entity.setStorageFormat(StorageFormatType.forValue(rs.getString("storage_domain_format_type")));
            entity.setAvailableDiskSize((Integer) rs.getObject("available_disk_size"));
            entity.setUsedDiskSize((Integer) rs.getObject("used_disk_size"));
            entity.setActualImagesSize(rs.getInt("actual_images_size"));
            entity.setCommittedDiskSize(rs.getInt("commited_disk_size"));
            entity.setStatus(StorageDomainStatus.forValue(rs.getInt("status")));
            entity.setStorageDomainSharedStatus(
                    StorageDomainSharedStatus.forValue(rs.getInt("storage_domain_shared_status")));
            entity.setAutoRecoverable(rs.getBoolean("recoverable"));
            entity.setContainsUnregisteredEntities(rs.getBoolean("contains_unregistered_entities"));
            entity.setLastTimeUsedAsMaster(rs.getLong("last_time_used_as_master"));
            return entity;
        }
    }

    @Override
    public List<StorageDomain> getAllByStoragePoolAndConnection(Guid storagePoolId, String connection) {
        return getCallsHandler().executeReadList("Getstorage_domains_By_storage_pool_id_and_connection",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("storage_pool_id", storagePoolId)
                        .addValue("connection", connection));
    }

    @Override
    public List<StorageDomain> listFailedAutorecoverables() {
        return getCallsHandler().executeReadList("GetFailingStorage_domains", StorageDomainRowMapper.instance, null);
    }

    @Override
    public List<StorageDomain> getPermittedStorageDomainsByStoragePool(Guid userId, ActionGroup actionGroup, Guid storagePoolId) {
        return getCallsHandler().executeReadList("Getstorage_domains_by_storage_pool_id_with_permitted_action",
                StorageDomainRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("user_id", userId)
                        .addValue("action_group_id", actionGroup.getId())
                        .addValue("storage_pool_id", storagePoolId));
    }

    @Override
    public List<Guid> getVmAndTemplatesIdsByStorageDomainId(Guid storageDomainId, boolean includeShareableDisks, boolean includeSnapshotDisks) {
        return getCallsHandler().executeReadList("GetVmAndTemplatesIdsByStorageDomainId",
                createGuidMapper(),
                getCustomMapSqlParameterSource()
                        .addValue("storage_domain_id", storageDomainId)
                        .addValue("include_shareable", includeShareableDisks)
                        .addValue("active_only", !includeSnapshotDisks));
    }

    @Override
    public long getNumberOfImagesInStorageDomain(Guid storageDomainId) {
        return getCallsHandler().executeRead("GetNumberOfImagesInStorageDomain",
                getLongMapper(),
                getCustomMapSqlParameterSource().addValue("storage_domain_id", storageDomainId));
    }

    /**
     * Gets the storage domain id of the given type for the given storage pool id.
     *
     * @param poolId The storage pool id,
     * @param type The storage domain type.
     * @return the storage domain id of the given type for the given storage pool id.
     */
    private Guid getStorageDomainId(Guid poolId, StorageDomainType type) {
        return getStorageDomainId(poolId, type, null);
    }

    /**
     * Gets the storage domain id of the given type for the given storage pool id, type and status.
     *
     * @param poolId The storage pool id,
     * @param type The storage domain type.
     * @return the storage domain id of the given type for the given storage pool id.
     */
    private Guid getStorageDomainId(Guid poolId, StorageDomainType type, StorageDomainStatus status) {
        Guid returnValue = Guid.Empty;
        StorageDomain domain = getStorageDomain(poolId, type, status);
        if (domain != null) {
            returnValue = domain.getId();
        }
        return returnValue;
    }
}
