

----------------------------------------------------------------
-- [vm_templates] Table
--




Create or replace FUNCTION InsertVmTemplate(v_child_count INTEGER,
 v_creation_date TIMESTAMP WITH TIME ZONE,
 v_description VARCHAR(4000) ,
 v_free_text_comment text,
 v_mem_size_mb INTEGER,
 v_name VARCHAR(40),
 v_num_of_sockets INTEGER,
 v_cpu_per_socket INTEGER,
 v_os INTEGER,
 v_vmt_guid UUID,
 v_vds_group_id UUID,
 v_num_of_monitors INTEGER,
 v_single_qxl_pci BOOLEAN,
 v_allow_console_reconnect BOOLEAN,
 v_status INTEGER,
 v_usb_policy INTEGER,
 v_time_zone VARCHAR(40) ,
 v_fail_back BOOLEAN ,
 v_vm_type INTEGER ,
 v_nice_level INTEGER,
 v_cpu_shares INTEGER,
 v_default_boot_sequence INTEGER,
 v_default_display_type INTEGER,
 v_priority INTEGER,
 v_auto_startup BOOLEAN,
 v_is_stateless BOOLEAN,
 v_is_smartcard_enabled BOOLEAN,
 v_is_delete_protected BOOLEAN,
 v_sso_method VARCHAR(32),
 v_is_disabled BOOLEAN,
 v_iso_path VARCHAR(4000) ,
 v_origin INTEGER ,
 v_initrd_url    VARCHAR(4000) ,
 v_kernel_url    VARCHAR(4000) ,
 v_kernel_params VARCHAR(4000) ,
 v_quota_id UUID,
 v_migration_support integer,
 v_dedicated_vm_for_vds UUID,
 v_tunnel_migration BOOLEAN,
 v_vnc_keyboard_layout	VARCHAR(16),
 v_min_allocated_mem INTEGER,
 v_is_run_and_pause BOOLEAN,
 v_created_by_user_id UUID,
 v_template_type VARCHAR(40),
 v_migration_downtime INTEGER,
 v_base_template_id UUID,
 v_template_version_name VARCHAR(40),
 v_serial_number_policy SMALLINT,
 v_custom_serial_number VARCHAR(255),
 v_is_boot_menu_enabled BOOLEAN,
 v_is_spice_file_transfer_enabled BOOLEAN,
 v_is_spice_copy_paste_enabled BOOLEAN,
 v_cpu_profile_id UUID,
 v_numatune_mode VARCHAR(20),
 v_predefined_properties VARCHAR(4000),
 v_userdefined_properties VARCHAR(4000))

RETURNS VOID
   AS $procedure$
DECLARE
v_template_version_number INTEGER;
BEGIN
  -- get current max version and use next
  SELECT max(template_version_number) + 1 into v_template_version_number
  from vm_static
  where vmt_guid = v_base_template_id
        and entity_type = 'TEMPLATE';

  -- if no versions exist it might return null, so this is a new base template
  if v_template_version_number is null then
    v_template_version_number = 1;
  end if;

INSERT
INTO vm_static(
    child_count,
    creation_date,
    description,
    free_text_comment,
    mem_size_mb,
    vm_name,
    num_of_sockets,
    cpu_per_socket,
    os,
    vm_guid,
    vds_group_id,
    num_of_monitors,
    single_qxl_pci,
    allow_console_reconnect,
    template_status,
    usb_policy,
    time_zone,
    fail_back,
    vm_type,
    nice_level,
    cpu_shares,
    default_boot_sequence,
    default_display_type,
    priority,
    auto_startup,
    is_stateless,
    iso_path,
    origin,
    initrd_url,
    kernel_url,
    kernel_params,
    entity_type,
    quota_id,
    migration_support,
    is_disabled,
    dedicated_vm_for_vds,
    is_smartcard_enabled,
    is_delete_protected,
    sso_method,
    tunnel_migration,
    vnc_keyboard_layout,
    min_allocated_mem,
    is_run_and_pause,
    created_by_user_id,
    migration_downtime,
    template_version_number,
    vmt_guid,
    template_version_name,
    serial_number_policy,
    custom_serial_number,
    is_boot_menu_enabled,
    is_spice_file_transfer_enabled,
    is_spice_copy_paste_enabled,
    cpu_profile_id,
    numatune_mode,
    predefined_properties,
    userdefined_properties)
