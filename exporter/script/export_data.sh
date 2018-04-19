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

echo "Creating table pantel-2decb.exported_data_consumption.$exportId"
read -r -d '' sqlForJoin << EOM
SELECT
   hc.bytes, ps.msisdnid, hc.timestamp
FROM
   \`pantel-2decb.data_consumption.hourly_consumption\` as hc
JOIN
  \`pantel-2decb.exported_pseudonyms.$exportId\` as ps
ON  ps.msisdn = hc.msisdn
EOM
bq --location=EU query --destination_table exported_data_consumption.$exportId --use_legacy_sql=false $sqlForJoin
echo "Table pantel-2decb.exported_data_consumption.$exportId created."

echo "Exporting data to csv"
csvfile=pantel-2decb-dataconsumption-export/$exportId.csv
bq --location=EU extract --destination_format=CSV exported_data_consumption.$exportId gs://$csvfile
echo "exported data to gs://$csvfile"