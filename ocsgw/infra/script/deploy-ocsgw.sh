#!/usr/bin/env bash

set -e

if [ ! -f ocsgw/infra/script/deploy-ocsgw.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

# Instance can be passed as first parameter
if [ ! -z "$1" ]; then
    INSTANCE=$1
fi

# Environment can be passed as second parameter
if [ ! -z "$2" ]; then
    ENV=$2
else
    ENV="dev"
fi

PROJECT_ID="$(gcloud config get-value project -q)"
OCSGW_VERSION="$(./gradlew ocsgw:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG_OCS="${OCSGW_VERSION}-${SHORT_SHA}"

echo PROJECT_ID=${PROJECT_ID}
echo OCSGW_VERSION=${OCSGW_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG_OCS=${TAG_OCS}
echo ENV=${ENV}

./gradlew ocsgw:clean ocsgw:build
docker build -t eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS} ocsgw
docker push eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS}

getInstance () {
    echo ""
    printf "Which instance to update\n"
    printf " 1)\n"
    printf " 2)\n"
    printf " 3) 1 and 2\n"
    read INSTANCE
}

deploy () {
    if [[ "$INSTANCE" == 1 ]]
    then
        ZONE="europe-west1-b"
    elif [[ "$INSTANCE" == 2 ]]
    then
        ZONE="europe-west1-c"
    else
        printf "Unknown instance %s\n" $INSTANCE
    fi

    echo "Deploying ocsgw instance ${INSTANCE} to GKE ${ENV}"
    gcloud compute instances update-container --zone ${ZONE} ocsgw-${ENV}-${INSTANCE} \
    --container-image eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS}
}


if [[ -z "$INSTANCE" ]]
then
    while true; do
      getInstance
      if [[ "$INSTANCE" == 1 ]] || [[ "$INSTANCE" == 2 ]]  || [[ "$INSTANCE" == 3 ]]
      then
        break
      fi
    done
fi


if [[ "$INSTANCE" == 1 ]] || [[ "$INSTANCE" == 2 ]]
then
    deploy
elif [[ "$INSTANCE" == 3 ]]
then
    INSTANCE=1
    deploy
    INSTANCE=2
    deploy
else
    printf "Unknown instance %s\n" $INSTANCE
fi