VALUES(
    v_child_count,
    v_creation_date,
    v_description,
    v_free_text_comment,
    v_mem_size_mb,
    v_name,
    v_num_of_sockets,
    v_cpu_per_socket,
    v_os,
    v_vmt_guid,
    v_vds_group_id,
    v_num_of_monitors,
    v_single_qxl_pci,
    v_allow_console_reconnect,
    v_status,
    v_usb_policy,
    v_time_zone,
    v_fail_back,
    v_vm_type,
    v_nice_level,
    v_cpu_shares,
    v_default_boot_sequence,
    v_default_display_type,
    v_priority,
    v_auto_startup,
    v_is_stateless,
    v_iso_path,
    v_origin,
    v_initrd_url,
    v_kernel_url,
    v_kernel_params,
    v_template_type,
    v_quota_id,
    v_migration_support,
    v_is_disabled,
    v_dedicated_vm_for_vds,
    v_is_smartcard_enabled,
    v_is_delete_protected,
    v_sso_method,
    v_tunnel_migration,
    v_vnc_keyboard_layout,
    v_min_allocated_mem,
    v_is_run_and_pause,
    v_created_by_user_id,
    v_migration_downtime,
    v_template_version_number,
    v_base_template_id,
    v_template_version_name,
    v_serial_number_policy,
    v_custom_serial_number,
    v_is_boot_menu_enabled,
    v_is_spice_file_transfer_enabled,
    v_is_spice_copy_paste_enabled,
    v_cpu_profile_id,
    v_numatune_mode,
    v_predefined_properties,
    v_userdefined_properties);
