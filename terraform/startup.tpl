#!/bin/bash
export DEBIAN_FRONTEND=noninteractive

adduser benchmark
echo 'benchmark:client' | chpasswd
usermod -aG google-sudoers benchmark

echo "TARGET_ENDPOINT = ${endpoint}" > /tmp/iplist

sudo apt update
sudo apt upgrade -y
sudo apt-get -y install unzip
sudo apt-get -y install default-jdk
java -version

unzip app.zip

touch startupFinished.txt

# Execute Kotlin App
./app/bin/app