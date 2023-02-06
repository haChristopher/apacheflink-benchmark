#!/bin/bash

# Loading Config File
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "config.sh"

####### Deploy Cluster, Kafka, Flink #######
cd terraform
# Create Cluster
terraform apply -target=google_container_node_pool.kafka_node_pool -auto-approve

# Deploy Kafka
terraform apply -target=kubectl_manifest.kafka_deplyoment -auto-approve
cd ..

# Deploy Flink
./scripts/deploy-flink.sh


####### Apache Flink Job Deployment #######

# Export Java Home for job graph parsing
export JAVA_HOME='/Library/Java/JavaVirtualMachines/jdk-11.0.16.1.jdk/Contents/Home'

# Job Deployment
# ./bin/flink run -m localhost:8081 /Users/christopher/Uni/repos/apacheflink-benchmark/benchmark-flink/build/libs/benchmark-flink-0.1-SNAPSHOT-all.jar




# Apply configuration
python3 scripts/setup_configurations.py



###### Build pipelines and client jars #######
# cd benchmark-client
# gradle build
# cd ..

# Use env here or config var
# gradle shadowJar -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-11.0.16.1.jdk/Contents/Home


####### Benchmark Clients Deployment #######

# Terraform
# mkdir -p ssh
# ssh-keygen -b 2048 -t rsa -q -N "" -C provisioner -f ssh/client-key

# Setup terraform and move data and config files to instances
cd terraform
# terraform apply -auto-approve
terraform apply -target=google_container_node_pool.kafka_node_pool -auto-approve
terraform apply -target=kubectl_manifest.kafka_deplyoment -auto-approve


gcloud container clusters get-credentials "benchmark-cluster" -z=europe-west3-c

terraform apply -target=kubectl_manifest.flink_deplyoment -auto-approve



terraform apply -target=google_compute_instance.clients -auto-approve

# terraform output -json instance_ips | jq -r '.[0]'

# Save terraform output to file to get instance ips
# terraform output -json > file.txt
# cd ..

# Running terraform and setting variables
# terraform apply -var="instance_count=1" -var=instance_type="e2-medium"
