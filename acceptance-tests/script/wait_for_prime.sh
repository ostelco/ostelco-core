#!/bin/sh

set -e

echo "AT waiting Prime to launch on 8080..."

while ! nc -z 172.16.238.4 8080; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Prime launched"

java -cp '/acceptance-tests.jar' org.junit.runner.JUnitCore \
     org.ostelco.at.okhttp.GetBalanceTest \
     org.ostelco.at.okhttp.GetProductsTest \
     org.ostelco.at.okhttp.PurchaseTest \
     org.ostelco.at.okhttp.ConsentTest \
     org.ostelco.at.okhttp.ProfileTest \
     org.ostelco.at.jersey.GetBalanceTest \
     org.ostelco.at.jersey.GetProductsTest \
     org.ostelco.at.jersey.PurchaseTest \
     org.ostelco.at.jersey.AnalyticsTest \
     org.ostelco.at.jersey.ConsentTest \
     org.ostelco.at.jersey.ProfileTest