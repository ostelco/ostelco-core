#!/usr/bin/env bash

# Script to deploy the OCS-gw to GCP Compute engine container

##### Constants

GCP_PROJECT_ID="$(gcloud config get-value project -q)"
OCSGW_VERSION="$(./gradlew ocsgw:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG_OCS="${OCSGW_VERSION}-${SHORT_SHA}"

##### Functions

checkRegion () {
    allKnownRegions=("europe-west1" "europe-west4" "asia-southeast1")

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

    echo
    echo "*******************************"
    echo "Deploying OCS-gw"
    echo "Instance : ocsgw-${ENVIRONMENT}-${REGION}-${ZONE}-${INSTANCE}"
    echo "*******************************"
    echo

    gcloud compute instances update-container --zone ${REGION}-${ZONE} ocsgw-${ENVIRONMENT}-${REGION}-${ZONE}-${INSTANCE} \
    --container-image eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS}
}

printInfo() {

echo
echo "Deployment script for OCS-gw to Google Cloud"
echo
echo "*******************************"
echo GCP_PROJECT_ID=${GCP_PROJECT_ID}
echo OCSGW_VERSION=${OCSGW_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG_OCS=${TAG_OCS}
echo "*******************************"
echo

}

printEnvironment() {

echo
echo "*******************************"
echo ENVIRONMENT=${ENVIRONMENT}
echo REGION=${REGION}
echo ZONE=${ZONE}
echo "*******************************"
echo

}

##### Main

set -e

if [[ ! -f ocsgw/infra/script/deploy-ocsgw.sh ]]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

printInfo

# Environment can be passed as first parameter ( dev / prod ) : default [dev]
if [[ ! -z "$1" ]]; then
    ENVIRONMENT=$1
else
    ENVIRONMENT="dev"
fi

# Region can be passed as second parameter ( europe-west1 /europe-west4 / asia-southeast1 ) : default [europe-west4]
if [[ ! -z "$2" ]]; then
    REGION=$2
else
    REGION="europe-west4"
fi

# Zone can be passed as third parameter (a/b/c/d/e/f) : default [b]
if [[ ! -z "$3" ]]; then
    ZONE=$3
else
    ZONE="b"
fi

# Instance number can be passed as forth parameter (1...n) : default [1]
if [[ ! -z "$4" ]]; then
    INSTANCE=$4
else
    INSTANCE=1
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

printEnvironment


echo "Building OCS-gw"
./gradlew ocsgw:clean ocsgw:build
docker build -t eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS} ocsgw

echo "Uploading Docker image"
docker push eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS}


deploy
