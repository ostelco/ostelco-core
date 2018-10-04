#!/usr/bin/env bash

set -e

if [ ! -f bq-metrics-extractor/cronjob/deploy-direct.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

kubectl config use-context $(kubectl config get-contexts --output name | grep private-cluster)

PROJECT_ID="$(gcloud config get-value project -q)"
EXTRACTOR_VERSION="$(gradle bq-metrics-extractor:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG="${EXTRACTOR_VERSION}-${SHORT_SHA}"

echo PROJECT_ID=${PROJECT_ID}
echo EXTRACTOR_VERSION=${EXTRACTOR_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG=${TAG}


gradle bq-metrics-extractor:clean bq-metrics-extractor:build
docker build -t eu.gcr.io/${PROJECT_ID}/bq-metrics-extractor:${TAG} bq-metrics-extractor
docker push eu.gcr.io/${PROJECT_ID}/bq-metrics-extractor:${TAG}

echo "Deploying bq-metrics-extractor to GKE"

sed -e s/EXTRACTOR_VERSION/${TAG}/g bq-metrics-extractor/cronjob/extractor.yaml | kubectl apply -f -