-- perform deletion from vm_ovf_generations to ensure that no record exists when performing insert to avoid PK violation.
DELETE FROM vm_ovf_generations gen WHERE gen.vm_guid = v_vmt_guid;
INSERT INTO vm_ovf_generations(vm_guid, storage_pool_id)
VALUES (v_vmt_guid, (SELECT storage_pool_id
                     FROM vds_groups vg
                     WHERE vg.vds_group_id = v_vds_group_id));
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION UpdateVmTemplate(v_child_count INTEGER,
 v_creation_date TIMESTAMP WITH TIME ZONE,
 v_description VARCHAR(4000) ,
 v_free_text_comment text,
 v_mem_size_mb INTEGER,
 v_name VARCHAR(40),
 v_num_of_sockets INTEGER,
 v_cpu_per_socket INTEGER,
 v_os INTEGER,
 v_vmt_guid UUID,
 v_vds_group_id UUID,
 v_num_of_monitors INTEGER,
 v_single_qxl_pci BOOLEAN,
 v_allow_console_reconnect BOOLEAN,
 v_status INTEGER,
 v_usb_policy INTEGER,
 v_time_zone VARCHAR(40) ,
 v_fail_back BOOLEAN ,
 v_vm_type INTEGER ,
 v_nice_level INTEGER,
 v_cpu_shares INTEGER,
 v_default_boot_sequence INTEGER,
 v_default_display_type INTEGER,
 v_priority INTEGER,
 v_auto_startup BOOLEAN,
 v_is_stateless BOOLEAN,
 v_is_smartcard_enabled BOOLEAN,
 v_is_delete_protected BOOLEAN,
 v_sso_method VARCHAR(32),
 v_is_disabled BOOLEAN,
 v_iso_path VARCHAR(4000) ,
 v_origin INTEGER ,
 v_initrd_url VARCHAR(4000) ,
 v_kernel_url VARCHAR(4000) ,
 v_kernel_params VARCHAR(4000),
 v_quota_id UUID,
 v_migration_support integer,
 v_dedicated_vm_for_vds uuid,
 v_tunnel_migration BOOLEAN,
 v_vnc_keyboard_layout VARCHAR(16),
 v_min_allocated_mem INTEGER,
 v_is_run_and_pause BOOLEAN,
 v_created_by_user_id UUID,
 v_template_type VARCHAR(40),
 v_migration_downtime INTEGER,
 v_template_version_name VARCHAR(40),
 v_serial_number_policy SMALLINT,
 v_custom_serial_number VARCHAR(255),
 v_is_boot_menu_enabled BOOLEAN,
 v_is_spice_file_transfer_enabled BOOLEAN,
 v_is_spice_copy_paste_enabled BOOLEAN,
 v_cpu_profile_id UUID,
 v_numatune_mode VARCHAR(20),
 v_predefined_properties VARCHAR(4000),
v_userdefined_properties VARCHAR(4000))
RETURNS VOID

	--The [vm_templates] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      UPDATE vm_static
      SET child_count = v_child_count,creation_date = v_creation_date,description = v_description, free_text_comment = v_free_text_comment,
      mem_size_mb = v_mem_size_mb,vm_name = v_name,num_of_sockets = v_num_of_sockets,
      cpu_per_socket = v_cpu_per_socket,os = v_os,
      vds_group_id = v_vds_group_id,num_of_monitors = v_num_of_monitors,
      single_qxl_pci = v_single_qxl_pci, allow_console_reconnect = v_allow_console_reconnect,
      template_status = v_status,usb_policy = v_usb_policy,time_zone = v_time_zone,
      fail_back = v_fail_back,
      vm_type = v_vm_type,
      nice_level = v_nice_level,
      cpu_shares = v_cpu_shares, default_boot_sequence = v_default_boot_sequence,
      default_display_type = v_default_display_type,
      priority = v_priority,auto_startup = v_auto_startup,is_stateless = v_is_stateless,
      iso_path = v_iso_path,origin = v_origin,initrd_url = v_initrd_url,
      kernel_url = v_kernel_url,kernel_params = v_kernel_params, _update_date = CURRENT_TIMESTAMP, quota_id = v_quota_id,
      migration_support = v_migration_support, dedicated_vm_for_vds = v_dedicated_vm_for_vds, is_smartcard_enabled = v_is_smartcard_enabled,
      is_delete_protected = v_is_delete_protected, sso_method = v_sso_method, is_disabled = v_is_disabled, tunnel_migration = v_tunnel_migration,
      vnc_keyboard_layout = v_vnc_keyboard_layout, min_allocated_mem = v_min_allocated_mem, is_run_and_pause = v_is_run_and_pause, created_by_user_id = v_created_by_user_id,
      migration_downtime = v_migration_downtime,
      template_version_name = v_template_version_name,
      serial_number_policy = v_serial_number_policy, custom_serial_number = v_custom_serial_number,
      is_boot_menu_enabled = v_is_boot_menu_enabled,
      is_spice_file_transfer_enabled = v_is_spice_file_transfer_enabled, is_spice_copy_paste_enabled = v_is_spice_copy_paste_enabled, cpu_profile_id = v_cpu_profile_id,
      numatune_mode = v_numatune_mode,
      predefined_properties = v_predefined_properties,userdefined_properties = v_userdefined_properties
      WHERE vm_guid = v_vmt_guid
      AND   entity_type = v_template_type;

      -- update template versions to new name
      update vm_static
        set vm_name = v_name
      where vm_guid <> v_vmt_guid and vmt_guid = v_vmt_guid and entity_type = v_template_type;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION UpdateVmTemplateStatus(
        v_vmt_guid UUID,
        v_status INTEGER)
