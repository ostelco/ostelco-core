#!/bin/bash

set -e

echo "Starting TCP test"

ifconfig

# tcpdump -i eth1 -w /pcap/trace_tcp.pcap &

# nc -l -p 3868 > /pcap/receiveData.txt


while true; do { echo -e 'Hello from TCP'; } | nc -l 3868; done