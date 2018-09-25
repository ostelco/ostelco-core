#!/bin/bash
#set -x

exportId=$1
if [ -z "$1" ]; then
    echo "To convert subscribers, specify the id of the export operation"
    exit
fi
exportId=${exportId//-}
exportId=${exportId,,}
projectId=pantel-2decb

csvfile=$projectId-dataconsumption-export/${exportId}-resultsegment-pseudoanonymized.csv
outputCsvfile=$projectId-dataconsumption-export/${exportId}-resultsegment-cleartext.csv
inputSubscriberTable=exported_pseudonyms.${exportId}_pseudo_subscriber
subscriberPseudonymsTable=exported_pseudonyms.${exportId}_subscriber
outputSubscriberTable=exported_pseudonyms.${exportId}_clear_subscriber


echo "Importing data from csv $csvfile"
bq --location=EU load --replace --source_format=CSV $projectId:$inputSubscriberTable gs://$csvfile /subscriber-schema.json
echo "Exported data to $inputSubscriberTable"

echo "Creating table $dataConsumptionTable"
# SQL for joining pseudonym & hourly consumption tables.
read -r -d '' sqlForJoin << EOM
SELECT
   DISTINCT(sub.subscriberId) as pseudoId, ps.subscriberId
FROM
   \`$inputSubscriberTable\` as sub
JOIN
  \`$subscriberPseudonymsTable\` as ps
ON  ps.pseudoid = sub.subscriberId
EOM

# Run the query using bq & dump results to the new table
bq --location=EU --format=none query --destination_table $outputSubscriberTable --replace --use_legacy_sql=false $sqlForJoin
echo "Created table $outputSubscriberTable"

echo "Exporting data to csv $outputCsvfile"
bq --location=EU extract --destination_format=CSV $outputSubscriberTable gs://$outputCsvfile
echo "Exported data to gs://$outputCsvfile"
