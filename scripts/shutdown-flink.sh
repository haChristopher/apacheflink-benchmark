#!/bin/bash

cd infrastructure

## Configuration and service definition
kubectl delete -f flink-configuration-configmap.yaml
kubectl delete -f jobmanager-service.yaml
## Create the deployments for the cluster
kubectl delete -f jobmanager-session-deployment-non-ha.yaml
kubectl delete -f taskmanager-session-deployment.yaml