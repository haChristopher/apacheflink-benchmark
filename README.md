# apacheflink-benchmark
Benchmarks the impact of allowed lateness on latency for Apache Flink.

## Installation & Setup

Install google cloud SDK (requires python):
https://cloud.google.com/sdk/docs/install

Install terraform with homebrew
```
brew tap hashicorp/tap
brew install hashicorp/tap/terraform
```

Install Gradle
```
brew install gradle
```

Install Kubectl
https://kubernetes.io/docs/tasks/tools/


Install JQuery (not necessary so far)
```
brew install jq
```

## Setting up Google Cloud

- create project and copy ID to config (not project number!)
- create service account [here](https://console.cloud.google.com/iam-admin/serviceaccounts) with admin/inhaber priviliges (copy service account keyfile path to config)
- Activate Kubelesst API: https://console.cloud.google.com/apis/library/container.googleapis.com
- Activate Compute Engine API: https://console.developers.google.com/apis/api/compute.googleapis.com
- Activate Cloud Resource Manager API: https://console.cloud.google.com/apis/library/cloudresourcemanager.googleapis.com

Config section should look like this:
```
####### Google Cloud Platform GKE #######
project_name="csb-benchmark-apachef"
service_account_key_file="/Users/Christopher/Uni/CSB/keys/csb-benchmark-apachef-71816b8a0e21.json"
cluster_name=csb-benchmark-flink-cluster
```

## Installing Java

Apache Flink needs Java 11 or 8 for this benchmark 11 was used. Install Java 11 and set Path to Java home in config.sh

```
###### Job deployment #######
java_11_home="/Library/Java/JavaVirtualMachines/jdk-11.0.16.1.jdk/Contents/Home"
```

## Installing Apache Flink

Install Apache Flink Locally: https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/try-flink/local_installation/

Set path to flink in config file:
```
####### SUT Deployment ######
pathToFlink="/Users/christopher/Uni/Tools/ApacheFlink/flink-1.16.0/bin/flink"
```

## Running the benchmark

First define your benchmark configuration in config.sh

Then run the benchmark:
```
./start.sh
```

Currently there is an issue with the mode env setting for the benchmark. Consumer and producer need to be deployed seperated and before the mode needs to be changed manually in App.kt

Results should appear in /results

## Running the benchmark (locally)

Install docker desktop and start a kubernetes cluster: https://docs.docker.com/desktop/kubernetes/

Install kubectl: https://kubernetes.io/docs/tasks/tools/

Check if kubectls is pointing to docker desktop
```
kubectl config get-contexts

# If not already pointing to docker-desktop
kubectl config use-context docker-desktop
```

### Setting up Apache Flink (Locally)

Install Apache Flink Locally: https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/try-flink/local_installation/

Deploy apache flink session mode on kubernetes:
```
./scripts/deploy-flink.sh
```

Check if deployed
```
kubectl get pods --all-namespaces
```

Setup port forward to see job dashboard
```
kubectl port-forward service/flink-jobmanager 8081:8081
```

For parsing and deploying the job JAVA_HOME needs to point to Java 11 (or 8), as higher versions are currently not supported by flink:
```
export JAVA_HOME='/Library/Java/JavaVirtualMachines/jdk-11.0.16.1.jdk/Contents/Home'
``` 

### Setting up Kafka (and useful commands)

Install Kafka: https://kafka.apache.org/quickstart

Move to installed kafka folder:
```
cd kafka_2.13-3.3.1
```

In two different terminals run:

```
# Starts Zookeeper instance from Kafka Folder
bin/zookeeper-server-start.sh config/zookeeper.properties
```

```
# Starts Kafka Broker from Kafka Folder
bin/kafka-server-start.sh config/server.properties
```

```
# Change config file or run afterwards or use custom server.properties file
./bin/kafka-configs.sh --alter --entity-type brokers --bootstrap-server localhost:9092 --entity-name 0 --add-config log.message.timestamp.type=LogAppendTime
```

Creating topics
```
bin/kafka-topics.sh --create --topic flink-input --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic flink-output --bootstrap-server localhost:9092

bin/kafka-topics.sh --bootstrap-server 172.16.0.33:19092 --topic flink-input --create --partitions 10 --replication-factor 1
bin/kafka-topics.sh --bootstrap-server 172.16.0.33:19092 --topic flink-output --create --partitions 10 --replication-factor 1

bin/kafka-topics.sh --bootstrap-server 172.16.0.52:19092 --list 

```

Delete topics
```
bin/kafka-topics.sh --delete --topic flink-input --bootstrap-server localhost:9092
bin/kafka-topics.sh --delete --topic flink-output --bootstrap-server localhost:9092
```

Monitor Output Topic if needed
```
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic flink-output
```

## Building different job pipelines

- Double check gradle pointing to the correct job
- Running locally in kubernets cluster access localhost services via 'host.docker.internal'

## Execute benchmark from docker container
...


# Data Analysis

Install python and dependendcies
```
pip install -r requirements.txt
```

Run the analysis:
```
python analyse.py myResults.csv
```

# Experiments
Following Experiences have been conducted

Variables:
percentage late = [25, 50, 75]
allowed lateness = [0s, 30s, 60s]
window size = 10s

Experiment 1 increase allowedLateness and Lateness
Experiment 2 increase percentage of late data