RETURNS VOID

   AS $procedure$
BEGIN
      UPDATE vm_static
      SET    template_status = v_status
      WHERE  vm_guid = v_vmt_guid;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION DeleteVmTemplates(v_vmt_guid UUID)
RETURNS VOID
   AS $procedure$
   DECLARE
   v_val  UUID;
BEGIN
        -- Get (and keep) a shared lock with "right to upgrade to exclusive"
		-- in order to force locking parent before children
      select   vm_guid INTO v_val FROM vm_static  WHERE vm_guid = v_vmt_guid FOR UPDATE;
      DELETE FROM vm_static
      WHERE vm_guid = v_vmt_guid;
		-- delete Template permissions --
      DELETE FROM permissions where object_id = v_vmt_guid;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetAllFromVmTemplates(v_user_id UUID, v_is_filtered boolean, v_entity_type VARCHAR(32)) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT vm_templates.*
      FROM vm_templates_view vm_templates
      WHERE entity_type = v_entity_type
      AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                          FROM   user_vm_template_permissions_view
                                          WHERE  user_id = v_user_id AND entity_id = vmt_guid))
      ORDER BY name;
END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION GetTemplateCount() RETURNS SETOF BIGINT STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT count (vm_templates.*)
      FROM vm_templates_view vm_templates;
END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION GetVmTemplatesByIds(v_vm_templates_ids VARCHAR(5000)) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
RETURN QUERY SELECT vm_templates.*
   FROM vm_templates_view vm_templates
   WHERE vm_templates.vmt_guid IN (SELECT * FROM fnSplitterUuid(v_vm_templates_ids));
END; $procedure$
LANGUAGE plpgsql;







Create or replace FUNCTION getAllVmTemplatesRelatedToQuotaId(v_quota_id UUID) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
	RETURN QUERY SELECT vm_templates.*
      FROM vm_templates_view vm_templates
      WHERE quota_id = v_quota_id
      UNION
      SELECT DISTINCT vm_templates.*
      FROM vm_templates_view vm_templates
      INNER JOIN vm_device vd ON vd.vm_id = vm_templates.vmt_guid
      INNER JOIN images ON images.image_group_id = vd.device_id AND images.active = TRUE
      INNER JOIN image_storage_domain_map ON image_storage_domain_map.image_id = images.image_guid
      WHERE image_storage_domain_map.quota_id = v_quota_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetVmTemplateByVmtGuid(v_vmt_guid UUID, v_user_id UUID, v_is_filtered boolean) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT vm_templates.*
      FROM vm_templates_view vm_templates
      WHERE vmt_guid = v_vmt_guid
      AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                        FROM   user_vm_template_permissions_view
                                        WHERE  user_id = v_user_id AND entity_id = v_vmt_guid));
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetVmTemplateByVmtName(v_vmt_name VARCHAR(255), v_user_id UUID, v_is_filtered boolean) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT vm_templates.*
      FROM vm_templates_view vm_templates
      WHERE name = v_vmt_name
      AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                        FROM   user_vm_template_permissions_view
                                        WHERE  user_id = v_user_id AND entity_id = vmt_guid));
END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION GetVmTemplateByVdsGroupId(v_vds_group_id UUID) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT vm_templates.*
      FROM vm_templates_view vm_templates
      WHERE vds_group_id = v_vds_group_id;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION GetVmTemplatesByStoragePoolId(v_storage_pool_id UUID) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT DISTINCT vm_templates.*
      FROM vm_templates_view vm_templates
      where vm_templates.storage_pool_id = v_storage_pool_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetVmTemplatesByImageId(v_image_guid UUID) RETURNS SETOF vm_templates_with_plug_info STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT *
      FROM vm_templates_with_plug_info t
      WHERE t.image_guid = v_image_guid;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetVmTemplatesByStorageDomainId(v_storage_domain_id UUID, v_user_id UUID, v_is_filtered boolean) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT DISTINCT vm_templates.*
      FROM vm_templates_view vm_templates
      INNER JOIN vm_device vd ON vd.vm_id = vm_templates.vmt_guid
      INNER JOIN images i ON i.image_group_id = vd.device_id AND i.active = TRUE
      where i.image_guid in(select image_id from image_storage_domain_map where storage_domain_id = v_storage_domain_id)
      AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                      FROM   user_vm_template_permissions_view
                                      WHERE  user_id = v_user_id AND entity_id = vm_templates.vmt_guid));
