#!/bin/bash

##
## Exploratory code to run an export using bigquery.
##


#
#  Get command line parameter, which should be an existing
#  directory in which to store the results
#

TARGET_DIR=$1
if [[ ! -z "$TARGET_DIR" ]] ; then
    echo "$0  Missing parameter"
    echo "usage  $0 target-dir"
    exit 1
fi


if [[ ! -d "$TARGET_DIR" ]] ; then
    echo "$0  parameter does not designate an existing directory"
    echo "usage  $0 target-dir"
    exit 1
fi

#
# Check for dependencies
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


EXPORTER_PODNAME=$(kubectl get pods | grep exporter- | awk '{print $1}')
if [[ -z "$EXPORTER_PODNAME" ]] ; then
    echo "ERROR: Unknown exporter podname"
    exit 1
fi


#
# Run an export inside the kubernetes cluster, then parse
# the output of the script thar ran the export
#
#TEMPFILE="$(mktemp /tmp/abc-script.XXXXXX)"
TEMPFILE="tmpfile.txt"

# kubectl exec -it "${EXPORTER_PODNAME}" -- /bin/bash -c /export_data.sh > "$TEMPFILE"

# # Fail if the exec failed
# retVal=$?
# if [ $retVal -ne 0 ]; then
#     echo "ERROR: Failed to export data:"
#     cat $TMPFILE
#     rm $TMPFILE
#     exit 1
# fi

#
# Parse the output of the tmpfile
#


EXPORT_ID=$(grep "Starting export job for" $TEMPFILE | awk '{print $5}' |  sed 's/\r$//' )


PURCHASES_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-purchases.csv"
SUB_2_MSISSDN_MAPPING_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-sub2msisdn.csv"
CONSUMPTION_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID.csv"

#
# Then copy the CSV files to local storage (current directory)
#

gsutil cp $PURCHASES_GS $TARGET_DIR
gsutil cp $SUB_2_MSISSDN_MAPPING_GS $TARGET_DIR
gsutil cp $CONSUMPTION_GS $TARGET_DIR

#
# Finally output the ID of the export, since that's
# what will be used by users of this script to access
# the output
#
echo $EXPORT_ID
