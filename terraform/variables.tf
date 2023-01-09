variable "instance_count" {
  type        = number
  description = "Number of benchmarking client instances"
  default     = 1
}

variable "instance_type" {
  type        = string
  description = "Type of benchmarking client instances"
  default     = "e2-micro"
}

variable "instance_disk_size" {
  type        = number
  description = "Size of instance disk in GB"
  default     = 40
}

variable "instance_region" {
  type        = string
  description = "Region of benchmarking client instances"
  default     = "europe-west3-a"
}

variable "benchmark_client_jar" {
  type        = string
  description = "Name of jar to deploy."
  default     = "app"
}

variable "ssh_user" {
  type        = string
  description = "User for ssh access."
  default     = "provisioner"
}

variable "ssh_key_path_private" {
  type        = string
  description = "Path to private SSH key"
  default     = "../ssh/client-key"
}

variable "ssh_key_path_public" {
  type        = string
  description = "Path to public SSH key"
  default     = "../ssh/client-key.pub"
}