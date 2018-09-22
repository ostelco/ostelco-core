#!/bin/bash
#set -x

exportId=$1
if [ -z "$1" ]; then
    exportId=$(uuidgen)
fi
exportId=${exportId//-}
exportId=${exportId,,}
projectId=pantel-2decb

pseudonymsTable=$projectId.exported_pseudonyms.$exportId
hourlyConsumptionTable=$projectId.data_consumption.hourly_consumption
dataConsumptionTable=exported_data_consumption.$exportId
csvfile=$projectId-dataconsumption-export/$exportId.csv

# Generate the pseudonym tables for this export
echo "Starting export job for $exportId"
pseudonymHost="pseudonym-server-service.default.svc.cluster.local"
startUrl="http://$pseudonymHost/pseudonym/export/$exportId"
httpStatus=$(curl -sL -w "%{http_code}" $startUrl -o /dev/null)

if [[ $httpStatus != 200 ]]; then
  echo "Failed to start the pseudonym table creation: $httpStatus"
  exit
fi

# Wait for the table creation to finish
echo "Waiting to finish table export for $exportId"
sleep 30 &
queryUrl="http://$pseudonymHost/pseudonym/exportstatus/$exportId"
httpStatus=$(curl -sL -w "%{http_code}" $queryUrl -o /dev/null)
if [[ $httpStatus != 200 ]]; then
  echo "Failed to query the table creation: $httpStatus"
  exit
fi

jsonResult=RUNNING
while [[ $jsonResult = RUNNING || $jsonResult = INITIAL ]]; do
  jsonResult=$(curl -X GET $queryUrl  2> /dev/null | sed -n -e 's/.*"status"://p'| cut -d \" -f 2)
done
if [[ $jsonResult != FINISHED ]]; then
  echo "Table creation failed $(curl -X GET $queryUrl  2> /dev/null)"
  exit
fi
echo "Created Table $pseudonymsTable"


echo "Creating table $dataConsumptionTable"
# SQL for joining pseudonym & hourly consumption tables.
read -r -d '' sqlForJoin << EOM
SELECT
   hc.bytes, ps.msisdnid, hc.timestamp
FROM
   \`$hourlyConsumptionTable\` as hc
JOIN
  \`$pseudonymsTable\` as ps
ON  ps.pseudonym = hc.msisdn
EOM
# Run the query using bq & dump results to the new table
bq --location=EU --format=none query --destination_table $dataConsumptionTable --replace --use_legacy_sql=false $sqlForJoin
echo "Created table $dataConsumptionTable"

echo "Exporting data to csv $csvfile"
bq --location=EU extract --destination_format=CSV $dataConsumptionTable gs://$csvfile
echo "Exported data to gs://$csvfile"