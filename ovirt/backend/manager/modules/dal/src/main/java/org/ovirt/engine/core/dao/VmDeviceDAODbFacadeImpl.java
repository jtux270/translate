package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.MapSqlParameterMapper;
import org.ovirt.engine.core.utils.SerializationFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

public class VmDeviceDAODbFacadeImpl extends
        MassOperationsGenericDaoDbFacade<VmDevice, VmDeviceId> implements VmDeviceDAO {

    public VmDeviceDAODbFacadeImpl() {
        super("VmDevice");
        setProcedureNameForGet("GetVmDeviceByDeviceId");
        setProcedureNameForGetAll("GetAllFromVmDevice");
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(VmDeviceId id) {
        return getCustomMapSqlParameterSource()
                .addValue("device_id", id.getDeviceId())
                .addValue("vm_id", id.getVmId());
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(VmDevice entity) {

        return createIdParameterMapper(entity.getId())
                .addValue("device", entity.getDevice())
                .addValue("type", entity.getType().getValue())
                .addValue("address", entity.getAddress())
                .addValue("boot_order", entity.getBootOrder())
                .addValue("spec_params", SerializationFactory.getSerializer().serialize(entity.getSpecParams()))
                .addValue("is_managed", entity.getIsManaged())
                .addValue("is_plugged", entity.getIsPlugged())
                .addValue("is_readonly", entity.getIsReadOnly())
                .addValue("alias", entity.getAlias())
                .addValue("custom_properties",
                        SerializationFactory.getSerializer().serialize(entity.getCustomProperties()))
                .addValue("snapshot_id", entity.getSnapshotId())
                .addValue("logical_name", entity.getLogicalName());
    }

    @Override
    protected RowMapper<VmDevice> createEntityRowMapper() {
        return VmDeviceRowMapper.instance;
    }

    @Override
    public boolean exists(VmDeviceId id) {
        return get(id) != null;
    }

    @Override
    public List<VmDevice> getVmDeviceByVmId(Guid vmId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId);

        return getCallsHandler().executeReadList("GetVmDeviceByVmId",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<VmDevice> getVmDevicesByDeviceId(Guid deviceId, Guid vmId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("device_id", deviceId)
                .addValue("vm_id", vmId);

        return getCallsHandler().executeReadList("GetVmDeviceByDeviceId",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<VmDevice> getVmDeviceByVmIdAndType(Guid vmId, VmDeviceGeneralType type) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId)
                .addValue("type", type.getValue());
        return getCallsHandler().executeReadList("GetVmDeviceByVmIdAndType",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<VmDevice> getVmDeviceByVmIdTypeAndDevice(Guid vmId, VmDeviceGeneralType type, String device) {
        return getVmDeviceByVmIdTypeAndDevice(vmId, type, device, null, false);
    }

    @Override
    public List<VmDevice> getVmDeviceByVmIdTypeAndDevice
            (Guid vmId, VmDeviceGeneralType type, String device, Guid userID, boolean isFiltered) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId)
                .addValue("type", type.getValue())
                .addValue("device", device)
                .addValue("user_id", userID)
                .addValue("is_filtered", isFiltered);

        return getCallsHandler().executeReadList("GetVmDeviceByVmIdTypeAndDevice",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<VmDevice> getUnmanagedDevicesByVmId(Guid vmId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId);
        return getCallsHandler().executeReadList("GetVmUnmanagedDevicesByVmId",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public boolean isMemBalloonEnabled(Guid vmId) {

        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId);

        Map<String, Object> dbResults =
                new SimpleJdbcCall(jdbcTemplate).withFunctionName("isMemBalloonEnabled").execute(
                        parameterSource);

        String resultKey = dialect.getFunctionReturnKey();
        return dbResults.get(resultKey) != null ? ((Boolean) dbResults.get(resultKey)).booleanValue() : false;

    }

    static class VmDeviceRowMapper implements RowMapper<VmDevice> {

        public static VmDeviceRowMapper instance = new VmDeviceRowMapper();

        @SuppressWarnings("unchecked")
        @Override
        public VmDevice mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            VmDevice vmDevice = new VmDevice();

            vmDevice.setId(new VmDeviceId(getGuidDefaultEmpty(rs, "device_id"),
                    getGuidDefaultEmpty(rs, "vm_id")));
            vmDevice.setDevice(rs.getString("device"));
            vmDevice.setType(VmDeviceGeneralType.forValue(rs.getString("type")));
            vmDevice.setAddress(rs.getString("address"));
            vmDevice.setBootOrder(rs.getInt("boot_order"));
            vmDevice.setSpecParams(SerializationFactory.getDeserializer()
                    .deserializeOrCreateNew(rs.getString("spec_params"), HashMap.class));
            vmDevice.setIsManaged(rs.getBoolean("is_managed"));
            vmDevice.setIsPlugged(rs.getBoolean("is_plugged"));
            vmDevice.setIsReadOnly(rs.getBoolean("is_readonly"));
            vmDevice.setAlias(rs.getString("alias"));
            vmDevice.setCustomProperties(SerializationFactory.getDeserializer()
                    .deserializeOrCreateNew(rs.getString("custom_properties"), LinkedHashMap.class));
            vmDevice.setSnapshotId(getGuid(rs, "snapshot_id"));
            vmDevice.setLogicalName(rs.getString("logical_name"));
            return vmDevice;
        }
    }

    @Override
    public void removeAll(List<VmDeviceId> removedDeviceIds) {
        for (VmDeviceId vmDeviceId : removedDeviceIds) {
            remove(vmDeviceId);
        }
    }

    @Override
    public void saveAll(List<VmDevice> newVmDevices) {
        for (VmDevice vmDevice : newVmDevices) {
            save(vmDevice);
        }
    }

    @Override
    public void clearDeviceAddress(Guid deviceId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("device_id", deviceId);

        getCallsHandler().executeModification("clearVmDeviceAddress", parameterSource);

    }

    @Override
    public void updateRuntimeInfo(VmDevice vmDevice) {
        MapSqlParameterSource paramsForUpdate = createParameterSourceForUpdate(vmDevice)
                .addValue("address", vmDevice.getAddress())
                .addValue("alias", vmDevice.getAlias());

        getCallsHandler().executeModification("UpdateVmDeviceRuntimeInfo", paramsForUpdate);
    }

    private MapSqlParameterSource createParameterSourceForUpdate(VmDevice vmDevice) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmDevice.getVmId())
                .addValue("device_id", vmDevice.getDeviceId());
        return parameterSource;
    }

    @Override
    public void updateHotPlugDisk(VmDevice vmDevice) {
        MapSqlParameterSource paramsForUpdate = createParameterSourceForUpdate(vmDevice)
                .addValue("is_plugged", vmDevice.getIsPlugged());
        getCallsHandler().executeModification("UpdateVmDeviceForHotPlugDisk", paramsForUpdate);
    }

    @Override
    public void updateBootOrder(VmDevice vmDevice) {
        MapSqlParameterSource paramsForUpdate = createParameterSourceForUpdate(vmDevice)
                .addValue("boot_order", vmDevice.getBootOrder());
        getCallsHandler().executeModification("UpdateVmDeviceBootOrder", paramsForUpdate);
    }

    @Override
    public MapSqlParameterMapper<VmDevice> getBatchMapper() {
        return new MapSqlParameterMapper<VmDevice>() {
            @Override
            public MapSqlParameterSource map(VmDevice entity) {
                MapSqlParameterSource paramValue = new MapSqlParameterSource()
                        .addValue("device_id", entity.getDeviceId())
                        .addValue("vm_id", entity.getVmId())
                        .addValue("address", entity.getAddress())
                        .addValue("alias", entity.getAlias())
                        .addValue("type", entity.getType())
                        .addValue("is_managed", entity.getIsManaged())
                        .addValue("is_plugged", entity.getIsPlugged())
                        .addValue("is_readonly", entity.getIsReadOnly())
                        .addValue("spec_params", entity.getSpecParams())
                        .addValue("boot_order", entity.getBootOrder())
                        .addValue("snapshot_id", entity.getSnapshotId())
                        .addValue("logical_name", entity.getLogicalName())
                        .addValue("device", entity.getDevice());

                return paramValue;
            }
        };
    }

    public MapSqlParameterMapper<VmDevice> getBootOrderBatchMapper() {
        return new MapSqlParameterMapper<VmDevice>() {
            @Override
            public MapSqlParameterSource map(VmDevice entity) {
                MapSqlParameterSource paramValue = new MapSqlParameterSource()
                        .addValue("device_id", entity.getDeviceId())
                        .addValue("vm_id", entity.getVmId())
                        .addValue("boot_order", entity.getBootOrder());
                return paramValue;
            }
        };
    }

    @Override
    public void updateBootOrderInBatch(List<VmDevice> vmDevices) {
        updateAllInBatch("UpdateVmDeviceBootOrder", vmDevices, getBootOrderBatchMapper());
    }
}
