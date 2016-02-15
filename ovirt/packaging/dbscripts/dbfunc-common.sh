
DBFUNC_COMMON_DBSCRIPTS_DIR="${DBFUNC_COMMON_DBSCRIPTS_DIR:-$(dirname "$0")}"

. "${DBFUNC_COMMON_DBSCRIPTS_DIR}/dbfunc-base.sh"

#DBFUNC_COMMON_MD5FILE=

dbfunc_common_hook_init_insert_data() {
	return 0
}

dbfunc_common_hook_pre_upgrade() {
	return 0
}

dbfunc_common_hook_views_refresh() {
	return 0
}

dbfunc_common_hook_materialized_views_install() {
	return 0
}

dbfunc_common_hook_materialized_views_drop() {
	return 0
}

dbfunc_common_hook_materialized_viewsrefresh_() {
	return 0
}

dbfunc_common_hook_sequence_numbers_update() {
	return 0
}

#cleans db by dropping all objects
dbfunc_common_schema_drop() {
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/common_sp.sql" > /dev/null
	local statement
	statement="$(
		dbfunc_psql_die --command="select * from generate_drop_all_seq_syntax();"
		dbfunc_psql_die --command="select * from generate_drop_all_tables_syntax();"
		dbfunc_psql_die --command="select * from generate_drop_all_views_syntax();"
		dbfunc_psql_die --command="select * from generate_drop_all_functions_syntax();"
		dbfunc_psql_die --command="select * from generate_drop_all_user_types_syntax();"
	)" || exit 1
	dbfunc_psql_die --command="${statement}" > /dev/null
}

dbfunc_common_restore_permissions() {
	local permissions="$1"
	echo "Applying custom users permissions on database objects..."
	if ! local output=$(dbfunc_psql_allow_errors --command="${permissions}" 2>&1); then
		echo "While running:"
		echo "${permissions}"
		echo "Output was:"
		echo "${output}"
		local fatal=$(echo "${output}" | grep -v 'ERROR: *relation [^ ]* does not exist')
		[ -n "${fatal}" ] && die "Errors while restoring custom permissions: ${fatal}"
	fi
}

dbfunc_common_schema_apply() {
	# check database connection
	dbfunc_psql_die --command="select 1;" > /dev/null

	echo "Creating schema ${DBFUNC_DB_USER}@${DBFUNC_DB_HOST}:${DBFUNC_DB_PORT}/${DBFUNC_DB_DATABASE}"
	if [ "$(dbfunc_psql_statement_parsable "
		select count(*) as count
		from pg_catalog.pg_tables
		where
			tablename = 'schema_version' and
			schemaname = 'public'
	")" -eq 0 ]; then
		echo "Creating fresh schema"
		_dbfunc_common_schema_create
	fi

	local permissions
	echo "Saving custom users permissions on database objects..."
	permissions="$(_dbfunc_common_get_custom_user_permissions)" || exit $?

	_dbfunc_common_schema_upgrade

	dbfunc_common_restore_permissions "${permissions}"
}

dbfunc_common_schema_refresh() {
	local permissions

	echo "Saving custom users permissions on database objects..."
	permissions="$(_dbfunc_common_get_custom_user_permissions)" || exit $?

	_dbfunc_common_schema_refresh_drop
	_dbfunc_common_schema_refresh_create

	dbfunc_common_restore_permissions "${permissions}"
}

# gets the configuration value of the given option name and version.
# usage: <some variable>=get_config_value <name> <version>
dbfunc_common_config_get_value() {
	local option_name="$1"
	local version="$2"

	dbfunc_psql_statement_parse_line "$(
		dbfunc_psql_statement_parsable "
			select option_value
			from vdc_options
			where
				option_name='${option_name}' and
				version='${version}'
		"
	)"
}

_dbfunc_common_language_create() {
	local lang="$1"

	if [ "$(
		dbfunc_psql_statement_parsable "
			select count(*)
			from pg_language
			where lanname='${lang}'
		"
	)" -eq 0 ]; then
		dbfunc_psql_die --command="create language '${lang}';" > /dev/null
	fi
}

