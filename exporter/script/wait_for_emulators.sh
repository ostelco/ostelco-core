#!/bin/bash

set -e

# TODO call start.sh from here. For some reason, it is not working
# ./start.sh

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /exporter.jar server /config/config.yaml

