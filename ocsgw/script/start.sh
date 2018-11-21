#!/bin/sh

# Start app for production
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /ocsgw.jar