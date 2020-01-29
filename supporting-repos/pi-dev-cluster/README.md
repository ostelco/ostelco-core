# Google GKE DEV cluster

[![CircleCI](https://circleci.com/gh/ostelco/infra/tree/master.svg?style=svg&circle-token=73f413df2d44cad888b45fe96d7a9d8f6898fc02)](https://circleci.com/gh/ostelco/infra/tree/dev)

Terraform config and Circleci pipeline to build and maintain Kubernetes cluster

## Pipeline docs

The following docs are available:
- [circleci-pipeline](docs/circleci-pipeline.md)
- [terraform-scripts](docs/terraform-scripts.md)


## Notes:

- The Circleci pipeline runs a plan step, waits for human approval before it applies changes.
- Some changes may cause a cluster recreation. **Inspect** the plan step results before you approve applying the changes.
- While the Terraform config can be run from any machine. It's **strongly discouraged** to do so to avoid Terraform state locking. 
  - Should a locking situation occur, then the solution would be to look in the [terraform state bucket](https://console.cloud.google.com/storage/browser/pi-ostelco-dev-terraform-state/clusters/dev/state/?project=pi-ostelco-dev&authuser=2&organizationId=7215087637) and delete the lock file.
- Another reason for not using terraform locally is that there could be version differences between the terraform running on your workstation and the one running on the CI.  These different versions use different formats in their state files, so running both towards the same state (in the same bucket) will cause inconsistencies and therefore failure.
- How to update terraform in the CI: Change the  [Dockerfile in .circleci](.circleci/Dockerfile) dirctory.  Update the terraform version and the tagname (which should reflect the version of terraform). Building the docker image needs to be done on a workstation and then pushed to the repository.  
- To run terraform script from your machine (discouraged, see above :-):
   - Get the service account keys from the [bucket](https://console.cloud.google.com/storage/browser/pi-ostelco-dev-service-accounts-keys?project=pi-ostelco-dev&authuser=2&organizationId=7215087637) it is stored (or generate a new one and put it where you like )
- To run the script it is necessary to set a few passwords as parameters to terraform using the -var option
- To activate the cluster locally, use the command gcloud auth activate-service-account --key-file /tmp/credentials.json, 
  using the path to an actual credentials file.
- To run from workstation, it is also necessary to set environment variable:
     export GOOGLE_CREDENTIALS=/tmp/credentials.json  
- When running terraform plan, you must either provide a password on the command or be prompted for one. The password itself can be found in the kubernetes engine config panel under the "cluster credentials" popup
- When making an upgrade, it is probably beneficial to have spare nodes.  The reason is that Kubernetes will try to move pods to spare nodes before shutting down old nodes.  If it can't do that, the upgrade may hang, or make the service unavailable during upgrade.


## TODO:

- It is possible to set the google endpoints using terraform's [google endpoints service](https://www.terraform.io/docs/providers/google/r/endpoints_service.html).   This however poses a questions: How to get the compiled descriptor file for the swagger and/or gRPC to the terraform script.  This is problematic since terraform is considered infrastructure, but the swagger/grpc specifiation is very much a part of the services we develop (prime, in particular).  This means we need to find some way for these two pieces to communicate.  On way of doing it would be to push the .pb file to a bucket and picking it up from there.
- 