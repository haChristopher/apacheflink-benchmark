#!/bin/bash


# Build pipeline jar
cd benchmark-flink
gradle shadowJar
cd ..

# Apache Flink Deployment

# Job Deployment
./bin/flink run -m localhost:8081 /Users/christopher/Uni/repos/apacheflink-benchmark/benchmark-flink/build/libs/benchmark-flink-0.1-SNAPSHOT.jar