END; $procedure$
LANGUAGE plpgsql;


--This SP returns all templates with permissions to run the given action by user
Create or replace FUNCTION fn_perms_get_templates_with_permitted_action(v_user_id UUID, v_action_group_id integer) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT vm_templates_view.*
      FROM vm_templates_view, user_vm_template_permissions_view
      WHERE vm_templates_view.vmt_guid = user_vm_template_permissions_view.entity_id AND
            user_vm_template_permissions_view.user_id = v_user_id AND
            (SELECT get_entity_permissions(v_user_id, v_action_group_id, vm_templates_view.vmt_guid, 4) IS NOT NULL);
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetVmTemplatesByNetworkId(v_network_id UUID) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vm_templates_view
   WHERE EXISTS (
      SELECT 1
      FROM vm_interface
      INNER JOIN vnic_profiles
      ON vnic_profiles.id = vm_interface.vnic_profile_id
      WHERE vnic_profiles.network_id = v_network_id
      AND vm_interface.vmt_guid = vm_templates_view.vmt_guid);
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION GetVmTemplatesByVnicProfileId(v_vnic_profile_id UUID) RETURNS SETOF vm_templates_view STABLE
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vm_templates_view
   WHERE EXISTS (
      SELECT 1
      FROM vm_interface
      WHERE vm_interface.vnic_profile_id = v_vnic_profile_id
      AND vm_interface.vmt_guid = vm_templates_view.vmt_guid);
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetTemplateVersionsForBaseTemplate(v_base_template_id UUID) RETURNS SETOF vm_templates_view STABLE
AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   from vm_templates_view
   where base_template_id = v_base_template_id and vmt_guid != v_base_template_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetTemplateWithLatestVersionInChain(v_template_id UUID) RETURNS SETOF vm_templates_view STABLE
AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   from vm_templates_view
   where base_template_id =
      (select vmt_guid from vm_static where vm_guid = v_template_id)
   order by template_version_number desc
   limit 1;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetAllVmTemplatesWithDisksOnOtherStorageDomain(v_storage_domain_id UUID) RETURNS SETOF vm_templates_view STABLE
AS $procedure$
BEGIN
RETURN QUERY SELECT DISTINCT templates.*
      FROM vm_templates_view templates
      INNER JOIN (SELECT vm_static.vm_guid
                  FROM vm_static
                  INNER JOIN vm_device vd ON vd.vm_id = vm_static.vm_guid
                  INNER JOIN images i ON i.image_group_id = vd.device_id
                  INNER JOIN (SELECT image_id
                              FROM image_storage_domain_map
                              WHERE image_storage_domain_map.storage_domain_id = v_storage_domain_id) isd_map
                              ON i.image_guid = isd_map.image_id WHERE entity_type = 'TEMPLATE') vms_with_disks_on_storage_domain ON templates.vmt_guid = vms_with_disks_on_storage_domain.vm_guid
      INNER JOIN vm_device vd ON vd.vm_id = templates.vmt_guid
      INNER JOIN images i ON i.image_group_id = vd.device_id
      INNER JOIN image_storage_domain_map on i.image_guid = image_storage_domain_map.image_id
      WHERE image_storage_domain_map.storage_domain_id != v_storage_domain_id;
END; $procedure$
LANGUAGE plpgsql;
