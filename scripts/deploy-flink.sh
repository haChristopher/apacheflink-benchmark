#!/bin/bash

cd infrastructure/flink

## Create namespace and service
kubectl create -f flink-namespace.yaml
sleep 5

# switch kubectl namespace
kubectl config set-context --current --namespace=flink-namespace

## Configuration and service definition
kubectl create -f flink-configuration-configmap.yaml
kubectl create -f jobmanager-service.yaml
## Create the deployments for the cluster
kubectl create -f jobmanager-session-deployment-non-ha.yaml
kubectl create -f taskmanager-session-deployment.yaml

sleep 5

echo 'Waiting for Flink to be available ...'
while [[ $(kubectl get pods -l app=flink-jobmanager -o 'jsonpath={..status.conditions[?(@.type=="Ready")].status}') != "True" ]]; do echo "waiting for pod" && sleep 1; done
echo 'Flink is available'

# Port Forwarding in the background
kubectl port-forward service/flink-jobmanager 8081:8081 &

# Switch back to default namespace
kubectl config set-context --current --namespace=default

