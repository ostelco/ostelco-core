#!/bin/bash
# IMPORTER_URL=http://127.00.1:8080/importer


#
# Figure out where this script is running from
#


# Absolute path to this script, e.g. /home/user/bin/foo.sh
SCRIPT=$(readlink -f "$0")
# Absolute path this script is in, thus /home/user/bin
SCRIPTPATH=$(dirname "$SCRIPT")
echo $SCRIPTPATH


## Check that credentials are set up

  #  tbd

## Fetch the input data form the exporter

EXPORT_ID=0802c66be1ce4e2dba22f988b3ce24f7
# EXPORT_ID=$($SCRIPTPATH/run-export.sh target-dir)

if [[ -z "$EXPORT_ID" ]] ; then
    echo "$0  Could not determine export ID, bailing out"
    exit 1
fi

## Calculate the output, and put into a file
## that will be a single-column CSV file containing the
## members of the updated segment

PROJECT_ID=$(gcloud config get-value project)

PURCHASES_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-purchases.csv"
SUB_2_MSISSDN_MAPPING_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-sub2msisdn.csv"
CONSUMPTION_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID.csv"
RESULT_SEGMENT_PSEUDO_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-resultsegment-pseudoanonymized.csv"
RESULT_SEGMENT_CLEAR_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-resultsegment-cleartext.csv"

SEGMENT_TMPFILE_PSEUDO="tmpsegment-pseudo.csv"
SEGMENT_TMPFILE_CLEAR="tmpsegment-clear.csv"
awk -F, '!/^subscriberId/{print $1'} "target-dir/$EXPORT_ID-sub2msisdn.csv" > $SEGMENT_TMPFILE_PSEUDO
gsutil cp $SEGMENT_TMPFILE_PSEUDO $RESULT_SEGMENT_PSEUDO_GS

## Run some script to make sure that we can get deanonumized pseudothing.
## At this point we give the actual content of that file, since we copy it back
## but eventually we may in fact send the URL instead of the actual data, letting
## the Prime read the dataset from google cloud storage instead.

## (so we should rally copy back $RESULT_SEGMENT_CLEARTEXT_GS insted of the _PSEUDO_
##  file)


gsutil cp $RESULT_SEGMENT_PSEUDO_GS $SEGMENT_TMPFILE_CLEAR


TMPFILE=tmpfile.yml

cat > $TMPFILE <<EOF
producingAgent:
  name: Simple agent
  version: 1.0

offer:
  id: test-offer
  # use existing product
  products:
    - 1GB_249NOK
  # use existing segment

segment:
    id: test-segment
    subscribers:
EOF


awk '{print "      - " $1}'  $SEGMENT_TMPFILE_CLEAR >> $TMPFILE 
# for x in $ALLSUBSCRIBERIDS ; do echo "      - $x" >> $TMPFILE ;  done


## Send it to the importer
echo curl --data-binary @$TMPFILE $IMPORTER_URL
