#!/usr/bin/env bash

set -e

if [ ! -f prime/script/deploy.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

GCP_PROJECT_ID="$(gcloud config get-value project -q)"
SHORT_SHA="$(git log -1 --pretty=format:%h)"

echo GCP_PROJECT_ID=${GCP_PROJECT_ID}
echo SHORT_SHA=${SHORT_SHA}

echo "Deploying prime to GKE"

gcloud container builds submit \
  --config prime/cloudbuild.dev.yaml \
  --substitutions SHORT_SHA=${SHORT_SHA} .