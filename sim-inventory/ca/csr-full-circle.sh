#!/bin/bash


# Run a full CSR cycle against a CA. Do it all from scratch, generating
# root certificate for the ca, generating the csr, signing the csr
# an injecting the certs in to java keyrings.



##
## Creating a requesting domain, and a CSR
##


REQUESTING_DOMAIN="example.org"
REQUESTER_KEY="${REQUESTING_DOMAIN}.key"
REQUEST_CSR="${REQUESTING_DOMAIN}.csr"
REQUEST_CRT="${REQUESTING_DOMAIN}.crgt"

# Generate a secret ckey for the requesting domain
openssl genrsa -out $REQUESTER_KEY 2048


# Generate a CSR using configuration from the oats.conf
# file (should later be parameterized)
openssl req -new -out oats.csr -config oats.conf

##
## Creating certificate authority (CA), sign the CSR, and
## publish the public part of the signing key.
##


# The domain of the CA
CA_DOMAIN=ca
CA_KEY="${CA_DOMAIN}.key"
CA_CRT="${CA_DOMAIN}.crt"
CA_SERIAL_NUMBER_FILE=${CA_DOMAIN}.srl

# Generate a secret ckey for the CA
openssl genrsa -out $CA_KEY  2048

# Generate a self-signed certificate for the CA
openssl req -new -x509 -key $CA_KEYe -out $CA_CRT

# Then sign the requesting certificate

openssl x509 -req -in ${REQUEST_CSR} -CA ${CA_CRT} -CAkey ${CA_KEY} -CAcreateserial -out $REQUEST_CRT

