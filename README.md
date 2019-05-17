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
    
Catalogue will be up in <https://localhost:8443/>

#. Re-install::

    cd docker
    sh re-install apiserver (To re-install apiserver)
    sh re-install mongodb (To re-install mongodb)
