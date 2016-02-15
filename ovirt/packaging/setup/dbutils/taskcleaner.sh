#!/bin/sh
###############################################################################################################
# The purpose of this utility is to display and clean asynchronous tasks and corresponding
# Job steps/Compensation data.
# The utility enables to
# Display
#     All async tasks
#     Only Zombie tasks
# Delete
#     All tasks
#     All Zombie tasks
#     A task related to a given task id
#     A Zombie task related to a given task id
#     All tasks related to a given command id
#     All Zombie tasks related to a given command id
#  Flags may be added (-C, -J) to specify if Job Steps & Compensation data
#  should be cleaned as well.
###############################################################################################################

. "$(dirname "$0")/dbfunc-base.sh"

cleanup() {
	dbfunc_cleanup
}
trap cleanup 0
dbfunc_init

usage() {
    cat << __EOF__
Usage: $0 [options]

    -h            - This help text.
    -v            - Turn on verbosity                         (WARNING: lots of output)
    -l LOGFILE    - The logfile for capturing output          (def. ${DBFUNC_LOGFILE})
    -s HOST       - The database servername for the database  (def. ${DBFUNC_DB_HOST})
    -p PORT       - The database port for the database        (def. ${DBFUNC_DB_PORT})
    -u USER       - The username for the database             (def. ${DBFUNC_DB_USER})
    -d DATABASE   - The database name                         (def. ${DBFUNC_DB_DATABASE})
    -t TASK_ID    - Removes a task by its Task ID.
    -c COMMAND_ID - Removes all tasks related to the given Command Id.
    -z            - Removes/Displays a Zombie task.
    -R            - Removes all tasks (use with -z to clear only zombie tasks).
    -C            - Clear related compensation entries.
    -J            - Clear related Job Steps.
    -A            - Clear all Job Steps and compensation entries.
    -q            - Quite mode, do not prompt for confirmation.

__EOF__
}

#Using two variables for sql commands in order to control command priority where data should be removed first from
#business_entity_snapshot and step table before removing it from the async_tasks table.
CMD1="";
CMD2="";
TASK_ID=""
COMMAND_ID=""
ZOMBIES_ONLY=
CLEAR_ALL=
CLEAR_COMPENSATION=
CLEAR_JOB_STEPS=
CLEAR_JOB_STEPS_AND_COMPENSATION=
QUITE_MODE=
FIELDS="task_id,task_type,status,started_at,result,action_type as command_type,command_id,step_id,storage_pool_id as DC"

while getopts hvl:s:p:u:d:t:c:zRCJAq option; do
	case "${option}" in
		\?) usage; exit 1;;
		h) usage; exit 0;;
		v) DBFUNC_VERBOSE=1;;
		l) DBFUNC_LOGFILE="${OPTARG}";;
		s) DBFUNC_DB_HOST="${OPTARG}";;
		p) DBFUNC_DB_PORT="${OPTARG}";;
		d) DBFUNC_DB_DATABASE="${OPTARG}";;
		u) DBFUNC_DB_USER="${OPTARG}";;
		t) TASK_ID="${OPTARG}";;
		c) COMMAND_ID="${OPTARG}";;
		z) ZOMBIES_ONLY=1;;
		R) CLEAR_ALL=1;;
		C) CLEAR_COMPENSATION=1;;
		J) CLEAR_JOB_STEPS=1;;
		A) CLEAR_JOB_STEPS_AND_COMPENSATION=1;;
		q) QUITE_MODE=1;;
	esac
done

caution() {
	if [ -z "${QUITE_MODE}" ]; then
		# Highlight the expected results of selected operation.
		cat << __EOF__
$(tput smso) $1 $(tput rmso)
Caution, this operation should be used with care. Please contact support prior to running this command
Are you sure you want to proceed? [y/n]
__EOF__
		read answer
		[ "${answer}" = "y" ] || die "Please contact support for further assistance."
	fi
}

