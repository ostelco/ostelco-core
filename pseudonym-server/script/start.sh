#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /pseudonym-server.jar server /config/config.yaml
