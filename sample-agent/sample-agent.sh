#!/bin/bash
IMPORTER_URL=http://127.00.1:8080/importer


## Check that credentials are set up

  #  tbd

## Fetch the input data form the exporter

#  tbd

## Calculate the output, and put into a file


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
  segments:
    - test-segment

EOF



## Send it to the importer
curl --data-binary @$TMPFILE $IMPORTER_URL
