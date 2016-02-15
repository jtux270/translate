package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.NumaTuneMode;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.SerialNumberPolicy;
import org.ovirt.engine.core.common.businessentities.SsoMethod;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public abstract class VmBaseDaoDbFacade<T extends VmBase> extends DefaultGenericDaoDbFacade<T, Guid> {
    public VmBaseDaoDbFacade(String entityStoredProcedureName) {
        super(entityStoredProcedureName);
    }

    protected MapSqlParameterSource createBaseParametersMapper(T entity) {
        return createIdParameterMapper(entity.getId())
                .addValue("description", entity.getDescription())
                .addValue("free_text_comment", entity.getComment())
                .addValue("creation_date", entity.getCreationDate())
                .addValue("mem_size_mb", entity.getMemSizeMb())
                .addValue("vnc_keyboard_layout", entity.getVncKeyboardLayout())
                .addValue("tunnel_migration", entity.getTunnelMigration())
                .addValue("vds_group_id", entity.getVdsGroupId())
                .addValue("num_of_sockets", entity.getNumOfSockets())
                .addValue("cpu_per_socket", entity.getCpuPerSocket())
                .addValue("os", entity.getOsId())
                .addValue("num_of_monitors", entity.getNumOfMonitors())
                .addValue("single_qxl_pci", entity.getSingleQxlPci())
                .addValue("allow_console_reconnect", entity.isAllowConsoleReconnect())
                .addValue("vm_type", entity.getVmType())
                .addValue("priority", entity.getPriority())
                .addValue("auto_startup", entity.isAutoStartup())
                .addValue("is_stateless", entity.isStateless())
                .addValue("is_smartcard_enabled", entity.isSmartcardEnabled())
                .addValue("is_delete_protected", entity.isDeleteProtected())
                .addValue("sso_method", entity.getSsoMethod().toString())
                .addValue("iso_path", entity.getIsoPath())
                .addValue("usb_policy", entity.getUsbPolicy())
                .addValue("time_zone", entity.getTimeZone())
                .addValue("fail_back", entity.isFailBack())
                .addValue("nice_level", entity.getNiceLevel())
                .addValue("cpu_shares", entity.getCpuShares())
                .addValue("default_boot_sequence", entity.getDefaultBootSequence())
                .addValue("default_display_type", entity.getDefaultDisplayType())
                .addValue("origin", entity.getOrigin())
                .addValue("initrd_url", entity.getInitrdUrl())
                .addValue("kernel_url", entity.getKernelUrl())
                .addValue("kernel_params", entity.getKernelParams())
                .addValue("quota_id", entity.getQuotaId())
                .addValue("migration_support", entity.getMigrationSupport().getValue())
                .addValue("dedicated_vm_for_vds", entity.getDedicatedVmForVds())
                .addValue("min_allocated_mem", entity.getMinAllocatedMem())
                .addValue("is_run_and_pause", entity.isRunAndPause())
                .addValue("created_by_user_id", entity.getCreatedByUserId())
                .addValue("migration_downtime", entity.getMigrationDowntime())
                .addValue("serial_number_policy", entity.getSerialNumberPolicy() == null ? null : entity.getSerialNumberPolicy().getValue())
                .addValue("custom_serial_number", entity.getCustomSerialNumber())
                .addValue("is_boot_menu_enabled", entity.isBootMenuEnabled())
                .addValue("is_spice_file_transfer_enabled", entity.isSpiceFileTransferEnabled())
                .addValue("is_spice_copy_paste_enabled", entity.isSpiceCopyPasteEnabled())
                .addValue("cpu_profile_id", entity.getCpuProfileId())
                .addValue("numatune_mode", entity.getNumaTuneMode().getValue())
                .addValue("predefined_properties", entity.getPredefinedProperties())
                .addValue("userdefined_properties", entity.getUserDefinedProperties());
    }

    /**
     * The common basic rowmapper for properties in VmBase.
     *  @param <T> a subclass of VmBase.
     */
    protected abstract static class AbstractVmRowMapper<T extends VmBase> implements RowMapper<T> {

        protected final void map(final ResultSet rs, final T entity) throws SQLException {
            entity.setMemSizeMb(rs.getInt("mem_size_mb"));
            entity.setOsId(rs.getInt("os"));
            entity.setNumOfMonitors(rs.getInt("num_of_monitors"));
            entity.setSingleQxlPci(rs.getBoolean("single_qxl_pci"));
            entity.setDefaultDisplayType(DisplayType.forValue(rs.getInt("default_display_type")));
            entity.setDescription(rs.getString("description"));
            entity.setComment(rs.getString("free_text_comment"));
            entity.setCreationDate(DbFacadeUtils.fromDate(rs.getTimestamp("creation_date")));
            entity.setNumOfSockets(rs.getInt("num_of_sockets"));
            entity.setCpuPerSocket(rs.getInt("cpu_per_socket"));
            entity.setTimeZone(rs.getString("time_zone"));
            entity.setVmType(VmType.forValue(rs.getInt("vm_type")));
            entity.setUsbPolicy(UsbPolicy.forValue(rs.getInt("usb_policy")));
            entity.setFailBack(rs.getBoolean("fail_back"));
            entity.setDefaultBootSequence(BootSequence.forValue(rs.getInt("default_boot_sequence")));
            entity.setNiceLevel(rs.getInt("nice_level"));
            entity.setCpuShares(rs.getInt("cpu_shares"));
            entity.setPriority(rs.getInt("priority"));
            entity.setAutoStartup(rs.getBoolean("auto_startup"));
            entity.setStateless(rs.getBoolean("is_stateless"));
            entity.setDbGeneration(rs.getLong("db_generation"));
            entity.setIsoPath(rs.getString("iso_path"));
            entity.setOrigin(OriginType.forValue(rs.getInt("origin")));
            entity.setKernelUrl(rs.getString("kernel_url"));
            entity.setKernelParams(rs.getString("kernel_params"));
            entity.setInitrdUrl(rs.getString("initrd_url"));
            entity.setSmartcardEnabled(rs.getBoolean("is_smartcard_enabled"));
            entity.setDeleteProtected(rs.getBoolean("is_delete_protected"));
            entity.setSsoMethod(SsoMethod.fromString(rs.getString("sso_method")));
            entity.setTunnelMigration((Boolean) rs.getObject("tunnel_migration"));
            entity.setVncKeyboardLayout(rs.getString("vnc_keyboard_layout"));
            entity.setRunAndPause(rs.getBoolean("is_run_and_pause"));
            entity.setCreatedByUserId(Guid.createGuidFromString(rs.getString("created_by_user_id")));
            entity.setMigrationDowntime((Integer) rs.getObject("migration_downtime"));
            entity.setSerialNumberPolicy(SerialNumberPolicy.forValue((Integer) rs.getObject("serial_number_policy")));
            entity.setCustomSerialNumber(rs.getString("custom_serial_number"));
            entity.setBootMenuEnabled(rs.getBoolean("is_boot_menu_enabled"));
            entity.setSpiceFileTransferEnabled(rs.getBoolean("is_spice_file_transfer_enabled"));
            entity.setSpiceCopyPasteEnabled(rs.getBoolean("is_spice_copy_paste_enabled"));
            entity.setMigrationSupport(MigrationSupport.forValue(rs.getInt("migration_support")));
            entity.setDedicatedVmForVds(getGuid(rs, "dedicated_vm_for_vds"));
            entity.setMinAllocatedMem(rs.getInt("min_allocated_mem"));
            entity.setQuotaId(getGuid(rs, "quota_id"));
            entity.setCpuProfileId(getGuid(rs, "cpu_profile_id"));
            entity.setNumaTuneMode(NumaTuneMode.forValue(rs.getString("numatune_mode")));
            String predefinedProperties = rs.getString("predefined_properties");
            String userDefinedProperties = rs.getString("userdefined_properties");
            entity.setPredefinedProperties(predefinedProperties);
            entity.setUserDefinedProperties(userDefinedProperties);
            entity.setCustomProperties(VmPropertiesUtils.getInstance().customProperties(predefinedProperties,
                    userDefinedProperties));
        }
    }
}
