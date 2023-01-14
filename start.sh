#!/bin/bash

# Loading Config File
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "config.sh"


## Create Cluster
./scripts/setup-cluster.sh

####### Kafka Deployment #######
./scripts/deploy-kafka.sh


####### Apache Flink Deployment #######

# Job Deployment
# ./bin/flink run -m localhost:8081 /Users/christopher/Uni/repos/apacheflink-benchmark/benchmark-flink/build/libs/benchmark-flink-0.1-SNAPSHOT.jar



###### Build pipelines and client jars #######
# cd benchmark-client
# gradle build
# cd ..


####### Benchmark Clients Deployment #######

# Terraform
mkdir -p ssh
ssh-keygen -b 2048 -t rsa -q -N "" -C provisioner -f ssh/client-key

# Setup terraform and move data and config files to instances
cd terraform
terraform apply -auto-approve
terraform output -json instance_ips | jq -r '.[0]'
cd ..

# Running terraform and setting variables
# terraform apply -var="instance_count=1" -var=instance_type="e2-medium"
