#!/bin/bash

#
# Exploratory code to run an export using bigquery.
#


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
TEMPFILE=tmpfile.txt
# mpfile=$(mktemp /tmp/abc-script.XXXXXX)

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

echo "export id = $EXPORT_ID"

PURCHASES_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-purchases.csv"
SUB_2_MSISSDN_MAPPING_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-sub2msisdn.csv"
CONSUMPTION_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID.csv"

#
# Then copy the CSV files to local storage (current directory)
#


gsutil cp $PURCHASES_GS .
gsutil cp $SUB_2_MSISSDN_MAPPING_GS .
gsutil cp $CONSUMPTION_GS .

