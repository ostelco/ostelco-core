#!/bin/bash

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 (dev|prod)" 1>&2
  exit 1
fi

DEPLOYMENT="$1"

TEMPLATE_LOCATION="gs://dataflow-templates/latest/PubSub_to_BigQuery"
REGION="europe-west1"

PROJECT_ID="pi-ostelco-$DEPLOYMENT"
TOPIC_PREFIX="projects/$PROJECT_ID/topics"

gcloud dataflow jobs run "purchases-$DEPLOYMENT" \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic="$TOPIC_PREFIX"/purchase-info,outputTableSpec="$PROJECT_ID":purchases.purchases --max-workers 3

gcloud dataflow jobs run "refunds-$DEPLOYMENT" \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic="$TOPIC_PREFIX"/analytics-refunds,outputTableSpec="$PROJECT_ID":purchases.refunds --max-workers 3

gcloud dataflow jobs run "data-consumption-$DEPLOYMENT" \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic="$TOPIC_PREFIX"/data-traffic,outputTableSpec="$PROJECT_ID":data_consumption.data_consumption --max-workers 3

gcloud dataflow jobs run "sim-provisionings-$DEPLOYMENT" \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic="$TOPIC_PREFIX"/sim-provisioning,outputTableSpec="$PROJECT_ID":sim_provisionings.sim_provisionings --max-workers 3

gcloud dataflow jobs run "subscription-status-updates-$DEPLOYMENT" \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic="$TOPIC_PREFIX"/subscription-status-update,outputTableSpec="$PROJECT_ID":sim_provisionings.subscription_status_updates --max-workers 3
