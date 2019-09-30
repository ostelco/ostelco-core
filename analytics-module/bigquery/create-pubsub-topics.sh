#!/bin/bash

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 (dev|prod)" 1>&2
  exit 1
fi

DEPLOYMENT="$1"

gcloud pubsub topics create purchase-info --project "pi-ostelco-$DEPLOYMENT" # purchases
gcloud pubsub topics create analytics-refunds --project "pi-ostelco-$DEPLOYMENT" # refunds
gcloud pubsub topics create data-traffic --project "pi-ostelco-$DEPLOYMENT" # consumption
gcloud pubsub topics create sim-provisioning --project "pi-ostelco-$DEPLOYMENT" # sim provisioning
gcloud pubsub topics create subscription-status-update --project "pi-ostelco-$DEPLOYMENT" # subscription status update
