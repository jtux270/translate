





----------------------------------------------------------------
-- [users] Table
--


Create or replace FUNCTION InsertUser(v_department VARCHAR(255) ,
	v_domain VARCHAR(255),
	v_email VARCHAR(255) ,
	v_name VARCHAR(255) ,
	v_note VARCHAR(255) ,
	v_surname VARCHAR(255) ,
	v_user_id UUID,
	v_username VARCHAR(255),
	v_external_id TEXT,
	v_namespace VARCHAR(2048))
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO users(department, domain, email, name, note, surname, user_id, username, external_id,namespace)
	VALUES(v_department, v_domain, v_email, v_name, v_note, v_surname, v_user_id, v_username, v_external_id, v_namespace);
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION UpdateUserImpl(
	v_department VARCHAR(255) ,
	v_domain VARCHAR(255),
	v_email VARCHAR(255) ,
	v_name VARCHAR(255) ,
	v_note VARCHAR(255) ,
	v_surname VARCHAR(255) ,
	v_user_id UUID,
	v_username VARCHAR(255),
    v_external_id TEXT,
	v_namespace VARCHAR(2048))
RETURNS INTEGER

	--The [users] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
DECLARE
	updated_rows INT;
BEGIN
      UPDATE users
      SET department = v_department,domain = v_domain,
      email = v_email,name = v_name,note = v_note,
      surname = v_surname,
      username = v_username,
      external_id = v_external_id,
      namespace = v_namespace,
      _update_date = CURRENT_TIMESTAMP
      WHERE external_id = v_external_id AND domain = v_domain;
      GET DIAGNOSTICS updated_rows = ROW_COUNT;
      RETURN updated_rows;

END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION UpdateUser(
	v_department VARCHAR(255) ,
	v_domain VARCHAR(255),
	v_email VARCHAR(255) ,
	v_name VARCHAR(255) ,
	v_note VARCHAR(255) ,
	v_surname VARCHAR(255) ,
	v_user_id UUID,
	v_username VARCHAR(255),
	v_last_admin_check_status BOOLEAN,
        v_external_id TEXT,
	v_namespace VARCHAR(2048))
RETURNS VOID

	--The [users] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      PERFORM UpdateUserImpl(v_department, v_domain, v_email, v_name, v_note, v_surname, v_user_id, v_username, v_external_id, v_namespace);
      UPDATE users SET
      last_admin_check_status = v_last_admin_check_status
      WHERE domain = v_domain AND external_id = v_external_id;
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION InsertOrUpdateUser(
	v_department VARCHAR(255) ,
	v_domain VARCHAR(255),
	v_email VARCHAR(255) ,
	v_name VARCHAR(255) ,
	v_note VARCHAR(255) ,
	v_surname VARCHAR(255) ,
	v_user_id UUID,
	v_username VARCHAR(255),
	v_external_id TEXT,
	v_namespace VARCHAR(2048))
RETURNS VOID
   AS $procedure$
DECLARE
   updated_rows INT;
BEGIN
       SELECT UpdateUserImpl(v_department, v_domain, v_email, v_name, v_note, v_surname, v_user_id, v_username, v_external_id, v_namespace) into updated_rows;
       if (updated_rows = 0) THEN
	    PERFORM InsertUser(v_department, v_domain, v_email, v_name, v_note, v_surname, v_user_id, v_username, v_external_id, v_namespace);
        End If;
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION DeleteUser(v_user_id UUID)
RETURNS VOID
   AS $procedure$
   DECLARE
   v_val  UUID;
BEGIN
			-- Get (and keep) a shared lock with "right to upgrade to exclusive"
			-- in order to force locking parent before children
      select   user_id INTO v_val FROM users  WHERE user_id = v_user_id     FOR UPDATE;
      DELETE FROM tags_user_map
      WHERE user_id = v_user_id;
      DELETE FROM users
      WHERE user_id = v_user_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetAllFromUsers(v_user_id UUID, v_is_filtered BOOLEAN) RETURNS SETOF users STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT users.*
      FROM users
      WHERE (NOT v_is_filtered OR EXISTS (SELECT 1
                                   FROM   users u, user_db_users_permissions_view p
                                   WHERE  u.user_id = v_user_id AND u.user_id = p.ad_element_id));
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetUserByUserId(v_user_id UUID, v_is_filtered BOOLEAN) RETURNS SETOF users STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT users.*
      FROM users
      WHERE user_id = v_user_id AND (NOT v_is_filtered OR
                                     EXISTS (SELECT 1 FROM  users u, user_db_users_permissions_view p
                                             WHERE  u.user_id = v_user_id AND u.user_id = p.ad_element_id));
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetUserByExternalId(v_domain VARCHAR(255), v_external_id TEXT) RETURNS SETOF users STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT users.*
      FROM users
      WHERE domain = v_domain AND external_id = v_external_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetUserByUserNameAndDomain(v_username VARCHAR(255), v_domain VARCHAR(255)) RETURNS SETOF users STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT users.*
      FROM users
      WHERE username = v_username AND domain = v_domain;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION GetUsersByVmGuid(v_vm_guid UUID) RETURNS SETOF users STABLE
   AS $procedure$
BEGIN
      RETURN QUERY SELECT users.*
      FROM users
      inner join permissions
      on users.user_id = permissions.ad_element_id
      WHERE permissions.object_type_id = 2
      and	permissions.object_id = v_vm_guid;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION UpdateLastAdminCheckStatus(v_userIds VARCHAR(4000))
RETURNS VOID
   AS $procedure$
   DECLARE
   v_id  UUID;
   v_tempId  VARCHAR(4000);
   myCursor cursor for select id from fnSplitter(v_userIds);
   v_result  INTEGER;
BEGIN
	-- get users and its groups
	-- get their permission based on ad_element_id.
	-- if one permissions role's type is ADMIN(1) then set the user last_admin_check_status to 1
   OPEN myCursor;
   FETCH myCursor into v_tempId;
   WHILE FOUND LOOP
      v_id := CAST(v_tempId AS UUID);
      select   count(*) INTO v_result from users where user_id in(select ad_element_id as user_id from permissions,roles
         where permissions.role_id = roles.id
         and ad_element_id in((select ad_groups.id from ad_groups,engine_sessions where engine_sessions.user_id = v_id
               and ad_groups.id in(select * from fnsplitteruuid(engine_sessions.group_ids))
               union
               select v_id))
         and (roles.role_type = 1 or permissions.role_id = '00000000-0000-0000-0000-000000000001'));
      update users set last_admin_check_status =
      case
      when v_result = 0 then FALSE
      else TRUE
      end
      where user_id = v_id;
      FETCH myCursor into v_tempId;
   END LOOP;
   CLOSE myCursor;
END; $procedure$
LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION GetSessionUserAndGroupsById(v_id UUID, v_engine_session_seq_id INTEGER)
RETURNS SETOF idUuidType STABLE
   AS $procedure$
BEGIN
   RETURN QUERY
   select ad_groups.ID from ad_groups,engine_sessions where engine_sessions.id = v_engine_session_seq_id
   and ad_groups.id in(select * from fnsplitteruuid(engine_sessions.group_ids))
   UNION
   select v_id
   UNION
   -- user is also member of 'Everyone'
   select 'EEE00000-0000-0000-0000-123456789EEE';
END; $procedure$
LANGUAGE plpgsql;
