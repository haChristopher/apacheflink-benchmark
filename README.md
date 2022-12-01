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

## Running the benchmark
```
terraform init
```

## Running the benchmark (locally on kubernetes)

Install docker desktop and start a kubernetes cluster: https://docs.docker.com/desktop/kubernetes/

Install kubectl: https://kubernetes.io/docs/tasks/tools/

Install Apache Flink Locally: https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/try-flink/local_installation/


Check if kubectls is pointing to docker desktop
```
kubectl config get-contexts
kubectl config use-context docker-desktop
```

Deploy apache flink session mode on kubernetes:
```
cd intoFlinkFolder
./bin/kubernetes-session.sh -Dkubernetes.cluster-id=my-first-flink-cluster
```

### Setting up Kafka

Install Kafka: https://kafka.apache.org/quickstart

In two different terminals run:

```
# Starts Zookeeper instance
bin/zookeeper-server-start.sh config/zookeeper.properties
```

```
# Starts Kafka Broker
$ bin/kafka-server-start.sh config/server.properties
```

Creating topics
```
bin/kafka-topics.sh --create --topic flink-input --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic flink-output --bootstrap-server localhost:9092
```



## Execute benchmark from docker container
...