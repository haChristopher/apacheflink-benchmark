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



## Execute benchmark from docker container
...