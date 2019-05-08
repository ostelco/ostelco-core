#!/bin/bash


##
## Map a list of pseudo-anonymized subscriber IDs into clear text
## subscriber identifiers.
##
## Takes a single parameter, the exportID, so usage is:
##
##     ./map_subscribers.sh  8972789sd897987rwefsa9879
##
## Based on the command line parameter, an input file is imported from
## the file storage.  The input is a file named
##
##     gs://$projectId-dataconsumption-export/${exportId}-resultsegment-pseudoanonymized.csv$exportId/
##
## This input file contains a single column, containing pseudoanonymized
## subscriber identifiers.
##
## The script proeduces a single output in the file:
##
##     gs://$projectId-dataconsumption-export/${exportId}-resultsegment-cleartext.csv
##
## It contains two columns, with headers, containing pseudo IDs, and the corresponding
## clear text subscriber ID.
##
##


##
## Check input parameters
##

if [[ $# -ne 1 ]] ; then
    echo "$0 ERROR:  Requires one command line parameter dentifying the export ID"
    exit 1
fi

exportId=$1
if [[ -z "$1" ]]; then
    echo "$0 ERROR: To convert subscribers, specify the id of the export operation"
    exit 1
fi

##
## Calculate locations of things to use.
##

exportId=${exportId//-}
exportId=${exportId,,}
# Set the projectId
if [[ -z "${GCP_PROJECT_ID}" ]]; then
  echo "Missing GCP_PROJECT_ID env var"
  exit
else
  projectId="${GCP_PROJECT_ID}"
fi

csvfile=$projectId-dataconsumption-export/${exportId}-resultsegment-pseudoanonymized.csv
outputCsvfile=$projectId-dataconsumption-export/${exportId}-resultsegment-cleartext.csv
inputSubscriberTable=exported_pseudonyms.${exportId}_pseudo_subscriber
subscriberPseudonymsTable=exported_pseudonyms.${exportId}_subscriber
outputSubscriberTable=exported_pseudonyms.${exportId}_clear_subscriber

##
## Import the from the csv file.
##

echo "$0: INFO Importing data from csv $csvfile"
bq --location=EU load --replace --source_format=CSV $projectId:$inputSubscriberTable gs://$csvfile /subscriber-schema.json
echo "Exported data to $inputSubscriberTable"


##
## Calculate the translation table
##
echo "$0: INFO Creating table $outputSubscriberTable"
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
echo "$0 INFO: Created table $outputSubscriberTable"


##
## Export data to the outut CSV file
##

echo "$0 INFO: Exporting data to csv $outputCsvfile"
bq --location=EU extract --destination_format=CSV $outputSubscriberTable gs://$outputCsvfile
echo "$0 INFO: Exported data to gs://$outputCsvfile"
