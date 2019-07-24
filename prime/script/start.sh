#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    -jar /prime.jar server /config/config.yaml
