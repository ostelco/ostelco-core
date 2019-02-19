#!/bin/bash -x

# Start app
exec java \
     -Dfile.encoding=UTF-8 \
     -Xshare:on \
     -jar /scaninfo-shredder.jar shred config/config.yaml
