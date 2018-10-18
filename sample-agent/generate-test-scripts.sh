#!/bin/bash

##
##
## This sets up a set of demo scripts that can be used
## in conjunction with the script "apply-yaml.sh" to
## apply changes to the product/segment/offer configuration
## in Prime.   It is intended as a vehicle for testing
## the interaction in basic ways, and will most likely
## be removed or replaced when we are more confident that
## the design of the import mechanism is fit for purpose.
## In the mean time, we'll use this mechanism as it provides
## great flexibility and transparency in to what is actually
## applied.
##

if [[ $# -ne 3 ]] ; then
    echo "$0 ERROR:  requires exactly three parameters"
    echo "$0 ERROR:  $0 target-dir  userid1 userid2"
   exit 1
fi

TARGET_DIR=$1
USER_1=$2
USER_2=$3

SEGMENT_1="demoSegment1"
SEGMENT_2="demoSegment2"
SEGMENT_3="demoSegment3"


if [[ ! -d "$TARGET_DIR" ]] ; then
    echo "$0 ERROR:  Target directory '$TARGET_DIR' does not exist or is not a directory"
    exit 1
fi


cat > $TARGET_DIR/init1.yml <<EOF
createOffer:
  id: demoOffer1
  createProducts:
  - sku: 1GB_200NOK
    price:
      amount: 20000
      currency: NOK
    properties:
      noOfBytes: 1_000_000_000
    presentation:
      isDefault: true
      offerLabel: Top Up
      priceLabel: 200 NOK
      productLabel: +1GB
  createSegments:
    - id: $SEGMENT_1
EOF


cat > $TARGET_DIR/init2.yml <<EOF
createOffer:
  id: demoOffer2
  createProducts:
  - sku: 2GB_200NOK
    price:
      amount: 20000
      currency: NOK
    properties:
      noOfBytes: 2_000_000_000
    presentation:
      isDefault: true
      offerLabel: Top Up
      priceLabel: 200 NOK
      productLabel: +2GB
  createSegments:
    - id: $SEGMENT_2
EOF

cat > $TARGET_DIR/init3.yml <<EOF
createOffer:
  id: demoOffer3
  createProducts:
  - sku: 1GB_50NOK
    price:
      amount: 5000
      currency: NOK
    properties:
      noOfBytes: 1_000_000_000
    presentation:
      offerDescription: Need more data? Get 1GB for the special price of 50 NOK
      isDefault: true
      offerLabel: Special offer
      priceLabel: 50 NOK
      productLabel: +1GB
  createSegments:
    - id: $SEGMENT_3
EOF

cat > $TARGET_DIR/step1.yml <<EOF
updateSegments:
  - id: $SEGMENT_1
    subscribers:
      - $USER_2
  - id: $SEGMENT_2
    subscribers:
      - $USER_1
  - id: $SEGMENT_3
EOF


cat > $TARGET_DIR/step2.yml <<EOF
updateSegments:
  - id: $SEGMENT_1
    subscribers:
      - $USER_2
  - id: $SEGMENT_2
    subscribers:
      - $USER_1
  - id: $SEGMENT_3
    subscribers:
      - $USER_1
EOF


cat > $TARGET_DIR/reset.yml <<EOF
updateSegments:
  - id: $SEGMENT_1
    subscribers:
      - $USER_1
      - $USER_2
  - id: $SEGMENT_2
  - id: $SEGMENT_3
EOF

echo "$0: INFO Successfully created demo scripts in directyory $TARGET_DIR"
echo "$0: INFO To initialize run initialization scripts:"
echo "$0: INFO"
echo "$0: INFO    ./apply_yaml.sh  offer $TARGET_DIR/init1.yml"
echo "$0: INFO    ./apply_yaml.sh  offer $TARGET_DIR/init2.yml"
echo "$0: INFO    ./apply_yaml.sh  offer $TARGET_DIR/init3.yml"
echo "$0: INFO"
echo "$0: INFO During the test, run the test steps:"
echo "$0: INFO"
echo "$0: INFO    ./apply_yaml.sh  segments $TARGET_DIR/step1.yml"
echo "$0: INFO    ./apply_yaml.sh  segments $TARGET_DIR/step2.yml"
echo "$0: INFO"
echo "$0: INFO To reset to initial state (e.g. before running a demo/test again):"
echo "$0: INFO"
echo "$0: INFO    ./apply_yaml.sh  segments $TARGET_DIR/reset.yml"
