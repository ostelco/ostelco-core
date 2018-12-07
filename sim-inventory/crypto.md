# Generating keystpore and truststpre

   keytool -genkey -alias mydomain -keyalg RSA -keystore KeyStore.jks -keysize 2048
   keytool -genkey -alias mydomain -keyalg RSA -keystore TrustStore.jks -keysize 2048


... for test use only.   Both have "secret" as their password.


