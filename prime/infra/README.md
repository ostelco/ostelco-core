# Prime Deployment

## TL;DR

For direct developer deployment for testing, use the [helm-deploy-dev-direct.sh](../script/helm-deploy-dev-direct.sh) shell script. 

To change traffic routing/distribution between prime-direct (in default namespace) and prime (in dev namespace):

> By default, prime direct accepts requests on the same endpoints (with the http header: x-mode=prime-direct) as prime dev which is deployed by CI.

- To set all traffic to prime-direct (without headers)
```bash
./prime/script/change-prime-direct-canary.sh <service name, e.g. api> full-weight
```

- To set all traffic to prime (without headers)
```bash
./prime/script/change-prime-direct-canary.sh <service name, e.g. api> zero-weight
```

- To set traffic with the header `x-mode=prime-direct` to prime direct:
```bash
./prime/script/change-prime-direct-canary.sh <service name, e.g. api> header
```

----- 

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

Run the [prime-direct deploy script](../script/helm-deploy-dev-direct.sh)

-----

### Secrets

 * Place certs at `certs/${API_URI}/`

 * Create k8s secrets

 * Use `-n namespace` in `kubectl create secret` if namespace is not _default_ namespace.

```bash
kubectl create secret generic prime-service-account.json --from-file prime/config/prime-service-account.json
```

Note: To update the secrets defined using yaml, delete and created them again. They are not updated.
 
```bash
kubectl create secret generic stripe-secrets \
  --from-literal=stripeApiKey='keep-stripe-api-key-here' \
  --from-literal=stripeEndpointSecret='keep-stripe-endpoint-secret-here'
```

```bash
kubectl create secret generic slack-secrets --from-literal=slackWebHookUri='https://hooks.slack.com/services/.../.../...'
```

```bash
kubectl create secret generic ocs-ostelco-ssl \
  --from-file=certs/${API_URI}/nginx.crt \
  --from-file=certs/${API_URI}/nginx.key
```

```bash
kubectl create secret generic api-ostelco-ssl \
  --from-file=certs/${API_URI}/nginx.crt \
  --from-file=certs/${API_URI}/nginx.key
```

```bash
kubectl create secret generic metrics-ostelco-ssl \
  --from-file=certs/${API_URI}/nginx.crt \
  --from-file=certs/${API_URI}/nginx.key
```

```bash
kubectl create secret generic jumio-secrets \
  --from-literal=apiToken='jumioApiToken' \
  --from-literal=apiSecret='jumioApiSecret'
```

```bash
kubectl create secret generic myinfo-secrets \
  --from-literal=apiClientId='myInfoApiClientId' \
  --from-literal=apiClientSecret='myInfoApiClientSecret' \
  --from-literal=serverPublicKey='myInfoServerPublicKey' \
  --from-literal=clientPrivateKey='myInfoClientPrivateKey'
```

```bash
kubectl create secret generic scaninfo-secrets --from-literal=bucketName='bucketname'
```

```bash
kubectl create secret generic mandrill-secrets --from-literal=mandrillApiKey='keep-mandrill-api-key-here'
```

```bash
kubectl create secret generic apple-id-auth-secrets \
  --from-literal=teamId='teamId' \
  --from-literal=keyId='keyId' \
  --from-literal=clientId='clientId' \
  --from-literal=privateKey='privateKey'
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
gcloud pubsub topics create active-users
gcloud pubsub topics create data-traffic
gcloud pubsub topics create purchase-info

gcloud pubsub topics create stripe-event

gcloud pubsub topics create ocs-ccr
gcloud pubsub topics create ocs-cca
gcloud pubsub topics create ocs-activate


gcloud pubsub subscriptions create stripe-event-store-sub --topic=stripe-event --topic-project=${GCP_PROJECT_ID}
gcloud pubsub subscriptions create stripe-event-report-sub --topic=stripe-event --topic-project=${GCP_PROJECT_ID}
gcloud pubsub subscriptions create stripe-event-recurring-payment-sub --topic=stripe-event --topic-project=${GCP_PROJECT_ID}

gcloud pubsub subscriptions create ocs-ccr-sub --topic=ocs-ccr --topic-project=${GCP_PROJECT_ID}
gcloud pubsub subscriptions create ocsgw-cca-sub --topic=ocs-cca --topic-project=${GCP_PROJECT_ID}
gcloud pubsub subscriptions create ocsgw-activate-sub --topic=ocs-activate --topic-project=${GCP_PROJECT_ID}
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
