#!/usr/bin/env bash

set -e

if [ ! -f exporter/deploy/deploy-dev.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

kubectl config use-context $(kubectl config get-contexts --output name | grep private-cluster)

PROJECT_ID="$(gcloud config get-value project -q)"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG="v${SHORT_SHA}"

echo PROJECT_ID=${PROJECT_ID}
echo SHORT_SHA=${SHORT_SHA}
echo TAG=${TAG}

docker build -t eu.gcr.io/${PROJECT_ID}/exporter:${TAG} exporter
docker push eu.gcr.io/${PROJECT_ID}/exporter:${TAG}

echo "Deploying exporter to GKE"

sed -e s/EXPORTER_VERSION/${TAG}/g exporter/exporter.yaml | kubectl apply -f -