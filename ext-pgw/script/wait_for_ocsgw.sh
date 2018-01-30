#!/bin/sh

set -e

echo "ext-pgw waiting ocsgw to launch on 8082..."

while ! nc -z 172.16.238.3 3868; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "ocsgw launched"


./start.sh
