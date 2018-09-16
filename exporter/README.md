# Exporter

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
#PROJECT_ID=pantel-2decb
export PROJECT_ID="$(gcloud config get-value project -q)"

# Create cluster
gcloud container clusters create private-cluster --scopes=default,bigquery,datastore,pubsub,sql,storage-rw --num-nodes=3

# Get authentication credentials for the cluster
gcloud container clusters get-credentials private-cluster

# Build the Docker image (In the folder with Dockerfile)
docker build -t eu.gcr.io/${PROJECT_ID}/exporter:v1 .

# Push to the registry
docker push eu.gcr.io/${PROJECT_ID}/exporter:v1

# Apply the deployment
kubectl apply -f ./exporter.yaml

# Details of the deployment
kubectl describe deployment exporter
kubectl get pods

# Login to the pod
kubectl exec -it <exporter pod name> -- /bin/bash

# Run exporter from the above shell
/export_data.sh

# Delete deployment
kubectl delete  deployment exporter

```