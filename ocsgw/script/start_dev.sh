#!/bin/sh

# Start app for production
exec java \
    -Dfile.encoding=UTF-8 \
    -Dlogback.configurationFile=/config/logback.dev.xml \
    -jar /ocsgw.jar