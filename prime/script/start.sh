#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    -agentpath:/opt/cprof/profiler_java_agent.so=-cprof_service=prime,-cprof_service_version=1.58.0,-logtostderr,-minloglevel=2,-cprof_enable_heap_sampling \
    -jar /prime.jar server /config/config.yaml
