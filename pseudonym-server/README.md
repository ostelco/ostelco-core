# Pseudonym Server

    #PROJECT_ID=pantel-2decb
    export PROJECT_ID="$(gcloud config get-value project -q)"
    export PSEUDONYM_VERSION="$(gradle properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"


Build the Docker image (In the folder with Dockerfile)

    docker build -t gcr.io/${PROJECT_ID}/pseudonym-server:${PSEUDONYM_VERSION} .

Push to the registry

    gcloud docker -- push gcr.io/${PROJECT_ID}/pseudonym-server:${PSEUDONYM_VERSION}

Update the tag (version) of prime's docker image in `pseudonym-server.yaml`.

Apply the deployment & service

    sed -e "s/PSEUDONYM_VERSION/$PSEUDONYM_VERSION/" pseudonym-server.yaml | kubectl apply -f -


Details of the deployment

    kubectl describe deployment pseudonym-server
    kubectl get pods


Helper Commands

Create cluster

    gcloud container clusters create private-cluster --scopes=default,bigquery,datastore,pubsub,sql,storage-rw --num-nodes=3

Delete cluster

    gcloud container clusters delete private-cluster

Delete service

    kubectl delete service pseudonym-server-service

Delete deployment

    kubectl delete  deployment pseudonym-server


Container to test DNS

    kubectl run curl --image=radial/busyboxplus:curl -i --tty
    nslookup pseudonym-server-service
    curl pseudonym-server-service.default.svc.cluster.local/pseudonym/current/47333

SQL for joining dataconsumption and pseudonyms table

    SELECT
      hc.bytes, ps.msisdnid, hc.timestamp
    FROM
      [pantel-2decb:data_consumption.hourly_consumption] as hc
    JOIN
      [pantel-2decb:exported_pseudonyms.3ebcdc4a7ecc4cd385e82087e49b7b7b] as ps
    ON  ps.msisdn = hc.msisdn

Login to gcr.io for pushing images

    docker login -u oauth2accesstoken -p "$(gcloud auth print-access-token)" https://gcr.io

