#!/bin/sh

set -e

echo "OCSGW waiting Prime to launch on 8080..."

while ! nc -z prime 8080; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Prime launched"

# Start app for testing
exec java \
    -Dfile.encoding=UTF-8 \
    -Dlogback.configurationFile=/config/logback.console.xml \
    -jar /ocsgw.jar

