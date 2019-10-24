#!/bin/bash

TEMPLATE_LOCATION="gs://dataflow-templates/latest/PubSub_to_BigQuery"
REGION="europe-west1"

PROJECT_ID="pi-ostelco-dev"
TOPIC_PREFIX="projects/$PROJECT_ID/topics"

gcloud dataflow jobs run purchases-dev \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic=$TOPIC_PREFIX/purchase-info,outputTableSpec=$PROJECT_ID:purchases.purchases --max-workers 3

gcloud dataflow jobs run refunds-dev \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic=$TOPIC_PREFIX/analytics-refunds,outputTableSpec=$PROJECT_ID:purchases.refunds --max-workers 3

gcloud dataflow jobs run data-consumption-dev \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic=$TOPIC_PREFIX/data-traffic,outputTableSpec=$PROJECT_ID:data_consumption.data_consumption --max-workers 3

gcloud dataflow jobs run sim-provisionings-dev \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic=$TOPIC_PREFIX/sim-provisioning,outputTableSpec=$PROJECT_ID:sim_provisionings.sim_provisionings --max-workers 3

gcloud dataflow jobs run subscription-status-updates-dev \
  --gcs-location $TEMPLATE_LOCATION --region $REGION \
  --parameters inputTopic=$TOPIC_PREFIX/subscription-status-update,outputTableSpec=$PROJECT_ID:sim_provisionings.subscription_status_updates --max-workers 3
