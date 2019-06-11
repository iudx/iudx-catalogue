# iudx-catalogue

[![Build Status](https://travis-ci.org/rbccps-iisc/iudx-catalogue.svg?branch=master)](https://travis-ci.org/rbccps-iisc/iudx-catalogue)

An Open Source implementation of India Urban Data Exchange (IUDX) Catalogue Service and Search APIs using Vert.x, an event driven and non-blocking high performance reactive framework, for identifying assets and data resources in Smart Cities.

Quickstart
========== 

#. Clone the repository::

    git clone https://github.com/rbccps-iisc/iudx-catalogue.git
    cd iudx-catalogue

#. Please install the following dependencies manually, skip if already installed

	- docker
	- docker-compose
    
#. Start the installation::

    cd docker
    sh install
    
Catalogue will be up in production mode at <https://localhost:8443/>

#. Re-install::

    cd docker
    sh re-install apiserver (To re-install apiserver)
    sh re-install mongodb (To re-install mongodb)

#. Start the test suite::

Before starting the test suite, make sure you get a Class-3 client certificate from <https://ca.iudx.org.in/>
 
Once you get the certificate, run the create client keystore script with the following options

    sh create_client_keystore.sh <certificate.pem> <private_key.pem>

This will create the client keystore file client.jks with which REST API testing can be performed. 

    cd docker
    sh test

Catalogue will be up in development mode at <https://localhost:18443/>. 

REST Assured testing will be performed. 

Development mode Catalogue containers will be deleted after testing.  

