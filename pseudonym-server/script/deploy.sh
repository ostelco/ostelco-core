#!/usr/bin/env bash

set -e
if [ -z "$1" ] || [ -z "$2" ]; then
  (>&2 echo "Usage: deploy.sh <module name> <git tag>")
  exit 1
fi
module=$1
tag=$2

if [ ! -f "$module/script/deploy.sh" ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

CHECK_REPO="$module/script/check_repo.sh"

if [ ! -f ${CHECK_REPO} ]; then
    (>&2 echo "Missing file - $CHECK_REPO")
    exit 1
fi

PROJECT_ID="$(gcloud config get-value project -q)"
BRANCH_NAME=$(git branch | grep \* | cut -d ' ' -f2)

echo PROJECT_ID=${PROJECT_ID}
echo BRANCH_NAME=${BRANCH_NAME}

echo "Deploying $module to GKE"

gcloud container builds submit \
  --config pseudonym-server/cloudbuild.yaml \
  --substitutions TAG_NAME=${tag},BRANCH_NAME=${BRANCH_NAME} .
