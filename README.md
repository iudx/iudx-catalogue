# IUDX Catalogue

[![Build Status](https://travis-ci.org/rbccps-iisc/iudx-catalogue.svg?branch=master)](https://travis-ci.org/rbccps-iisc/iudx-catalogue)

An Open Source implementation of India Urban Data Exchange (IUDX) Catalogue Service and Search APIs using Vert.x, an event driven and non-blocking high performance reactive framework, for identifying assets and data resources in Smart Cities.

## Quickstart

1. Clone the repository
```
  git clone https://github.com/iudx/iudx-catalogue && cd iudx-catalogue
```
2. If your host machine is Ubuntu or Debian based, simply run ``./requirements``. Otherwise, install the following dependencies manually:

  - ``docker``
  - ``docker-compose``
  
3. Start the installation. Do one of the below:
   
   - Quick Installation
     
     - Pulls the jars from the latest release for fast installation. Takes ~1 minute to install. 
    
     ```
       cd docker
       ./quick_install
     ```
   - Regular Installation

     - Downloads all maven dependencies compiles the java source files. Takes ~8 minutes to install.
      
     ```
       cd docker
       ./install
     ```

The catalogue server will be up at <https://localhost:8443/>

## Re-installation and testing

* To re-install individual components, run
```
  cd docker
  ./re-install apiserver #To re-install apiserver
  ./re-install mongodb #To re-install mongodb
```
* Running tests needs a few steps to be completed first

  - Make sure you get a Class-3 client certificate from <https://ca.iudx.org.in/>
  - Once a certificate has been obtained, run the ``create_client_keystore`` script with the following options

  ```
    ./create_client_keystore <certificate.pem> <private_key.pem>
  ```
  - The above script will create a client keystore file ``client.jks``. Now run
    
  ```
    cd docker
    ./test
  ```
  - The catalogue will be up as a development server at <https://localhost:18443/> and REST assured testing will be performed

## A note on dealing with IUDX Certificates

* To decode IUDX certificates for authorising users, do the following:
  
  - Obtain a server side certificate for the catalogue server from a well-known CA (eg. letsencrypt).
  - Create a .p12 file from the obtained certificate by following the steps below:
  
  ```
    cp /etc/letsencrypt/live/<domain_name>/fullchain.pem .
    cp /etc/letsencrypt/live/<domain_name>/privkey.pem .
    cat fullchain.pem privkey.pem > combined.pem
    openssl pkcs12 -export -in combined.pem -out my-keystore.p12
  ``` 
  - Convert the .p12 file to a JKS file using:
  
  ``` 
    keytool -importkeystore -srckeystore my-keystore.p12 -srcstoretype pkcs12 -destkeystore my-keystore.jks
  ``` 
  - Import <https://ca.iudx.org.in/> certificate as a trusted CA for decoding IUDX certificates
  
  ```
    keytool -import -trustcacerts -alias iudx-ca -file ca.iudx.org.in.crt -keystore my-keystore.jks
  ```
  - Run ``./apply_changes`` to effect the changes

## Contributors, read this

* If you have used ``./install`` script to install the catalogue, then first-time compilation takes about 8 minutes. You can monitor this using ``docker logs -f apiserver``. However, if you use the ``./quick_install`` script then the jar files from the latest release are pulled and the catalogue will be up immediately.

### Applying code changes and testing

* Simply run ``./apply_changes`` from the base directory of the repository to effect the new changes
