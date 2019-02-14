#!/bin/bash



###
### Run a full CSR cycle against a CA. Do it all from scatch, generating
### root certificate for the ca, generating the csr, signing the csr
### an injecting the certs in to java keyrings.
###


## To reset the crypto artefacts repository, start script by setting RESET_CRYPTO_ARTEFACTS variable
## to a non-null value, e.g. "RESET_CRYPTO_ARTEFACTS=yes ./circle-simulated.sh"


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
    exit 1
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


# Reset if requested
if [[ -n "$RESET_CRYPTO_ARTEFACTS" ]] ; then 
    if [[ -f "$ARTEFACT_ROOT" ]] ; then 
	rm -rf $ARTEFACT_ROOT
    fi
fi
    
SIM_MANAGER="sim-mgr"
SMDPPLUS="sm-dp-plus"

ACTORS="$SIM_MANAGER $SMDPPLUS"

for  x in $ACTORS ; do 
    if [[ ! -d "$ARTEFACT_ROOT/$x" ]] ; then 
        mkdir -p "$ARTEFACT_ROOT/$x"
    fi
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



function keystore_filename {
    local actor=$1
    local role=$2
    local keystore_type=$3
    echo $(generate_filename $actor "${role}_${keystore_type}" "jks" )
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


function combined_crt_filename {
    local actor=$1
    local role=$2
    echo $(generate_filename $actor "combined_$role" "crt" )
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
## Generating Certificate Signing Requests (CSRs) and signing them.
##


function generate_cert_config {
    local cert_config=$1
    local key
file=$2
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

## 
## The actors then use their own CA  keys to sign their own CA
## and SK certs.
##


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

    if [[ -f "$crt_file"  ]] ; then 
	echo "Self signed certificate file '$crt_file' already exist, not creating again"
    else 
	generate_cert_config "$cert_config" "$keyfile" "$distinguished_name" "$country" "$state" "$location" "$organization" "$common_name"
	openssl req \
            -days 730 \
	    -config $cert_config \
	    -new -x509 -sha256  \
	    -keyout $keyfile \
	    -out $crt_file
    fi
}


self_signed_cert "$SIM_MANAGER"  "ca" "not-really-ostelco.org" "NO" "Oslo" "Oslo" "Not really ostelco" "*.not-really-ostelco.org" 
self_signed_cert $SMDPPLUS       "ca" "not-really-smdp.org"    "NO" "Oslo" "Oslo" "Not really SMDP org" "*.not-really-ostelco.org" 

##
## Now generate all the CSRs for both actors.
##


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
    
    if [[ -f "$csr_file"  ]] ; then 
	echo "Certificate signing request (CSR) file  file '$csr_file' already exist, not creating again"
    else 
	generate_cert_config "$cert_config" "$keyfile" "$distinguished_name" "$country" "$state" "$location" "$organization" "$common_name"
	openssl req -days 730 -new -out "$csr_file" -config "$cert_config"
    fi
}


generate_csr "$SIM_MANAGER" "ck" "not-really-ostelco.org" "NO" "Oslo" "Oslo" "Not really ostelco" "*.not-really-ostelco.org" 
generate_csr "$SIM_MANAGER" "sk" "not-really-ostelco.org" "NO" "Oslo" "Oslo" "Not really ostelco" "*.not-really-ostelco.org" 
generate_csr $SMDPPLUS "ck" "not-really-smdp.org" "NO" "Oslo" "Oslo" "Not really SMDP org" "*.not-really-ostelco.org" 
generate_csr $SMDPPLUS "sk" "not-really-smdp.org" "NO" "Oslo" "Oslo" "Not really SMDP org" "*.not-really-ostelco.org" 


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

    if [[ ! -f "$csr_file" ]] ; then 
	(>&2 echo "$0: Error. Could not find csr  $csr_file")
	exit 1
    fi

    if [[ ! -f "$ca_crt" ]] ; then 
	(>&2 echo "$0: Error. Could not find CA crt  $csr_file")
	exit 1
    fi

    if [[ ! -f "$crt_file" ]] ; then  
	openssl x509 -req -in $csr_file -CA $ca_crt -CAkey $ca_key -CAcreateserial -out $crt_file
    else
	echo "Signed certificate already exists in file $crt_file, not creating again"
    fi
    if [[ ! -f "$crt_file" ]] ; then
	echo "Could not create signed certificate file $crt_file"
    fi
}


echo "Sign server certificates using own CA"
sign_csr "$SIM_MANAGER"      "sk" "$SIM_MANAGER"      "ca"
sign_csr $SMDPPLUS           "sk"  $SMDPPLUS          "ca"


echo "Countersign client certificates"
sign_csr "$SIM_MANAGER"  "ck" "$SIM_MANAGER"      "ca" 
sign_csr $SMDPPLUS       "ck"  $SMDPPLUS          "ca"



