# Deploy to production

## Deploy OCS gateway to GCP

./ocsgw/infra/script/deploy-ocsgw.sh

## Deploy to kubernetes cluster on GCP

Set env variable

    export GCP_PROJECT_ID="$(gcloud config get-value project -q)"

For the commands below:

    <app> = <deployemnt> = pseudonym-server | prime
    <service> = pseudonym-server-service | prime-service
    <cluster-name> = private-cluster
    <zone> = europe-west1-b

Create cluster

```bash
gcloud container clusters \
  create private-cluster --scopes=default,bigquery,datastore,pubsub,sql,storage-rw \
  --num-nodes=3 \
  --zone europe-west1-b
```

If cluster already exists, fetch authentication credentials for the Kubernetes cluster

    gcloud container clusters get-credentials <cluster-name> --zone <zone>


Build the Docker image (In the folder with Dockerfile)

    docker build -t eu.gcr.io/${GCP_PROJECT_ID}/<app>:<version> .

Push to the registry

    docker push eu.gcr.io/${GCP_PROJECT_ID}/<app>:<version>

Apply the deployment

    kubectl apply -f ./<deployement>.yaml

Details of the deployment

    kubectl describe deployment <deployment>
    kubectl get pods

Deploy the service

    kubectl apply -f ./<service>.yaml

Details of service

    kubectl describe service <service>

### Cleanup kubernetes

Delete service

    kubectl delete service <service>

Delete deployment

    kubectl delete  deployment <deployment>

Delete cluster

    gcloud container clusters delete private-cluster
