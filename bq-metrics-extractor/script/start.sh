#!/bin/bash -x

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /bq-metrics-extractor.jar query --pushgateway pushgateway:9091 config/config.yaml
