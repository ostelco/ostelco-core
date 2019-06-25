#!/bin/bash

# This script generates a self  SSL certificate for a given input domain.

#### input 
DOMAIN_NAME=$1
####

#### sanity check 
if [ -z "${DOMAIN_NAME}" ]; then
  echo "ERROR: No domain-name was provided in input. Aborting!"  > /dev/stderr
  exit 1
fi
####

# Real path is not on every linux distribution.

# SCRIPT_REAL_PATH=$(dirname $(realpath $0))
SCRIPT_REAL_PATH="$( cd "$(dirname "$0")" ; pwd -P )"

pushd ${SCRIPT_REAL_PATH}

CERTS_DIR=../certs/${DOMAIN_NAME}

if [ -d ${CERTS_DIR} ]; then
  echo "Found the matching domain in certs. Generating SSL certs for domain ${DOMAIN_NAME} in ${CERTS_DIR} ..."

  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout ${CERTS_DIR}/nginx.key \
    -out ${CERTS_DIR}/nginx.crt \
    -subj "/CN=${DOMAIN_NAME}"

  echo "Here are the generated certs in ${CERTS_DIR} ..."
  ls -l ${CERTS_DIR}

else
  echo "Could not find a matching domain name in certs for ${DOMAIN_NAME} in ${CERTS_DIR}"  > /dev/stderr
  exit 1
fi

popd
