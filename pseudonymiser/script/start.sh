#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /pseudonymiser.jar server /config/config.yaml
