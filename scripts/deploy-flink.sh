#!/bin/bash

cd infrastructure
## Configuration and service definition
kubectl create -f flink-configuration-configmap.yaml
kubectl create -f jobmanager-service.yaml
## Create the deployments for the cluster
kubectl create -f jobmanager-session-deployment-non-ha.yaml
kubectl create -f taskmanager-session-deployment.yaml

# Port Forwarding
kubectl port-forward service/flink-jobmanager 8081:8081