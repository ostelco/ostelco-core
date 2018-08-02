#!/bin/bash

echo "prime waiting for neo4j to launch on 7687..."

while ! nc -z neo4j 7687; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "neo4j launched"

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /prime.jar server /config/config.yaml
