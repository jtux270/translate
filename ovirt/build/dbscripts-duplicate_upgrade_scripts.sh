#!/bin/sh

OUT="$(find "$(dirname "$0")/../packaging/dbscripts" | grep -P '\d{2}_\d{2}_\d{2,8}' -o | uniq -d)"

if [ -n "${OUT}" ]; then
	echo "Found duplicate upgrade scripts with version $(echo ${OUT}), please resolve and retry" >&2
	exit 1
fi

exit 0
