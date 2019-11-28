# Deploying Prime to Kubernetes

### TL;DR

* **Option 1:** Run this from project root folder (ostelco-core) on `master` branch


    prime/script/deploy.sh  

* **Option 2:** Push a tag `prime-X.Y.Z` on `master` branch.


===


### Setup

Set variables by doing this in `prime` directory:

    #GCP_PROJECT_ID=$(gcloud config get-value project -q)

```bash
export GCP_PROJECT_ID="$(gcloud config get-value project -q)"
echo "GCP_PROJECT_ID=GCP_PROJECT_ID"
export PRIME_VERSION="$(gradle properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
echo "PRIME_VERSION=$PRIME_VERSION"
```    

Reference:
 * https://cloud.google.com/endpoints/docs/grpc/get-started-grpc-kubernetes-engine

## Deploying to GKE using GCP Container/Cloud Builder

### Using CLI
In the project (ostelco-core) root folder: 

```bash
gcloud container builds submit \
  --config prime/prod/cloudbuild.yaml \
  --substitutions TAG_NAME=${PRIME_VERSION},BRANCH_NAME=$(git branch | grep \* | cut -d ' ' -f2) .
```

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

```bash
kubectl create secret generic prime-service-account.json --from-file prime/config/prime-service-account.json
```

Reference:
 * https://cloud.google.com/kubernetes-engine/docs/concepts/secret
 * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-files-from-a-pod
 * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-environment-variables

## Endpoint

Generate self-contained protobuf descriptor file - `ocs_descriptor.pb`

```bash
pyenv versions
pyenv local 3.5.2
pip install grpcio grpcio-tools

python -m grpc_tools.protoc \
  --include_imports \
  --include_source_info \
  --proto_path=ocs-grpc-api/src/main/proto \
  --descriptor_set_out=ocs_descriptor.pb \
  ocs.proto

```

Deploy endpoints

```bash
gcloud endpoints services deploy ocs_descriptor.pb prime/infra/prod/ocs-api.yaml
```

## Deployment & Service

Increment the docker image tag (version) for next two steps.
 
Build the Docker image (In the folder with Dockerfile)

```bash
docker build -t eu.gcr.io/${GCP_PROJECT_ID}/prime:${PRIME_VERSION} prime
```

Push to the registry

```bash
docker push eu.gcr.io/${GCP_PROJECT_ID}/prime:${PRIME_VERSION}
```

Update the tag (version) of prime's docker image in `infra/prod/prime.yaml`.

Apply the deployment & service

```bash
sed -e 's/PRIME_VERSION/${PRIME_VERSION}/g; s/_GCP_PROJECT_ID/'"${GCP_PROJECT_ID}"'/g' prime/infra/prod/prime.yaml | kubectl apply -f -
```

Details of the deployment

```bash
kubectl describe deployment prime
kubectl get pods
```

Details of service

```bash
kubectl describe service prime-service
```

## API Endpoint

```bash
gcloud endpoints services deploy prime/infra/prod/prime-customer-api.yaml
```

## SSL secrets for api.ostelco.org, ocs.ostelco.org
The endpoints runtime expects the SSL configuration to be named
as `nginx.crt` and `nginx.key`. Sample command to create the secret:
```bash
kubectl create secret generic api-ostelco-ssl \
  --from-file=./nginx.crt \
  --from-file=./nginx.key
```
The secret for ...
 * `api.ostelco.org` is in `api-ostelco-ssl`
 * `ocs.ostelco.org` is in `ocs-ostelco-ssl`

# For Dev cluster

## One time setup

### Cluster
 * Create cluster

```bash
gcloud container clusters create dev-cluster \
  --scopes=default,bigquery,datastore,pubsub,sql,storage-rw \
  --zone=europe-west1-b \
  --num-nodes=1
```
 * Create node-pool
```bash
gcloud container node-pools create dev-node-pool \
  --cluster=dev-cluster \
  --machine-type=n1-standard-2 \
  --scopes=default,bigquery,datastore,pubsub,sql,storage-rw \
  --num-nodes=3 \
  --zone=europe-west1-b \
  --enable-autorepair
```
 * Delete default pool
```bash
gcloud container node-pools delete default-pool \
  --cluster=dev-cluster \
  --zone=europe-west1-b
```

### Endpoints

 * OCS gRPC endpoint

Generate self-contained protobuf descriptor file - ocs_descriptor.pb

```bash
pip install grpcio grpcio-tools

python -m grpc_tools.protoc \
  --include_imports \
  --include_source_info \
  --proto_path=ocs-grpc-api/src/main/proto \
  --descriptor_set_out=ocs_descriptor.pb \
  ocs.proto
```

Deploy endpoints

```bash
gcloud endpoints services deploy ocs_descriptor.pb prime/infra/dev/ocs-api.yaml
```

 * Client API HTTP endpoint

```bash
gcloud endpoints services deploy prime/infra/dev/prime-customer-api.yaml
```

## Deploy to Dev cluster

### Setup Neo4j

```bash
kubectl apply -f prime/infra/dev/neo4j.yaml
```

Then, import initial data into neo4j using `tools/neo4j-admin-tools`.

### Deploy prime

```bash
prime/script/deploy-dev-direct.sh
```

OR

```bash
export GCP_PROJECT_ID="$(gcloud config get-value project -q)"
export SHORT_SHA="$(git log -1 --pretty=format:%h)"

echo GCP_PROJECT_ID=${GCP_PROJECT_ID}
echo SHORT_SHA=${SHORT_SHA}

docker build -t eu.gcr.io/${GCP_PROJECT_ID}/prime:${SHORT_SHA} .
docker push eu.gcr.io/${GCP_PROJECT_ID}/prime:${SHORT_SHA}
sed -e s/PRIME_VERSION/${SHORT_SHA}/g prime/infra/dev/prime.yaml | kubectl apply -f -
```

## Logs

Goto `https://console.cloud.google.com/logs/viewer` and advanced search for 

```text
resource.type="container"
resource.labels.cluster_name="dev-cluster"
logName="projects/${GCP_PROJECT_ID}/logs/prime"
```
