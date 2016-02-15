#!/bin/sh
#
# ovirt-engine-backup - oVirt engine backup and restore utility
# Copyright (C) 2013-2015 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#	 http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Clean the environment, see bz 1172191
[ -z "${BACKUP_ENV_CLEAN}" ] && exec -c env -i PATH="${PATH}" BACKUP_ENV_CLEAN=1 "$0" "$@"

# Load the prolog:
. "$(dirname "$(readlink -f "$0")")"/engine-prolog.sh

my_load_config() {
	load_config

	DWH_CONFIG=/etc/ovirt-engine-dwh/ovirt-engine-dwhd.conf
	for f in "${DWH_CONFIG}" "${DWH_CONFIG}".d/*.conf; do
		[ -e "${f}" ] && . "${f}"
	done

	JASPER_PROPERTIES=/var/lib/ovirt-engine-reports/build-conf/master.properties
	if [ -e "${JASPER_PROPERTIES}" ]; then
		load_jasper_reports_db_creds
	else
		JASPER_PROPERTIES=
	fi
}

get_jasper_db_cred() {
	python -c "

# Copied from otopi:src/otopi/__init__.py
import sys

def _pythonModulesCompat():
    \"\"\"Rename modules to match python3 names.\"\"\"
    if sys.version_info[0] < 3:
        import ConfigParser
        sys.modules['configparser'] = ConfigParser

_pythonModulesCompat()

import configparser
import io
import os

params = sys.argv

config = configparser.ConfigParser()

def escape(s, chars):
    ret = ''
    for c in s:
        if c in chars:
            ret += '\\\\'
        ret += c
    return ret

config.optionxform = str

with open('${JASPER_PROPERTIES}') as f:
    config.readfp(
        io.StringIO(
          '[default]' + f.read().decode('utf-8')
        )
    )

for i in range(1, len(params)-1, 2):
    s = params[i]
    t = params[i+1]
    v = config.get('default', t)
    print ('%s=\"%s\"' % (s, escape(v, '\"\\\\\$')))
" "$@"
}

load_jasper_reports_db_creds() {
	eval $(get_jasper_db_cred \
		REPORTS_DB_HOST dbHost \
		REPORTS_DB_PORT dbPort \
		REPORTS_DB_USER dbUsername \
		REPORTS_DB_PASSWORD dbPassword \
		REPORTS_DB_DATABASE js.dbName
	)
	# TODO Drop this when reports support secured connection/secured host validation.
	# Currently set only for provisionDB - the rest of the code effectively ignores this
	REPORTS_DB_SECURED=False
	REPORTS_DB_SECURED_VALIDATION=False
}

# Globals
BACKUP_PATHS="/etc/ovirt-engine
/etc/ovirt-engine-dwh
/etc/ovirt-engine-reports
/etc/pki/ovirt-engine
/etc/ovirt-engine-setup.conf.d
/var/lib/ovirt-engine-reports/build-conf
/var/lib/ovirt-engine-reports/ovirt-engine-reports.war/WEB-INF/js.quartz.properties
#/var/lib/ovirt-engine-reports
/etc/httpd/conf.d/ovirt-engine-root-redirect.conf
/etc/httpd/conf.d/ssl.conf
/etc/httpd/conf.d/z-ovirt-engine-proxy.conf
/etc/httpd/conf.d/z-ovirt-engine-reports-proxy.conf
/etc/yum/pluginconf.d/versionlock.list
/etc/firewalld/services/ovirt-https.xml
/etc/firewalld/services/ovirt-http.xml
/etc/firewalld/services/ovirt-postgres.xml"
# Add /var/lib/ovirt-engine except a few
VAR_LIB_EXCLUSIONS="/var/lib/ovirt-engine/backups
/var/lib/ovirt-engine/jboss_runtime"
for p in /var/lib/ovirt-engine/*; do
	echo "${VAR_LIB_EXCLUSIONS}" | grep -q "^${p}\$" || BACKUP_PATHS="${BACKUP_PATHS}
${p}"
done

ENGINE_TABLES_TO_CLEAN_ON_RESTORE="async_tasks
async_tasks_entities
business_entity_snapshot
command_entities
job
step"

MYPGPASS=""
TEMP_FOLDER=""
FILE=""
DB_BACKUP_FILE_NAME="engine_backup.db"
DWHDB_BACKUP_FILE_NAME="dwh_backup.db"
REPORTSDB_BACKUP_FILE_NAME="reports_backup.db"

FAILURE_NOTIFIED=

cleanup() {
	ec="$?"
	if [ -n "${ENGINE_DB_USER}" -a "${ec}" = '1' -a "${MODE}" = "backup" -a -z "${FAILURE_NOTIFIED}" ]; then
		notify_engine "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" -1 "Failed"
		FAILURE_NOTIFIED=1
	fi

	[ -n "${TEMP_FOLDER}" ] && rm -rf "${TEMP_FOLDER}"
}

trap cleanup 0

usage() {
	cat << __EOF__
engine-backup: back up and restore ovirt-engine environment
USAGE:
    $0 [--mode=MODE] [--scope=SCOPE] [--file=FILE] [--log=FILE]
 MODE is one of the following:
    backup                          back up system into FILE
    restore                         restore system from FILE
 SCOPE is one of the following:
    all                             If MODE=backup: backup everything below.
                                    If MODE=restore: restore everything found in FILE.
    files                           product files only
    db                              Engine database only
    dwhdb                           Data Warehouse database only
    reportsdb                       Reports database only
    The option --scope can be passed more than once, with different scopes.
 --file=FILE                        file to use during backup or restore
 --log=FILE                         log file to use during backup or restore
 --archive-compressor=COMPRESSOR
    Use COMPRESSOR to compress the backup file, can be one of:
    gzip
    bzip2
    xz
    None
 --files-compressor=COMPRESSOR      compress the product files, same options as --archive-compressor
 --keep-temporary-data              do not clean up temporary data on restore
 --db-compressor=COMPRESSOR         compress the Engine database, same options as --archive-compressor
 --db-dump-format=FORMAT
    Engine database dump format; see pg_dump(1) for details. Can be one of:
    plain
    custom
 --db-restore-jobs=JOBS             number of restore jobs for the Engine database,
                                    when using custom dump format and compressor None.
                                    Passed to pg_restore -j. Defaults to 2.
 --provision-db                     create a PostgreSQL database for the Engine on restore
 --change-db-credentials            activate the following options, to restore
                                    the Engine database using credentials other
                                    than those stored in the backup itself.
                                    If used, existing credentials are ignored.
 --db-host=host                     set Engine database host
 --db-port=port                     set Engine database port
 --db-user=user                     set Engine database user
 --db-passfile=file                 set Engine database password - read from file
 --db-password=pass                 set Engine database password
 --db-password                      set Engine database password - interactively
 --db-name=name                     set Engine database name
 --db-secured                       set a secured connection for the Engine database
 --db-secured-validation            validate host for Engine database
 --dwh-db-compressor=COMPRESSOR     compress the Data Warehouse database, same options as --archive-compressor
 --dwh-db-dump-format=FORMAT        Data Warehouse database dump format, same options as --db-dump-format
 --dwh-db-restore-jobs=JOBS         for Data Warehouse database, same as --db-restore-jobs
 --provision-dwh-db                 create a PostgreSQL database for Data Warehouse on restore
 --change-dwh-db-credentials        activate the following options, to restore
                                    the Data Warehouse database using credentials other
                                    than those stored in the backup itself.
                                    If used, existing credentials are ignored.
 --dwh-db-host=host                 set Data Warehouse database host
 --dwh-db-port=port                 set Data Warehouse database port
 --dwh-db-user=user                 set Data Warehouse database user
 --dwh-db-passfile=file             set Data Warehouse database password - read from file
 --dwh-db-password=pass             set Data Warehouse database password
 --dwh-db-password                  set Data Warehouse database password - interactively
 --dwh-db-name=name                 set Data Warehouse database name
 --dwh-db-secured                   set a secured connection for the Data Warehouse database
 --dwh-db-secured-validation        validate host for Data Warehouse database
 --reports-db-compressor=COMPRESSOR compress the Reports database, same options as --archive-compressor
 --reports-db-dump-format=FORMAT    Reports database dump format, same options as --db-dump-format
 --reports-db-restore-jobs=JOBS     for Reports database, same as --db-restore-jobs
 --provision-reports-db             create a PostgreSQL database for Reports on restore
 --change-reports-db-credentials    activate the following options, to restore
                                    the Reports database using credentials other
                                    than those stored in the backup itself.
                                    If used, existing credentials are ignored.
 --reports-db-host=host             set Reports database host
 --reports-db-port=port             set Reports database port
 --reports-db-user=user             set Reports database user
 --reports-db-passfile=file         set Reports database password - read from file
 --reports-db-password=pass         set Reports database password
 --reports-db-password              set Reports database password - interactively
 --reports-db-name=name             set Reports database name
 --reports-db-secured               set a secured connection for the Reports database
 --reports-db-secured-validation    validate host for Reports database

 --fast-restore                     the default for backup, equivalent to:
         --archive-compressor=gzip \\
         --files-compressor=xz \\
         --db-dump-format=custom \\
         --db-compressor=None \\
         --dwh-db-dump-format=custom \\
         --dwh-db-compressor=None \\
         --reports-db-dump-format=custom \\
         --reports-db-compressor=None

    In addition, you should pass, when restoring:
        --db-restore-jobs=N \\
        --dwh-db-restore-jobs=N \\
        --reports-db-restore-jobs=N
        where 'N' is around 150% of available cpu cores.

 --small-size                       for a small backup file, equivalent to:
         --archive-compressor=xz \\
         --files-compressor=xz \\
         --db-dump-format=plain \\
         --db-compressor=xz \\
         --dwh-db-dump-format=plain \\
         --dwh-db-compressor=xz \\
         --reports-db-dump-format=plain \\
         --reports-db-compressor=xz

 --fast-backup                      for a fast backup, equivalent to:
         --archive-compressor=gzip \\
         --files-compressor=xz \\
         --db-dump-format=custom \\
         --db-compressor=None \\
         --dwh-db-dump-format=custom \\
         --dwh-db-compressor=None \\
         --reports-db-dump-format=custom \\
         --reports-db-compressor=None

 You can use one of --fast-restore, --small-size, --fast-backup, and after that
 one of the other compressor/format options for further fine-tuning.


 ENVIRONMENT VARIABLES

 OVIRT_ENGINE_DATABASE_PASSWORD
     Database password as if provided by --db-password=pass option.
 OVIRT_DWH_DATABASE_PASSWORD
     Database password as if provided by --dwh-db-password=pass option.
 OVIRT_REPORTS_DATABASE_PASSWORD
     Database password as if provided by --reports-db-password=pass option.

 Wiki

 See http://www.ovirt.org/Ovirt-engine-backup for more info.

 To create a new user/database:

 create role <user> with login encrypted password '<password>';
 create database <database> owner <user> template template0
 encoding 'UTF8' lc_collate 'en_US.UTF-8' lc_ctype 'en_US.UTF-8';

 Open access in the firewall/iptables/etc. to the PostgreSQL port,
 5432/tcp by default.

 Locate pg_hba.conf within your distribution,
 common locations are:
  - /var/lib/pgsql/data/pg_hba.conf
  - /etc/postgresql-*/pg_hba.conf
  - /etc/postgresql/*/main/pg_hba.conf

 and open access there by adding the following lines:

 host    <database>      <user>          0.0.0.0/0               md5
 host    <database>      <user>          ::0/0                   md5

 Replace <user>, <password>, <database> with appropriate values.
 Repeat for Engine, Data Warehouse, Reports as required.

__EOF__
	return 0
}

MODE=
DEFAULT_SCOPE=all
SCOPE=
SCOPE_FILES=
SCOPE_ENGINE_DB=
SCOPE_DWH_DB=
SCOPE_REPORTS_DB=
KEEP_TEMPORARY_DATA=
ARCHIVE_COMPRESS_OPTION=z
FILES_COMPRESS_OPTION=J
DB_DUMP_COMPRESSOR=
DB_DUMP_FORMAT=custom
DB_RESTORE_JOBS=2
PROVISIONING=
PROVISION_DB=
POSTGRESQL_DEFAULT_PORT=5432
CHANGE_DB_CREDENTIALS=
MY_DB_HOST=
MY_DB_PORT="${POSTGRESQL_DEFAULT_PORT}"
MY_DB_USER=
ORIG_DB_USER=
MY_DB_PASSWORD="${OVIRT_ENGINE_DATABASE_PASSWORD}"
MY_DB_DATABASE=
MY_DB_SECURED=False
MY_DB_SECURED_VALIDATION=False
MY_DB_CREDS=
DWH_DB_DUMP_COMPRESSOR=
DWH_DB_DUMP_FORMAT=custom
DWH_DB_RESTORE_JOBS=2
PROVISION_DWH_DB=
CHANGE_DWH_DB_CREDENTIALS=
MY_DWH_DB_HOST=
MY_DWH_DB_PORT="${POSTGRESQL_DEFAULT_PORT}"
MY_DWH_DB_USER=
ORIG_DWH_DB_USER=
MY_DWH_DB_PASSWORD="${OVIRT_DWH_DATABASE_PASSWORD}"
MY_DWH_DB_DATABASE=
MY_DWH_DB_SECURED=False
MY_DWH_DB_SECURED_VALIDATION=False
MY_DWH_DB_CREDS=
REPORTS_DB_DUMP_COMPRESSOR=
REPORTS_DB_DUMP_FORMAT=custom
REPORTS_DB_RESTORE_JOBS=2
PROVISION_REPORTS_DB=
CHANGE_REPORTS_DB_CREDENTIALS=
MY_REPORTS_DB_HOST=
MY_REPORTS_DB_PORT="${POSTGRESQL_DEFAULT_PORT}"
MY_REPORTS_DB_USER=
ORIG_REPORTS_DB_USER=
MY_REPORTS_DB_PASSWORD="${OVIRT_REPORTS_DATABASE_PASSWORD}"
MY_REPORTS_DB_DATABASE=
MY_REPORTS_DB_SECURED=False
MY_REPORTS_DB_SECURED_VALIDATION=False
MY_REPORTS_DB_CREDS=

compressor_to_tar_option() {
	local res
	case "$1" in
		gzip) res=z ;;
		bzip2) res=j ;;
		xz) res=J ;;
		None) res= ;;
		*) die "invalid compressor '${v}'"
	esac
	echo "${res}"
}

compressor_to_command() {
	local res
	case "$1" in
		gzip|bzip2|xz) res="$1" ;;
		None) res= ;;
		*) die "invalid compressor '${v}'"
	esac
	echo "${res}"
}

parse_dump_format() {
	local res
	case "$1" in
		plain|custom) res="$1" ;;
		*) die "invalid dump format '${v}'"
	esac
	echo "${res}"
}

parse_jobs() {
	local res
	case "$1" in
		''|*[!0-9]*) die "invalid number of jobs" ;;
		*) res="$1"
	esac
	echo "${res}"
}

set_scope() {
	local s="$1"
	case "${s}" in
		all)
			SCOPE_FILES=1
			SCOPE_ENGINE_DB=1
			SCOPE_DWH_DB=1
			SCOPE_REPORTS_DB=1
			;;
		files)
			SCOPE_FILES=1
			;;
		db)
			SCOPE_ENGINE_DB=1
			;;
		dwhdb)
			SCOPE_DWH_DB=1
			;;
		reportsdb)
			SCOPE_REPORTS_DB=1
			;;
		*) die "invalid scope '${s}'"
	esac

	if [ -z "${SCOPE}" ]; then
		SCOPE="${s}"
	else
		SCOPE="${SCOPE},${s}"
	fi
}

parseArgs() {
	local DB_PASSFILE

	while [ -n "$1" ]; do
		local x="$1"
		local v="${x#*=}"
		shift
		case "${x}" in
			--mode=*)
				MODE="${v}"
				case "${MODE}" in
					backup|restore);;
					*) die "invalid mode" ;;
				esac
			;;
			--scope=*)
				set_scope "${v}"
			;;
			--file=*)
				FILE="${v}"
			;;
			--log=*)
				LOG="${v}"
			;;
			--archive-compressor=*)
				ARCHIVE_COMPRESS_OPTION=$(compressor_to_tar_option "${v}")
				[ $? != 0 ] && logdie "failed parsing compressor"
			;;
			--files-compressor=*)
				FILES_COMPRESS_OPTION=$(compressor_to_tar_option "${v}")
				[ $? != 0 ] && logdie "failed parsing compressor"
			;;
			--keep-temporary-data)
				KEEP_TEMPORARY_DATA=1
			;;
			--db-compressor=*)
				DB_DUMP_COMPRESSOR=$(compressor_to_command "${v}")
				[ $? != 0 ] && logdie "failed parsing compressor"
			;;
			--db-dump-format=*)
				DB_DUMP_FORMAT=$(parse_dump_format "${v}")
				[ $? != 0 ] && logdie "failed parsing dump format"
			;;
			--db-restore-jobs=*)
				DB_RESTORE_JOBS=$(parse_jobs "${v}")
				[ $? != 0 ] && logdie "failed parsing jobs"
			;;
			--provision-db)
				PROVISION_DB=1
				PROVISIONING=1
			;;
			--change-db-credentials)
				CHANGE_DB_CREDENTIALS=1
			;;
			--db-host=*)
				MY_DB_HOST="${v}"
			;;
			--db-port=*)
				MY_DB_PORT="${v}"
			;;
			--db-user=*)
				MY_DB_USER="${v}"
				ORIG_DB_USER="${ENGINE_DB_USER}"
			;;
			--db-passfile=*)
				DB_PASSFILE="${v}"
				[ -r "${DB_PASSFILE}" ] || \
					die "Can not read password file ${DB_PASSFILE}"
				read -r MY_DB_PASSWORD < "${DB_PASSFILE}"
			;;
			--db-password=*)
				MY_DB_PASSWORD="${v}"
			;;
			--db-password)
				MY_DB_PASSWORD="$(readdbpassword Engine)" || exit 1
			;;
			--db-name=*)
				MY_DB_DATABASE="${v}"
			;;
			--db-secured)
				MY_DB_SECURED="True"
			;;
			--db-sec-validation)
				MY_DB_SECURED_VALIDATION="True"
			;;
			--dwh-db-compressor=*)
				DWH_DB_DUMP_COMPRESSOR=$(compressor_to_command "${v}")
				[ $? != 0 ] && logdie "failed parsing compressor"
			;;
			--dwh-db-dump-format=*)
				DWH_DB_DUMP_FORMAT=$(parse_dump_format "${v}")
				[ $? != 0 ] && logdie "failed parsing dump format"
			;;
			--dwh-db-restore-jobs=*)
				DWH_DB_RESTORE_JOBS=$(parse_jobs "${v}")
				[ $? != 0 ] && logdie "failed parsing jobs"
			;;
			--provision-dwh-db)
				PROVISION_DWH_DB=1
				PROVISIONING=1
			;;
			--change-dwh-db-credentials)
				CHANGE_DWH_DB_CREDENTIALS=1
			;;
			--dwh-db-host=*)
				MY_DWH_DB_HOST="${v}"
			;;
			--dwh-db-port=*)
				MY_DWH_DB_PORT="${v}"
			;;
			--dwh-db-user=*)
				MY_DWH_DB_USER="${v}"
				ORIG_DWH_DB_USER="${DWH_DB_USER}"
			;;
			--dwh-db-passfile=*)
				DB_PASSFILE="${v}"
				[ -r "${DB_PASSFILE}" ] || \
					die "Can not read password file ${DB_PASSFILE}"
				read -r MY_DWH_DB_PASSWORD < "${DB_PASSFILE}"
			;;
			--dwh-db-password=*)
				MY_DWH_DB_PASSWORD="${v}"
			;;
			--dwh-db-password)
				MY_DWH_DB_PASSWORD="$(readdbpassword DWH)" || exit 1
			;;
			--dwh-db-name=*)
				MY_DWH_DB_DATABASE="${v}"
			;;
			--dwh-db-secured)
				MY_DWH_DB_SECURED="True"
			;;
			--dwh-db-sec-validation)
				MY_DWH_DB_SECURED_VALIDATION="True"
			;;
			--reports-db-compressor=*)
				REPORTS_DB_DUMP_COMPRESSOR=$(compressor_to_command "${v}")
				[ $? != 0 ] && logdie "failed parsing compressor"
			;;
			--reports-db-dump-format=*)
				REPORTS_DB_DUMP_FORMAT=$(parse_dump_format "${v}")
				[ $? != 0 ] && logdie "failed parsing dump format"
			;;
			--reports-db-restore-jobs=*)
				REPORTS_DB_RESTORE_JOBS=$(parse_jobs "${v}")
				[ $? != 0 ] && logdie "failed parsing jobs"
			;;
			--provision-reports-db)
				PROVISION_REPORTS_DB=1
				PROVISIONING=1
			;;
			--change-reports-db-credentials)
				CHANGE_REPORTS_DB_CREDENTIALS=1
			;;
			--reports-db-host=*)
				MY_REPORTS_DB_HOST="${v}"
			;;
			--reports-db-port=*)
				MY_REPORTS_DB_PORT="${v}"
			;;
			--reports-db-user=*)
				MY_REPORTS_DB_USER="${v}"
				ORIG_REPORTS_DB_USER="${REPORTS_DB_USER}"
			;;
			--reports-db-passfile=*)
				DB_PASSFILE="${v}"
				[ -r "${DB_PASSFILE}" ] || \
					die "Can not read password file ${DB_PASSFILE}"
				read -r MY_REPORTS_DB_PASSWORD < "${DB_PASSFILE}"
			;;
			--reports-db-password=*)
				MY_REPORTS_DB_PASSWORD="${v}"
			;;
			--reports-db-password)
				MY_REPORTS_DB_PASSWORD="$(readdbpassword Reports)" || exit 1
			;;
			--reports-db-name=*)
				MY_REPORTS_DB_DATABASE="${v}"
			;;
			--reports-db-secured)
				MY_REPORTS_DB_SECURED="True"
			;;
			--reports-db-sec-validation)
				MY_REPORTS_DB_SECURED_VALIDATION="True"
			;;
			--fast-restore)
				ARCHIVE_COMPRESS_OPTION=z
				FILES_COMPRESS_OPTION=J
				DB_DUMP_FORMAT=custom
				DB_DUMP_COMPRESSOR=
				DWH_DB_DUMP_FORMAT=custom
				DWH_DB_DUMP_COMPRESSOR=
				REPORTS_DB_DUMP_FORMAT=custom
				REPORTS_DB_DUMP_COMPRESSOR=
			;;
			--small-size)
				ARCHIVE_COMPRESS_OPTION=J
				FILES_COMPRESS_OPTION=J
				DB_DUMP_FORMAT=plain
				DB_DUMP_COMPRESSOR=xz
				DWH_DB_DUMP_FORMAT=plain
				DWH_DB_DUMP_COMPRESSOR=xz
				REPORTS_DB_DUMP_FORMAT=plain
				REPORTS_DB_DUMP_COMPRESSOR=xz
			;;
			--fast-backup)
				ARCHIVE_COMPRESS_OPTION=z
				FILES_COMPRESS_OPTION=J
				DB_DUMP_FORMAT=custom
				DB_DUMP_COMPRESSOR=
				DWH_DB_DUMP_FORMAT=custom
				DWH_DB_DUMP_COMPRESSOR=
				REPORTS_DB_DUMP_FORMAT=custom
				REPORTS_DB_DUMP_COMPRESSOR=
			;;
			--help)
				usage
				exit 0
			;;
			*)
				usage
				exit 1
			;;
		esac
	done

	if [ -z "${SCOPE}" ]; then
		set_scope "${DEFAULT_SCOPE}"
	fi
}

verifyArgs() {
	[ -n "${MODE}" ] || die "--mode=<backup|restore> is missing"
	[ -n "${FILE}" ] || die "--file is missing"
	[ -n "${LOG}" ] || die "--log is missing"
	if [ "${MODE}" == "restore" ] ; then
		[ -e "${FILE}" ] || die "${FILE} does not exist"
	fi
	if [ -n "${CHANGE_DB_CREDENTIALS}" ]; then
		[ -n "${PROVISION_DB}" ] && die "Cannot change credentials if provisioning a database"
		[ -n "${MY_DB_HOST}" ] || die "--db-host is missing"
		[ -n "${MY_DB_USER}" ] || die "--db-user is missing"
		[ -n "${MY_DB_PASSWORD}" ] || \
			die "--db-passfile or --db-password is missing"
		[ -n "${MY_DB_DATABASE}" ] || die "--db-name is missing"
	fi
	[ -z "${CHANGE_DB_CREDENTIALS}" -a \
		\( \
			-n "${MY_DB_HOST}" \
			-o -n "${MY_DB_USER}" \
			-o -n "${MY_DB_PASSWORD}" \
			-o -n "${MY_DB_DATABASE}" \
		\) \
	] && die "Please use --change-db-credentials to change engine db credentials"
	if [ -n "${CHANGE_DWH_DB_CREDENTIALS}" ]; then
		[ -n "${PROVISION_DWH_DB}" ] && die "Cannot change credentials if provisioning a database"
		[ -n "${MY_DWH_DB_HOST}" ] || die "--dwh-db-host is missing"
		[ -n "${MY_DWH_DB_USER}" ] || die "--dwh-db-user is missing"
		[ -n "${MY_DWH_DB_PASSWORD}" ] || \
			die "--dwh-db-passfile or --dwh-db-password is missing"
		[ -n "${MY_DWH_DB_DATABASE}" ] || die "--dwh-db-name is missing"
	fi
	[ -z "${CHANGE_DWH_DB_CREDENTIALS}" -a \
		\( \
			-n "${MY_DWH_DB_HOST}" \
			-o -n "${MY_DWH_DB_USER}" \
			-o -n "${MY_DWH_DB_PASSWORD}" \
			-o -n "${MY_DWH_DB_DATABASE}" \
		\) \
	] && die "Please use --change-dwh-db-credentials to change dwh db credentials"
	if [ -n "${CHANGE_REPORTS_DB_CREDENTIALS}" ]; then
		[ -n "${PROVISION_REPORTS_DB}" ] && die "Cannot change credentials if provisioning a database"
		[ -n "${MY_REPORTS_DB_HOST}" ] || die "--reports-db-host is missing"
		[ -n "${MY_REPORTS_DB_USER}" ] || die "--reports-db-user is missing"
		[ -n "${MY_REPORTS_DB_PASSWORD}" ] || \
			die "--reports-db-passfile or --reports-db-password is missing"
		[ -n "${MY_REPORTS_DB_DATABASE}" ] || die "--reports-db-name is missing"
	fi
	[ -z "${CHANGE_REPORTS_DB_CREDENTIALS}" -a \
		\( \
			-n "${MY_REPORTS_DB_HOST}" \
			-o -n "${MY_REPORTS_DB_USER}" \
			-o -n "${MY_REPORTS_DB_PASSWORD}" \
			-o -n "${MY_REPORTS_DB_DATABASE}" \
		\) \
	] && die "Please use --change-reports-db-credentials to change reports db credentials"
}

# Expects user/host/port/database in the environment.
# Note that in most shells they can be local to the caller and will be inherited.
pg_cmd() {
	local cmd="$1"
	shift

	local use_d=
	# pg_dump 8.x does not accept '-d database'.
	# psql and pg_dump accept it as first non-option argument.
	# pg_restore requires '-d'.
	[ "${cmd}" = "pg_restore" ] && use_d=1

	log "pg_cmd running: ${cmd} -w -U ${user} -h ${host} -p ${port} ${use_d:+-d} ${database} $*"
	PGPASSFILE="${MYPGPASS}" "${cmd}" -w -U "${user}" -h "${host}" -p "${port}" ${use_d:+-d} "${database}" "$@"
}

dobackup() {
	output "Backing up:"
	log "Generating pgpass"
	generatePgPass
	if [ -n "${ENGINE_DB_USER}" ]; then
		notify_engine "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" 0 "Started"
	fi

	# Create temporary folder
	local tardir="${TEMP_FOLDER}/tar"
	log "Creating temp folder ${tardir}"
	mkdir "${tardir}" || logdie "Cannot create '${tardir}'"
	mkdir "${tardir}/db" || logdie "Cannot create '${tardir}/db'"

	if [ -n "${SCOPE_FILES}" ] ; then
		output "- Files"
		log "Backing up files to ${tardir}/files"
		backupFiles "${BACKUP_PATHS}" "${tardir}/files"
	fi

	if [ -n "${SCOPE_ENGINE_DB}" -a -n "${ENGINE_DB_USER}" ]; then
		output "- Engine database '"${ENGINE_DB_DATABASE}"'"
		log "Backing up database to ${tardir}/db/${DB_BACKUP_FILE_NAME}"
		backupDB "${tardir}/db/${DB_BACKUP_FILE_NAME}" "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" "${DB_DUMP_COMPRESSOR}" "${DB_DUMP_FORMAT}"
	fi
	if [ -n "${SCOPE_DWH_DB}" -a -n "${DWH_DB_USER}" ]; then
		output "- DWH database '"${DWH_DB_DATABASE}"'"
		log "Backing up dwh database to ${tardir}/db/${DWHDB_BACKUP_FILE_NAME}"
		backupDB "${tardir}/db/${DWHDB_BACKUP_FILE_NAME}" "${DWH_DB_USER}" "${DWH_DB_HOST}" "${DWH_DB_PORT}" "${DWH_DB_DATABASE}" "${DWH_DB_DUMP_COMPRESSOR}" "${DWH_DB_DUMP_FORMAT}"
	fi
	if [ -n "${SCOPE_REPORTS_DB}" -a -n "${REPORTS_DB_USER}" ]; then
		output "- Reports database '"${REPORTS_DB_DATABASE}"'"
		log "Backing up reports database to ${tardir}/db/${REPORTSDB_BACKUP_FILE_NAME}"
		backupDB "${tardir}/db/${REPORTSDB_BACKUP_FILE_NAME}" "${REPORTS_DB_USER}" "${REPORTS_DB_HOST}" "${REPORTS_DB_PORT}" "${REPORTS_DB_DATABASE}" "${REPORTS_DB_DUMP_COMPRESSOR}" "${REPORTS_DB_DUMP_FORMAT}"
	fi
	echo "${PACKAGE_VERSION}" > "${tardir}/version" || logdie "Can't create ${tardir}/version"
	dump_config_for_restore > "${tardir}/config" || logdie "Can't create ${tardir}/config"
	log "Creating md5sum at ${tardir}/md5sum"
	createmd5 "${tardir}" "${tardir}/md5sum"
	output "Packing into file '${FILE}'"
	log "Creating tarball ${FILE}"
	createtar "${tardir}" "${FILE}"
	if [ -n "${ENGINE_DB_USER}" ]; then
		notify_engine "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" 1 "Finished"
	fi

}

createtar() {
	local dir="$1"
	local file="$2"
	tar -C "${dir}" -cpSs"${ARCHIVE_COMPRESS_OPTION}"f "${file}" . || logdie "Cannot create '${file}'"
}

createmd5() {
	local tardir="$1"
	local md5file="$2"
	find "${tardir}" -type f -printf "%P\n" | while read -r file; do
		( cd "${tardir}" && md5sum "${file}" ) >> "${md5file}" || logdie "Cannot create checksum for '${file}'"
	done || logdie "Find execution failed"
}

verifymd5() {
	local tardir="$1"
	local md5file="$2"
	( cd "${tardir}" && md5sum -c "${md5file}" --status ) || logdie "Checksum verification failed"
}

backupFiles() {
	local paths="$1"
	local target="$2"
	echo "${paths}" | \
		while read -r path; do
			[ -e "${path}" ] && echo "${path}"
		done | \
		sed 's;^/;;' | \
		tar -C / --files-from - -cpSs"${FILES_COMPRESS_OPTION}"f "${target}" || logdie "Failed backing up ${paths}"
}

backupDB() {
	local file="$1"
	local user="$2"
	local host="$3"
	local port="$4"
	local database="$5"
	local compressor="$6"
	local format="$7"

	local pgdump_log="${TEMP_FOLDER}/pgdump.log"
	local failed_msg=

	if [ -n "${compressor}" ]; then
		pg_cmd pg_dump \
			-E "UTF8" \
			--disable-dollar-quoting \
			--disable-triggers \
			--format="${format}" \
			--no-owner \
			--no-privileges \
			2> "${pgdump_log}" \
			| "${compressor}" > "${file}" \
			|| failed_msg="${compressor} failed compressing the backup of database ${database}"
	else
		pg_cmd pg_dump \
			-E "UTF8" \
			--disable-dollar-quoting \
			--disable-triggers \
			--format="${format}" \
			--no-owner \
			--no-privileges \
			2> "${pgdump_log}" \
			> "${file}" \
			|| failed_msg="Database ${database} backup failed"
	fi

	if [ -s "${pgdump_log}" ]; then
		cat "${pgdump_log}" >> "${LOG}"
		[ -z "${failed_msg}" ] && failed_msg="Database ${database} backup failed"
	fi
	[ -n "${failed_msg}" ] && logdie "${failed_msg}"
}

dorestore() {
	output "Preparing to restore:"
	if [ -r "${ENGINE_UP_MARK}" ]; then
		ps "$(cat ${ENGINE_UP_MARK})" | grep -q 'ovirt-engine.py' &&
			logdie "Engine service is active - can not restore backup"
	fi

	if [ -n "${CHANGE_DB_CREDENTIALS}" ]; then
		output "- Setting credentials for Engine database '${MY_DB_DATABASE}'"
		setMyEngineDBCredentials
		generatePgPass
		verifyConnection "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}"
	fi
	if [ -n "${CHANGE_DWH_DB_CREDENTIALS}" ]; then
		output "- Setting credentials for DWH database '${MY_DWH_DB_DATABASE}'"
		setMyDwhDBCredentials
		generatePgPass
		verifyConnection "${DWH_DB_USER}" "${DWH_DB_HOST}" "${DWH_DB_PORT}" "${DWH_DB_DATABASE}"
	fi
	if [ -n "${CHANGE_REPORTS_DB_CREDENTIALS}" ]; then
		output "- Setting credentials for Reports database '${MY_REPORTS_DB_DATABASE}'"
		setMyReportsDBCredentials
		generatePgPass
		verifyConnection "${REPORTS_DB_USER}" "${REPORTS_DB_HOST}" "${REPORTS_DB_PORT}" "${REPORTS_DB_DATABASE}"
	fi

	output "- Unpacking file '${FILE}'"
	log "Opening tarball ${FILE} to ${TEMP_FOLDER}"
	tar -C "${TEMP_FOLDER}" -pSsxf "${FILE}" || logdie "cannot open ${TEMP_FOLDER}"
	log "Verifying md5"
	verifymd5 "${TEMP_FOLDER}" "md5sum"
	log "Verifying version"
	verifyVersion

	. "${TEMP_FOLDER}/config"

	# Refresh scope vars according to what actually found
	[ -s "${TEMP_FOLDER}/files" ] || SCOPE_FILES=
	[ -s "${TEMP_FOLDER}/db/${DB_BACKUP_FILE_NAME}" ] || SCOPE_ENGINE_DB=
	[ -s "${TEMP_FOLDER}/db/${DWHDB_BACKUP_FILE_NAME}" ] || SCOPE_DWH_DB=
	[ -s "${TEMP_FOLDER}/db/${REPORTSDB_BACKUP_FILE_NAME}" ] || SCOPE_REPORTS_DB=

	output "Restoring:"
	if [ -n "${SCOPE_FILES}" ] ; then
		output "- Files"
		log "Restoring files"
		restoreFiles "${BACKUP_PATHS}" "${TEMP_FOLDER}/files"
	fi

	log "Reloading configuration"
	my_load_config

	if [ -n "${PROVISIONING}" ]; then
		output "Provisioning PostgreSQL users/databases:"
		if [ -n "${PROVISION_DB}" -a -n "${SCOPE_ENGINE_DB}" ]; then
			provisionDB "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" "${ENGINE_DB_PASSWORD}" "${ENGINE_DB_SECURED}" "${ENGINE_DB_SECURED_VALIDATION}"
		fi
		if [ -n "${PROVISION_DWH_DB}" -a -n "${SCOPE_DWH_DB}" ]; then
			provisionDB "${DWH_DB_USER}" "${DWH_DB_HOST}" "${DWH_DB_PORT}" "${DWH_DB_DATABASE}" "${DWH_DB_PASSWORD}" "${DWH_DB_SECURED}" "${DWH_DB_SECURED_VALIDATION}"
		fi
		if [ -n "${PROVISION_REPORTS_DB}" -a -n "${SCOPE_REPORTS_DB}" ]; then
			provisionDB "${REPORTS_DB_USER}" "${REPORTS_DB_HOST}" "${REPORTS_DB_PORT}" "${REPORTS_DB_DATABASE}" "${REPORTS_DB_PASSWORD}" "${REPORTS_DB_SECURED}" "${REPORTS_DB_SECURED_VALIDATION}"
		fi
		output "Restoring:"
	fi

	[ -n "${CHANGE_DB_CREDENTIALS}" ] && setMyEngineDBCredentials
	[ -n "${CHANGE_DWH_DB_CREDENTIALS}" ] && setMyDwhDBCredentials
	[ -n "${CHANGE_REPORTS_DB_CREDENTIALS}" ] && setMyReportsDBCredentials

	log "Generating pgpass"
	generatePgPass # Must run after configuration reload
	log "Verifying connection"
	[ -n "${SCOPE_ENGINE_DB}" -a -n "${ENGINE_DB_USER}" ] && verifyConnection "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}"
	[ -n "${SCOPE_DWH_DB}" -a -n "${DWH_DB_USER}" ] && verifyConnection "${DWH_DB_USER}" "${DWH_DB_HOST}" "${DWH_DB_PORT}" "${DWH_DB_DATABASE}"
	[ -n "${SCOPE_REPORTS_DB}" -a -n "${REPORTS_DB_USER}" ] && verifyConnection "${REPORTS_DB_USER}" "${REPORTS_DB_HOST}" "${REPORTS_DB_PORT}" "${REPORTS_DB_DATABASE}"

	if [ -n "${SCOPE_ENGINE_DB}" -a -n "${ENGINE_DB_USER}" ]; then
		output "- Engine database '"${ENGINE_DB_DATABASE}"'"
		log "Restoring engine database backup at ${TEMP_FOLDER}/db/${DB_BACKUP_FILE_NAME}"
		restoreDB "${TEMP_FOLDER}/db/${DB_BACKUP_FILE_NAME}" "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" "${ORIG_DB_USER}" "${DB_DUMP_COMPRESSOR}" "${DB_DUMP_FORMAT}" "${DB_RESTORE_JOBS}"
		if [ -z "${KEEP_TEMPORARY_DATA}" ]; then
			output "  - Cleaning up temporary tables in engine database '${ENGINE_DB_DATABASE}'"
			cleanDbTempData "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" "${ENGINE_TABLES_TO_CLEAN_ON_RESTORE}"
			resetDwhCurrentlyRunning "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" "${ENGINE_TABLES_TO_CLEAN_ON_RESTORE}"
		fi
	fi
	if [ -n "${SCOPE_DWH_DB}" -a -n "${DWH_DB_USER}" ]; then
		output "- DWH database '"${DWH_DB_DATABASE}"'"
		log "Restoring dwh database backup at ${TEMP_FOLDER}/db/${DWHDB_BACKUP_FILE_NAME}"
		restoreDB "${TEMP_FOLDER}/db/${DWHDB_BACKUP_FILE_NAME}" "${DWH_DB_USER}" "${DWH_DB_HOST}" "${DWH_DB_PORT}" "${DWH_DB_DATABASE}" "${ORIG_DWH_DB_USER}" "${DWH_DB_DUMP_COMPRESSOR}" "${DWH_DB_DUMP_FORMAT}" "${DWH_DB_RESTORE_JOBS}"
	fi
	if [ -n "${SCOPE_REPORTS_DB}" -a -n "${REPORTS_DB_USER}" ]; then
		output "- Reports database '"${REPORTS_DB_DATABASE}"'"
		log "Restoring REPORTS database backup at ${TEMP_FOLDER}/db/${REPORTSDB_BACKUP_FILE_NAME}"
		restoreDB "${TEMP_FOLDER}/db/${REPORTSDB_BACKUP_FILE_NAME}" "${REPORTS_DB_USER}" "${REPORTS_DB_HOST}" "${REPORTS_DB_PORT}" "${REPORTS_DB_DATABASE}" "${ORIG_REPORTS_DB_USER}" "${REPORTS_DB_DUMP_COMPRESSOR}" "${REPORTS_DB_DUMP_FORMAT}" "${REPORTS_DB_RESTORE_JOBS}"
	fi
	[ -n "${CHANGE_DB_CREDENTIALS}" ] && changeEngineDBConf
	[ -n "${CHANGE_DWH_DB_CREDENTIALS}" -o -n "${CHANGE_DB_CREDENTIALS}" ] && changeDwhDBConf
	[ -n "${CHANGE_REPORTS_DB_CREDENTIALS}" ] && changeReportsDBConf
	output "You should now run engine-setup."
}

verifyConnection() {
	local user="$1"
	local host="$2"
	local port="$3"
	local database="$4"

	local pgrestorelog="${TEMP_FOLDER}/pg-restore-log"

	pg_cmd psql -c "select 1" >> "${LOG}" 2>&1 \
		|| logdie "Can't connect to database '${database}'. Please see '${0} --help'."

	pg_cmd psql -t -c "show lc_messages" 2> /dev/null \
		| grep -q '^ *en_US.UTF-8$' \
		|| logdie "lc_messages is set to an unsupported value in postgresql.conf. Please set it to en_US.UTF-8 and restart postgresql."

	local IGNORED_PATTERN=$(cat << __EOF | tr '\012' '|' | sed 's/|$//'
^create extension
^create procedural language
__EOF
)

	pg_cmd pg_dump -s 2> "${pgrestorelog}" | \
		grep -Evi "${IGNORED_PATTERN}" | \
		grep -iq '^create' && \
		logdie "Database '${database}' is not empty"

	if [ -s "${pgrestorelog}" ]; then
		cat "${pgrestorelog}" >> "${LOG}"
		logdie "Failed checking if database '${database}' is empty"
	fi
}

verifyVersion() {
	local installed_version="$(echo ${PACKAGE_VERSION} | cut -d . -f 1-2)"
	local backup_version="$(cat ${TEMP_FOLDER}/version | cut -d . -f 1-2)"
	[ "${installed_version}" == "${backup_version}" ] \
		|| logdie "Backup version '${backup_version}' doesn't match installed version"
}

bz_cat() {
	file="$1"

	if [ -f "${file}" ]; then
		cat < "${file}"
	elif [ -f "${file}.bz2" ]; then
		bzcat < "${file}.bz2"
	else
		logdie "${file} and ${file}.bz2 not found"
	fi
}

provisionDB() {
	local user="$1"
	local host="$2"
	local port="$3"
	local database="$4"
	local password="$5"
	local secured="$6"
	local secured_host_validation="$7"

	log "provisionDB: user ${user} host ${host} port ${port} database ${database} secured ${secured} secured_host_validation ${secured_host_validation}"
	output "- user '${user}', database '${database}'"

	local pgprovisionlog="${TEMP_FOLDER}/pg-provision-log"
	local answerfile="${TEMP_FOLDER}/pg-provision-answer-file"

	[ "${host}" != 'localhost' ] && logdie "Can provision database only in localhost"
	[ "${port}" != "${POSTGRESQL_DEFAULT_PORT}" ] && "Can provision database only with the default port of PostgreSQL"
	[ "${secured}" != 'False' ] && logdie "Cannot provision database with secured connection"
	[ "${secured_host_validation}" != 'False' ] && logdie "Cannot provision database with secured host validation"
	[ -z "${user}" -o -z "${database}" -o -z "${password}" ] && logdie "Some database credentials missing - cannot provision database"

	cat << __EOF__ > "${answerfile}"
[environment:default]
OVESETUP_PROVISION_DB/host=str:localhost
OVESETUP_PROVISION_DB/port=int:5432
OVESETUP_PROVISION_DB/secured=bool:False
OVESETUP_PROVISION_DB/securedHostValidation=bool:False
OVESETUP_PROVISION_DB/database=str:${database}
OVESETUP_PROVISION_DB/user=str:${user}
OVESETUP_PROVISION_DB/password=str:${password}
OVESETUP_PROVISION_DB/dumper=str:pg_custom
OVESETUP_PROVISION_DB/filter=none:None
OVESETUP_PROVISION_DB/restoreJobs=int:2
OVESETUP_CORE/engineStop=bool:False
__EOF__

	/usr/share/ovirt-engine/setup/bin/ovirt-engine-provisiondb --config-append="${answerfile}" < /dev/null > "${pgprovisionlog}" 2>&1
	provrc=$?
	cat "${pgprovisionlog}" >> "${LOG}"  2>&1 \
		|| logdie "Failed to append pg provisioning log to restore log"
	case "${provrc}" in
		0) : ;;
		12) logdie "Provisioning is not supported" ;;
		13) logdie "Existing database '${database}' or user '${user}' found and temporary ones created - Please clean up everything and try again" ;;
		*) logdie "Provisioning database '${database}' failed, please check the log for details" ;;
	esac
}

restoreDB() {
	local backupfile="$1"
	local user="$2"
	local host="$3"
	local port="$4"
	local database="$5"
	local orig_user="$6"
	local compressor="$7"
	local format="$8"
	local jobsnum="$9"

	log "restoreDB: backupfile ${backupfile} user ${user} host ${host} port ${port} database ${database} orig_user ${orig_user} compressor ${compressor} format ${format} jobsnum ${jobsnum}"
	local pgrestorelog="${TEMP_FOLDER}/pg-restore-log"
	local failed_msg=

	if [ "${format}" = "plain" ]; then
		if [ -z "${compressor}" ]; then
			pg_cmd psql -f "${backupfile}" > "${pgrestorelog}"  2>&1 \
				|| failed_msg="Database ${database} restore failed"
		else
			# Requires the compressor to support '-d'. All our current ones do.
			"${compressor}" -d < "${backupfile}" | \
				pg_cmd psql > "${pgrestorelog}"  2>&1 \
				|| failed_msg="Database ${database} restore failed"
		fi
	elif [ "${format}" = "custom" ]; then
		if [ -z "${compressor}" ]; then
			pg_cmd pg_restore --no-owner --no-privileges -j "${jobsnum}" "${backupfile}" > "${pgrestorelog}"  2>&1
		else
			# Requires the compressor to support '-d'. All our current ones do.
			"${compressor}" -d < "${backupfile}" | \
				pg_cmd pg_restore --no-owner --no-privileges > "${pgrestorelog}"  2>&1
		fi
	else
		logdie "Unsupported format ${format}"
	fi

	cat "${pgrestorelog}" >> "${LOG}"  2>&1 \
		|| logdie "Failed to append pg log to restore log"

	[ -n "${failed_msg}" ] && logdie "${failed_msg}"

	local IGNORED_ERRORS=$(cat << __EOF | egrep -v '^$|^#' | tr '\012' '|' | sed 's/|$//'
language "plpgsql" already exists
must be owner of language plpgsql
must be owner of extension plpgsql
#
# older versions of dwh used uuid-ossp, which requires special privs,
# is not used anymore, and emits the following errors for normal users.
permission denied for language c
function public.uuid_generate_v1\(\) does not exist
function public.uuid_generate_v1mc\(\) does not exist
function public.uuid_generate_v3\(uuid, text\) does not exist
function public.uuid_generate_v4\(\) does not exist
function public.uuid_generate_v5\(uuid, text\) does not exist
function public.uuid_nil\(\) does not exist
function public.uuid_ns_dns\(\) does not exist
function public.uuid_ns_oid\(\) does not exist
function public.uuid_ns_url\(\) does not exist
function public.uuid_ns_x500\(\) does not exist
__EOF
)
	local numerrors=$(grep 'ERROR: ' "${pgrestorelog}" | grep -Ev "${IGNORED_ERRORS}" | wc -l)
	[ ${numerrors} -ne 0 ] && logdie "Errors while restoring database ${database}"
}

cleanDbTempData() {
	local user="$1"
	local host="$2"
	local port="$3"
	local database="$4"
	local tables_to_clean="$5"
	echo "${tables_to_clean}" | while read -r table; do
		log "truncating ${table}"
		pg_cmd psql \
			-t \
			-c "TRUNCATE TABLE ${table} cascade" \
			>> "${LOG}"  2>&1 \
			|| logdie "Failed cleaning up ${table}"
	done || logdie "Failed cleaning up temp data"
}

resetDwhCurrentlyRunning() {
	local user="$1"
	local host="$2"
	local port="$3"
	local database="$4"
	local psqlout="${TEMP_FOLDER}/psql-dwhrunning-out"

	local sel_q="SELECT var_value FROM dwh_history_timekeeping WHERE var_name='DwhCurrentlyRunning'"
	local upd_q="UPDATE dwh_history_timekeeping SET var_value='0' WHERE var_name='DwhCurrentlyRunning'"

	pg_cmd psql -t -c "${sel_q}" > "${psqlout}" 2>> "${LOG}" \
		|| logdie "Failed checking DwhCurrentlyRunning"

	if grep -q '1' "${psqlout}"; then
		output '  - Resetting DwhCurrentlyRunning in dwh_history_timekeeping in engine database'
		pg_cmd psql -t -c "${upd_q}" >> "${LOG}" 2>&1 \
			|| logdie "Failed resetting DwhCurrentlyRunning"
	fi
}

restoreFiles() {
	local paths="$1"
	local archive="$2"
	tar -C / -pSsxf "${archive}" || logdie "Failed restoring ${paths}"
	if selinuxenabled; then
		echo "${paths}" | while read -r path; do
			if [ -e "${path}" ]; then
				restorecon -R "${path}" || logdie "Failed setting selinux context for ${path}"
			fi
		done || logdie "Failed setting selinux contexts"
	fi
}

setMyEngineDBCredentials() {
	local options

	[ "${MY_DB_SECURED}" = "True" ] && \
		options="${options}&ssl=true"
	[ "${MY_DB_SECURED_VALIDATION}" != "True" ] && \
		options="${options}&sslfactory=org.postgresql.ssl.NonValidatingFactory"

	[ -n "${options}" ] && options="${options#&}"

	local encpass="$(sed 's;\(["\$]\);\\\1;g' << __EOF__
${MY_DB_PASSWORD}
__EOF__
)"

	MY_DB_CREDS="$(cat << __EOF__
ENGINE_DB_HOST="${MY_DB_HOST}"
ENGINE_DB_PORT="${MY_DB_PORT}"
ENGINE_DB_USER="${MY_DB_USER}"
ENGINE_DB_PASSWORD="${encpass}"
ENGINE_DB_DATABASE="${MY_DB_DATABASE}"
ENGINE_DB_SECURED="${MY_DB_SECURED}"
ENGINE_DB_SECURED_VALIDATION="${MY_DB_SECURED_VALIDATION}"
ENGINE_DB_DRIVER="org.postgresql.Driver"
ENGINE_DB_URL="jdbc:postgresql://\${ENGINE_DB_HOST}:\${ENGINE_DB_PORT}/\${ENGINE_DB_DATABASE}?${options}"
__EOF__
)"
	eval "${MY_DB_CREDS}"
}

setMyDwhDBCredentials() {
	local options

	[ "${MY_DWH_DB_SECURED}" = "True" ] && \
		options="${options}&ssl=true"
	[ "${MY_DWH_DB_SECURED_VALIDATION}" != "True" ] && \
		options="${options}&sslfactory=org.postgresql.ssl.NonValidatingFactory"

	[ -n "${options}" ] && options="${options#&}"

	local encpass="$(sed 's;\(["\$]\);\\\1;g' << __EOF__
${MY_DWH_DB_PASSWORD}
__EOF__
)"

	MY_DWH_DB_CREDS="$(cat << __EOF__
DWH_DB_HOST="${MY_DWH_DB_HOST}"
DWH_DB_PORT="${MY_DWH_DB_PORT}"
DWH_DB_USER="${MY_DWH_DB_USER}"
DWH_DB_PASSWORD="${encpass}"
DWH_DB_DATABASE="${MY_DWH_DB_DATABASE}"
DWH_DB_SECURED="${MY_DWH_DB_SECURED}"
DWH_DB_SECURED_VALIDATION="${MY_DWH_DB_SECURED_VALIDATION}"
DWH_DB_DRIVER="org.postgresql.Driver"
DWH_DB_URL="jdbc:postgresql://\${DWH_DB_HOST}:\${DWH_DB_PORT}/\${DWH_DB_DATABASE}?${options}"
__EOF__
)"
	eval "${MY_DWH_DB_CREDS}"
}

setMyReportsDBCredentials() {
	local options

	[ "${MY_REPORTS_DB_SECURED}" = "True" ] && \
		options="${options}&ssl=true"
	[ "${MY_REPORTS_DB_SECURED_VALIDATION}" != "True" ] && \
		options="${options}&sslfactory=org.postgresql.ssl.NonValidatingFactory"

	[ -n "${options}" ] && options="${options#&}"

	local encpass="$(sed 's;\(["\$]\);\\\1;g' << __EOF__
${MY_REPORTS_DB_PASSWORD}
__EOF__
)"

	MY_REPORTS_DB_CREDS="$(cat << __EOF__
REPORTS_DB_HOST="${MY_REPORTS_DB_HOST}"
REPORTS_DB_PORT="${MY_REPORTS_DB_PORT}"
REPORTS_DB_USER="${MY_REPORTS_DB_USER}"
REPORTS_DB_PASSWORD="${encpass}"
REPORTS_DB_DATABASE="${MY_REPORTS_DB_DATABASE}"
REPORTS_DB_SECURED="${MY_REPORTS_DB_SECURED}"
REPORTS_DB_SECURED_VALIDATION="${MY_REPORTS_DB_SECURED_VALIDATION}"
REPORTS_DB_DRIVER="org.postgresql.Driver"
REPORTS_DB_URL="jdbc:postgresql://\${REPORTS_DB_HOST}:\${REPORTS_DB_PORT}/\${REPORTS_DB_DATABASE}?${options}"
__EOF__
)"
	eval "${MY_REPORTS_DB_CREDS}"
}

changeEngineDBConf() {
	local conf="${ENGINE_ETC}/engine.conf.d/10-setup-database.conf"
	[ -f "${conf}" ] || logdie "Can not find ${conf}"

	local backup="${conf}.$(date +"%Y%m%d%H%M%S")"
	log "Backing up ${conf} to ${backup}"
	cp -a "${conf}" "${backup}" || die "Failed to backup ${conf}"
	output "Rewriting ${conf}"
	printf "%s\n" "${MY_DB_CREDS}" > "${conf}"
}

changeDwhDBConf() {
	local conf="${DWH_CONFIG}.d/10-setup-database.conf"
	[ -f "${conf}" ] || logdie "Can not find ${conf}"

	local backup="${conf}.$(date +"%Y%m%d%H%M%S")"
	log "Backing up ${conf} to ${backup}"
	cp -a "${conf}" "${backup}" || die "Failed to backup ${conf}"
	output "Rewriting ${conf}"
	if [ -z "${MY_DB_CREDS}" ]; then
		MY_DB_HOST="${ENGINE_DB_HOST}"
		MY_DB_PORT="${ENGINE_DB_PORT}"
		MY_DB_USER="${ENGINE_DB_USER}"
		MY_DB_PASSWORD="${ENGINE_DB_PASSWORD}"
		MY_DB_DATABASE="${ENGINE_DB_DATABASE}"
		MY_DB_SECURED="${ENGINE_DB_SECURED}"
		MY_DB_SECURED_VALIDATION="${ENGINE_DB_SECURED_VALIDATION}"
		setMyEngineDBCredentials
	fi
	if [ -z "${MY_DWH_DB_CREDS}" ]; then
		MY_DWH_DB_HOST="${DWH_DB_HOST}"
		MY_DWH_DB_PORT="${DWH_DB_PORT}"
		MY_DWH_DB_USER="${DWH_DB_USER}"
		MY_DWH_DB_PASSWORD="${DWH_DB_PASSWORD}"
		MY_DWH_DB_DATABASE="${DWH_DB_DATABASE}"
		MY_DWH_DB_SECURED="${DWH_DB_SECURED}"
		MY_DWH_DB_SECURED_VALIDATION="${DWH_DB_SECURED_VALIDATION}"
		setMyDwhDBCredentials
	fi
	printf "%s\n" "${MY_DB_CREDS}" > "${conf}"
	printf "%s\n" "${MY_DWH_DB_CREDS}" >> "${conf}"
}

changeReportsDBConf() {
	local conf="${JASPER_PROPERTIES}"
	[ -f "${conf}" ] || logdie "Can not find ${conf}"

	local backup="${conf}.$(date +"%Y%m%d%H%M%S")"
	log "Backing up ${conf} to ${backup}"
	cp -a "${conf}" "${backup}" || die "Failed to backup ${conf}"
	output "Rewriting ${conf}"
	cat << __EOF__ > "${conf}"
# File locations
reportsHome=/var/lib/ovirt-engine-reports
reportsWar=/var/lib/ovirt-engine-reports/ovirt-engine-reports.war
currentConf=/var/lib/ovirt-engine-reports/build-conf
appServerDir=/var/lib/ovirt-engine-reports

appServerType=jboss7

# database type
dbType=postgresql

# database location and connection settings
dbHost=${REPORTS_DB_HOST}
dbPort=${REPORTS_DB_PORT}
dbUsername=${REPORTS_DB_USER}
dbPassword=${REPORTS_DB_PASSWORD}
js.dbName=${REPORTS_DB_DATABASE}

# web app name
# (set one of these to deploy to a non-default war file name)
webAppNameCE=ovirt-engine-reports
webAppNamePro=ovirt-engine-reports

# Database
maven.jdbc.groupId=postgresql
maven.jdbc.artifactId=postgresql
maven.jdbc.version=9.2-1002.jdbc4
deployJDBCDriver=false
__EOF__
}

generatePgPass() {
	local password="${ENGINE_DB_PASSWORD}"
	local dwh_password="${DWH_DB_PASSWORD}"
	local reports_password="${REPORTS_DB_PASSWORD}"
	MYPGPASS="${TEMP_FOLDER}/.pgpass"

	touch "${MYPGPASS}" || logdie "Can't touch ${MYPGPASS}"
	chmod 0600 "${MYPGPASS}" || logdie "Can't chmod ${MYPGPASS}"

	#
	# we need client side psql library
	# version as at least in rhel for 8.4
	# the password within pgpassfile is
	# not escaped.
	# the simplest way is to checkout psql
	# utility version.
	#
	if ! psql -V | grep -q ' 8\.'; then
		password="$(printf "%s" "${password}" | sed -e 's/\\/\\\\/g' -e 's/:/\\:/g')"
		dwh_password="$(printf "%s" "${dwh_password}" | sed -e 's/\\/\\\\/g' -e 's/:/\\:/g')"
		reports_password="$(printf "%s" "${reports_password}" | sed -e 's/\\/\\\\/g' -e 's/:/\\:/g')"
	fi

	cat > "${MYPGPASS}" << __EOF__
${ENGINE_DB_HOST}:${ENGINE_DB_PORT}:${ENGINE_DB_DATABASE}:${ENGINE_DB_USER}:${password}
__EOF__
	[ -n "${DWH_DB_USER}" ] && cat >> "${MYPGPASS}" << __EOF__
${DWH_DB_HOST}:${DWH_DB_PORT}:${DWH_DB_DATABASE}:${DWH_DB_USER}:${dwh_password}
__EOF__
	[ -n "${REPORTS_DB_USER}" ] && cat >> "${MYPGPASS}" << __EOF__
${REPORTS_DB_HOST}:${REPORTS_DB_PORT}:${REPORTS_DB_DATABASE}:${REPORTS_DB_USER}:${reports_password}
__EOF__
}

log() {
	local m="$1"
	local date="$(date '+%Y-%m-%d %H:%M:%S')"
	local pid="$$"
	printf "%s\n" "${date} ${pid}: ${m}" >> "${LOG}"
}

logdie() {
	local m="$1"
	log "FATAL: ${m}"
	if [ -n "${ENGINE_DB_USER}" -a "${MODE}" = "backup" ]; then
		notify_engine "${ENGINE_DB_USER}" "${ENGINE_DB_HOST}" "${ENGINE_DB_PORT}" "${ENGINE_DB_DATABASE}" -1 "${m}"
		FAILURE_NOTIFIED=1
	fi
	die "${m}"
}

output() {
	local m="$1"
	log "OUTPUT: ${m}"
	printf "%s\n" "${m}"
}

readdbpassword() {
	local app="$1"
	(
		cleanup() {
			[ -n "${STTY_ORIG}" ] && stty "${STTY_ORIG}"
		}

		STTY_ORIG=
		trap cleanup 0
		[ -t 0 ] || die "Standard input is not a terminal"
		STTY_ORIG="$(stty -g)"
		stty -echo || die "Failed to disable terminal input echo"
		printf "Enter ${app} database password: " >&2
		read -r dbpass
		echo >&2
		cat << __EOF__
${dbpass}
__EOF__
	)
}

dump_config_for_restore() {
	local var
	local VARS_TO_SAVE="DB_DUMP_COMPRESSOR
DB_DUMP_FORMAT
DWH_DB_DUMP_COMPRESSOR
DWH_DB_DUMP_FORMAT
REPORTS_DB_DUMP_COMPRESSOR
REPORTS_DB_DUMP_FORMAT"
	echo "${VARS_TO_SAVE}" | while read -r var; do
		eval echo "${var}=\${${var}}"
	done
}

notify_engine() {
	local user="$1"
	local host="$2"
	local port="$3"
	local database="$4"
	local status="$5"
	local message="$6"

	message="$(printf "%s" "${message}" | sed "s/'/''/g")"

	local logpath="$(readlink -f ${LOG})"

	do_notify() {
		local scope="$1"
		pg_cmd psql -t -c "SELECT LogEngineBackupEvent('${scope}', now(), ${status}, '${message}', '${ENGINE_FQDN}', '${logpath}');" \
			>> "${LOG}"  2>&1 \
			|| logdie "Failed notifying engine"
	}

	output "Notifying engine"
	[ -n "${SCOPE_FILES}" ] && do_notify 'files'
	[ -n "${SCOPE_ENGINE_DB}" ] && do_notify 'db'
	[ -n "${SCOPE_DWH_DB}" ] && do_notify 'dwhdb'
	[ -n "${SCOPE_REPORTS_DB}" ] && do_notify 'reportsdb'

	unset -f do_notify
}

## Main

my_load_config

# Do this in function so we do not lose $@
parseArgs "$@"
verifyArgs

TEMP_FOLDER="$(mktemp -d -t engine-backup.XXXXXXXXXX)" || logdie "Can't create temporary directory"

log "Start of engine-backup mode ${MODE} scope ${SCOPE} file ${FILE}"

generatePgPass
do${MODE}
output "Done."

# vim: set noexpandtab shiftwidth=8 tabstop=8:
