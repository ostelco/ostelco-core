#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /dataflow-pipelines.jar
