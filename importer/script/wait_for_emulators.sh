#!/bin/bash

set -e

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /importer.jar server /config/config.yaml

