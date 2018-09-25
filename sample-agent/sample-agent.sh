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

EXPORTID=0802c66be1ce4e2dba22f988b3ce24f7
# EXPORTID=$($SCRIPTPATH/run-export.sh target-dir)

if [[ -z "$EXPORTID" ]] ; then
    echo "$0  Could not determine export ID, bailing out"
    exit 1
fi

## Calculate the output, and put into a file

ALLSUBSCRIBERIDS=$(awk -F, '!/^subscriberId/{print $1'} "target-dir/$EXPORTID-sub2msisdn.csv")

echo $ALLSUBSCRIBERIDS
											      
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

for x in $ALLSUBSCRIBERIDS ; do echo "      - $x" >> $TMPFILE ;  done




## Send it to the importer
echo curl --data-binary @$TMPFILE $IMPORTER_URL