[ -n "${DBFUNC_DB_USER}" ] || die "Please specify user name"
[ -n "${DBFUNC_DB_DATABASE}" ] || die "Please specify database"

if [ "${TASK_ID}" != "" -o "${COMMAND_ID}" != "" -o -n "${CLEAR_ALL}" -o -n "${CLEAR_COMPENSATION}" -o -n "${CLEAR_JOB_STEPS}" ]; then #delete operations block
	if [ -n "${TASK_ID}" ]; then
		if [ -n "${ZOMBIES_ONLY}" ]; then
			CMD2="SELECT DeleteAsyncTaskZombiesByTaskId('${TASK_ID}');"
			if [ -n "${CLEAR_JOB_STEPS}" ]; then
				CMD1="SELECT DeleteJobStepsByTaskId('${TASK_ID}');"
				if [ -n "${CLEAR_COMPENSATION}" ]; then
					caution "This will remove the given Zombie Task, its Job Steps and related Compensation data!!!"
					CMD1="${CMD1}SELECT DeleteEntitySnapshotByZombieTaskId('${TASK_ID}');"
				else
					caution "This will remove the given Zombie Task and its related Job Steps!!!"
				fi
			else
				if [ -n "${CLEAR_COMPENSATION}" ]; then
					caution "This will remove the given Zombie Task and related Compensation data!!!"
					CMD1="${CMD1}SELECT DeleteEntitySnapshotByZombieTaskId('${TASK_ID}');"
				else
					caution "This will remove the given Zombie Task!!!"
				fi
			fi
		else
			CMD2="SELECT Deleteasync_tasks('${TASK_ID}');"
			if [ -n "${CLEAR_JOB_STEPS}" ]; then
				CMD1="SELECT DeleteJobStepsByTaskId('${TASK_ID}');"
				if [ -n "${CLEAR_COMPENSATION}" ]; then
					caution "This will remove the given Task its Job Steps and related Compensation data!!!"
					CMD1="${CMD1}SELECT DeleteEntitySnapshotByTaskId('${TASK_ID}');"
				else
					caution "This will remove the given Task and its related Job Steps!!!"
				fi
			else
				if [ -n "${CLEAR_COMPENSATION}" ]; then
					caution "This will remove the given Task and its related Compensation data!!!"
					CMD1="${CMD1}SELECT DeleteEntitySnapshotByTaskId('${TASK_ID}');"
				else
					caution "This will remove the given Task!!!"
				fi
			fi
		fi
	elif [ "${COMMAND_ID}" != "" ]; then
		if [ -n "${ZOMBIES_ONLY}" ]; then
			CMD2="SELECT DeleteAsyncTaskZombiesByCommandId('${COMMAND_ID}');"
			if [ -n "${CLEAR_COMPENSATION}" ]; then
				CMD1="SELECT delete_entity_snapshot_by_command_id('${COMMAND_ID}');"
				if [ -n "${CLEAR_JOB_STEPS}" ]; then
					caution "This will remove all Zombie Tasks of the given Command its Job Steps and its related Compensation data!!!"
					CMD1="${CMD1}SELECT DeleteJobStepsByZombieCommandId('${COMMAND_ID}');"
				else
					caution "This will remove all Zombie Tasks of the given Command and its related Compensation data!!!"
				fi
			else
				if [ -n "${CLEAR_JOB_STEPS}" ]; then
					caution "This will remove all Zombie Tasks of the given Command and its Job Steps!!!"
					CMD1="${CMD1}SELECT DeleteJobStepsByZombieCommandId('${COMMAND_ID}');"
				else
					caution "This will remove all Zombie Tasks of the given Command!!!"
				fi
			fi
		else
			CMD2="SELECT DeleteAsyncTaskByCommandId('${COMMAND_ID}');"
			if [ -n "${CLEAR_COMPENSATION}" ]; then
				CMD1="SELECT delete_entity_snapshot_by_command_id('${COMMAND_ID}');"
				if [ -n "${CLEAR_JOB_STEPS}" ]; then
					caution "This will remove all Tasks of the given Command its Job Steps and its related Compensation data!!!"
					CMD1="${CMD1}SELECT DeleteJobStepsByCommandId('${COMMAND_ID}');"
				else
					caution "This will remove all Tasks of the given Command and its related Compensation data!!!"
				fi
			else
				if [ -n "${CLEAR_JOB_STEPS}" ]; then
					caution "This will remove all Tasks of the given Command and its Job Steps!!!"
					CMD1="${CMD1}SELECT DeleteJobStepsByCommandId('${COMMAND_ID}');"
				else
					caution "This will remove all Tasks of the given Command!!!"
				fi
			fi
		fi
	elif [ -n "${CLEAR_ALL}" ]; then
		if [ -n "${ZOMBIES_ONLY}" ]; then
			CMD2="SELECT DeleteAsyncTasksZombies();"
			if [ -n "${CLEAR_JOB_STEPS_AND_COMPENSATION}" ]; then
				caution "This will remove all Zombie Tasks in async_tasks table, and all Job Steps and Compensation data!!!"
				CMD1="SELECT DeleteAllJobs(); SELECT DeleteAllEntitySnapshot();"
			else
				if [ -n "${CLEAR_COMPENSATION}" ]; then
					CMD1="${CMD1}SELECT DeleteEntitySnapshotZombies();"
					if [ -n "${CLEAR_JOB_STEPS}" ]; then
						caution "This will remove all Zombie Tasks in async_tasks table, its related Job Steps and Compensation data!!!"
						CMD1="${CMD1}SELECT DeleteJobStepsZombies();"
					else
						caution "This will remove all Zombie Tasks in async_tasks table and its related Compensation data!!!"
					fi
				else
					if [ -n "${CLEAR_JOB_STEPS}" ]; then
						caution "This will remove all Zombie Tasks in async_tasks table and its related Job Steps!!!"
						CMD1="${CMD1}SELECT DeleteJobStepsZombies();"
					else
						caution "This will remove all Zombie Tasks in async_tasks table!!!"
					fi
				fi
			fi
		else
			CMD2="TRUNCATE TABLE async_tasks cascade;"
			if [ -n "${CLEAR_JOB_STEPS_AND_COMPENSATION}" ]; then
				caution "This will remove all Tasks in async_tasks table, and all Job Steps and Compensation data!!!"
				CMD1="SELECT DeleteAllJobs(); SELECT DeleteAllEntitySnapshot();"
			else
				if [ -n "${CLEAR_COMPENSATION}" ]; then
					CMD1="TRUNCATE TABLE business_entity_snapshot cascade;"
					if [ -n "${CLEAR_JOB_STEPS}" ]; then
						caution "This will remove all Tasks in async_tasks table, its related Job Steps and Compensation data!!!"
						CMD1="${CMD1}TRUNCATE TABLE step cascade;"
					else
						caution "This will remove all async_tasks table content and its related Compensation data!!!"
					fi
				else
					if [ -n "${CLEAR_JOB_STEPS}" ]; then
						caution "This will remove all Tasks in async_tasks table and its related Job Steps!!!"
						CMD1="${CMD1}TRUNCATE TABLE step cascade;"
					else
						caution "This will remove all async_tasks table content!!!"
					fi
				fi
			fi
		fi
	else
		die "Please specify task"
	fi
elif [ -n "${ZOMBIES_ONLY}" ]; then #only display operations block
	CMD1="SELECT ${FIELDS} FROM GetAsyncTasksZombies();"
else
	CMD1="SELECT ${FIELDS} FROM GetAllFromasync_tasks();"
fi

# Install taskcleaner procedures
dbfunc_psql_die --file="$(dirname "$0")/taskcleaner_sp.sql" > /dev/null
# Execute
dbfunc_psql_die --command="${CMD1}${CMD2}"
