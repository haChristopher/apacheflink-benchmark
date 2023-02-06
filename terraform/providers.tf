terraform {
  required_version = ">= 0.13"
  required_providers {
    kubectl = {
      source  = "gavinbunney/kubectl"
      version = ">= 1.10.0"
    }
  }
}

data "google_client_config" "provider" {}

provider "google" {
  credentials = file("/Users/Christopher/Uni/CSB/keys/csb-benchmark-apachef-71816b8a0e21.json")
  project     = var.project_id
  region      = var.instance_region
}

provider kubernetes {
  cluster_ca_certificate  = base64decode(google_container_cluster.kafka_cluster.master_auth.0.cluster_ca_certificate)
  host                    = "https://${google_container_cluster.kafka_cluster.endpoint}"
  token                   = data.google_client_config.provider.access_token
}

provider kubectl {
  cluster_ca_certificate  = base64decode(google_container_cluster.kafka_cluster.master_auth.0.cluster_ca_certificate)
  host                    = "https://${google_container_cluster.kafka_cluster.endpoint}"
  token                   = data.google_client_config.provider.access_token
  load_config_file        = false
}