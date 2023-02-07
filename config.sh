#!/bin/bash

####### Google Cloud Platform GKE #######
project_name="csb-benchmark-apachef"
service_account_key_file="/Users/Christopher/Uni/CSB/keys/csb-benchmark-apachef-71816b8a0e21.json"
cluster_name=csb-benchmark-flink-cluster

####### GKE Cluster #######
gke_region="europe-west3-c"
gke_num_nodes=3
gke_machine_type="n1-standard-1"

####### SUT Deployment ######
flink_version="1.16.0"
scala_version="2.12"
dataset="custom" # custom / user-activity / ...
pathToFlink="/Users/christopher/Uni/Tools/ApacheFlink/flink-1.16.0/bin/flink"

###### Job deployment #######
java_11_home="/Library/Java/JavaVirtualMachines/jdk-11.0.16.1.jdk/Contents/Home"

###### Experiment Setup #######
number_producers=3
number_consumers=1

# Pipeline settings
allowedLateness=0
lateAfter=0
windowsize=60

# Client settings
messagePerSecond=50
percentageLate=0
latenessOfMessagesinSeconds=60
numberOfThreads=5