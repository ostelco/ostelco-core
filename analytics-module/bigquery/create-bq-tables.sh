#!/bin/bash

bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-dev:purchases.purchases purchases.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-dev:purchases.refunds refunds.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-dev:data_consumption.data_consumption data_consumption.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-dev:sim_provisionings.sim_provisionings sim_provisionings.json
bq mk --table --time_partitioning_type=DAY --time_partitioning_field=timestamp pi-ostelco-dev:sim_provisionings.subscription_status_updates subscription_status_updates.json
