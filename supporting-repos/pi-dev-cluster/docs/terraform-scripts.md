# Terraform scripts

## Terraform Modules

The Terraform scripts in this repo use Terraform modules to create the **DEV** and the **PROD** clusters. The two modules used are:

- [terraform-google-gke-cluster](https://github.com/ostelco/ostelco-terraform-modules/tree/master/terraform-google-gke-cluster) : launches a cluster and deletes its default node pool.
- [terraform-google-gke-node-pool](https://github.com/ostelco/ostelco-terraform-modules/tree/master/terraform-google-gke-node-pool) :  launches new node pools and associate them to existing clusters.

An [example](https://github.com/ostelco/ostelco-terraform-modules/blob/master/example/zonal.tf) of using the modules together is available. The input variables for each of the modules are listed in their README files ([cluster](https://github.com/ostelco/ostelco-terraform-modules/tree/master/terraform-google-gke-cluster) , [node pools](https://github.com/ostelco/ostelco-terraform-modules/tree/master/terraform-google-gke-node-pool)). 

## Terraform scripts

The **DEV** cluster terraform config is available in a file called `dev-cluster.tf`. 

> The TF file name can be changed to anything else. It also can be split to multiple files (e.g, variables.tf & outputs.tf & main.tf)

The **secrets** config is available in [secrets/main.tf](../secrets/main.tf).

## Terraform state

The terraform state files are stored in Google buckets. 

- For the **DEV** cluster: it is stored in a bucket called: `pi-ostelco-dev-terraform-state/clusters/dev/state` in the `pi-ostelco-dev` project.
- The **secrets** terraform state is stored in `pi-ostelco-dev-terraform-state/secrets/dev/state` in the `pi-ostelco-dev` project.

