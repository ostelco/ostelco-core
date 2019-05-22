#!/usr/bin/env bash

set -e
#
# Check for dependencies being satisfied
#

DEPENDENCIES="firebase"

for dep in $DEPENDENCIES ; do
    if [[ -z $(which $dep) ]] ; then
        echo "ERROR: Could not find dependency $dep"
    fi
done


if [ ! -f ./scripts/deploy.sh ]; then
    (>&2 echo "Run this script from project root dir")
    exit 1
fi

REACT_APP_DEPLOYMENT_ENV=production yarn build

firebase --project redotter-admin deploy