_dbfunc_common_schema_refresh_drop() {
	_dbfunc_common_views_drop
	_dbfunc_common_sps_drop
}

_dbfunc_common_schema_refresh_create() {
	dbfunc_common_hook_views_refresh
	_dbfunc_common_sps_refresh
}

_dbfunc_common_schema_create() {

	_dbfunc_common_language_create "plpgsql"

	#set database min error level
	dbfunc_psql_die --command="ALTER DATABASE \"${DBFUNC_DB_DATABASE}\" SET client_min_messages=ERROR;" > /dev/null

	echo "Creating tables..."
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/create_tables.sql" > /dev/null

	echo "Creating functions..."
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/create_functions.sql" > /dev/null

	echo "Creating common functions..."
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/common_sp.sql" > /dev/null

	#inserting initial data
	dbfunc_common_hook_init_insert_data

	#remove checksum file in clean install in order to run views/sp creation
	[ -n "${DBFUNC_COMMON_MD5FILE}" ] && rm -f "${DBFUNC_COMMON_MD5FILE}" > /dev/null 2>&1
}

_dbfunc_common_schema_upgrade() {

	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/upgrade/03_03_0000_set_version.sql" > /dev/null

	local files="$(_dbfunc_common_get_files "upgrade" 1)"
	if [ -n "${files}" ]; then
		local state="FAILED"
		local comment=""
		local updated=0
		_dbfunc_common_validate_version_uniqueness
		if [ -z "${DBFUNC_COMMON_MD5FILE}" ] || ! _dbfunc_common_is_view_or_sp_changed; then
			echo "upgrade script detected a change in Config, View or Stored Procedure..."
			_dbfunc_common_run_pre_upgrade
			updated=1
		fi

		# get current version
		local current="$(_dbfunc_common_get_current_version)"
		# we should remove leading blank (from select result) and zero in order not to treat number as octal
		local last="$(expr substr "${current}" 3 7)"
		local file
		echo "${files}" | while read file; do
			before="$(_dbfunc_common_get_db_time)"
			checksum="$(md5sum "${file}" | cut -d " " -f1)"
			ver="$(_dbfunc_common_get_file_version "${file}")"
			if [ "${ver}" -gt "${current}" ] ; then
				# we should remove leading zero in order not to treat number as octal
				local xver="$(expr substr "${ver}" 2 7)"
				# taking major revision , i.e 03010000=>301
				local xverMajor="$(expr substr "${xver}" 1 3)"
				local lastMajor="$(expr substr "${last}" 1 3)"

				# check for gaps in upgrade
				# check gaps only for identical major revisions
				if [ "${xverMajor}" -eq "${lastMajor}" ]; then
					if [ $((${xver} - ${last})) -gt 10 ]; then
						_dbfunc_common_set_last_version
						die "Illegal script version number ${ver},version should be in max 10 gap from last installed version: 0${last}
Please fix numbering to interval 0$(( ${last} + 1)) to 0$(( ${last} + 10)) and run the upgrade script."
					fi
				fi
				# check if script was already installed with other version name.
				local installed_version="$(_dbfunc_common_get_installed_version "${checksum}")"
				if [ -n "${installed_version}" ]; then
					echo "Skipping upgrade script ${file}, already installed by ${installed_version}"
					state="SKIPPED"
					after="$(_dbfunc_common_get_db_time)"
					last="${xver}"
					comment="Installed already by ${installed_version}"
				else
					# force pre upgrade to run in case no md5 change was
					# found but we still upgrade, like in db restore.
					if [ "${updated}" = 0 ]; then
						_dbfunc_common_run_pre_upgrade
						updated=1
					fi
					_dbfunc_common_run_required_scripts "${file}"
					_dbfunc_common_run_file "${file}"
					code=$?
					if [ "${code}" -eq 0 ]; then
						state="INSTALLED"
						after=$(_dbfunc_common_get_db_time)
						last=$xver
						comment=""
					else
						_dbfunc_common_set_last_version
						exit "${code}"
					fi
				fi
				dbfunc_psql_die --command="
					insert into schema_version(
						version,
						script,
						checksum,
						installed_by,
						started_at,
						ended_at,
						state,
						current,
						comment
					)
					values (
						trim('${ver}'),
						'$(echo "${file}" | sed "s#^${DBFUNC_COMMON_DBSCRIPTS_DIR}/##")',
						'${checksum}',
						'${DBFUNC_DB_USER}',
						cast(trim('${before}') as timestamp),
						cast(trim('${after}') as timestamp),
						'${state}',
						false,
						'${comment}'
					);
				" > /dev/null
			fi
		done || exit $?
		_dbfunc_common_set_last_version

		# restore views & SPs if dropped
		if [ "${updated}" -eq 1 ]; then
			_dbfunc_common_run_post_upgrade
		else
			echo "database is up to date."
		fi
	fi
}

