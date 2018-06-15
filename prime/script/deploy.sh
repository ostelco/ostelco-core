#!/usr/bin/env bash

set -e

if [ ! -f prime/script/deploy.sh ]; then
    echo "Run this script from project root dir (ostelco-core)"
    exit 1
fi

export PROJECT_ID="$(gcloud config get-value project -q)"
export PRIME_VERSION="$(gradle prime:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
export BRANCH_NAME=$(git branch | grep \* | cut -d ' ' -f2)

echo PROJECT_ID=${PROJECT_ID}
echo PRIME_VERSION=${PRIME_VERSION}
echo BRANCH_NAME=${BRANCH_NAME}

prime/script/check_repo.sh ${BRANCH_NAME}

echo "Deploying prime to GKE"

gcloud container builds submit \
  --config prime/cloudbuild.yaml \
  --substitutions TAG_NAME=${PRIME_VERSION},BRANCH_NAME=${BRANCH_NAME} .