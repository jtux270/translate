

----------------------------------------------------------------
-- [image_group_storage_domain_map] Table
--




Create or replace FUNCTION Insertimage_storage_domain_map(v_image_id UUID,
    v_storage_domain_id UUID,
    v_quota_id UUID,
    v_disk_profile_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO image_storage_domain_map(image_id, storage_domain_id, quota_id, disk_profile_id)
	VALUES(v_image_id, v_storage_domain_id, v_quota_id, v_disk_profile_id);
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Deleteimage_storage_domain_map(v_image_id UUID,
	v_storage_domain_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN

   DELETE FROM image_storage_domain_map
   WHERE image_id = v_image_id AND storage_domain_id = v_storage_domain_id;

END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION Deleteimage_storage_domain_map_by_image_id(v_image_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN

   DELETE FROM image_storage_domain_map
   WHERE image_id = v_image_id;

END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION Getimage_storage_domain_mapByimage_id(v_image_id UUID)
RETURNS SETOF image_storage_domain_map STABLE
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM image_storage_domain_map
   WHERE image_id = v_image_id;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Getimage_storage_domain_mapBystorage_domain_id(v_storage_domain_id UUID) RETURNS SETOF image_storage_domain_map STABLE
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM image_storage_domain_map
   WHERE storage_domain_id = v_storage_domain_id;

END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION UpdateQuotaForImageAndSnapshots(v_disk_id UUID, v_storage_domain_id UUID, v_quota_id UUID)
RETURNS VOID
AS $procedure$
BEGIN
UPDATE image_storage_domain_map as isdm
    SET quota_id = v_quota_id
    FROM images as i
    WHERE i.image_group_id = v_disk_id AND i.image_guid = isdm.image_id AND storage_domain_id = v_storage_domain_id;
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION UpdateDiskProfileByImageGroupId(v_image_group_id UUID, v_storage_domain_id UUID, v_disk_profile_id UUID)
RETURNS VOID
AS $procedure$
BEGIN
UPDATE image_storage_domain_map as isdm
    SET disk_profile_id = v_disk_profile_id
    FROM images as i
    WHERE i.image_group_id = v_image_group_id AND i.image_guid = isdm.image_id AND storage_domain_id = v_storage_domain_id;
END; $procedure$
LANGUAGE plpgsql;

