#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /hss-adapter.jar server /config/config.yaml
