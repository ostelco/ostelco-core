#!/bin/bash

set -e

echo "AT waiting ocsgw to launch on 3868..."

while ! nc -z 172.16.238.3 3868; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "ocsgw launched"

echo "AT waiting Prime to launch on 8080..."

while ! nc -z prime 8080; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Prime launched"

java -cp '/acceptance-tests.jar' org.junit.runner.JUnitCore org.ostelco.at.TestSuite