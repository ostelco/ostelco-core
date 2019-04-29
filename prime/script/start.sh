#!/bin/bash

# XXX REMOVE BEFORE FLIGHT (only for debugging
#     idemia backchannel)

nohup tcpdump -s 1500  portrange 8000-9000 -w dumpfile.pcap > dump.out &


# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /prime.jar server /config/config.yaml
