#!/usr/bin/env bash

set -e

if [ ! -f ocsgw/infra/script/deploy-ocsgw-dev.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

kubectl config use-context $(kubectl config get-contexts --output name | grep dev-cluster)

PROJECT_ID="$(gcloud config get-value project -q)"
OCSGW_VERSION="$(./gradlew ocsgw:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG_OCS="${OCSGW_VERSION}-${SHORT_SHA}-dev"

echo PROJECT_ID=${PROJECT_ID}
echo OCSGW_VERSION=${OCSGW_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG_OCS=${TAG_OCS}

./gradlew ocsgw:clean ocsgw:build
docker build -t eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS} ocsgw
docker push eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS}

echo "Deploying ocsgw to GKE Dev"

gcloud compute instances update-container ocsgw-dev \
    --container-image eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS}