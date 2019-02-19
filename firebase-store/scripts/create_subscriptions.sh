#!/usr/bin/env bash

GCP_PROJECT_ID=$(gcloud config get-value project -q)
export MSISDN=
export EMAIL=

export URL_ENCODED_EMAIL=$(echo "$EMAIL" | sed 's/\./%2E/g' | sed 's/@/%40/g')
echo firebase --project ${GCP_PROJECT_ID}  --data "\"$MSISDN\"" database:set /v2/subscriptions/"$URL_ENCODED_EMAIL"