##
##  Generate keytool files based on the keys stored
##  in the crypto storage.
##

#
#  Generate and/or populate a keystore file with 
#  the certificates given as fourth argument and onwards.
#  First argument is the actor managing the keystore, the
#  second is the role for which it is used (e.g. "client keys")
#  third argument is either "trust" or "keys" depending on if this
#  keystore is used in a "trustkeys" or "secretkeys" role.
#
#  Usage:
# 
#     populate_keystore "sim-manager" "ck" "trust" foobar.crt  bartz.crt ..
#
#
function populate_keystore {
    local actor=$1 ; shift
    local role=$1  ; shift
    local keystore_type=$1; shift
    local certs="$*"
    
    local keystore_filename=$(keystore_filename $actor $role $keystore_type)
    local common_password="superSecreet"


# keytool -import -trustcacerts -alias mydomain -file mydomain.crt -keystore keystore.jks

    echo "Creating keystore $keystore_filename"
    for cert in $certs ; do 
	echo "     Importing cert $cert"
	keytool  \
             -noprompt -storepass "${common_password}"  \
             -importcert -trustcacerts -alias "$(basename $(dirname $cert))_$(basename $cert)" \
             -file $cert -keystore $keystore_filename
    done
}



#
# Generate all the eight kinds of combinations that can be made.
# One trust/keys pair for each of the four {sim_manager, smdpplus} x {ck, sk}
# combinations.
#



# populate_keystore $SIM_MANAGER "ck" "trust"  $(crt_filename $SMDPPLUS    "ca")
# populate_keystore $SIM_MANAGER "ck" "keys"   $(crt_filename $SIM_MANAGER "sk")
# populate_keystore $SIM_MANAGER "sk" "trust"  $(crt_filename $SMDPPLUS    "ca")
# populate_keystore $SIM_MANAGER "sk" "keys"   $(crt_filename $SIM_MANAGER "sk")


# populate_keystore $SMDPPLUS "ck" "trust"     $(crt_filename $SIM_MANAGER  "ca")
# populate_keystore $SMDPPLUS "ck" "keys"      $(crt_filename $SMDPPLUS     "sk")


# Maybe this is the clue I need?
#   https://docs.oracle.com/cd/E19509-01/820-3503/ggfhb/index.html
#   https://stackoverflow.com/questions/19552380/no-certificate-matches-private-key-while-generating-p12-file
#   https://coderwall.com/p/3t4xka/import-private-key-and-certificate-into-java-keystore
#   https://www.ibm.com/support/knowledgecenter/en/SSWHYP_4.0.0/com.ibm.apimgmt.cmc.doc/task_apionprem_generate_pkcs_certificate.html
#   https://www.pixelstech.net/article/1450354633-Using-keytool-to-create-certificate-chain

#   https://stackoverflow.com/questions/41293778/jetty-javax-net-ssl-sslhandshakeexception-no-cipher-suites-in-common
#     - claims that both key and cert must have same alias in keystore

# openssl pkcs12 -export -in $(crt_filename $SMDPPLUS "sk")  -inkey $(key_filename $SMDPPLUS "sk")  -chain -CAfile $(crt_filename $SMDPPLUS "ca")  -name "not-really-ostelco.org" -out  mykeystore.pkcs12

# cat $(crt_filename $SMDPPLUS "sk") $(crt_filename $SMDPPLUS "ca") > caChain.pem
# openssl pkcs12 -inkey $(key_filename $SMDPPLUS "sk") -in $(crt_filename $SMDPPLUS "sk")  -export -out mykeystore.pkcs12 -CAfile caChain.pem -chain

# keytool  -noprompt -storepass superSecreet  -v -importkeystore -srckeystore mykeystore.pkcs12  -srcstoretype PKCS12 -destkeystore $(keystore_filename $SMDPPLUS "sk" "keys" )  -deststoretype JKS

populate_keystore $SMDPPLUS "sk" "trust"     $(crt_filename $SIM_MANAGER  "ca")  $(crt_filename $SMDPPLUS  "ca")
#  populate_keystore $SMDPPLUS "sk" "keys"      $(crt_filename $SMDPPLUS     "ca")  $(crt_filename $SMDPPLUS  "sk")
# populate_keystore $SMDPPLUS "sk" "keys"   mykeystore.pkcs12 
                                          


# keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass superSecreet -validity 360 -keysize 2048

# openssl req -new -x509 -newkey rsa:2048 -sha256 -key jetty.key -out jetty.crt
# openssl pkcs12 -inkey jetty.key -in jetty.crt -export -out idp-browser.p12
openssl pkcs12 -inkey $(key_filename $SMDPPLUS "sk") -in $(crt_filename $SMDPPLUS "sk")  -export -out mykeystore2.pkcs12