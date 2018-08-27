#!/bin/bash

PANTEL_SECRETS_FILE=$(cat /tmp/pantel-prod.json | base64 -w 0)

if [ -z "${PANTEL_SECRETS_FILE}" ] ; then
  echo "the PANTEL_SECRETS_FILE env var is empty. Can't proceed. Exiting ..."
  exit 1
fi
echo; echo "======> Creating pantel-prod.json file, using the env variable PANTEL_SECRETS_FILE"
for LOCATION in $(find . -name .gitignore  -exec grep pantel-prod.json  '{}' '+' ); do
  DIR_NAME=$(dirname $LOCATION)
  echo "Creating secrets file: ${DIR_NAME}/pantel-prod.json ..."
  echo ${PANTEL_SECRETS_FILE} | base64 -d >  ${DIR_NAME}/pantel-prod.json
  ls -l ${DIR_NAME}/pantel-prod.json
done
echo ''

