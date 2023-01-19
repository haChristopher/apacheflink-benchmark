# apacheflink-benchmark
Benchmarking client for apache flink

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

## Running the benchmark

```
terraform init
```

## Running the benchmark (locally)

Install docker desktop and start a kubernetes cluster: https://docs.docker.com/desktop/kubernetes/

Install kubectl: https://kubernetes.io/docs/tasks/tools/

Check if kubectls is pointing to docker desktop
```
kubectl config get-contexts

# If not already pointing to docker-desktop
kubectl config use-context docker-desktop
```

### Setting up Apache Flink

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

### Setting up Kafka

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

Creating topics
```
bin/kafka-topics.sh --create --topic flink-input --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic flink-output --bootstrap-server localhost:9092
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