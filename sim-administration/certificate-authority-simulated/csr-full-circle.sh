#!/bin/bash

###
### Run a full CSR cycle against a CA. Do it all from scratch, generating
### root certificate for the ca, generating the csr, signing the csr
### an injecting the certs in to java keyrings.
###

##
## Check for dependencies
##
DEPENDENCIES="keytool openssl"

for tool in $DEPENDENCIES ; do 
  if [[ -z "$(which $tool)" ]] ; then
    (>&2 echo "$0: Error. Could not find dependency $tool")
    exit 0
  fi
done

##
##  Reset by deleting all old certificates etc.
##

REQ_DIR=requester
CA_DIR=cert-auth
CA_DOMAIN=ca
CA_SERIAL_NUMBER_FILE=${CA_DOMAIN}.srl


rm -rf $REQ_DIR $CA_DIR $CA_SERIAL_NUMBER_FILE
mkdir  $REQ_DIR $CA_DIR


##
## Creating a requesting domain, and a CSR
##


REQUESTING_DOMAIN="not-really-ostelco.org"
REQUESTER_KEY="${REQ_DIR}/${REQUESTING_DOMAIN}.key"
REQUESTER_PUBKEY="${REQ_DIR}/${REQUESTING_DOMAIN}.pubkey"
REQUESTER_PUBKEY_PEM="${REQ_DIR}/${REQUESTING_DOMAIN}-pub.pem"
REQUEST_CSR="${REQ_DIR}/${REQUESTING_DOMAIN}.csr"
REQUEST_CRT="${REQ_DIR}/${REQUESTING_DOMAIN}.crt"
REQUESTER_CONF=requester.conf


# Generate a secret ckey for the requesting domain
openssl genrsa -out $REQUESTER_KEY 2048

# Then extract the pubkey
openssl rsa -in $REQUESTER_KEY -pubout -out $REQUESTER_PUBKEY

openssl x509 -pubkey -noout -in $REQUEST_CRT > $REQUESTER_PUBKEY_PEM

##
## Creating certificate authority (CA) keys & cert.
##

# The domain of the CA
CA_KEY="${CA_DIR}/${CA_DOMAIN}.key"
CA_PUBKEY="${CA_DIR}/${CA_DOMAIN}.pubkey"
CA_PUBKEY_PEM="${CA_DIR}/${CA_DOMAIN}-pub.pem"
CA_CRT="${CA_DIR}/${CA_DOMAIN}.crt"
CA_CONF=ca.conf

# Generate a secret ckey for the CA
openssl genrsa -out $CA_KEY  2048

# Then extract the public part of the CA key
openssl rsa -in $CA_KEY -pubout -out $CA_PUBKEY


# Generate a self-signed certificate for the CA
openssl req -new -x509 -key $CA_KEY -out $CA_CRT -config $CA_CONF
openssl x509 -pubkey -noout -in $CA_CRT > $CA_PUBKEY_PEM

##
##  Use CA to sign request CSR.
##


# Generate a CSR using configuration from the oats.conf
# file (should later be parameterized)
openssl req -new -out $REQUEST_CSR -config $REQUESTER_CONF


# Then sign the requesting certificate
openssl x509 -req -in $REQUEST_CSR -CA $CA_CRT -CAkey $CA_KEY -CAcreateserial -out $REQUEST_CRT



##
## In TLS/SSL there are two types of keystores, 
##   * Truststore: Stores _public_ certificates of  known
##     and trusted root authorities.
##   
##   * Keystore: Stores private certificates of clients and
##     servers
##
## In this section we generate both, for use in a client/server setup.

##
## The locations/passwords for the keystores that the
## output will eventually be placed in.
## 

REQUESTER_KEYSTORE="${REQ_DIR}/requester-keystore.jks"
REQUESTER_TRUSTSTORE="${REQ_DIR}/requester-truststore.jks"
REQUESTER_KEYSTORE_PASSWORD="verySecret1238%%/"
REQUESTER_TRUSTSTORE_PASSWORD="verySecret1238%%/"


keytool  -noprompt -storepass "${REQUESTER_KEYSTORE_PASSWORD}"   -import -trustcacerts -alias requesterKey -file $REQUEST_CRT -keystore $REQUESTER_KEYSTORE
keytool  -noprompt -storepass "${REQUESTER_TRUSTSTORE_PASSWORD}" -import -trustcacerts -alias MyRootCa     -file $CA_CRT      -keystore $REQUESTER_TRUSTSTORE

