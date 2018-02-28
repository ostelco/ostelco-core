#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /auth-server.jar server /config/config.yaml
