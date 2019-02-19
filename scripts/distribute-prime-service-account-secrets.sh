#!/bin/bash

# This script finds directories where prime-service-account.json is gitignored in and copies
# the PRIME_SERVICE_ACCOUNT_SECRETS_FILE environment variable into these directories.
# These files are needed for the docker compose acceptance tests.

#### sanity check
if [ -z "${PRIME_SERVICE_ACCOUNT_SECRETS_FILE}" ] ; then
  echo "ERROR: PRIME_SERVICE_ACCOUNT_SECRETS_FILE env var is empty. Aborting!"
  exit 1
fi
####

echo; echo "======> Creating prime-service-account.json file, using the env variable PRIME_SERVICE_ACCOUNT_SECRETS_FILE"
for LOCATION in $(find . -name .gitignore  -exec grep prime-service-account.json  '{}' '+' ); do
  DIR_NAME=$(dirname $LOCATION)
  echo "Creating secrets file: ${DIR_NAME}/prime-service-account.json ..."
  echo ${PRIME_SERVICE_ACCOUNT_SECRETS_FILE} | base64 -d >  ${DIR_NAME}/prime-service-account.json
  ls -l ${DIR_NAME}/prime-service-account.json
done
echo ''
