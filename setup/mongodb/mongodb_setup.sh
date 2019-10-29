#!/bin/sh
mongo --username root --password rootPassXXX --authenticationDatabase admin --host localhost --port 27017 --eval 'db.createCollection("catalogue");'
#mongo admin --eval 'db.createUser( { user: "root", pwd: "root", roles:["root"] } );'
#mongo catalogue --eval 'db.createUser( { user: "cat-admin", pwd: "cat-admin", roles:["readWrite"] } );'
#mongo catalogue --eval 'db.createCollection("catalogue");'
