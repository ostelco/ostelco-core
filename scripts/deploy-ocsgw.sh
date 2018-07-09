#!/bin/bash

#
#  Script to deploy OCS gateway to test deployment.
#  The last thing the script does is to look at the logs
#  of the running ocsgw.  The script needs to be stopped by
#  ctr-c
#

echo "Starting to deploy OCSGW to test installation"
echo "The last thing this script will do is to look  at logs from the ocsgw"
echo "It will continue to do so until terminated by ^C"

scp -oProxyJump=loltel@10.6.101.1 build/deploy/ostelco-core.zip  ubuntu@192.168.0.123:.
ssh -A -Jloltel@10.6.101.1 ubuntu@192.168.0.123 <<EOF
  if [ ! -f ostelco-core.zip ] ; then 
     echo "Couldn't find ostelco-core.zip"
     exit 1
  fi
  if [ -d ostelco-core ] ; then 
    cd ostelco-core
    sudo docker-compose down
    cd ..
    rm -rf ostelco-core
  fi
  unzip ostelco-core.zip -d ostelco-core
  cd ostelco-core
  sudo docker-compose up -d --build
  sudo docker-compose logs -f
EOF
