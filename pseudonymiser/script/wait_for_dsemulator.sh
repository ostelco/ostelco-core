#!/bin/bash

set -e

echo "pseudonymiser waiting Datastore emulator to launch on datastore-emulator:8081..."

ds=$(curl --silent  http://datastore-emulator:8081  | head -n1)
until [[ $ds == 'Ok' ]] ; do
    printf 'pseudonymiser waiting ...'
    sleep 5
    ds=$(curl --silent http://datastore-emulator:8081  | head -n1)
done

echo "Datastore emulator launched"

# TODO call start.sh from here. For some reason, it is not working
# ./start.sh

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /pseudonymiser.jar server /config/config.yaml

