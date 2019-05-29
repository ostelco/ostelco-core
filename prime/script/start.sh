#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /prime.jar server /config/config.yaml
