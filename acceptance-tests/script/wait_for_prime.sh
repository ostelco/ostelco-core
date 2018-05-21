#!/bin/sh

set -e

echo "AT waiting Prime to launch on 8080..."

while ! nc -z 172.16.238.4 8080; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Prime launched"

java -cp '/acceptance-tests.jar' org.junit.runner.JUnitCore \
     org.ostelco.at.GetBalanceTest \
     org.ostelco.at.GetProductsTest \
     org.ostelco.at.PurchaseTest \
     org.ostelco.at.AnalyticsTest \
     org.ostelco.at.ConsentTest \
     org.ostelco.at.ProfileTest