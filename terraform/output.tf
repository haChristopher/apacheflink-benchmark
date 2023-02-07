output "instance_ips_producer" {
  value = google_compute_instance.client_producer.*.network_interface.0.access_config.0.nat_ip
}

output "instance_ips_consumers" {
  value = google_compute_instance.client_consumer.*.network_interface.0.access_config.0.nat_ip
}