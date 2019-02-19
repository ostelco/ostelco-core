## Exporter

This contains a set of scripts to generate the data for analayis. The export script
`export_data.sh` creates a new big query table with a new uuid which maps the pseudonyms to
msisdn. This table is then used to join the hourly data consumption to create a new
pseudonymised version of hourly consumption. The output doesn't contain msisdns. The output
table is then exported as a csv file in google cloud storage. There is also a script
`delete_export_data.sh` to delete these tables & files.

Currently this is deployed as a pod. The commands can be run by logging into the container.

Tables created:
1) exported_pseudonyms.<exportId> : The pseudonyms table, which can be used to reverse lookup msisdns
2) exported_data_consumption.<exportId> : The output hourly consumption table, used to export data to csv

How to deploy/use this in kubernetes cluster

```
# To deploy in prod
exporter/deploy/deploy.sh

# To deploy in dev
exporter/deploy/deploy-dev.sh

# Details of the deployment
kubectl describe deployment exporter
kubectl get pods

# Login to the pod
kubectl exec -it <exporter pod name> -- /bin/bash

# Run exporter from the above shell
/export_data.sh
# This results in 3 csv files in GCS
1) Data consumption Records: gs://GCP_PROJECT_ID-dataconsumption-export/<exportID>.csv
2) Purchase Records: gs://GCP_PROJECT_ID-dataconsumption-export/<exportID>-purchases.csv
3) Subscriber to MSISDN mappings: gs://GCP_PROJECT_ID-dataconsumption-export/<exportID>-sub2msisdn.csv

# Run subsciber reverse lookup from the above shell
/map_subscribers.sh <exportID>

# Delete all tables and files for an export
/delete_export_data.sh <exportID>

# Delete deployment
kubectl delete  deployment exporter

```


## How to get data from the exporter

  .... tbd