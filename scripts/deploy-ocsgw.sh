#!/bin/bash

#
#  Script to deploy OCS gateway to test deployment.
#  The last thing the script does is to look at the logs
#  of the running ocsgw.  The script needs to be stopped by
#  ctr-c
#

variant=dev
host_ip=192.168.0.124
if [ "$1" = prod ] ; then
   host_ip=192.168.0.123
   variant=prod
fi

echo "Starting update.."
echo "Creating zip files"
gradle pack

if [ ! -f build/deploy/ostelco-core-${variant}.zip ]; then
    echo "build/deploy/ostelco-core-${variant}.zip not found!"
    exit 1
fi

echo "Starting to deploy OCSGW to $variant"
echo "The last thing this script will do is to look  at logs from the ocsgw"
echo "It will continue to do so until terminated by ^C"


scp -oProxyJump=loltel@10.6.101.1 build/deploy/ostelco-core-${variant}.zip  ubuntu@${host_ip}:.
ssh -A -Jloltel@10.6.101.1 ubuntu@${host_ip} <<EOF
  if [ ! -f ostelco-core-${variant}.zip ] ; then
     echo "Couldn't find ostelco-core-${variant}.zip"
     exit 1
  fi
  if [ -d ostelco-core ] ; then 
    cd ostelco-core
    sudo docker-compose down
    cd ..
    rm -rf ostelco-core
  fi
  unzip ostelco-core-${variant}.zip -d ostelco-core
  cd ostelco-core
  sudo docker-compose up -d --build
  sudo docker-compose logs -f
EOF
