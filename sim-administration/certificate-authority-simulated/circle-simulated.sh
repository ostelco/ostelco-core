#!/bin/bash



###
### Run a full CSR cycle against a CA. Do it all from scatch, generating
### root certificate for the ca, generating the csr, signing the csr
### an injecting the certs in to java keyrings.
###


##
## Key length. Should be 2048, but may be smaller during testing.
##
KEY_LENGTH=2048


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
## Setting up directories for the various
## roles, deleting old files. Each of the actors
## (smdp+ and sim manager) are represented by
## a directory.  That directory is then populated with
## misc keys, certificats and csrs.
##

ARTEFACT_ROOT=crypto-artefacts
if [[ -x "$ARTEFACT_ROOT" ]] ; then 
   rm -f $ARTEFACT_ROOT
fi

ACTORS="sim-mgr sm-dp-plus"
for  x in $ACTORS ; do 
  mkdir -p "$ARTEFACT_ROOT/$x"
done


##
## Generate filenames from actorname and role
##

function generate_filename {
    local actor=$1
    local role=$2
    local suffix=$3
    
    if [[ -z "$actor" ]] ; then
	(>&2 echo "No actor given to generate_filename")
	exit 1
    fi
    if [[ -z "$role" ]] ; then
	(>&2 echo "No role given to generate_filename")
	exit 1
    fi

    if [[ -z "$suffix" ]] ; then
	(>&2 echo "No suffix given to generate_filename")
	exit 1
    fi

    echo "${ARTEFACT_ROOT}/${actor}/${role}.${suffix}"
}

function csr_filename {
    local actor=$1
    local role=$2
    echo $(generate_filename $actor $role "csr" )
}


function crt_filename {
    local actor=$1
    local role=$2
    echo $(generate_filename $actor $role "crt" )
}


function key_filename {
    local actor=$1
    local role=$2
    echo $(generate_filename $actor $role "key" )
}


function crt_config_filename {
    local actor=$1
    local role=$2
    echo $(generate_filename $actor $role "csr_config" )
}



##
##  Generating keys
##

function generate_key {
   local output_file=$1
   local dirname=$(dirname $output_file)
   mkdir -p $dirname
   openssl genrsa -out $output_file $KEY_LENGTH
}




#
# Both of the actors have  three keys.  One certificate authority
# key (_ca), one server key (_sk), and one client key (_ck)
#

# for actor in  $ACTORS ; do
#     for role in "ca" "sk" "ck" ; do
#          generate_key $(key_filename $actor $role)
#     done
# done


##
## Generating Certificate Signing Requests (CSRs) and signing them.
##


function generate_cert_config {
    local cert_config=$1
    local keyfile=$2
    local distinguished_name=$3
    local country=$4
    local state=$5
    local location=$6
    local organization=$7
    local common_name=$8

    
    echo "local cert_config=$1"
    echo "local keyfile=$2"
    echo "local distinguished_name=$3"
    echo "local country=$4"
    echo "local state=$5"
    echo "local location=$6"
    echo "local organization=$7"
    echo "local common_name=$8"


    cat > $cert_config <<EOF
# The main section is named req because the command we are using is req
# (openssl req ...)
[ req ]
# This specifies the default key size in bits. If not specified then 512 is
# used. It is used if the -new option is used. It can be overridden by using
# the -newkey option. 
default_bits = $KEY_LENGTH

# This is the default filename to write a private key to. If not specified the
# key is written to standard output. This can be overridden by the -keyout
# option.
default_keyfile = $keyfile

# If this is set to no then if a private key is generated it is not encrypted.
# This is equivalent to the -nodes command line option. For compatibility
# encrypt_rsa_key is an equivalent option. 
encrypt_key = no

# This option specifies the digest algorithm to use. Possible values include
# md5 sha1 mdc2. If not present then MD5 is used. This option can be overridden
# on the command line.
default_md = sha1

# if set to the value no this disables prompting of certificate fields and just
# takes values from the config file directly. It also changes the expected
# format of the distinguished_name and attributes sections.
prompt = no

# if set to the value yes then field values to be interpreted as UTF8 strings,
# by default they are interpreted as ASCII. This means that the field values,
# whether prompted from a terminal or obtained from a configuration file, must
# be valid UTF8 strings.
utf8 = yes

# This specifies the section containing the distinguished name fields to
# prompt for when generating a certificate or certificate request.
distinguished_name = $distinguished_name

# this specifies the configuration file section containing a list of extensions
# to add to the certificate request. It can be overridden by the -reqexts
# command line switch. See the x509v3_config(5) manual page for details of the
# extension section format.
req_extensions = my_extensions

[ $distinguished_name ]
C = $country
ST = $state
L = $location
O  = $organization
CN = $common_name

[ my_extensions ]
basicConstraints=CA:FALSE
subjectAltName=@my_subject_alt_names
subjectKeyIdentifier = hash

[ my_subject_alt_names ]
DNS.1 = $common_name
# Multiple domains could be listed here, but we're not doing
# doing that now.
EOF
}

