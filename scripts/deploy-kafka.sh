#!/bin/bash

cd infrastructure/kafka

## Create namespace and service
kubectl create -f kafka-namespace.yaml
sleep 5

# switch kubectl namespace
kubectl config set-context --current --namespace=kafka-cluster

# Create Service
# cp kafka-service.template kafka-service.yaml

kubectl create -f kafka-service.yaml
kubectl create -f kafka-service-local.yaml

# Get External IP for kafka service
echo 'Waiting for Kafka Service to get internal IP assigned ...'
sleep 120
kafka_ip=$(kubectl get svc kafka2 -n kafka-cluster -o jsonpath="{.status.loadBalancer.ingress[*].ip}")
echo "Kafka Service IP: $kafka_ip"

# Set environment variable for other scripts
export KAFKA_IP=$kafka_ip

# Create copy from kafka.yaml
cp kafka.template kafka-new.yaml

# Replace placeholder in kafka-new.yaml with external ip
sed -i '' -e "s/{{INTERNALIP_PLACE}}/$kafka_ip/g" kafka-new.yaml
sed -i '' -e "s/{{REPLICA_PLACE}}/1/g" kafka-new.yaml
sed -i '' -e "s/{{ID_PLACE}}/1/g" kafka-new.yaml

## Create Zookeper Kafka and Service (validate false is because of weird error with volume claims)
kubectl create -f zookeeper.yaml --validate=false
kubectl create -f kafka-new.yaml --validate=false

# kubectl rollout restart deployment kafka-new
# kubectl rollout status deployment kafka-new --timeout=90s

sleep 5

echo 'Waiting for Kafka to be available ...'
while [[ $(kubectl get pods -l app=kafka-broker-1 -o 'jsonpath={..status.conditions[?(@.type=="Ready")].status}') != "True" ]]; do echo "waiting for pod" && sleep 1; done
echo 'Kafka is available'


# Switch back to default namespace
kubectl config set-context --current --namespace=default

cd ...
