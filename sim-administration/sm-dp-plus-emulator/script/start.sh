#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /sm-dp-plus-emulator.jar server /config/config.yaml
