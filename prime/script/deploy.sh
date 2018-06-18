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

PROJECT_ID="$(gcloud config get-value project -q)"
PRIME_VERSION="$(gradle prime:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
BRANCH_NAME=$(git branch | grep \* | cut -d ' ' -f2)

echo PROJECT_ID=${PROJECT_ID}
echo PRIME_VERSION=${PRIME_VERSION}
echo BRANCH_NAME=${BRANCH_NAME}

${CHECK_REPO} ${BRANCH_NAME}

echo "Deploying prime to GKE"

gcloud container builds submit \
  --config prime/cloudbuild.yaml \
  --substitutions TAG_NAME=${PRIME_VERSION},BRANCH_NAME=${BRANCH_NAME} .