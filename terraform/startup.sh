#!/bin/bash
# export DEBIAN_FRONTEND=noninteractive

touch start.txt

cd home/provisioner/

adduser benchmark
echo 'benchmark:client' | chpasswd
usermod -aG google-sudoers benchmark

sudo apt update
sudo apt upgrade -y
sudo apt-get -y install unzip
sudo apt-get -y install default-jdk
java -version

unzip app.zip

touch startupFinished.txt

# Execute Kotlin App
./app/bin/app --args="${type}"
