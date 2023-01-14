#!/bin/bash

cd infrastructure/kafka/

kubectl delete -f zookeeper.yml -n kafka-cluster
kubectl delete -f kafka-service.yml -n kafka-cluster
kubectl delete -f kafka.yml -n kafka-cluster
kubectl delete -f kafka-namespace.yml