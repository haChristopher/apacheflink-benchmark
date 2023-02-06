#!/bin/bash

cd infrastructure/flink

# switch kubectl namespace
kubectl config set-context --current --namespace=flink-namespace

## Configuration and service definition
kubectl create -f flink-configuration-configmap.yaml
kubectl create -f jobmanager-service.yaml
## Create the deployments for the cluster
kubectl create -f jobmanager-session-deployment-non-ha.yaml
kubectl create -f taskmanager-session-deployment.yaml

# Port Forwarding in the background
kubectl port-forward service/flink-jobmanager 8081:8081 &

# Switch back to default namespace
kubectl config set-context --current --namespace=default

