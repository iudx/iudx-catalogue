#!/bin/bash

echo "System Update"
sudo apt-get update
echo "Done"
echo "Installing Docker"
sudo apt-get install apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install docker-ce
sudo usermod -aG docker $USER
echo "Done"
echo "Installing Docker Compose"
sudo apt-get install docker-compose
echo "Done"
echo "Installing Java"
sudo apt install openjdk-11-jre-headless
echo "Done"
echo "Installing Maven"
sudo apt install maven
echo "Done"
echo "Adding Docker to auto-start on boot"
sudo systemctl enable docker
echo "Done"
echo "Requirements installed. Now you can run install script"
