// Configure the Google Cloud provider
provider "google" {
  credentials = file("/Users/Christopher/Uni/CSB/keys/csb-benchmark-apachef-71816b8a0e21.json")
  project     = "csb-benchmark-apachef"
  region      = var.instance_region
}

# Virtual Private Network
resource "google_compute_network" "vpc_network" {
  name = "benchmark-network"
}

resource "google_compute_firewall" "basic" {
  name = "benchmark-bacis"
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
      timeout     = "30s"
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
      timeout     = "30s"
    }
  }

  network_interface {
    network = google_compute_network.vpc_network.name

    access_config {
      // Gives the VM an external IP
    }
  }

  depends_on = [ 
    google_compute_network.vpc_network, 
    google_compute_firewall.basic,
  ]
}