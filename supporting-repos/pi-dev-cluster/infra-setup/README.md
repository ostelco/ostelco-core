# Infra setup on a new Google Cloud project

Before Prime can be deployed into a GKE Kubernetes cluster, it requires certain dependencies to exist. The deployment pipelines also need certain credentials and service accounts to exist. These dependencies can be created manually, but to make the process reproducible a script is needed.

## Why a script?

Both Google Deployment Manager and Terraform have been tested to do this task but they fell short of accomplishing all the steps.  Google Deployment Manager does not have support for all Google resources (relevant to this setup, it does not support cloud endpoints for instance). Terraform has a broader support (since it is open source and the community chips in) than Google Deployment Manager. However, certain steps in the setup require launching python commands and/or java executables. Additionally, certain Google Cloud services (e.g. Datastore ) don't have much public APIs (see [here](https://github.com/terraform-providers/terraform-provider-google/issues/1253) for instance).

Because of the above reasons, using Google Deployment Manager or Terraform would require additional scripts/manual steps to complement it. A script is used to simplify the steps needed for a first time setup.

## Why docker image?

The setup script requires certain tools to be available. Instead of installing these tools on a developer machine and to avoid the `works-on-my-machine` problem, the script is built into a docker image that has all the necessary tools to accomplish the needed tasks.

## What's created?


The script creates the following resources:
- Google storage buckets [for prime's data storage and also for other purposes like terraform state storage for the GKE clusters ]
- Create service accounts, grant them certain permissions and generate key files from those service accounts and store them in buckets 
- Pub/sub topics 
- Generate and deploy cloud endpoint files
- Deploy dataflows

## Prerequisites 

- The user running the script must have project owner permissions.
- The user running the script must be added as a verified owner in Google Webmaster Central. See [here](https://cloud.google.com/endpoints/docs/openapi/verify-domain-name) for details.

## Usage

1) create a new Google Cloud project
2) invite user(s) and give them the right access permission
3) a user with `project owner` permissions can then run the following command: 

```
docker run --rm -it \
--env PROJECT=<project id> \
--env DATAFLOW_REGION=<Google cloud region for dataflows> \
eu.gcr.io/pi-ostelco-dev/infra 
```

The user will be prompted to login to his/her google cloud account. This is needed to authorize creating endpoints and other resources. 