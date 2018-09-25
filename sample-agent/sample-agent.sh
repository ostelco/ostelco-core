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


#
# Get coordinates telling the script where in this world
# it is, and what files it should relate to and so on.
#

. $SCRIPTPATH/check_dependencies_get_environment_coordinates.sh

## Check that credentials are set up

  #  tbd

## Fetch the input data form the exporter

EXPORT_ID=0802c66be1ce4e2dba22f988b3ce24f7
# EXPORT_ID=$($SCRIPTPATH/run-export.sh target-dir)

. $SCRIPTPATH/check_dependencies_get_environment_coordinates.sh

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


IMPORTFILE_YML=tmpfile.yml

cat > $IMPORTFILE_YML <<EOF
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


awk '{print "      - " $1}'  $SEGMENT_IMPORTFILE_CLEAR >> $IMPORTFILE_YML 

## Send it to the importer
echo curl --data-binary @$IMPORTFILE_YML $IMPORTER_URL
