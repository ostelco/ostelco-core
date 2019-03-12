#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /ext-myinfo-emulator.jar server /config/config.yaml
