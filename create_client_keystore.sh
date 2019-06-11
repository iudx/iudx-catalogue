#!bin/bash
echo 'Creating client keystore'
cp my-keystore.jks client.jks
echo 'Copied JKS file'
openssl pkcs12 -export -inkey $1 -in $2 -out client.pfx -passout pass:password -name client-certificate
echo 'Created PFX file from client certificate and key'
keytool -importkeystore -srckeystore client.pfx -srcstoretype pkcs12 -srcalias client-certificate -srcstorepass password -destkeystore client.jks -deststoretype jks -deststorepass password -destalias client-certificate
echo 'Removing intermediate files'
rm -rf client.pfx
echo 'Creater JKS file for REST Client'
