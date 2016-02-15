package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.StorageDomainOvfInfo;
import org.ovirt.engine.core.common.businessentities.StorageDomainOvfInfoStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.GuidUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class StorageDomainOvfInfoDbFacadeImpl extends DefaultGenericDaoDbFacade<StorageDomainOvfInfo, Guid> implements StorageDomainOvfInfoDao{

    public StorageDomainOvfInfoDbFacadeImpl() {
        super("StorageDomainOvfInfo");
    }

    @Override
    public void updateOvfUpdatedInfo(Collection<Guid> ids, StorageDomainOvfInfoStatus status, StorageDomainOvfInfoStatus exceptStatus) {
        getCallsHandler().executeModification("UpdateOvfUpdatedInfo", getCustomMapSqlParameterSource()
                .addValue("storage_domains_ids", StringUtils.join(ids, ','))
                .addValue("status", status.getValue())
                .addValue("except_status", exceptStatus.getValue()));
    }

    @Override
    public List<Guid> loadStorageDomainIdsForOvfIds(Collection<Guid> ovfIds) {
        return getCallsHandler().executeReadList("LoadStorageDomainsForOvfIds",
                createGuidMapper(),
                getCustomMapSqlParameterSource().addValue("ovfs_ids", StringUtils.join(ovfIds, ",")));
    }

    private static final RowMapper<StorageDomainOvfInfo> storageDomainInfoRowMapper = new RowMapper<StorageDomainOvfInfo>() {

        @Override
        public StorageDomainOvfInfo mapRow(ResultSet resultSet, int i) throws SQLException {
            StorageDomainOvfInfo toReturn = new StorageDomainOvfInfo();
            toReturn.setStorageDomainId(getGuid(resultSet, "storage_domain_id"));
            toReturn.setStatus(StorageDomainOvfInfoStatus.forValue(resultSet.getInt("status")));
            toReturn.setOvfDiskId(getGuid(resultSet, "ovf_disk_id"));
            Timestamp timestamp = resultSet.getTimestamp("last_updated");
            if (timestamp != null) {
                toReturn.setLastUpdated(new Date(timestamp.getTime()));
            }
            String storedOvfs = resultSet.getString("stored_ovfs_ids");
            if (storedOvfs != null && !storedOvfs.isEmpty()) {
                toReturn.setStoredOvfIds(GuidUtils.getGuidListFromString(resultSet.getString("stored_ovfs_ids")));
            } else {
                toReturn.setStoredOvfIds(new LinkedList<Guid>());
            }
            return toReturn;
        }
    };

    @Override
    protected MapSqlParameterSource createFullParametersMapper(StorageDomainOvfInfo entity) {
        return createIdParameterMapper(entity.getId()).addValue("storage_domain_id", entity.getStorageDomainId())
                .addValue("status", entity.getStatus().getValue())
                .addValue("last_updated", entity.getLastUpdated())
                .addValue("stored_ovfs_ids", StringUtils.join(entity.getStoredOvfIds(), ','));
    }

    public List<StorageDomainOvfInfo> getAllForDomain(Guid guid) {
        return getCallsHandler().executeReadList("LoadStorageDomainInfoByDomainId",
                storageDomainInfoRowMapper,
                getCustomMapSqlParameterSource().addValue("storage_domain_id", guid));
    }

    @Override
    public StorageDomainOvfInfo get(Guid guid) {
        return getCallsHandler().executeRead("LoadStorageDomainInfoByDiskId",
                storageDomainInfoRowMapper,
                getCustomMapSqlParameterSource().addValue("disk_id", guid));
    }

    @Override
    public List<StorageDomainOvfInfo> getAll() {
        throw new NotImplementedException();
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid guid) {
        return getCustomMapSqlParameterSource().addValue("ovf_disk_id", guid);
    }

    @Override
    protected RowMapper<StorageDomainOvfInfo> createEntityRowMapper() {
        return storageDomainInfoRowMapper;
    }
}
