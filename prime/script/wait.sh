#!/bin/bash

# exit if failed
set -e

echo "prime waiting for neo4j to launch on 7687..."

while ! nc -z neo4j 7687; do
  sleep 1 # wait for 1 second before check again
done

echo "neo4j launched"

echo "prime waiting Datastore emulator to launch on datastore-emulator:8081..."

ds=$(curl --silent  http://datastore-emulator:8081  | head -n1)
until [[ $ds == 'Ok' ]] ; do
    printf 'prime waiting for Datastore emulator to launch...'
    sleep 5
    ds=$(curl --silent http://datastore-emulator:8081  | head -n1)
done

echo "Datastore emulator launched"

echo "prime waiting pubsub emulator to launch on pubsub-emulator:8085..."

ds=$(curl --silent  http://pubsub-emulator:8085  | head -n1)
until [[ $ds == 'Ok' ]] ; do
    printf 'prime waiting for pubsub emulator to launch...'
    sleep 5
    ds=$(curl --silent http://pubsub-emulator:8085  | head -n1)
done

echo "Pubsub emulator launched"

echo "Creating topics and subscriptions...."

curl  -X PUT pubsub-emulator:8085/v1/projects/pantel-2decb/topics/data-traffic
curl  -X PUT pubsub-emulator:8085/v1/projects/pantel-2decb/topics/pseudo-traffic
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/pantel-2decb/topics/data-traffic","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/pantel-2decb/subscriptions/test-pseudo

echo "Done creating topics and subscriptions"

# Forward the local port 9090 to datastore-emulator:8081
if [ -z $(type socat) ]; then echo "socat not installed."; exit 1; fi
socat TCP-LISTEN:9090,fork TCP:datastore-emulator:8081 &

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /prime.jar server /config/config.yaml
