# Deploying Prime to Kubernetes

    #PROJECT_ID=pantel-2decb
    export PROJECT_ID="$(gcloud config get-value project -q)"
    export PRIME_VERSION="$(gradle properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"

Reference:
 * https://cloud.google.com/endpoints/docs/grpc/get-started-grpc-kubernetes-engine

## Secrets

    kubectl create secret generic pantel-prod.json --from-file config/pantel-prod.json

Reference:
 * https://cloud.google.com/kubernetes-engine/docs/concepts/secret
 * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-files-from-a-pod
 * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-environment-variables

## Endpoint

Generate self-contained protobuf descriptor file - api_descriptor.pb

    pip install grpcio grpcio-tools

    python -m grpc_tools.protoc \
        --include_imports \
        --include_source_info \
        --proto_path=../ocs-api/src/main/proto \
        --descriptor_set_out=api_descriptor.pb \
        ocs.proto

Deploy endpoints

    gcloud endpoints services deploy api_descriptor.pb infra/ocs-api_config.yaml

## Deployment & Service

Increment the docker image tag (version) for next two steps.
 
Build the Docker image (In the folder with Dockerfile)

    docker build -t gcr.io/${PROJECT_ID}/prime:${PRIME_VERSION} .
Push to the registry

    gcloud docker -- push gcr.io/${PROJECT_ID}/prime:${PRIME_VERSION}

Update the tag (version) of prime's docker image in `infra/prime.yaml`.

Apply the deployment & service

    sed -e s/PRIME_VERSION/$PRIME_VERSION/g infra/prime.yaml | kubectl apply -f -
    

Details of the deployment

    kubectl describe deployment prime
    kubectl get pods

Details of service

    kubectl describe service prime-service

## API Endpoint

    gcloud endpoints services deploy infra/prime-client-api.yaml

## SSL secrets for api.ostelco.org & ocs.ostelco.org
The endpoints runtime expects the SSL configuration to be named
as `nginx.crt` and `nginx.key`. Sample command to create the secret:
```
    kubectl create secret generic api-ostelco-ssl \
     --from-file=./nginx.crt --from-file=./nginx.key
```
The secret for api.ostelco.org is in `api-ostelco-ssl` & the one for
ocs.ostelco.org is in `ocs-ostelco-ssl`
