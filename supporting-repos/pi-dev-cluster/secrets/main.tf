# This script is no longer used. Secrets are managed manually.

variable "cluster_endpoint" {
  description = "cluster endpoint without https://"
}

variable "cluster_admin_password" {
  description = "cluster admin password"
}

variable "keys_dir" {
  description = "the root path to the directory containing cluster keys"
}

variable "namespace" {
  description = "the namespace to be create and where secrets will be placed."
  default = "dev"
}

variable "jumio_api_secret" {
  
}

variable "jumio_api_token" {
  
}

variable "stripe_api_secret" {
  
}

variable "stripe_api_key" {
  
}

variable "slack_webhook_uri" {
  
}

variable "scaninfo_bucket_name" {
  
}

variable "scaninfo_master_key_uri" {
  
}


variable "prime-sa-key-path" {
  
}

variable "dns-sa-key-path" {
  
}

provider "kubernetes" {
  host = "https://${var.cluster_endpoint}"

  username = "admin"
  password = "${var.cluster_admin_password}"

  client_certificate     = "${file("${var.keys_dir}/dev_cluster_client_certificate.crt")}"
  client_key             = "${file("${var.keys_dir}/dev_cluster_client_key.key")}"
  cluster_ca_certificate = "${file("${var.keys_dir}/dev_cluster_cluster_ca.crt")}"
}

resource "kubernetes_namespace" "namespace" {
  metadata {
    labels {
      created_by = "terraform"
    }

    name = "${var.namespace}"
  }
}

resource "kubernetes_secret" "jumio-secrets" {
  metadata {
    name = "jumio-secrets"
    namespace = "${var.namespace}"
    labels = {
      created_by = "terraform"
    }
  }

  data {
    apiSecret = "${var.jumio_api_secret}" 
    apiToken = "${var.jumio_api_token}" 
  }
 depends_on = ["kubernetes_namespace.namespace"]
}

resource "kubernetes_secret" "stripe-secrets" {
  metadata {
    name = "stripe-secrets"
    namespace = "${var.namespace}"
    labels = {
      created_by = "terraform"
    }
  }

  data {
    stripeApiKey = "${var.stripe_api_key}"
    stripeEndpointSecret = "${var.stripe_api_secret}"
  }
  depends_on = ["kubernetes_namespace.namespace"]
}

resource "kubernetes_secret" "slack-secrets" {
  metadata {
    name = "slack-secrets"
    namespace = "${var.namespace}"
    labels = {
      created_by = "terraform"
    }
  }

  data {
    slackWebHookUri = "${var.slack_webhook_uri}" 
  }
  depends_on = ["kubernetes_namespace.namespace"]
}

resource "kubernetes_secret" "scaninfo-secrets" {
  metadata {
    name = "scaninfo-secrets"
    namespace = "${var.namespace}"
    labels = {
      created_by = "terraform"
    }
  }

  data {
    bucketName = "${var.scaninfo_bucket_name}"
  }
  depends_on = ["kubernetes_namespace.namespace"]
}

resource "kubernetes_secret" "scaninfo-keys" {
  metadata {
    name = "scaninfo-keys"
    namespace = "${var.namespace}"
    labels = {
      created_by = "terraform"
    }
  }

  data {
    masterKeyUri = "${var.scaninfo_master_key_uri}"
  }
  depends_on = ["kubernetes_namespace.namespace"]
}

resource "kubernetes_secret" "prime-sa-key" {
  metadata {
    name = "prime-sa-key"
    namespace = "${var.namespace}"
    labels = {
      created_by = "terraform"
    }
  }

  data {
    prime-service-account.json = "${file("${var.prime-sa-key-path}")}"
  }
  depends_on = ["kubernetes_namespace.namespace"]
}

resource "kubernetes_secret" "dns-sa-key" {
  metadata {
    name = "clouddns-svc-acct-key"
    namespace = "kube-system"
    labels = {
      created_by = "terraform"
    }
  }

  data {
    service-account.json = "${file("${var.dns-sa-key-path}")}"
  }
}

# the backend config for storing terraform state in GCS 
# requires setting GOOGLE_CREDNETIALS to contain the path to your Google Cloud service account json key.
terraform {
  backend "gcs" {
    bucket = "pi-ostelco-dev-terraform-state"
    prefix = "secrets/dev/state"
  }
}