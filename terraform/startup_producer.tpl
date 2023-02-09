#!/bin/bash
export DEBIAN_FRONTEND=noninteractive

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

echo $type > type.txt

# Execute Kotlin App
sudo ./app/bin/app --args="produce"
