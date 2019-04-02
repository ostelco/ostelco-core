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

#java -cp '/acceptance-tests.jar' org.junit.runner.JUnitCore \
#     org.ostelco.at.okhttp.GetProductsTest \
#     org.ostelco.at.okhttp.BundlesAndPurchasesTest \
#     org.ostelco.at.okhttp.SourceTest \
#     org.ostelco.at.okhttp.PurchaseTest \
#     org.ostelco.at.okhttp.CustomerTest \
#     org.ostelco.at.okhttp.GraphQlTests \
#     org.ostelco.at.jersey.GetProductsTest \
#     org.ostelco.at.jersey.BundlesAndPurchasesTest \
#     org.ostelco.at.jersey.SourceTest \
#     org.ostelco.at.jersey.PurchaseTest \
#     org.ostelco.at.jersey.PlanTest \
#     org.ostelco.at.jersey.CustomerTest \
#     org.ostelco.at.jersey.GraphQlTests \
#     org.ostelco.at.jersey.JumioKycTest \
#     org.ostelco.at.pgw.OcsTest
