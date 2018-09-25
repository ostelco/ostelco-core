#!/bin/bash

##
## Run an export, return the identifier for the export, put the
## files from the export in a directory denoted as the single
## command line parameter.
##



# Absolute path to this script, e.g. /home/user/bin/foo.sh
SCRIPT=$(readlink -f "$0")
# Absolute path this script is in, thus /home/user/bin
SCRIPTPATH=$(dirname "$SCRIPT")
echo $SCRIPTPATH


#
#  Get command line parameter, which should be an existing
#  directory in which to store the results
#

TARGET_DIR=$1
if [[ -z "$TARGET_DIR" ]] ; then
    echo "$0  Missing parameter"
    echo "usage  $0 target-dir"
    exit 1
fi

if [[ ! -d "$TARGET_DIR" ]] ; then
    echo "$0  parameter does not designate an existing directory"
    echo "usage  $0 target-dir"
    exit 1
fi

$SCRIPTPATH/check_dependencies_get_environment_coordinates.sh

#
# Run an export inside the kubernetes cluster, then parse
# the output of the script thar ran the export
#
#TEMPFILE="$(mktemp /tmp/abc-script.XXXXXX)"
TEMPFILE="tmpfile.txt"

kubectl exec -it "${EXPORTER_PODNAME}" -- /bin/bash -c /export_data.sh > "$TEMPFILE"

# Fail if the exec failed
retVal=$?
if [ $retVal -ne 0 ]; then
    echo "ERROR: Failed to export data:"
    cat $TMPFILE
    rm $TMPFILE
    exit 1
fi

#
# Parse the output of the tmpfile, getting the export ID, and
# the google filestore URLs for the output files.
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
# Clean up the tempfile
#

rm "$TEMPFILE"

#
# Finally output the ID of the export, since that's
# what will be used by users of this script to access
# the output
#

echo $EXPORT_ID
exit 0
