package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.StorageDomainDynamic;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class StorageDomainDynamicDAODbFacadeImpl extends BaseDAODbFacade implements StorageDomainDynamicDAO{

    private static final class StorageDomainDynamicRowMapper implements RowMapper<StorageDomainDynamic> {
        public static final StorageDomainDynamicRowMapper instance = new StorageDomainDynamicRowMapper();

        @Override
        public StorageDomainDynamic mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            StorageDomainDynamic entity = new StorageDomainDynamic();
            entity.setAvailableDiskSize((Integer) rs
                    .getObject("available_disk_size"));
            entity.setId(getGuidDefaultEmpty(rs, "id"));
            entity.setUsedDiskSize((Integer) rs
                    .getObject("used_disk_size"));
            return entity;
        }
    }


    @Override
    public StorageDomainDynamic get(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("id", id);
        return getCallsHandler().executeRead("Getstorage_domain_dynamicByid",
                StorageDomainDynamicRowMapper.instance,
                parameterSource);
    }


    @Override
    public void save(StorageDomainDynamic domain) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("available_disk_size",
                        domain.getAvailableDiskSize())
                .addValue("id", domain.getId())
                .addValue("used_disk_size", domain.getUsedDiskSize());

        getCallsHandler().executeModification("Insertstorage_domain_dynamic", parameterSource);
    }

    @Override
    public void update(StorageDomainDynamic domain) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("available_disk_size",
                        domain.getAvailableDiskSize())
                .addValue("id", domain.getId())
                .addValue("used_disk_size", domain.getUsedDiskSize());

        getCallsHandler().executeModification("Updatestorage_domain_dynamic", parameterSource);
    }

    @Override
    public void remove(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("id", id);

        getCallsHandler().executeModification("Deletestorage_domain_dynamic", parameterSource);
    }


    @Override
    public List<StorageDomainDynamic> getAll() {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource();
        return getCallsHandler().executeReadList("GetAllFromstorage_domain_dynamic",
                StorageDomainDynamicRowMapper.instance,
                parameterSource);
    }

}
