#!/bin/bash

cd infrastructure/flink

# switch kubectl namespace
kubectl config set-context --current --namespace=flink-namespace

## Configuration and service definition
kubectl delete -f flink-configuration-configmap.yaml
kubectl delete -f jobmanager-service.yaml
## Create the deployments for the cluster
kubectl delete -f jobmanager-session-deployment-non-ha.yaml
kubectl delete -f taskmanager-session-deployment.yaml

# Switch back to default namespace
kubectl config set-context --current --namespace=default

# Kill port-forwarding process
pgrep kubectl | xargs kill -9