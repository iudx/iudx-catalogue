#!/bin/sh
mongo catalogue --eval 'db.createCollection("schemas"); db.createCollection("items")'
#mongo admin --eval 'db.createUser( { user: "root", pwd: "root", roles:["root"] } );'
#mongo catalogue --eval 'db.createUser( { user: "cat-admin", pwd: "cat-admin", roles:["readWrite"] } );'
