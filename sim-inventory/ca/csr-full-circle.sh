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

# Generate a secret ckey for the requesting domain
openssl genrsa -out $REQUESTER_KEY 2048


# Generate a CSR for the requesting domain
openssl req -new -key $REQUESTER_KEY  -out $REQUEST_CSR



##
## Creating certificate authority (CA), sign the CSR, and
## publish the public part of the signing key.
##


# The domain of the CA
CA_DOMAIN=ca.org
CA_KEY="${CA_DOMAIN}.key"

# Generate a secret ckey for the CA
openssl genrsa -out $CA_KEY  2048

