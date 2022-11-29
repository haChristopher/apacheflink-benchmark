// Configure the Google Cloud provider
provider "google" {
  credentials = file("/Users/Christopher/Uni/CSB/csb-kubeless-benchmark-d4a5ced18bab.json")
  project     = "csb-apacheflink-benchmark"
  region      = var.instance_region
}
