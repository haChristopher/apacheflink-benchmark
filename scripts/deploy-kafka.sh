#!/bin/bash

## Create namespace
kubectl create -f infrastructure/kafka-namespace.yaml

## Create Zookeper Kafka and Service
kubectl create -f infrastructure/zookeeper.yaml -n kafka-cluster
kubectl create -f infrastructure/kafka-service.yaml -n kafka-cluster
kubectl create -f infrastructure/kafka.yml.yaml -n kafka-cluster
