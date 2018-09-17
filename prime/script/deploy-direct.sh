#!/usr/bin/env bash

set -e

if [ ! -f prime/script/deploy.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

CHECK_REPO="prime/script/check_repo.sh"

if [ ! -f ${CHECK_REPO} ]; then
    (>&2 echo "Missing file - $CHECK_REPO")
    exit 1
fi

kubectl config use-context $(kubectl config get-contexts --output name | grep private-cluster)

BRANCH_NAME=$(git branch | grep \* | cut -d ' ' -f2)
echo BRANCH_NAME=${BRANCH_NAME}
${CHECK_REPO} ${BRANCH_NAME}

PROJECT_ID="$(gcloud config get-value project -q)"
PRIME_VERSION="$(gradle prime:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG="${PRIME_VERSION}-${SHORT_SHA}"

echo PROJECT_ID=${PROJECT_ID}
echo PRIME_VERSION=${PRIME_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG=${TAG}


gradle prime:clean prime:build
docker build -t eu.gcr.io/${PROJECT_ID}/prime:${TAG} prime
docker push eu.gcr.io/${PROJECT_ID}/prime:${TAG}

echo "Deploying prime to GKE"

sed -e s/PRIME_VERSION/${TAG}/g prime/infra/prod/prime.yaml | kubectl apply -f -