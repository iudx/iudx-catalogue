# IUDX Catalogue

[![Build Status](https://travis-ci.org/rbccps-iisc/iudx-catalogue.svg?branch=master)](https://travis-ci.org/rbccps-iisc/iudx-catalogue)

An Open Source implementation of India Urban Data Exchange (IUDX) Catalogue Service and Search APIs using Vert.x, an event driven and non-blocking high performance reactive framework, for identifying assets and data resources in Smart Cities.

Quickstart
========== 

1. Clone the repository
```
  git clone https://github.com/iudx/iudx-catalogue.git
  cd iudx-catalogue
```
2. Please install the following dependencies manually or run `./requirements`, skip if already installed

  - docker
  - docker-compose
  
3. Start the installation
  * Quick Installation
    - Pulls the jars from the latest release in github for fast installation. Takes ~1 minute to install. 
```
      cd docker
      ./install -q
```
  * Regular Installation
    - Performs a fresh install by downloading all the dependencies and compiling the java source files. Takes ~8 minutes to install.
 ```
      cd docker
      ./install
 ```
Catalogue will be up in production mode at <https://localhost:8443/>

4. Re-install
```
  cd docker
  ./re-install apiserver # To re-install apiserver
  ./re-install mongodb # To re-install mongodb
```
5. Start the test suite

Before starting the test suite, make sure you get a Class-3 client certificate from <https://ca.iudx.org.in/>
 
Once you get the certificate, run the create client keystore script with the following options
```
  sh create_client_keystore.sh <certificate.pem> <private_key.pem>
```
This will create the client keystore file client.jks with which REST API testing can be performed. 
```
  cd docker
  sh test
```
Catalogue will be up in development mode at <https://localhost:18443/>. 

REST Assured testing will be performed. 
