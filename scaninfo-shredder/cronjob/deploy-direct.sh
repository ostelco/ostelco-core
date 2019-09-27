#!/usr/bin/env bash

set -e

if [ ! -f scaninfo-shredder/cronjob/deploy-direct.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

kubectl config use-context $(kubectl config get-contexts --output name | grep pi-prod)

GCP_PROJECT_ID="$(gcloud config get-value project -q)"
SHREDDER_VERSION="$(gradle scaninfo-shredder:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG="${SHREDDER_VERSION}-${SHORT_SHA}"

echo GCP_PROJECT_ID=${GCP_PROJECT_ID}
echo SHREDDER_VERSION=${SHREDDER_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG=${TAG}


gradle scaninfo-shredder:clean scaninfo-shredder:build
docker build -t eu.gcr.io/${GCP_PROJECT_ID}/scaninfo-shredder:${TAG} scaninfo-shredder
docker push eu.gcr.io/${GCP_PROJECT_ID}/scaninfo-shredder:${TAG}

echo "Deploying scaninfo-shredder to GKE"

sed -e 's/SHREDDER_VERSION/'"${TAG}"'/g; s/GCP_PROJECT_ID/'"${GCP_PROJECT_ID}"'/g' scaninfo-shredder/cronjob/shredder.yaml | kubectl apply -f -
