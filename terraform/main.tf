// Configure the Google Cloud provider
provider "google" {
  credentials = file("/Users/Christopher/Uni/CSB/keys/csb-benchmark-apachef-71816b8a0e21.json")
  project     = "csb-benchmark-apachef"
  region      = var.instance_region
}

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
  location = var.instance_region
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
  node_count = 1

  node_config {
    preemptible  = true
    machine_type = var.gke_machine_type

    # Google recommends custom service accounts that have cloud-platform scope and permissions granted via IAM Roles.
    # service_account = google_service_account.default.email
    oauth_scopes    = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]
  }
}

// A single Compute Engine instance
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

  // Add startup script to instance with variables.
  metadata_startup_script = templatefile("${path.module}/startup.tpl", { 
    endpoint = "",
    ip_addrs = ["10.0.0.1", "10.0.0.2"] 
  })

  metadata = {
    ssh-keys = "${var.ssh_user}:${file(var.ssh_key_path_public)}"
    serial-port-enable = true
  }

  // Copy benchmark client application to instance
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

  // Copy config file for application (could also be generated in main dir)
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
      // Gives the VM an external IP
    }
  }

  # depends_on = [ 
  #   google_compute_network.vpc_network,
  #   google_compute_subnetwork.benchmark_subnet,
  #   google_compute_firewall.basic,
  # ]
}