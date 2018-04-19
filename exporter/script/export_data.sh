#!/bin/bash

exportId=$(uuidgen)
exportId=${exportId//-}
exportId=${exportId,,}
echo "Starting export job for $exportId"

pseudonymHost="pseudonym-server-service.default.svc.cluster.local"
startUrl="http://$pseudonymHost/pseudonym/export/$exportId"
httpStatus=$(curl -sL -w "%{http_code}" $startUrl -o /dev/null)

if [[ $httpStatus != 200 ]]; then
  echo "Error starting the job $httpStatus"
  exit
else
  echo "Started job $httpStatus"
fi


echo "Waiting to finish table export for $exportId"
sleep 30 &

queryUrl="http://$pseudonymHost/pseudonym/exportstatus/$exportId"
httpStatus=$(curl -sL -w "%{http_code}" $queryUrl -o /dev/null)
if [[ $httpStatus != 200 ]]; then
  echo "Cannot find the job $httpStatus"
  exit
fi

jsonResult=RUNNING
while [[ $jsonResult = RUNNING ]]; do
  jsonResult=$(curl -X GET $queryUrl  2> /dev/null | sed -n -e 's/.*"status"://p'| cut -d \" -f 2)
done

echo "Exported table for $exportId"

