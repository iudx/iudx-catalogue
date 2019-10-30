#!/bin/bash
# bash generate random alphanumeric string
#

# bash generate random 32 character alphanumeric string (upper and lowercase) and 
USER=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 8 | head -n 1)
echo 'User ID is : '$USER

# bash generate random 32 character alphanumeric string (upper and lowercase) and 
PASSWORD=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
echo 'Password is : '$PASSWORD

cat > .env << EOF1
MONGO_INITDB_ROOT_USERNAME=$USER
MONGO_INITDB_ROOT_PASSWORD=$PASSWORD
EOF1

echo 'Update properties file'

file=config.properties

cd ..
sed -i 's/database_user=.*/database_user='$USER'/' $file
sed -i 's/database_password=.*/database_password='$PASSWORD'/' $file
echo 'Updated '$file
