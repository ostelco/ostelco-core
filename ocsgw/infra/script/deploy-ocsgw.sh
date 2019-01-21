#!/usr/bin/env bash


getInstance () {
    echo
    printf "Which instance to update\n"
    printf " 1)\n"
    printf " 2)\n"
    printf " 3) 1 and 2\n"
    read INSTANCE
    echo
}

checkRegion () {
    allKnownRegions=("europe-west1" "asia-southeast1")

    for knownRegion in "${allKnownRegions[@]}";
    do
        if [[ ${knownRegion} == ${REGION} ]]
        then
            return 0
        fi
    done
    return 1
}

checkEnvironment () {
    allKnownEnvironments=("dev" "prod")

    for knownEnvironment in "${allKnownEnvironments[@]}";
        do
        if [[ ${knownEnvironment} == ${ENVIRONMENT} ]]
        then
            return 0
        fi
    done
    return 1
}

deploy () {
    if [[ "$INSTANCE" == 1 ]]
    then
        ZONE="${REGION}-b"
    elif [[ "$INSTANCE" == 2 ]]
    then
        ZONE="${REGION}-c"
    else
        printf "Unknown instance %s\n" ${INSTANCE}
    fi

    echo
    echo "*******************************"
    echo "Deploying OCS-gw"
    echo "Instance : ${INSTANCE}"
    echo "Environment : ${ENVIRONMENT}"
    echo "Zone : ${ZONE}"
    echo "*******************************"
    echo

    gcloud compute instances update-container --zone ${ZONE} ocsgw-${ZONE}-${ENVIRONMENT}-${INSTANCE} \
    --container-image eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS}
}

set -e

if [[ ! -f ocsgw/infra/script/deploy-ocsgw.sh ]]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

# Instance can be passed as first parameter ( 1 - 3 )
if [[ ! -z "$1" ]]; then
    INSTANCE=$1
fi

# Environment can be passed as second parameter ( dev / prod ) : default [dev]
if [[ ! -z "$2" ]]; then
    ENVIRONMENT=$2
else
    ENVIRONMENT="dev"
fi

# Region can be passed as third parameter ( europe-west1 / asia-southeast1 ) : default [europe-west1]
if [[ ! -z "$3" ]]; then
    REGION=$3
else
    REGION="europe-west1"
fi


PROJECT_ID="$(gcloud config get-value project -q)"
OCSGW_VERSION="$(./gradlew ocsgw:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG_OCS="${OCSGW_VERSION}-${SHORT_SHA}"

echo "Deployment script for OCS-gw to Google Cloud"
echo
echo "*******************************"
echo PROJECT_ID=${PROJECT_ID}
echo OCSGW_VERSION=${OCSGW_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG_OCS=${TAG_OCS}
echo "*******************************"
echo



# If instance is not passed to the script we get it from terminal
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

if ! checkEnvironment;
then
    echo "Not a valid environment : "${ENVIRONMENT}
    exit 1
fi

if ! checkRegion;
then
    echo "Not a valid region : ${REGION}"
    exit 1
fi


echo "Building OCS-gw"
./gradlew ocsgw:clean ocsgw:build
docker build -t eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS} ocsgw

echo "Uploading Docker image"
docker push eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS}


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
    printf "Unknown instance : %s\n" ${INSTANCE}
fi
