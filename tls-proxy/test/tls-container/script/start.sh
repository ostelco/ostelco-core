#!/bin/bash

set -e
set -x

tcpdump -i eth1 -w /pcap/trace_tls.pcap &

sleep 5



echo "Starting TLS test"

# socat stdio openssl-connect:ocsgw-proxy.ostelco.org:3868,cert=/etc/cert/ssl.pem,cafile=/etc/cert/ssl.crt

# openssl s_client -connect ocsgw-proxy.ostelco.org:3868 -cert /etc/cert/ssl.pem -key /etc/cert/ssl.pem -showcerts

# echo -e "Hello from TLS" | openssl s_client -connect ocsgw-proxy.ostelco.org:3868 -cert /etc/cert/ssl.pem -key /etc/cert/ssl.pem -debug -msg -servername ocsgw-proxy.ostelco.org -showcerts  -ign_eof

echo -e "Hello from TLS" | openssl s_client -connect ocsgw-proxy.ostelco.org:3868 -cert /etc/cert/ssl.pem -key /etc/cert/ssl.pem -msg -servername ocsgw-proxy.ostelco.org -showcerts -ign_eof