#drops views before upgrade or refresh operations
_dbfunc_common_views_drop() {
	# common stored procedures are executed first (for new added functions to be valid)
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/common_sp.sql" > /dev/null
	dbfunc_psql_die --command="select * from generate_drop_all_views_syntax();" | \
		dbfunc_psql_die > /dev/null
}

#drops sps before upgrade or refresh operations
_dbfunc_common_sps_drop() {
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/common_sp.sql" > /dev/null
	local statement
	statement="$(
		dbfunc_psql_die --command="select * from generate_drop_all_functions_syntax();"
	)" || exit 1
	dbfunc_psql_die --command="${statement}" > /dev/null

	# recreate generic functions
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/create_functions.sql" > /dev/null
}

#refreshes sps
_dbfunc_common_sps_refresh() {
	echo "Creating stored procedures..."
	local file
	find "${DBFUNC_COMMON_DBSCRIPTS_DIR}" -name '*sp.sql' | sort | while read file; do
		echo "Creating stored procedures from ${file}..."
		dbfunc_psql_die --file="${file}" > /dev/null
	done || exit $?
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/common_sp.sql" > /dev/null
}

_dbfunc_common_get_custom_user_permissions() {
	# Looking for permissions not related to postgres, public our ours (custom user permissions)
	dbfunc_pg_dump_die --schema-only |
		sed -n -e '/^grant/Ip' |
		sed -e "/to \(public\|postgres\)\|${DBFUNC_DB_USER};/Id"
}

_dbfunc_common_run_pre_upgrade() {
	#Dropping all views & sps
	_dbfunc_common_schema_refresh_drop
	# common stored procedures are executed first (for new added functions to be valid)
	dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/common_sp.sql" > /dev/null
	#update sequence numers
	dbfunc_common_hook_sequence_numbers_update
	#run pre upgrade scripts
	_dbfunc_common_psql_statements_in_dir 'pre_upgrade'
	dbfunc_common_hook_materialized_views_install
	#drop materialized views to support views changesin upgrade
	#Materialized views are restored in the post_upgrade step
	dbfunc_common_hook_materialized_views_drop
	dbfunc_common_hook_pre_upgrade
}

_dbfunc_common_run_post_upgrade() {
	#Refreshing  all views & sps & run post-upgrade scripts
	_dbfunc_common_schema_refresh_create
	#Running post-upgrade scripts
	_dbfunc_common_psql_statements_in_dir 'post_upgrade'
	#run custom materialized views if exists
	custom_materialized_views_file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/upgrade/post_upgrade/custom/create_materialized_views.sql"
	if [ -f "${custom_materialized_views_file}" ]; then
		echo "running custom materialized views from '${custom_materialized_views_file}'..."
		if ! dbfunc_psql --file="${custom_materialized_views_file}"; then
			#drop all custom views
			dbfunc_psql --command="select DropAllCustomMaterializedViews();" > /dev/null
			echo "Illegal syntax in custom Materialized Views, Custom Materialized Views were dropped."
		fi
	fi
	dbfunc_common_hook_materialized_viewsrefresh_
}

# Runs all the SQL scripts in directory upgrade/$1/
_dbfunc_common_psql_statements_in_dir() {
	local dir="$1"
	_dbfunc_common_get_files "upgrade/${dir}" 1 | while read file; do
		_dbfunc_common_run_file "${file}"
	done || exit $?
}

