# Virtual Private Network
resource "google_compute_network" "vpc_network" {
  name = "benchmark-network"
  auto_create_subnetworks = false
}

# Subnet for VPC
resource "google_compute_subnetwork" "benchmark_subnet" {
  name    = var.subnet_name
  network = google_compute_network.vpc_network.name
  region  = var.region
  ip_cidr_range = "172.16.0.0/16"
}

# Firewall settings
resource "google_compute_firewall" "basic" {
  name = "benchmark-firewall"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "8080", "1000-2000", "22"]
  }
  
  allow {
    protocol = "icmp"
  }

  target_tags = ["benchmark-vm-instance"]
  source_ranges = ["0.0.0.0/0"]
  depends_on = [google_compute_network.vpc_network]
}

resource "google_container_cluster" "kafka_cluster" {
  name     = "benchmark-cluster"
  location = var.instance_region # "europe-west3"
  network = google_compute_network.vpc_network.name
  subnetwork = google_compute_subnetwork.benchmark_subnet.name

  # We can't create a cluster with no node pool defined, but we want to only use
  # separately managed node pools. So we create the smallest possible default
  # node pool and immediately delete it.
  remove_default_node_pool = true
  initial_node_count       = 1
}

resource "google_container_node_pool" "kafka_node_pool" {
  name       = "kafka-node-pool"
  location   = var.instance_region
  cluster    = google_container_cluster.kafka_cluster.name
  node_count = 3

  # autoscaling {
  #   max_node_count = 6
  #   min_node_count = 2
  # }

  node_config {
    # preemptible  = true
    machine_type = "e2-medium" #var.gke_machine_type

    # Google recommends custom service accounts that have cloud-platform scope and permissions granted via IAM Roles.
    # service_account = google_service_account.default.email
    oauth_scopes    = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]
  }
}

############### KAFKA SETUP ################

resource "kubernetes_namespace" "kafka" {
  metadata {
    name = "kafka-cluster"
  }
}

data "kubectl_path_documents" "kafka" {
  pattern = "../infrastructure/kafka/*.yaml"
}

resource "kubectl_manifest" "kafka_deplyoment" {
  for_each  = toset(data.kubectl_path_documents.kafka.documents)
  yaml_body = each.value
  wait_for_rollout = true
  override_namespace = kubernetes_namespace.kafka.metadata[0].name

  depends_on = [
    google_container_cluster.kafka_cluster,
    kubernetes_namespace.kafka,
    google_container_node_pool.kafka_node_pool
  ]
}

############### Apache Flink SETUP ################
# This Approach was not working, because of yaml formatting issues with kubectl_manifest
# and the |+ literal scalar format. For now I only use this this to create the namespace
# and the rest is done using kubectl directly.

resource "kubernetes_namespace" "flink" {
  metadata {
    name = "flink-namespace"
  }
}

# data "kubectl_path_documents" "flink" {
#   pattern = "../infrastructure/flink/*.yaml"
# }

# resource "kubectl_manifest" "flink_deplyoment" {
#   for_each  = toset(data.kubectl_path_documents.flink.documents)
#   yaml_body = each.value
#   wait_for_rollout = true
#   override_namespace = kubernetes_namespace.flink.metadata[0].name

#   depends_on = [
#     google_container_cluster.kafka_cluster,
#     kubernetes_namespace.flink,
#     google_container_node_pool.kafka_node_pool
#   ]
# }


############### BENCHMARK CLIENT SETUP ################

# A single Compute Engine instance
resource "google_compute_instance" "clients" {
  name                      = "benchmark-client-vm-${count.index}"
  machine_type              = var.instance_type
  zone                      = var.instance_region
  count                     = var.instance_count
  allow_stopping_for_update = "false"
  tags         = ["benchmark-vm-instance"]

  boot_disk {
    initialize_params {
      image = "ubuntu-2204-jammy-v20221101a"
	    size = var.instance_disk_size
    }
  }

  # Add startup script to instance with variables.
  metadata_startup_script = templatefile("${path.module}/startup.tpl", { 
    endpoint = "",
    ip_addrs = ["10.0.0.1", "10.0.0.2"] 
  })

  metadata = {
    ssh-keys = "${var.ssh_user}:${file(var.ssh_key_path_public)}"
    serial-port-enable = true
  }

  # Copy benchmark client application to instance
  provisioner "file" {
    source      = "../benchmark-client/app/build/distributions/app.zip"
    destination = "app.zip"

    connection {
      type        = "ssh"
      host        = self.network_interface.0.access_config.0.nat_ip
      user        = var.ssh_user
      private_key = file(var.ssh_key_path_private)
      agent       = "false"
      timeout     = "60s"
    }
  }

  # Copy config file for application (could also be generated in main dir)
  provisioner "file" {
    source      = "../benchmark-client/app/config.properties"
    destination = "config.properties"

    connection {
      type        = "ssh"
      host        = self.network_interface.0.access_config.0.nat_ip
      user        = var.ssh_user
      private_key = file(var.ssh_key_path_private)
      agent       = "false"
      timeout     = "60s"
    }
  }

  network_interface {
    network = google_compute_network.vpc_network.name
    subnetwork = google_compute_subnetwork.benchmark_subnet.name

    access_config {
      # Gives the VM an external IP
    }
  }

  depends_on = [ 
    google_compute_network.vpc_network,
    google_compute_subnetwork.benchmark_subnet,
    google_compute_firewall.basic,
  ]
}