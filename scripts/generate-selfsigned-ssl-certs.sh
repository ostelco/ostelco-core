#!/bin/bash


DOMAIN_NAME=$1
if [ -z "${DOMAIN_NAME}" ]; then
  echo "No domain-name was provided. Abnormal exit ..."
  exit 1
fi

# Real path is not on every linux distribution.

# SCRIPT_REAL_PATH=$(dirname $(realpath $0))
SCRIPT_REAL_PATH="$( cd "$(dirname "$0")" ; pwd -P )"

pushd ${SCRIPT_REAL_PATH}

CERTS_DIR=../certs/${DOMAIN_NAME}
# OCSGW_CONFIG_DIR=../ocsgw/config
# ESP_SSL_DIR=../esp

if [ -d ${CERTS_DIR} ]; then
  echo "Found the matching domain in certs. Generating SSL certs for domain ${DOMAIN_NAME} in ${CERTS_DIR} ..."

  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout ${CERTS_DIR}/nginx.key \
    -out ${CERTS_DIR}/nginx.crt \
    -subj "/CN=${DOMAIN_NAME}"

  echo "Here are the generated certs in ${CERTS_DIR} ..."
  ls -l ${CERTS_DIR}

  # echo
  # echo "Copying the generated nginx.crt to 'ocsgw/config/' directory"
  # cp ${CERTS_DIR}/nginx.crt ${OCSGW_CONFIG_DIR}
  # ls -l ${OCSGW_CONFIG_DIR}/nginx.crt
  # echo
  # echo ; echo "Copying the generated nginx.* to ${ESP_SSL_DIR} ..."
  # cp ${CERTS_DIR}/nginx.* ${ESP_SSL_DIR}/
  # ls -l ${ESP_SSL_DIR}
  # echo
  #sudo keytool -import -alias ${DOMAIN_NAME} -keystore /usr/lib/jvm/jdk1.8.0/jre/lib/security/cacerts -file ${CERTS_DIR}/nginx.crt -noprompt -storepass changeit
else
  echo "Could not find a matching domain name in certs for ${DOMAIN_NAME}"
fi

popd


