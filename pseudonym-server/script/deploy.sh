#!/usr/bin/env bash

set -e
module=""
if [ -z "$1" ]; then
  echo "module is blank"
  exit 1
else
  module=$1
fi
tag=""
if [ -z "$2" ]; then
  echo "tag is blank"
  exit 1
else
  tag=$2
fi

if [ ! -f pseudonym-server/script/deploy.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

CHECK_REPO="pseudonym-server/script/check_repo.sh"

if [ ! -f ${CHECK_REPO} ]; then
    (>&2 echo "Missing file - $CHECK_REPO")
    exit 1
fi

PROJECT_ID="$(gcloud config get-value project -q)"
BRANCH_NAME=$(git branch | grep \* | cut -d ' ' -f2)

echo PROJECT_ID=${PROJECT_ID}
echo BRANCH_NAME=${BRANCH_NAME}

echo "Deploying pseudonym-server to GKE"

gcloud container builds submit \
  --config pseudonym-server/cloudbuild.yaml \
  --substitutions TAG_NAME=${tag},BRANCH_NAME=${BRANCH_NAME} .
