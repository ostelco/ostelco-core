#!/bin/sh

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /ocsgw.jar
