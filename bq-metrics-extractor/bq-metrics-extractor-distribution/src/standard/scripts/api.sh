#!/bin/bash
export JAVA_HOME=/opt/java

cd /opt/api/
./bin/bq-metrics-extractor-distribution server var/conf/server.yml
