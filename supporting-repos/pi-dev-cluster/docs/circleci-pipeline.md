# Circleci Pipeline

The pipeline manages the GKE clusters for the **DEV** environment.

## Google Projects

For complete separation and better access control, the **DEV** and **PROD** clusters are created in two different Google cloud projects. Namely, `pi-ostelco-dev` & `pi-ostelco-prod`.

The production cluster (and it's project) will only be accessible by admins or other team members who are delegated to cover for admins.
The production cluster is managed in a separate [repo](https://github.com/ostelco/pi-prod-cluster).

## Circleci config

Circleci [config](../.circleci/config.yml) is available under `.circleci/`

### the pipeline steps

The steps are the same for both the **DEV** & **PROD** clusters. 

`run terraform plan` --> `wait for a manual approval of the plan in circleci` --> `if approved, apply the changes with terraform plan and copy/update cluster certificates and keys to a Google bucket` --> `create/update secrets in the cluster using terraform` --> `deploy utilities in the cluster using helmsman`


### circleci docker images

Pipeline related docker images are served from a publicly accessible Google container registry deployed in the `pi-ostelco-prod` project. These images are tool images and should contain no secrets.

#### terraform-gcloud 
The terraform jobs in the circleci pipeline needs the following tools:

- Terraform
- gcloud

It is created and pushed to a public Google Container Registry (GCR) manually as `eu.gcr.io/pi-ostelco-prod/terraform-gcloud:11.7` where `11.7` is the Terraform version.  It is created with a [dockerfile](../.circleci/Dockerfile) and has an [entrypoint script](../.circleci/docker-entrypoint.sh) which reads Google credentials from environment variables, stores it inside the container in `/tmp/credentials.json` and authenticates to Google Cloud using it. 

### other CI/CD scripts

- [store_cluster_certs.sh](../.circleci/store_cluster_certs.sh) : copies the cluster certificates and keys which are retrieved by Terraform output to a Google cloud storage bucket.

### Utility apps
Deploying utility apps (cert-manager, ambassador ...etc) is done using [helmsman](https://github.com/Praqma/helmsman). The docker image used is the public image from dockerhub. 
These apps are considered part of the infrastructure and are deployed from code (using their official helm charts) as part of the circleci pipeline. The [desired state for deploying them](../.circleci/helmsman-dev-utilities.toml) can be changed to update certain apps or add additional utilities. Application specific configurations (helm values files) can be found in [apps](../.circleci/apps/) subdirectory.

> To update/upgrade certain utility apps, you need to make sure they exist and are editable in the [desired state file](../.circleci/helmsman-dev-utilities.toml) by setting `protected = false` for the specific app and then edit the app specific values file in [apps/](../.circleci/apps/)


### circleci environment variables

##### Cluster-specific env vars

| variable                     | description                                                                                                                                                                                                  | optional |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| PI_DEV_GOOGLE_CREDENTIALS    | Google cloud service account credentials from the **pi-ostelco-dev** project which has the following permissions: Compute Instance Admin (v1), Kubernetes Engine Admin, Service Account User, Storage Object Admin  | No       |
| DEV_CLUSTER_PASSWORD         | the admin password for the DEV cluster                                                                                                                                                                      | No       |
| PI_DEV_K8S_KEY_STORE_BUCKET  | The bucket for storing DEV cluster certificates and keys. The format is: `gs://bucket-name`. The bucket should pre-exist in the **pi-ostelco-dev** project. If not specified, the default is: `gs://pi-ostelco-dev-k8s-key-store`                    | Yes      |

##### K8S Secrets

> UPDATE: secrets are no longer part of the CI pipeline. They are managed manually!

These are secrets create by terraform in the cluster. This makes a newly created cluster ready to deploy Prime out of the box with all the required secrets being populated. See the [terraform script for creating secrets]((../secrets/main.tf)) for details about what is created.

> When new secrets are needed, the [terraform script](../secrets/main.tf) needs to be adjusted to add them and then their values need to be added as env vars.

Secrets from accessing Google Cloud (e.g DNS credentials needed for letsencrypt) are fetched from a bucket during the pipeline execution and also created/managed using the terraform script.

| variable                     | description                                                                                                                                                                                                  | optional |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| NAMESPACE    | The namespace where you want the PRIME secrets to be created. Default is `dev`  | Yes       |
| JUMIO_API_SECRET    | JUMIO API SECRET for Prime.  | No       |
| JUMIO_API_TOKEN    | JUMIO API TOKEN for Prime.  | No       |
| SCANINFO_BUCKET_NAME    |  SCANINFO BUCKET NAME for Prime.  | No       |
| SCANINFO_MASTER_KEY_URI    | SCANINFO MASTER KEY URI for Prime.  | No       |
| SLACK_WEBHOOK_URI    | SLACK WEBHOOK URI for Prime.  | No       |
| STRIPE_API_KEY    | STRIPE API KEY for Prime.  | No       |
| STRIPE_API_SECRET    | STRIPE API SECRET for Prime.  | No       |

##### App-specific secrets
 These are secrets for specific utility apps. They are passed to the relevant helm chart using [helmsman desired state file](../.circleci/helmsman-dev-utilities.toml)

 | variable                     | description                                                                                                                                                                                                  | optional |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| GRAFANA_ADMIN_PASSWORD    | admin password for grafana  | No       |