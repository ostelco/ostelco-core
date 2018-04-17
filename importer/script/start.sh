#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /exporter.jar server /config/config.yaml
