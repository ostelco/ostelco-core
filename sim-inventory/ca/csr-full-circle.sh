#!/bin/bash


# Run a full CSR cycle against a CA. Do it all from scratch, generating
# root certificate for the ca, generating the csr, signing the csr
# an injecting the certs in to java keyrings.



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
REQUEST_CSR="${REQ_DIR}/${REQUESTING_DOMAIN}.csr"
REQUEST_CRT="${REQ_DIR}/${REQUESTING_DOMAIN}.crt"
REQUESTER_CONF=requester.conf
REQUESTER_KEYSTORE="${REQ_DIR}/requester-keystore.jks"
REQUESTER_KEYSTORE_PASSWORD="verySecret1238%%/"

# Generate a secret ckey for the requesting domain
openssl genrsa -out $REQUESTER_KEY 2048

# Then extract the pubkey
openssl rsa -in $REQUESTER_KEY -pubout -out $REQUESTER_PUBKEY



# Generate a CSR using configuration from the oats.conf
# file (should later be parameterized)
openssl req -new -out $REQUEST_CSR -config $REQUESTER_CONF

##
## Creating certificate authority (CA) keys & cert.
##

# The domain of the CA
CA_KEY="${CA_DIR}/${CA_DOMAIN}.key"
CA_PUBKEY="${CA_DIR}/${CA_DOMAIN}.pubkey"
CA_CRT="${CA_DIR}/${CA_DOMAIN}.crt"
CA_CONF=ca.conf

# Generate a secret ckey for the CA
openssl genrsa -out $CA_KEY  2048

# Then extract the public part of the CA key
openssl rsa -in $CA_KEY -pubout -out $CA_PUBKEY

# Generate a self-signed certificate for the CA
openssl req -new -x509 -key $CA_KEY -out $CA_CRT -config $CA_CONF

##
##  Use CA to sign request CSR.
##


# Then sign the requesting certificate
openssl x509 -req -in $REQUEST_CSR -CA $CA_CRT -CAkey $CA_KEY -CAcreateserial -out $REQUEST_CRT


##
## Import the signed certificate and the CA public cert 
## into a java keyring.
##


keytool  -noprompt -storepass "${REQUESTER_KEYSTORE_PASSWORD}" -import -trustcacerts -alias root -file $REQUEST_CRT -keystore $REQUESTER_KEYSTORE

