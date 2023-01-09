output "instance_ips" {
  // get all the ip addresses
  value = google_compute_instance.clients.*.network_interface.0.access_config.0.nat_ip
}