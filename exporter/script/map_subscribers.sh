#!/bin/bash
#set -x

exportId=$1
if [ -z "$1" ]; then
    echo "To convert subscribers, specify the id of the export operation"
    exit
fi
exportId=${exportId//-}
exportId=${exportId,,}
# Set the projectId
if [[ -z "${PROJECT_ID}" ]]; then
  projectId=pantel-2decb
else
  projectId="${PROJECT_ID}"
fi

csvfile=$projectId-dataconsumption-export/${exportId}-resultsegment-pseudoanonymized.csv
outputCsvfile=$projectId-dataconsumption-export/${exportId}-resultsegment-cleartext.csv
inputSubscriberTable=exported_pseudonyms.${exportId}_pseudo_subscriber
subscriberPseudonymsTable=exported_pseudonyms.${exportId}_subscriber
outputSubscriberTable=exported_pseudonyms.${exportId}_clear_subscriber


echo "Importing data from csv $csvfile"
bq --location=EU load --replace --source_format=CSV $projectId:$inputSubscriberTable gs://$csvfile /subscriber-schema.json
echo "Exported data to $inputSubscriberTable"

echo "Creating table $outputSubscriberTable"
# SQL for joining pseudonym & hourly consumption tables.
read -r -d '' sqlForJoin << EOM
CREATE TEMP FUNCTION URLDECODE(url STRING) AS ((
  SELECT SAFE_CONVERT_BYTES_TO_STRING(
    ARRAY_TO_STRING(ARRAY_AGG(
        IF(STARTS_WITH(y, '%'), FROM_HEX(SUBSTR(y, 2)), CAST(y AS BYTES)) ORDER BY i
      ), b''))
  FROM UNNEST(REGEXP_EXTRACT_ALL(url, r"%[0-9a-fA-F]{2}|[^%]+")) AS y WITH OFFSET AS i
));

SELECT
   DISTINCT(sub.subscriberId) as pseudoId, URLDECODE(ps.subscriberId) as subscriberId
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
