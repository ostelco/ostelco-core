#!/bin/sh

set -e

echo "Waiting for Prime to launch on 8082..."

while ! nc -z prime 8082; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Prime launched"

echo "OCSGW waiting ESP to launch on 80..."

while ! nc -z ocs.dev.ostelco.org 80; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "ESP launched"

echo "OCSGW waiting Metrics ESP to launch on 80..."

while ! nc -z metrics.dev.ostelco.org 80; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Metrics ESP launched"

# Start app for testing
exec java \
    -Dfile.encoding=UTF-8 \
    -Dlogback.configurationFile=/config/logback.console.xml \
    -jar /ocsgw.jar

