#!/bin/bash

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 (dev|prod)" 1>&2
  exit 1
fi

DEPLOYMENT="$1"

bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-"$DEPLOYMENT":purchases.purchases purchases.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-"$DEPLOYMENT":purchases.refunds refunds.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-"$DEPLOYMENT":data_consumption.data_consumption data_consumption.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-"$DEPLOYMENT":sim_provisionings.sim_provisionings sim_provisionings.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-"$DEPLOYMENT":sim_provisionings.subscription_status_updates subscription_status_updates.json