# 
# The actors then use their own CA  keys to sign their own CA
# and SK certs.
#


function generate_csr {
    local actor=$1
    local role=$2
    local distinguished_name=$3
    local country=$4
    local state=$5
    local location=$6
    local organization=$7
    local common_name=$8

    local keyfile=$(key_filename $actor $role)
    local cert_config=$(crt_config_filename $actor $role)
    local csr_file=$(csr_filename $actor $role)
    
    generate_cert_config "$cert_config" "$keyfile" "$distinguished_name" "$country" "$state" "$location" "$organization" "$common_name"
    openssl req -new -out "$csr_file" -config "$cert_config"
}


function self_signed_cert {
    local actor=$1
    local role=$2
    local distinguished_name=$3
    local country=$4
    local state=$5
    local location=$6
    local organization=$7
    local common_name=$8

    local keyfile=$(key_filename $actor $role)
    local cert_config=$(crt_config_filename $actor $role)
    local crt_file=$(crt_filename $actor $role)
    
    generate_cert_config "$cert_config" "$keyfile" "$distinguished_name" "$country" "$state" "$location" "$organization" "$common_name"
#    openssl req -new -out "$crt_file" -config "$cert_config"


# openssl req -config example-com.conf -new -x509 -sha256 -newkey rsa:2048 -nodes \
#    -keyout example-com.key.pem -days 365 -out example-com.cert.pem

    openssl req \
        -config $cert_config \
	-new -x509 -sha256  \
        -keyout $keyfile \
        -out $crt_file -days 365
}


self_signed_cert "sim-mgr"    "ca" "not-really-ostelco.org" "NO" "Oslo" "Oslo" "Not really ostelco" "*.not-really-ostelco.org" 
self_signed_cert "sm-dp-plus" "ca" "not-really-smdp.org"    "NO" "Oslo" "Oslo" "Not really SMDP org" "*.not-really-ostelco.org" 




##
## Now generate all the CSRs for both actors.
##

generate_csr "sim-mgr" "ck" "not-really-ostelco.org" "NO" "Oslo" "Oslo" "Not really ostelco" "*.not-really-ostelco.org" 
generate_csr "sim-mgr" "sk" "not-really-ostelco.org" "NO" "Oslo" "Oslo" "Not really ostelco" "*.not-really-ostelco.org" 

generate_csr "sm-dp-plus" "ck" "not-really-smdp.org" "NO" "Oslo" "Oslo" "Not really SMDP org" "*.not-really-ostelco.org" 
generate_csr "sm-dp-plus" "sk" "not-really-smdp.org" "NO" "Oslo" "Oslo" "Not really SMDP org" "*.not-really-ostelco.org" 



##
## Then sign the various CSRs
##

function sign_csr {
    local issuer_actor=$1
    local issuer_role=$2
    local signer_actor=$3
    local signer_role=$4

    local csr_file=$(csr_filename $issuer_actor $issuer_role)
    local crt_file=$(crt_filename $issuer_actor $issuer_role)
    local ca_crt=$(crt_filename $signer_actor $signer_role)
    local ca_key=$(key_filename $signer_actor $signer_role)

    # TODO: CHeck that all the input files exist
    echo openssl x509 -req -in $csr_file -CA $ca_crt -CAkey $ca_key -CAcreateserial -out $crt_file
    openssl x509 -req -in $csr_file -CA $ca_crt -CAkey $ca_key -CAcreateserial -out $crt_file
    # TODO: Check that all the output files exist and that the exit code is zero
}




echo "Sign server certificates using own CA"
sign_csr "sim-mgr"      "sk" "sim-mgr"      "ca"
sign_csr "sm-dp-plus"   "sk" "sm-dp-plus"   "ca"


echo "Countersign client certificates"
sign_csr "sim-mgr"      "ck" "sim-mgr"      "ca" 
sign_csr "sm-dp-plus"   "ck" "sm-dp-plus"   "ca"


