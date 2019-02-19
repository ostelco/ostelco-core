#!/usr/bin/env bash

GCP_PROJECT_ID=$(gcloud config get-value project -q)

for MSISDN in {178..190}
do
    echo firebase --project ${GCP_PROJECT_ID}  --data '0' database:set /v2/balance/4790300${MSISDN}
done
