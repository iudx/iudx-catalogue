#!/bin/bash

cd iudx-catalogue

mvn clean compile assembly:single

cp target/catalogue-apiserver-0.0.1-jar-with-dependencies.jar .

tmux new-session -d -s cat-server 'java -jar catalogue-apiserver-0.0.1-jar-with-dependencies.jar -d64 -Xms512m -Xmx4g'
