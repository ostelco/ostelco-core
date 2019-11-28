#!/bin/bash

##
##  Utility script for running regression tests on a developer workstation.
##  It is an automation of the procedure described in the docs/TEST.md file.
##  It is a very good idea to run this script frequently, and at least before
##  committing something in a PR.
##


DEPS="gradle openssl"
for dep in $DEPS ; do
    if [[ -z "$(which $dep)" ]] ; then
	echo "$0  ERROR:  Missing dependency $dep"
	exit 1
    fi
done

#
# Check for the presence of the GCP service account file - (PSA) prime-service-account.json
#

PSA_DIRS=$(grep -i prime-service-account.json $(find . -name '.gitignore') | awk -F: '{print $1}' | sort | uniq | sed 's/.gitignore//g')

for PSA_DIR in $PSA_DIRS ; do
    PSA_FILE="${PSA_DIR}prime-service-account.json"
    if [[ ! -f "$PSA_FILE" ]] ; then
	echo "$0 ERROR:   Expected to find $PSA_FILE, but didn't.  Cannot run regression tests."
	exit 1
    fi
done


if [[ -z "$STRIPE_API_KEY" ]] ; then
   echo "$0 ERROR:  STRIPE_API_KEY is not set"
   exit 0
fi


#
# If necessary, create self-signed certificate for nginx with domain 
#  `ocs.dev.ostelco.org` and place them at following location:
#    * In `certs/ocs.dev.ostelco.org`, keep `nginx.key` and `nginx.cert`.
#    * In `ocsgw/cert`, keep `ocs.cert`.
#

if [[ ! -f "ocsgw/cert/ocs.crt" ]] ; then 
   (cd certs/ocs.dev.ostelco.org ; 
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./nginx.key -out ./nginx.crt -subj '/CN=ocs.dev.ostelco.org' ; 
     cp nginx.crt ../../ocsgw/cert/ocs.crt)
fi

#
# Then build (or not, we're using gradle) the whole system
#

gradle build

#
# Finally cd into the acceptance test directory, build everything
# from scratch and run the tests
#

cd acceptance-tests
gradlew clean build  
docker-compose up --build --abort-on-container-exit



