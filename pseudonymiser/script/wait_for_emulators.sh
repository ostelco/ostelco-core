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

# Forward the local port 9090 to datastore-emulator:8081
# The
socat TCP-LISTEN:9090,fork TCP:datastore-emulator:8081 &

# TODO call start.sh from here. For some reason, it is not working
# ./start.sh

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /pseudonymiser.jar server /config/config.yaml