_dbfunc_common_run_required_scripts() {
	local script="$1"
	# check for helper functions that the script needs
	# source scripts must be defined in the first lines of the script
	local line
	while read line; do
		expr="$(echo "${line}" | cut -d " " -f1 | grep "\-\-#source")"
		[ -z "${expr}" ] && break
		local sql="$(echo "${line}" | cut -d " " -f2)"
		echo "${sql}" | grep -q "_sp.sql" || \
			die "invalid source file ${sql} in ${file}, source files must end with '_sp.sql'"
		echo "Running helper functions from '${sql}' for '${file}'"
		dbfunc_psql_die --file="${DBFUNC_COMMON_DBSCRIPTS_DIR}/${sql}" > /dev/null
	done < "${script}"
}

_dbfunc_common_run_file() {
	local file="$1"
	if [ -x "${file}" ]; then
		# delegate all DBFUNC_ vars in subshell
		echo "Running upgrade shell script '${file}'..."
		(
			eval "$(set | grep '^DBFUNC_' | sed 's/^\([^=]*\)=.*/export \1/')"
			"${file}"
		)
	else
		echo "Running upgrade sql script '${file}'..."
		dbfunc_psql_die --file="${file}" > /dev/null
	fi
}

_dbfunc_common_get_current_version() {
	dbfunc_psql_statement_parsable "
		select version
		from schema_version
		where current = true
		order by id
		LIMIT 1
	"
}

_dbfunc_common_get_installed_version() {
	local cheksum="$1"
	dbfunc_psql_statement_parsable "
		select version
		from schema_version
		where
			checksum = '${cheksum}' and
			state = 'INSTALLED'
	"
}

_dbfunc_common_set_last_version() {
	local id="$(
		dbfunc_psql_statement_parsable "
			select max(id)
			from schema_version
			where state in ('INSTALLED','SKIPPED')
		"
	)"
	dbfunc_psql_die --command="
		update schema_version
		set current=(id=${id});
	" > /dev/null
}

_dbfunc_common_get_db_time(){
	dbfunc_psql_statement_parsable "select now()"
}

# gets a directory and required depth and return all sql & sh files
_dbfunc_common_get_files() {
	local dir="$1"
	local maxdepth="$2"
	find "${DBFUNC_COMMON_DBSCRIPTS_DIR}/${dir}" \
		-maxdepth "${maxdepth}" \
		-name '*.sql' -or -name '*.sh' | \
		sort
}

_dbfunc_common_is_view_or_sp_changed() {
	local files="$(_dbfunc_common_get_files "upgrade" 3)"
	local md5sum_tmp_file="${DBFUNC_COMMON_MD5FILE}.tmp"

	{
		_dbfunc_common_get_files "upgrade" 3
		find "${DBFUNC_COMMON_DBSCRIPTS_DIR}" -name 'create_*views.sql' -or -name '*_sp.sql'
	} | sort | uniq | xargs -d '\n' md5sum > "${DBFUNC_COMMON_MD5FILE}.tmp"

	diff -s -q "${DBFUNC_COMMON_MD5FILE}" "${DBFUNC_COMMON_MD5FILE}.tmp" > /dev/null 2>&1
	result=$?

	#  0 - identical , 1 - differ , 2 - error
	if [ $result -eq 0 ] ; then
		rm -f "${DBFUNC_COMMON_MD5FILE}.tmp"
	else
		# there is a diff or md5 file does not exist
		mv -f "${DBFUNC_COMMON_MD5FILE}.tmp" "${DBFUNC_COMMON_MD5FILE}"
	fi
	return $result
}

_dbfunc_common_get_file_version() {
	local file="$1"
	basename "${file}" | sed -e 's#\(..........\).*#\1#' -e 's/_//g'
}

_dbfunc_common_validate_version_uniqueness() {
	local file
	_dbfunc_common_get_files "upgrade" 1 | while read file; do
		ver="$(_dbfunc_common_get_file_version "${file}")"
		[ "${ver}" != "${prev}" ] || die "Operation aborted, found duplicate version: ${ver}"
		prev="${ver}"
	done || exit $?
}
