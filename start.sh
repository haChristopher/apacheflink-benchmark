#!/bin/bash

############# Loading Configuration File #########################
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "config.sh"

############# Deploy Cluster, Kafka, Flink #######################
cd terraform
terraform apply -target=google_container_node_pool.kafka_node_pool -auto-approve
terraform apply -target=google_container_node_pool.flink_node_pool -auto-approve
sleep 10
cd ..

# Retrieve kubernetes credentials for kubectl
gcloud container clusters get-credentials "kafka-cluster" -z=europe-west3-c

# Deploy Kafka Service
./scripts/deploy-kafka.sh

kafka_ip=$(kubectl get svc kafka2 -n kafka-cluster -o jsonpath="{.status.loadBalancer.ingress[*].ip}")
export KAFKA_IP=$kafka_ip

# # # Deploy Flink
gcloud container clusters get-credentials "flink-cluster" -z=europe-west3-c
./scripts/deploy-flink.sh

# ####### Apache Flink Job Deployment #######

# create copy of flink properties
cd benchmark-flink/src/main/resources/
cp config.properties.template config.properties

# replace with environment variable
sed -i '' -e "s/{{KAFKA_IP_PLACE}}/$KAFKA_IP/g" config.properties
sed -i '' -e "s/{{ALLOWEDLATE_PLACE}}/$allowedLateness/g" config.properties
sed -i '' -e "s/{{LATEAFTER_PLACE}}/$lateAfter/g" config.properties
sed -i '' -e "s/{{WINDOWSIZE_PLACE}}/$windowsize/g" config.properties
cd ..
cd ..
cd ..

# Export Java Home for job graph parsing
export JAVA_HOME= $java_11_home

# # Build Flink Job
gradle shadowJar -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-11.0.16.1.jdk/Contents/Home

# # Job Deployment
$pathToFlink run --detached -m localhost:8081 /Users/christopher/Uni/repos/apacheflink-benchmark/benchmark-flink/build/libs/benchmark-flink-0.1-SNAPSHOT-all.jar

cd ..

############# Benchmark Clients Build ##############################

# create copy of client properties
cd benchmark-client/app/
cp config.properties.template config.properties

# replace with environment variable
sed -i '' -e "s/{{KAFKA_IP_PLACE}}/$KAFKA_IP/g" config.properties
sed -i '' -e "s/{{MSP_PER_SEC_PLACE}}/$messagePerSecond/g" config.properties
sed -i '' -e "s/{{PERCENTAGE_LATE_PLACE}}/$percentageLate/g" config.properties
sed -i '' -e "s/{{TIME_LATE_PLACE}}/$latenessOfMessagesinSeconds/g" config.properties
sed -i '' -e "s/{{NUM_THREADS_PLACE}}/$numberOfThreads/g" config.properties
cd ..
gradle build
cd ..

# ####### Benchmark Clients Deployment #######

# Generate SSH Key can be commentened out to reuse keys (also ask if you want to create new ones)
rm -r ssh
mkdir -p ssh
ssh-keygen -b 2048 -t rsa -q -N "" -C provisioner -f ssh/client-key

# Setup terraform and move data and config files to instances
cd terraform

# Deploy Benchmark Clients
terraform apply -var="producer_count=${number_producers}" -var="instance_type=e2-medium" -target=google_compute_instance.client_producer -auto-approve
terraform apply -var="consumer_count=${number_consumers}" -var="instance_type=e2-medium" -target=google_compute_instance.client_consumer -auto-approve

cd ..

# Sleep random time until clients are ready
sleep 180

cd terraform
touch start.txt

# Copy start signal file to each becnhmark client using scp
for i in $(terraform output -json instance_ips_producer | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r start.txt \
        provisioner@$i:~/start.txt
done

# Copy start signal file to each becnhmark client using scp
for i in $(terraform output -json instance_ips_consumers | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r start.txt \
        provisioner@$i:~/start.txt
done

rm start.txt


# sleep for benchmarkruntime
sleep 30m

######## Retrieve results and shutdown #########

cd terraform

for i in $(terraform output -json instance_ips_producer | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r provisioner@$i:~/messages/ \
        ../results/producer_$i
done

for i in $(terraform output -json instance_ips_consumers | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r provisioner@$i:~/results/ \
        ../results/consumer_$i
done

# Shutdown Benchmark Clients
terraform destroy -auto-approve