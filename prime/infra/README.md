# Prime Deployment

## Dev deployment

Prime is automatically deployed into the dev cluster through the circleci pipeline. 

Important Notes:

- The pipeline deploys Prime in the `dev` namespace. DO NOT modify this deployment.
- `Secrets` are created manually and are NOT accessible across namespaces.
- The `default namespace` is available for developers to deploy their own test instances of Prime. See below for details on how to do this.
- The `pipeline` is triggered by merges to develop. It builds the code, builds a new docker image with a new tag, updates the cloud endpoints (API specs) before it then deploys Prime.
- The `pipeline` uses `helm` to deploy to the dev cluster using [the values file (helm deployment config file)](../../.circleci/prime-dev-values.yaml). See below for details on how to update this file.

## Updating helm deployment config

If changes are required to the kubernetes deployment of Prime, you need to edit the [helm values file](../../.circleci/prime-dev-values.yaml) used by the pipeline. 

### Adding non-secret environment variables
Non-secret environment variables can be defined in the `env` section of the helm values file. Example: 
```yaml
env: 
    FIREBASE_ROOT_PATH: dev
    NEO4J_HOST: neo4j-neo4j.neo4j.svc.cluster.local
```
### Adding Secrets as environment variables
Secrets are created manually using `kubectl create secret ... -n dev`
> Remember to create the secrets in the `dev` namespace before deployment is triggered.

Environment variables from k8s secrets can be defined in the `envFromSecret` section of the helm values file. Example: 
```yaml
envFromSecret:
  - name: SLACK_WEBHOOK_URI # name of the environment variable that will be exposed to Prime
    secretName: slack-secrets # name of the secret which should pre-exist in the namespace
    secretKey: slackWebHookUri # name of the k8s secret key to get the value from
  - name: ANOTHER_ENV_FROM_SECRET
    secretName: myExistingSecret
    secretKey: theKeyInsideTheSecret  
```

### Adding Secrets as volumes mounted to specific paths

Secrets are created manually using `kubectl create secret ... -n dev`
> Remember to create the secrets in the `dev` namespace before deployment is triggered.

Environment variables from k8s secrets can be defined in the `secretVolumes` section of the helm values file. Example: 

```yaml
secretVolumes:
  - secretName: "prime-sa-key"  # the secret name
    containerMountPath: "/secret" # the path in the container where the secret is mounted
  # mount a secret on specific path with key projection 
  - secretName: "simmgr-test-secrets"  
    containerMountPath: "/certs"
    secretKey: idemiaClientCert
    secretPath: idemia-client-cert.jks # this is the file name that will appear in the volume
```

### ESP containers config

Each esp container is defined and configured in its own section. The `esp` section defines the ESP image config (which is common). 

```yaml
ocsEsp: 
  enabled: true # whether to have that esp or not
  env: {} # any env vars to pass to the esp container
  endpointAddress: ocs.dev.oya.world # the cloud endpoint address
  ports: # ports exposed from that esp container. format is: <port-name>: <port-number>
    http2_port: 9000
```

### Services

Services are configured in the `services` section of the helm values file. 

> It is very important to set `grpcOrHttp2: true` if the service being exposed is a GRPC or HTTP2 service

```yaml
services:
  ocs:
    name: ocs # service name
    type: ClusterIP # k8s service type
    port: 80 # the service port 
    targetPort: 9000 # the target port in the prime container
    portName: grpc # the name of the service port
    host: ocs.dev.oya.world # the DNS at which this service is reachable. This served by Ambassador.
    grpcOrHttp2: true # whether this service exposes an HTTP2 or GRPC service.
```

### TLS certs

The TLS certificates are managed by `cert-manager` and are automatically created from the helm chart. TLS creation is configured in the `certs` section of the helm values file.

```yaml
certs: 
  enabled: true # enabled means create a TLS cert
  dnsProvider: dev-clouddns # the DNS provider configuration. This preconfigured and should not be changed.
  issuer: letsencrypt-production # the Letsencrypt API to use. letsencrypt-production for valid certs or letsencrypt-staging for invalid staging certs.
  tlsSecretName: dev-oya-tls # the name of the secret containing credentials to talk to DNS provider API
  hosts: # a list of the hosts to be included in the cert. wildcard domains must be wrapped in single quotes 
    - '*.dev.oya.world'
```

