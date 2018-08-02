#!/bin/sh

set -e

echo "OCSGW waiting ESP to launch on 80..."

while ! nc -z 172.16.238.4 80; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "ESP launched"

echo "OCSGW waiting Prime to launch on 8082..."

while ! nc -z 172.16.238.5 8082; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Prime launched"

# Start app for testing
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /ocsgw.jar

