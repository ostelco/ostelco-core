#!/usr/bin/env bash

set -e

kubectl config use-context $(kubectl config get-contexts --output name | grep dev-cluster)

PROJECT_ID=$(gcloud config get-value project -q 2> /dev/null)
TAG=$(git log -1 --pretty=format:%h)-dev

echo "Building eu.gcr.io/${PROJECT_ID}/simmanager:${TAG}"

docker build -t eu.gcr.io/${PROJECT_ID}/simmanager:${TAG} .
docker push eu.gcr.io/${PROJECT_ID}/simmanager:${TAG}

echo "Deploying eu.gcr.io/${PROJECT_ID}/simmanager:${TAG} to GKE"

#sed -e s/SIMMANAGER_VERSION/${TAG}/g simmanager-deploy.yaml | kubectl apply -f -