## Deploying developer test instances 

To avoid pipeline waiting time, you can take a short cut and deploy your feature branch directly into the cluster.

**Important Notes**
- Developer tests can be done in the `default` namespace.
- Secrets will need to be replicated into the `default` namespace from the `dev` namespace.

> You can copy secrets between namespaces with the following command : `$ kubectl get secret <existing-secret-name> --namespace=dev --export -o yaml | kubectl apply --namespace=default -f - `

**Steps:**

1. Build the prime docker image from your feature branch and tag it with your custom tag (e.g. eu.gcr.io/pi-ostelco-dev/prime:feature-xyz)

2. Push the built image into the docker registry. 
```bash
# auth is needed since the docker registry is private
$ gcloud auth login
$ docker push eu.gcr.io/pi-ostelco-dev/prime:feature-xyz
```

3. [Install helm](https://helm.sh/docs/using_helm/#install-helm)

4. Make your own copy of the helm values file.

Copy the [sample developer helm values file](developer-tests-prime-values.yaml) and edit the service DNS hosts in the `services` section with unique custom prefixes (e.g. feature-xyz-api.test.oya.world).

5. run the following helm commands:

> Note: the helm release name must be unique. A good example might be feature name or developer name.

> Note: you can change the helm chart version below to a specific version of the prime helm chart. 

```bash 
# the first command is only needed once
$ helm repo add ostelco https://storage.googleapis.com/pi-ostelco-helm-charts-repo/
$ helm repo update
# if your kube context is not configured to point to the dev cluster, then configure it 
$ kubectl config use-context gke_pi-ostelco-dev_europe-west1-c_pi-dev
$ RELEASE_NAME=<some-unique-name>
$ helm upgrade ${RELEASE_NAME} ostelco/prime --version 0.4.3 --install -f <path-to-your-custom-values-file> --set prime.tag=feature-xyz
```
you can then watch for your pods being created with this command:

```bash 
$ kubectl get pods -n dev -l release=${RELEASE_NAME} -w
```

Once your pods are in the `Running` state, you can test the APIs of your custom deployment on: feature-xyz-prime-api-name.test.oya.world (e.g. https://feature-xyz-api.test.oya.world)

To delete your custom deployment:

```bash
$ helm delete --purge ${RELEASE_NAME}
```
-------------- 
# Legacy setup below
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
kubectl create secret generic imeiDb.csv.zip --from-file imeiDb.csv.zip
```

Reference:
 * https://cloud.google.com/kubernetes-engine/docs/concepts/secret
 * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-files-from-a-pod
 * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-environment-variables

## Endpoint

Generate self-contained protobuf descriptor file - `ocs_descriptor.pb` & `metrics_descriptor.pb`

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

python -m grpc_tools.protoc \
  --include_imports \
  --include_source_info \
  --proto_path=analytics-grpc-api/src/main/proto \
  --descriptor_set_out=metrics_descriptor.pb \
  prime_metrics.proto
```

Deploy endpoints

```bash
gcloud endpoints services deploy ocs_descriptor.pb prime/infra/prod/ocs-api.yaml
gcloud endpoints services deploy metrics_descriptor.pb prime/infra/prod/metrics-api.yaml
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

## SSL secrets for api.ostelco.org, ocs.ostelco.org & metrics.ostelco.org
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
 * `metrics.ostelco.org` is in `metrics-ostelco-ssl`

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
### Secrets

 * Place `*.dev.ostelco.org` cert at `certs/dev.ostelco.org`

 * Create k8s secrets

```bash
kubectl create secret generic prime-service-account.json --from-file prime/config/prime-service-account.json
```

Note: To update the secrets defined using yaml, delete and created them again. They are not updated.
 
```bash
kubectl create secret generic stripe-secrets --from-literal=stripeApiKey='keep-stripe-api-key-here'
```

```bash
kubectl create secret generic slack-secrets --from-literal=slackWebHookUri='https://hooks.slack.com/services/.../.../...'
```

```bash
kubectl create secret generic ocs-ostelco-ssl \
  --from-file=certs/dev.ostelco.org/nginx.crt \
  --from-file=certs/dev.ostelco.org/nginx.key
```

```bash
kubectl create secret generic api-ostelco-ssl \
  --from-file=certs/dev.ostelco.org/nginx.crt \
  --from-file=certs/dev.ostelco.org/nginx.key
```

```bash
kubectl create secret generic metrics-ostelco-ssl \
  --from-file=certs/dev.ostelco.org/nginx.crt \
  --from-file=certs/dev.ostelco.org/nginx.key
```

```bash
kubectl create secret generic jumio-secrets --from-literal=apiToken='jumioApiToken' --from-literal=apiSecret='jumioApiSecret'
```

```bash
kubectl create secret generic scaninfo-secrets --from-literal=bucketName='bucketname'
```

```bash
kubectl create secret generic mandrill-secrets --from-literal=mandrillApiKey='keep-mandrill-api-key-here'
```

### Keysets for scan information store

The keys are generated using `Tinkey` tool provied as part of the google tink project
More information can be found here: https://github.com/google/tink/blob/v1.2.2/docs/TINKEY.md

To create a new private key set for testing
```bash
bazel-bin/tools/tinkey/tinkey create-keyset --key-template ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM --out test_keyset_pvt_cltxt
```
 Generate a public key set from the above private keyset.
```bash
bazel-bin/tools/tinkey/tinkey create-public-keyset --in test_keyset_pvt_cltxt --out test_keyset_pub_cltxt
```

The keysets for production (public key only) needs to be encrypted using GCP KMS. More details can be found in docs for `--master-key-uri` option in `tinkey`

- Create Key ring and master key to be used for encrypting the public keys
    ```bash
    gcloud kms keyrings create scan-dev --location global
    gcloud kms keys create scan-info --location global --keyring scan-dev --purpose encryption
    ```

- Set the master key URI for decrypting the keysets.
    ```bash
    kubectl create secret generic scaninfo-keys --from-literal=masterKeyUri='gcp-kms://projects/${GCP_PROJECT_ID}/locations/global/keyRings/scan-dev/cryptoKeys/scan-info'
    ```
- Create keysets for an environment. Use `tinkey` to generate
    ```bash
    bazel-bin/tools/tinkey/tinkey create-keyset --key-template ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM --out clear_encrypt_key_global_pvt
    bazel-bin/tools/tinkey/tinkey create-public-keyset --in clear_encrypt_key_global_pvt --out clear_encrypt_key_global_pub
    bazel-bin/tools/tinkey/tinkey convert-keyset --out encrypt_key_global --in clear_encrypt_key_global_pub \
    --new-master-key-uri gcp-kms://projects/${GCP_PROJECT_ID}/locations/global/keyRings/scan-dev/cryptoKeys/scan-info
    
    bazel-bin/tools/tinkey/tinkey create-keyset --key-template ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM --out clear_encrypt_key_sg_pvt
    bazel-bin/tools/tinkey/tinkey create-public-keyset --in clear_encrypt_key_sg_pvt --out clear_encrypt_key_sg_pub
    bazel-bin/tools/tinkey/tinkey convert-keyset --out encrypt_key_sg --in clear_encrypt_key_sg_pub \
    --new-master-key-uri gcp-kms://projects/${GCP_PROJECT_ID}/locations/global/keyRings/scan-dev/cryptoKeys/scan-info
    ```
- Set the encryption keysets (public keys only) as kubernetes secrets.
    ```bash
    kubectl create secret generic scaninfo-keysets --from-file=./encrypt_key_global --from-file=./encrypt_key_sg --namespace dev
    ```
Prime will use CloudKMS (through tink library) to decrypt the keysets. It requires an IAM role to enable these APIs.
```bash
gcloud projects add-iam-policy-binding ${GCP_PROJECT_ID} --member serviceAccount:prime-service-account@${GCP_PROJECT_ID}.iam.gserviceaccount.com  --role roles/cloudkms.cryptoKeyEncrypterDecrypter
```

### Cloud Pub/Sub

```bash
gcloud pubsub topics create purchase-info
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

python -m grpc_tools.protoc \
  --include_imports \
  --include_source_info \
  --proto_path=analytics-grpc-api/src/main/proto \
  --descriptor_set_out=metrics_descriptor.pb \
  prime_metrics.proto
```

Deploy endpoints

```bash
gcloud endpoints services deploy ocs_descriptor.pb prime/infra/dev/ocs-api.yaml
gcloud endpoints services deploy metrics_descriptor.pb prime/infra/dev/metrics-api.yaml
```

 * Client API HTTP endpoint

```bash
gcloud endpoints services deploy prime/infra/dev/prime-customer-api.yaml
```

## Deploy to Dev cluster

### Deploy monitoring

```bash
kubectl apply -f prime/infra/dev/monitoring.yaml

# If the above command fails on creating clusterroles / clusterbindings you need to add a role to the user you are using to deploy
# You can read more about it here https://github.com/coreos/prometheus-operator/issues/357
kubectl create clusterrolebinding cluster-admin-binding --clusterrole cluster-admin --user $(gcloud config get-value account)

#
kubectl apply -f prime/infra/dev/monitoring-pushgateway.yaml 
```

#### Prometheus dashboard
```bash
kubectl port-forward --namespace=monitoring $(kubectl get pods --namespace=monitoring | grep prometheus-core | awk '{print $1}') 9090
```

#### Grafana dashboard
__`Has own its own load balancer and can be accessed directly. Discuss if this is OK or find and implement a different way of accessing the grafana dashboard.`__

Can be accessed directly from external ip
```bash
kubectl get services --namespace=monitoring | grep grafana | awk '{print $4}'
```

#### Push gateway
```bash
# Push a metric to pushgateway:8080 (specified in the service declaration for pushgateway)
kubectl run curl-it --image=radial/busyboxplus:curl -i --tty --rm
echo "some_metric 4.71" | curl -v  --data-binary @- http://pushgateway:8080/metrics/job/some_job
```

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

## Connect using Neo4j Browser

Check [docs/NEO4J.md](../docs/NEO4J.md)

## Deploy dataflow pipeline for raw_activeusers

```bash
# For dev cluster
gcloud dataflow jobs run active-users-dev \
    --gcs-location gs://dataflow-templates/latest/PubSub_to_BigQuery \
    --region europe-west1 \
    --parameters \
inputTopic=projects/${GCP_PROJECT_ID}/topics/active-users-dev,\
outputTableSpec=${GCP_PROJECT_ID}:ocs_gateway_dev.raw_activeusers


# For production cluster
gcloud dataflow jobs run active-users \
    --gcs-location gs://dataflow-templates/latest/PubSub_to_BigQuery \
    --region europe-west1 \
    --parameters \
inputTopic=projects/${GCP_PROJECT_ID}/topics/active-users,\
outputTableSpec=${GCP_PROJECT_ID}:ocs_gateway.raw_activeusers

```

## Deploy dataflow pipeline for raw_purchases

```bash
# For dev cluster
gcloud dataflow jobs run purchase-records-dev \
    --gcs-location gs://dataflow-templates/latest/PubSub_to_BigQuery \
    --region europe-west1 \
    --parameters \
inputTopic=projects/${GCP_PROJECT_ID}/topics/purchase-info-dev,\
outputTableSpec=${GCP_PROJECT_ID}:purchases_dev.raw_purchases


# For production cluster
gcloud dataflow jobs run purchase-records \
    --gcs-location gs://dataflow-templates/latest/PubSub_to_BigQuery \
    --region europe-west1 \
    --parameters \
inputTopic=projects/${GCP_PROJECT_ID}/topics/purchase-info,\
outputTableSpec=${GCP_PROJECT_ID}:purchases.raw_purchases

```
