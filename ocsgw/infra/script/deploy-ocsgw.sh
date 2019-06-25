#!/usr/bin/env bash

# Script to deploy the OCS-gw to GCP Compute engine container

##### Constants

GCP_PROJECT_ID="$(gcloud config get-value project -q)"
OCSGW_VERSION="$(./gradlew ocsgw:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG_OCS="${OCSGW_VERSION}-${SHORT_SHA}"

##### Functions

checkRegion () {
    allKnownRegions=("europe-west1" "europe-west3" "europe-west4" "asia-southeast1")

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

checkIfImageExist() {
    if [[ -z "$(docker images eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS} -q)" ]]; then
        echo "eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS} not found in registry"
        echo "creating new image"
        return 1
    else
        echo "eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS} already in registry"
        echo "using existing image"
        return 0
    fi
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

printUsage() {
    echo ""
    echo "The script require following parameters"
    echo "<environment> <region> <zone> <instance>"
    echo ""
    echo ""
    echo "example:"
    echo "./ocsgw/infra/script/deploy-ocsgw.sh prod asia-southeast1 c 1"
}

##### Main

set -e

if [[ ! -f ocsgw/infra/script/deploy-ocsgw.sh ]]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

printInfo

# Environment can be passed as first parameter ( dev / prod )
if [[ ! -z "$1" ]]; then
    ENVIRONMENT=$1
else
    echo "no environment set"
    printUsage
    exit 1
fi

# Region can be passed as second parameter ( europe-west1 /europe-west3 / europe-west4 / asia-southeast1 )
if [[ ! -z "$2" ]]; then
    REGION=$2
else
    echo "no region set"
    printUsage
    exit 1
fi

# Zone can be passed as third parameter (a/b/c/d/e/f)
if [[ ! -z "$3" ]]; then
    ZONE=$3
else
    echo "no zone set"
    printUsage
    exit 1
fi

# Instance number can be passed as forth parameter (1...n)
if [[ ! -z "$4" ]]; then
    INSTANCE=$4
else
    echo "no instance set"
    printUsage
    exit 1
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

if ! checkIfImageExist;
then
    echo "Building OCS-gw"
    ./gradlew ocsgw:clean ocsgw:build
    docker build -t eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS} ocsgw

    echo "Uploading Docker image"
    docker push eu.gcr.io/${GCP_PROJECT_ID}/ocsgw:${TAG_OCS}
fi


deploy
