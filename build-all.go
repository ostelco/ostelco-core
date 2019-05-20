//usr/bin/env go run "$0" "$@"; exit "$?"

// EXPERIMENTAL:   This may or may not end up as production code.
// INTENT:         Replace the current build-all.sh script with a go program
//   	           that can be run from the command line as if it were a script
//		   The intent is straignt forward replacement, but with type
//                 safety and perhaps a bit of additional reliability.
//                 The build-all.sh script isn't used by a lot of people, so
//                 it's some distance "off broadway", and that may help the test
//                 very prestigious.  On the other hand, the script does contain
//                 sufficient complexity to be a worthy target


// #!/bin/bash
// 
// ##
// ##  Build script that will build everything and run acceptance tests.
// ##  Prior to running this script it is necesary to set th4
// ##  STRIPE_API_KEY environment variable to a key that is valid for a
// ##  Stripe test account.  See instructions in docs/TEST.md for how to
// ##  get one.
// ##
// 
// 
// 
// #
// # Cd to script directory
// #
// 
// cd $(dirname $0)
// 
// 
// DEPENDENCIES="docker-compose ./gradlew docker cmp"
// 
// #
// # Do we have the dependencies (in this case only gradle, but copy/paste
// # made the test more generic .-)
// #
// for dep in $DEPENDENCIES ; do
//  if [[ -z "$(which $dep)" ]] ; then
//    echo "Couldn't find dependency $dep"
//    exit 1
//  fi
// done
// 
// 
// #
// # Generate certificates for ESP endpoints, if needed
// # (the script will check if they are needed)
// #
// 
// if [[ -f "certs/ocs.dev.ostelco.org/nginx.crt" ]] ; then
//  if  [[ -f  "ocsgw/cert/metrics.crt" ]] ; then
//   if  [[ -n "$(cmp certs/ocs.dev.ostelco.org/nginx.crt ocsgw/cert/metrics.crt)"  ]] ; then
//       rm certs/ocs.dev.ostelco.org/nginx.crt ocsgw/cert/metrics.crt
//   fi
//  fi
// fi
// 
// 
// if [[ ! -f "certs/ocs.dev.ostelco.org/nginx.crt" ]] ; then
//     scripts/generate-selfsigned-ssl-certs.sh   ocs.dev.ostelco.org
// fi
// 
// if [[ ! -f  "ocsgw/cert/metrics.crt" ]] ; then
//     cp certs/ocs.dev.ostelco.org/nginx.crt ocsgw/cert/ocs.crt
// fi
// 
// 
// 
// if [[ -f  "certs/metrics.dev.ostelco.org/nginx.crt" ]] ; then
//  if [[  "ocsgw/cert/metrics.crt" ]] ; then
//   if  [[ -n  "$(cmp certs/metrics.dev.ostelco.org/nginx.crt ocsgw/cert/metrics.crt)" ]] ; then
//       rm "certs/metrics.dev.ostelco.org/nginx.crt" "ocsgw/cert/metrics.crt"
//   fi
//  fi
// fi
// 
// if [[ ! -f "certs/metrics.dev.ostelco.org/nginx.crt" ]] ; then
//     scripts/generate-selfsigned-ssl-certs.sh   metrics.dev.ostelco.org
// fi
// 
// if [[ ! -f "ocsgw/cert/metrics.crt" ]] ; then
//     cp certs/metrics.dev.ostelco.org/nginx.crt ocsgw/cert/metrics.crt
// fi
// 
// 
// #
// # Ensure that the GCP project is known to building  process
// #
// 
// if [[ -z "$GCP_PROJECT_ID" ]] ; then
//    echo "You need to set the GCP_PROJECT_ID otherwise we'll not be able to run acceptance tests"
//    exit 1
// fi
// 
// #
// # Setting up service account files where they are needed.
// #
// 
// 
// DIRS_THAT_NEEDS_SERVICE_ACCOUNT_CONFIGS=" \
//   acceptance-tests/config \
//   dataflow-pipelines/config \
//   ocsgw/config \
//   bq-metrics-extractor/config \
//   auth-server/config prime/config"
// 
// SERVICE_ACCOUNT_MD5="c54b903790340dd9365fa59fce3ad8e2"
// SERVICE_ACCOUNT_JSON="prime-service-account.json"
// 
// if [[ ! -f "${SERVICE_ACCOUNT_JSON}" ]] ; then
//     echo "$0 ERROR: Could not find master service-account file  ${SERVICE_ACCOUNT_JSON}, aborting."
//     exit 1    
// fi
// 
// if [[ $(md5 -q "${SERVICE_ACCOUNT_JSON}") != $SERVICE_ACCOUNT_MD5 ]] ; then
//     echo "$0 ERROR: MD5 checksum of service account file ${SERVICE_ACCOUNT_JSON} does not match expectation, aborting."
//     exit 1
// fi
// 
// for DIR in $DIRS_THAT_NEEDS_SERVICE_ACCOUNT_CONFIGS ; do
//     echo "Checking $DIR"
//     FILE="$DIR/${SERVICE_ACCOUNT_JSON}"
//     if [[ ! -f $FILE ]] ; then
// 	if [[ $(md5 -q "${SERVICE_ACCOUNT_JSON}") = $SERVICE_ACCOUNT_MD5 ]] ; then
// 	    echo "$0 INFO: Couldn't find service account file '$FILE' so copying one from root directory"
// 	    cp "${SERVICE_ACCOUNT_JSON}" "$FILE"
// 	else
// 	    echo "$0 ERROR: Could not find service account file $FILE, aborting."
// 	    exit 1
// 	fi
//     fi
//     if [[ $(md5 -q $FILE) != $SERVICE_ACCOUNT_MD5 ]] ; then
// 	if [[ $(md5 -q "${SERVICE_ACCOUNT_JSON}") = $SERVICE_ACCOUNT_MD5 ]] ; then
// 	    echo "$0 INFO: Service account  '$FILE'  has wrong MD5 checksum, but the one in the root directory has the right one, so we're copying that."
// 	    cp "${SERVICE_ACCOUNT_JSON}" "$FILE"
// 	else
// 	    echo "$0 ERROR: MD5 checksum of service account file  '$FILE' is not $SERVICE_ACCOUNT_MD5, aborting."	
// 	    exit 1
// 	fi
//     fi
// done
// 
// 
// 
// #
// # Do we have the necessary environment variables set
// # to run payment tests?
// #
// 
// if [[ -z "$STRIPE_API_KEY" ]] ; then
//     echo "$0 ERROR: STRIPE_API_KEY is not set.  Se instructions in docs/TEST.md for how to get one."
//     exit 1
// fi
// 
// if [[ -z "$STRIPE_ENDPOINT_SECRET" ]] ; then
//     export  STRIPE_ENDPOINT_SECRET=thisIsARandomString
//     echo "$0 INFO: Couldn't find variable STRIPE_ENDPOINT_SECRET, setting it to dummy value '$STRIPE_ENDPOINT_SECRET'"
// fi
// 
// 
// if [[ -z "$( docker version | grep Version:)" ]] ; then
//     echo "$0 INFO: Docker not running, please start it before trying again'"
//     exit 1
// fi
// 
// 
// #
// # Then start running the build
// #
// 
// ./gradlew build
// 
// #
// # If that didn't go too well, then bail out.
// #
// 
// if [[ $? -ne 0 ]] ; then echo
//    echo "Compilation failed, aborting. Not running acceptance tests."
//    exit 1
// fi
// 
// #
// # .... but it did go well, so we'll proceed to acceptance test
// #
// 
// echo "$0 INFO: Building/unit tests went well, Proceeding to acceptance tests."
// 
// docker-compose down
// docker-compose up --build --abort-on-container-exit
