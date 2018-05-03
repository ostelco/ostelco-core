# Deploy to production

## Deploy to on-premise adjoining Packet gateway   

### Package
 
    gradle clean pack

With unit testing:
    
    gradle clean test pack
    
* This creates zip file `build/deploy/ostelco-core.zip`

### Run

* Upload and unzip `ostelco-core.zip` file.


    scp build/deploy/ostelco-core.zip loltel@10.6.101.1:ostelco-core/
    ssh -A loltel@10.6.101.1
    scp ostelco-core/ostelco-core.zip ubuntu@192.168.0.123:.
    ssh ubuntu@192.168.0.123
    unzip ostelco-core.zip -d ostelco-core

* Run in docker


    cd ostelco-core
    sudo docker-compose up -d --build

    sudo docker-compose logs -f

    sudo docker logs -f ocsgw
    sudo docker logs -f auth-server


## Deploy to kubernetes cluster on GCP

Set env variable

    export PROJECT_ID="$(gcloud config get-value project -q)"

For the commands below:

    <app> = <deployemnt> = pseudonym-server | prime
    <service> = pseudonym-server-service | prime-service
    <cluster-name> = private-cluster
    <zone> = europe-west1-b

Create cluster

    gcloud container clusters create private-cluster --scopes=default,bigquery,datastore,pubsub,sql,storage-rw --num-nodes=3

If cluster already exists, fetch authentication credentials for the Kubernetes cluster

    gcloud container clusters get-credentials <cluster-name> --zone <zone>


Build the Docker image (In the folder with Dockerfile)

    docker build -t gcr.io/${PROJECT_ID}/<app>:<version> .

Push to the registry

    gcloud docker -- push gcr.io/${PROJECT_ID}/<app>:<version>

Apply the deployment

    kubectl apply -f ./<deployement>.yaml

Details of the deployment

    kubectl describe deployment <deployment>
    kubectl get pods

Deploy the service

    kubectl apply -f ./<service>.yaml

Details of service
    
    kubectl describe service <service>

### Cleanup

Delete service

    kubectl delete service <service>

Delete deployment
    
    kubectl delete  deployment <deployment>

Delete cluster

    gcloud container clusters delete private-cluster
