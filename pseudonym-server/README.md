# Pseudonym Server


How to deploy this in kubernetes cluster

```
#PROJECT_ID=pantel-2decb
export PROJECT_ID="$(gcloud config get-value project -q)"

# Create cluster
gcloud container clusters create private-cluster --scopes=default,bigquery,datastore,pubsub,sql,storage-rw --num-nodes=3

# Build the Docker image (In the folder with Dockerfile)
docker build -t gcr.io/${PROJECT_ID}/pseudonym-server:v1 .
# Push to the registry
gcloud docker -- push gcr.io/${PROJECT_ID}/pseudonym-server:v1

# Apply the deployment
kubectl apply -f ./pseudonym-server.yaml

# Details of the deployment
kubectl describe deployment pseudonym-server
kubectl get pods

# Deploy the service
kubectl apply -f ./pseudonym-server-service.yaml
# Details of service
kubectl describe service pseudonym-server-service

# Delete service
kubectl delete service pseudonym-server-service
# Delete deployment
kubectl delete  deployment pseudonym-server

# Delete cluster
gcloud container clusters delete private-cluster

# Container to test DNS
kubectl run curl --image=radial/busyboxplus:curl -i --tty
nslookup pseudonym-server-service
curl pseudonym-server-service.default.svc.cluster.local/pseudonym/current/47333

# SQL for joining dataconsumption and pseudonyms table
SELECT
  hc.bytes, ps.msisdnid, hc.timestamp
FROM
  [pantel-2decb:data_consumption.hourly_consumption] as hc
JOIN
  [pantel-2decb:exported_pseudonyms.3ebcdc4a7ecc4cd385e82087e49b7b7b] as ps
ON  ps.msisdn = hc.msisdn

```