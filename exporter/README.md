# Exporter

How to deploy this in kubernetes cluster

```
#PROJECT_ID=pantel-2decb
export PROJECT_ID="$(gcloud config get-value project -q)"

# Create cluster
gcloud container clusters create private-cluster --scopes=default,bigquery,datastore,pubsub,sql,storage-rw --num-nodes=3

# Get authentication credentials for the cluster
gcloud container clusters get-credentials private-cluster

# Build the Docker image (In the folder with Dockerfile)
docker build -t gcr.io/${PROJECT_ID}/exporter:v1 .

# Push to the registry
gcloud docker -- push gcr.io/${PROJECT_ID}/exporter:v1

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