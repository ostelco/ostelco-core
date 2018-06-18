# Deploying Prime to Kubernetes

### TL;DR

* **Option 1:** Run this from project root folder (ostelco-core) on `master` branch


    prime/script/deploy.sh  

* **Option 2:** Push a tag `prime-X.Y.Z` on `master` branch.


===


### Setup

Set variables by doing this in `prime` directory:

    #PROJECT_ID=pantel-2decb
    
    export PROJECT_ID="$(gcloud config get-value project -q)"
    echo "PROJECT_ID=$PROJECT_ID"
    export PRIME_VERSION="$(gradle properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
    echo "PRIME_VERSION=$PRIME_VERSION"

Reference:
 * https://cloud.google.com/endpoints/docs/grpc/get-started-grpc-kubernetes-engine

## Deploying to GKE using GCP Container/Cloud Builder

### Using CLI
In the project (ostelco-core) root folder: 

    gcloud container builds submit \
        --config prime/cloudbuild.yaml \
        --substitutions TAG_NAME=$PRIME_VERSION,BRANCH_NAME=$(git branch | grep \* | cut -d ' ' -f2) .

#### Limitations
 * Remove .git from `.gcloudignore` and detect branch name and check for uncommitted changes. 

### Using build trigger
 
 * A build trigger is configured in GCP Container/Cloud Builder to build and deploy prime to GKE cluster
   just by adding a git tag on `master` branch.
 * The tag name should be `prime-*`

#### Limitations
 * When using build trigger, the version tag on docker images is `prime-X.Y.Z` instead of `X.Y.Z`.

### Future Improvements
 * Create a custom build docker image. (suggestion by Vihang).
 * Run AT as quality gate. (suggestion by Remseth).
 * Use it for CI. Currently it is only CD. (suggestion by Remseth).
 * Use `git-sha` along/instead with version (suggestion by Håvard).

### References
 * Config: https://cloud.google.com/container-builder/docs/build-config
 * Running locally: https://cloud.google.com/container-builder/docs/build-debug-locally
 * Cloud builders: https://cloud.google.com/container-builder/docs/cloud-builders
 * Customization: https://cloud.google.com/container-builder/docs/create-custom-build-steps
 * Optimization: https://cloud.google.com/container-builder/docs/speeding-up-builds
 * Custom Github web-hooks: https://cloud.google.com/container-builder/docs/configure-third-party-notifications
 * Storing secrets for AT: https://cloud.google.com/container-builder/docs/securing-builds/use-encrypted-secrets-credentials

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
