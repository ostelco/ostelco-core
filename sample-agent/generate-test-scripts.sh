#!/bin/bash

if [[ $# -ne 2 ]] ; then
    echo "$0 ERROR:  requires exactly three parameters"
    echo "$0 ERROR:  $0 target-dir  userid1 userid2"
   exit 1
fi

TARGET_DIR=$1
USER_1=$2
USER_2=$3


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
      amount: 200
      currency: NOK
    properties:
      noOfBytes: 1_000_000_000
    presentation:
      isDefault: true
      offerLabel: Top Up
      priceLabel: 200 NOK
  createSegments:
    - id: demoSegment1
      subscribers:
        - $USER_1
	- $USER_2
EOF


cat > $TARGET_DIR/init2.yml <<EOF
createOffer:
  id: demoOffer2
  createProducts:
  - sku: 2GB_200NOK
    price:
      amount: 200
      currency: NOK
    properties:
      noOfBytes: 2_000_000_000
    presentation:
      isDefault: true
      offerLabel: Top Up
      priceLabel: 200 NOK
  createSegments:
    - id: demoSegment2
EOF

cat > $TARGET_DIR/init3.yml <<EOF
createOffer:
  id: demoOffer3
  createProducts:
  - sku: 1GB_50NOK
    price:
      amount: 50
      currency: NOK
    properties:
      noOfBytes: 1_000_000_000
    presentation:
      isDefault: true
      offerLabel: Special offer
      priceLabel: 50 NOK
  createSegments:
    - id: demoSegment3
      subscribers:
	- $USER_2

EOF

cat > $TARGET_DIR/step1.yml <<EOF
updateSegments:
  - id: s1
    subscribers:
      - $USER_1
  - id: s2
    subscribers:
      - $USER_2
  - id: s3
    subscribers:
EOF


cat > $TARGET_DIR/step2.yml <<EOF
updateSegments:
 - id: s1
    subscribers:
      - $USER_1
  - id: s2
    subscribers:
      - $USER_2
  - id: s3
    subscribers:
      - $USER_2
EOF


cat > $TARGET_DIR/reset.yml <<EOF
updateSegments:
 - id: s1
    subscribers:
      - $USER_1
      - $USER_2
  - id: s2
    subscribers:
  - id: s3
    subscribers:
EOF
