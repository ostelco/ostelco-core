
variable "project_id" {
  description = "Google Cloud project ID."
  default = "pi-ostelco-dev"
}

variable "regional" {
  description = "whether the cluster should be created in multiple zones or not."
  default = true
}

variable "cluster_region" {
  default     = "europe-west1"
  description = "The region where the cluster will be created."
}

variable "cluster_zones" {
  default     = ["europe-west1-b", "europe-west1-c" , "europe-west1-d"]
  description = "The zone(s) where the cluster will be created."
}

variable "cluster_admin_password" {
  description = "password for cluster admin. Must be 16 characters at least."
}

# Configure the Google Cloud provider
provider "google-beta" {
  project = "${var.project_id}"
  region  = "${var.cluster_region}"
}

module "gke" {
  source              = "github.com/ostelco/ostelco-terraform-modules//terraform-google-gke-cluster"
  project_id            = "${var.project_id}"
  cluster_password      = "${var.cluster_admin_password}"
  cluster_name          = "pi-dev"
  cluster_description   = "Development cluster for Ostelco Pi."
  cluster_version       = "1.13.6-gke.6"
  cluster_zones         = "${var.cluster_zones}"
  regional              = "${var.regional}"

}

module "prime-nodes" {
  source         = "github.com/ostelco/ostelco-terraform-modules//terraform-google-gke-node-pool"
  project_id     = "${var.project_id}"
  regional       = "${var.regional}"
  cluster_name   = "${module.gke.cluster_name}" # creates implicit dependency
  cluster_region = "${var.cluster_region}"
  
  node_pool_name = "prime-nodes"
  pool_min_node_count    = "1"
  initial_node_pool_size = "1"
  pool_max_node_count    = "3"
  node_tags              = ["dev", "prime"]
  auto_upgrade           = true
  pool_node_machine_type = "n1-standard-1"

  node_labels = {
    "target"         = "prime"
    "machineType" = "n1-standard-1"
    "env"         = "dev"
  }
  
  # oauth_scopes define what Google API nodes in the pool have access to.
  # list of APIs can be found here: https://developers.google.com/identity/protocols/googlescopes
  oauth_scopes = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_write",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/service.management",
      "https://www.googleapis.com/auth/pubsub",
      "https://www.googleapis.com/auth/datastore",
      "https://www.googleapis.com/auth/bigquery",
      "https://www.googleapis.com/auth/sqlservice.admin",
      "https://www.googleapis.com/auth/ndev.clouddns.readwrite", 
      "https://www.googleapis.com/auth/servicecontrol",
    ]


}

module "neo4j-nodes" {
  source         = "github.com/ostelco/ostelco-terraform-modules//terraform-google-gke-node-pool"
  project_id     = "${var.project_id}"
  regional       = "${var.regional}"
  cluster_name   = "${module.gke.cluster_name}" # creates implicit dependency
  cluster_region = "${var.cluster_region}"
  
  node_pool_name = "neo4j-nodes"
  pool_min_node_count    = "1"
  initial_node_pool_size = "1"
  pool_max_node_count    = "3"
  node_tags              = ["dev", "neo4j"]
  auto_upgrade           = true
  pool_node_machine_type = "n1-standard-2"

  node_labels = {
    "target"         = "neo4j"
    "machineType" = "n1-standard-2"
    "env"         = "dev"
  }
  
  # oauth_scopes define what Google API nodes in the pool have access to.
  # list of APIs can be found here: https://developers.google.com/identity/protocols/googlescopes
  oauth_scopes = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_write",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/service.management",
      "https://www.googleapis.com/auth/pubsub",
      "https://www.googleapis.com/auth/datastore",
      "https://www.googleapis.com/auth/bigquery",
      "https://www.googleapis.com/auth/sqlservice.admin",
      "https://www.googleapis.com/auth/ndev.clouddns.readwrite", 
      "https://www.googleapis.com/auth/servicecontrol",
    ]


}


module "utilities-nodes" {
  source         = "github.com/ostelco/ostelco-terraform-modules//terraform-google-gke-node-pool"
  project_id     = "${var.project_id}"
  regional       = "${var.regional}"
  cluster_name   = "${module.gke.cluster_name}" # creates implicit dependency
  cluster_region = "${var.cluster_region}"
  
  node_pool_name = "utilities-nodes"
  pool_min_node_count    = "1"
  initial_node_pool_size = "1"
  pool_max_node_count    = "3"
  node_tags              = ["dev", "utilities"]
  auto_upgrade           = true
  pool_node_machine_type = "n1-standard-1"

  node_labels = {
    "target"         = "utilities"
    "machineType" = "n1-standard-1"
    "env"         = "dev"
  }
  
  # oauth_scopes define what Google API nodes in the pool have access to.
  # list of APIs can be found here: https://developers.google.com/identity/protocols/googlescopes
  oauth_scopes = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_write",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/service.management",
    ]


}

resource "google_compute_address" "static_ambassador_ip" {
  provider = "google-beta"
  name = "ambassador-static-ip"
  description = "A static external IP for dev Ambassador LB"
}

output "dev_cluster_ambassador_ip" {
  sensitive = true
  value = "${google_compute_address.static_ambassador_ip.address}"
}


output "dev_cluster_endpoint" {
  sensitive = true
  value = "${module.gke.cluster_endpoint}"
}

output "dev_cluster_client_certificate" {
  sensitive = true
  value = "${module.gke.cluster_client_certificate}"
}

output "dev_cluster_client_key" {
  sensitive = true
  value = "${module.gke.cluster_client_key}"
}

output "dev_cluster_ca_certificate" {
  sensitive = true
  value = "${module.gke.cluster_ca_certificate}"
}

# the backend config for storing terraform state in GCS 
# requires setting GOOGLE_CREDNETIALS to contain the path to your Google Cloud service account json key.
terraform {
  backend "gcs" {
    bucket = "pi-ostelco-dev-terraform-state"
    prefix = "clusters/dev/state"
  }
}
