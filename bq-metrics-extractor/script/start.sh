#!/bin/bash -x

# Start app
exec java \
     -Dfile.encoding=UTF-8 \
     -Xshare:on \
     -jar /bq-metrics-extractor.jar query --pushgateway prometheus-pushgateway.kube-system.svc.cluster.local:9091 config/config.yaml
