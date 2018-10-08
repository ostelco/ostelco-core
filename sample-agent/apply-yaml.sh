#!/bin/bash -x

set -e

###
### SEND PRE_WRITTEN YAML SCRIPT TO THE IMPORTER.
###

#
#  Get command line parameter, which should be an existing
#  file containing a yaml file.
#

YAML_SCRIPTNAME=$1
if [[ -z "$YAML_SCRIPTNAME" ]] ; then
    echo "$0  Missing script"
    echo "usage  $0 yaml-script"
    exit 1
fi

if [[ ! -f "$YAML_SCRIPTNAME" ]] ; then
    echo "$0  $YAML_SCRIPTNAME does not exist or is not a file"
    echo "usage  $0 yaml-script"
    exit 1
fi

###
### PRELIMINARIES
###

# Be able to die from inside procedures

#
# Check for dependencies being satisfied
#

DEPENDENCIES="gcloud kubectl gsutil"

for dep in $DEPENDENCIES ; do
   if [[ -z $(which $dep) ]] ; then
     echo "ERROR: Could not find dependency $dep"
   fi
done

#
#  Figure out relevant parts of the environment and check their
#  sanity.
#

PROJECT_ID=$(gcloud config get-value project)

if [[ -z "$PROJECT_ID" ]] ; then
    echo "ERROR: Unknown google project ID"
    exit 1
fi

PRIME_PODNAME=$(kubectl get pods | grep prime- | awk '{print $1}')
if [[ -z "$PRIME_PODNAME" ]] ; then
    echo "ERROR: Unknown prime podname"
    exit 1
fi


##
## Checking the assumption that localhost forwarding is actually
## working.
##

EXPECTED_FROM_GET_TO_IMPORT='{"code":405,"message":"HTTP 405 Method Not Allowed"}'
RESULT_FROM_GET_PROBE="$(curl http://127.0.0.1:8080/import/offer 2>/dev/null)"

if [[ "$EXPECTED_FROM_GET_TO_IMPORT"  != "$RESULT_FROM_GET_PROBE" ]] ; then
    echo "$0  ERROR: Did not get expected result when probing importer, bailing out"
    echo "$0: ERROR: Assuming that prime is running at $PRIME_PODNAME"
    echo "$0: ERROR: and that you have done"
    echo "$0: ERRIR: kubectl port-forward $PRIME_PODNAME 8080:8080"
    echo "$0: ERROR: Please check if this is working"
    exit 1
fi


##
## Send it to the importer
## (assuming the kubectl port forwarding is enabled)

# SEGMENT_IMPORTER_URL=http://127.0.0.1:8080/import/segments
# curl -X PUT -H "Content-type: text/vnd.yaml" --data-binary @$YAML_SCRIPTNAME $SEGMENT_IMPORTER_URL


IMPORTER_URL=http://127.0.0.1:8080/import/offer
curl -X POST -H "Content-type: text/vnd.yaml" --data-binary @$YAML_SCRIPTNAME $IMPORTER_URL
