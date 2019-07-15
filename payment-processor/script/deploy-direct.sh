#!/usr/bin/env bash

set -e

if [ ! -f payment-processor/script/deploy-direct.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

kubectl config use-context $(kubectl config get-contexts --output name | grep pi-dev)

GCP_PROJECT_ID="$(gcloud config get-value project -q)"
MONITOR_VERSION="$(gradle payment-processor:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG="${MONITOR_VERSION}-${SHORT_SHA}"

echo GCP_PROJECT_ID=${GCP_PROJECT_ID}
echo MONITOR_VERSION=${MONITOR_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG=${TAG}

docker build -t eu.gcr.io/${GCP_PROJECT_ID}/stripe-monitor:${TAG} payment-processor/script
docker push eu.gcr.io/${GCP_PROJECT_ID}/stripe-monitor:${TAG}

echo "Deploying stripe-monitor to GKE"

sed -e 's/MONITOR_VERSION/'"${TAG}"'/g; s/GCP_PROJECT_ID/'"${GCP_PROJECT_ID}"'/g' prime/infra/stripe-monitor-task.yaml | kubectl apply -f -
