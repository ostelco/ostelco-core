#!/bin/bash -x

# Start app
exec java \
     -Dfile.encoding=UTF-8 \
     --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.io=ALL-UNNAMED \
     -Xshare:on \
     -jar /scaninfo-shredder.jar shred config/config.yaml
