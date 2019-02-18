#!/bin/bash
#set -x

exportId=$1
if [ -z "$1" ]; then
    echo "Specify the id of the export operation you want to delete"
    exit
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

msisdnPseudonymsTable=exported_pseudonyms.${exportId}_msisdn
subscriberPseudonymsTable=exported_pseudonyms.${exportId}_subscriber
sub2msisdnMappingsTable=exported_data_consumption.${exportId}_sub2msisdn
dataConsumptionTable=exported_data_consumption.$exportId
purchaseRecordsTable=exported_data_consumption.${exportId}_purchases
csvfile=$projectId-dataconsumption-export/$exportId.csv
purchasesCsvfile=$projectId-dataconsumption-export/$exportId-purchases.csv
sub2msisdnCsvfile=$projectId-dataconsumption-export/$exportId-sub2msisdn.csv

inputSubscriberTable=exported_pseudonyms.${exportId}_pseudo_subscriber
outputSubscriberTable=exported_pseudonyms.${exportId}_clear_subscriber

echo "Cleaning all data for export $exportId"
echo "Deleting Table $msisdnPseudonymsTable"
bq rm -f -t $msisdnPseudonymsTable

echo "Deleting Table $subscriberPseudonymsTable"
bq rm -f -t $subscriberPseudonymsTable

echo "Deleting Table $sub2msisdnMappingsTable"
bq rm -f -t $sub2msisdnMappingsTable

echo "Deleting Table $dataConsumptionTable"
bq rm -f -t $dataConsumptionTable

echo "Deleting Table $purchaseRecordsTable"
bq rm -f -t $purchaseRecordsTable

echo "Deleting Table $inputSubscriberTable"
bq rm -f -t $inputSubscriberTable

echo "Deleting Table $outputSubscriberTable"
bq rm -f -t $outputSubscriberTable

echo "Deleting csv gs://$csvfile"
gsutil rm gs://$csvfile

echo "Deleting csv gs://$purchasesCsvfile"
gsutil rm gs://$purchasesCsvfile

echo "Deleting csv gs://$sub2msisdnCsvfile"
gsutil rm gs://$sub2msisdnCsvfile

echo "Finished cleanup for the export $exportId"