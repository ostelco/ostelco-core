#!/bin/bash
#set -x

exportId=$1
if [ -z "$1" ]; then
    exportId=$(uuidgen)
fi
exportId=${exportId//-}
exportId=${exportId,,}

# Set the projectId
if [[ -z "${GCP_PROJECT_ID}" ]]; then
  echo "Missing GCP_PROJECT_ID env var"
  exit
else
  projectId="${GCP_PROJECT_ID}"
fi

# Set the datasetModifier
if [[ -z "${DATASET_MODIFIER}" ]]; then
  datasetModifier=""
else
  datasetModifier="${DATASET_MODIFIER}"
fi

msisdnPseudonymsTable=$projectId.exported_pseudonyms.${exportId}_msisdn
subscriberPseudonymsTable=$projectId.exported_pseudonyms.${exportId}_subscriber
sub2msisdnMappingsTable=exported_data_consumption.${exportId}_sub2msisdn
hourlyConsumptionTable=$projectId.data_consumption${datasetModifier}.hourly_consumption
dataConsumptionTable=exported_data_consumption.$exportId
rawPurchasesTable=$projectId.purchases${datasetModifier}.raw_purchases
purchaseRecordsTable=exported_data_consumption.${exportId}_purchases
csvfile=$projectId-dataconsumption-export/$exportId.csv
purchasesCsvfile=$projectId-dataconsumption-export/$exportId-purchases.csv
sub2msisdnCsvfile=$projectId-dataconsumption-export/$exportId-sub2msisdn.csv

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
echo "Created Table $msisdnPseudonymsTable"
echo "Created Table $subscriberPseudonymsTable"


echo "Creating table $dataConsumptionTable"
# SQL for joining pseudonym & hourly consumption tables.
read -r -d '' sqlForJoin << EOM
SELECT
   hc.bytes, ps.pseudoid as msisdnid, hc.timestamp
FROM
   \`$hourlyConsumptionTable\` as hc
JOIN
  \`$msisdnPseudonymsTable\` as ps
ON  ps.pseudonym = hc.msisdn
EOM
# Run the query using bq & dump results to the new table
bq --location=EU --format=none query --destination_table $dataConsumptionTable --replace --use_legacy_sql=false $sqlForJoin
echo "Created table $dataConsumptionTable"

echo "Exporting data to csv $csvfile"
bq --location=EU extract --destination_format=CSV $dataConsumptionTable gs://$csvfile
echo "Exported data to gs://$csvfile"

echo "Creating table $purchaseRecordsTable"
# SQL for joining subscriber pseudonym & purchase record tables.
read -r -d '' sqlForJoin2 << EOM
SELECT
   TIMESTAMP_MILLIS(pr.timestamp) as timestamp , ps.pseudoid as subscriberId, pr.product.sku, pr.product.price.amount, product.price.currency
FROM
   \`$rawPurchasesTable\` as pr
JOIN
  \`$subscriberPseudonymsTable\` as ps
ON  ps.pseudonym = pr.subscriberId
EOM

# Run the query using bq & dump results to the new table
bq --location=EU --format=none query --destination_table $purchaseRecordsTable --replace --use_legacy_sql=false $sqlForJoin2
echo "Created table $purchaseRecordsTable"

echo "Exporting data to csv $purchasesCsvfile"
bq --location=EU extract --destination_format=CSV $purchaseRecordsTable gs://$purchasesCsvfile
echo "Exported data to gs://$purchasesCsvfile"

echo "Exporting data to csv $sub2msisdnCsvfile"
bq --location=EU extract --destination_format=CSV $sub2msisdnMappingsTable gs://$sub2msisdnCsvfile
echo "Exported data to gs://$sub2msisdnCsvfile"
