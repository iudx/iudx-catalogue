#!/bin/sh
cd iudx-catalogue
java -jar catalogue-apiserver-0.0.1-fat.jar -conf src/main/conf/docker_conf.json
