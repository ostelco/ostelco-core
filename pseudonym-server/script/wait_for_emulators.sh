#!/bin/bash

set -e

echo "pseudonym-server waiting Datastore emulator to launch on datastore-emulator:8081..."

ds=$(curl --silent  http://datastore-emulator:8081  | head -n1)
until [[ $ds == 'Ok' ]] ; do
    printf 'pseudonymiser waiting ...'
    sleep 5
    ds=$(curl --silent http://datastore-emulator:8081  | head -n1)
done

echo "Datastore emulator launched"

echo "pseudonym-server waiting pubsub emulator to launch on gpubsub-emulator:8085..."

ds=$(curl --silent  http://gpubsub-emulator:8085  | head -n1)
until [[ $ds == 'Ok' ]] ; do
    printf 'pseudonymiser waiting ...'
    sleep 5
    ds=$(curl --silent http://gpubsub-emulator:8085  | head -n1)
done

echo "Pubsub emulator launched"

echo "Creating topics and subscriptions...."

curl  -X PUT gpubsub-emulator:8085/v1/projects/pantel-2decb/topics/data-traffic
curl  -X PUT gpubsub-emulator:8085/v1/projects/pantel-2decb/topics/pseudo-traffic
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/pantel-2decb/topics/data-traffic","ackDeadlineSeconds":10}' gpubsub-emulator:8085/v1/projects/pantel-2decb/subscriptions/test-pseudo

echo "Done creating topics and subscriptions"

# Forward the local port 9090 to datastore-emulator:8081
# The
socat TCP-LISTEN:9090,fork TCP:datastore-emulator:8081 &
socat TCP-LISTEN:9080,fork TCP:gpubsub-emulator:8085 &


# TODO call start.sh from here. For some reason, it is not working
# ./start.sh

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /pseudonym-server.jar server /config/config.yaml

