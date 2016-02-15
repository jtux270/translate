package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.VmEntityType;

/**
 * Abstract row mapper that maps the fields of {@link Disk}.
 *
 * @param <T> The type of disk to map for.
 */
abstract class AbstractDiskRowMapper<T extends Disk> extends AbstractBaseDiskRowMapper<T> {

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T entity = super.mapRow(rs, rowNum);
        entity.setNumberOfVms(rs.getInt("number_of_vms"));
        String vmNames = rs.getString("vm_names");
        entity.setVmNames(StringUtils.isEmpty(vmNames) ? null
                : new ArrayList<String>(Arrays.asList(vmNames.split(","))));
        String entityType = rs.getString("entity_type");
        handleEntityType(entityType, entity);

        return entity;
    }

    private static void handleEntityType(String entityType, Disk entity) {
        if (entityType != null && !entityType.isEmpty()) {
            VmEntityType vmEntityType = VmEntityType.valueOf(entityType);
            entity.setVmEntityType(vmEntityType);
        }
    }

}
