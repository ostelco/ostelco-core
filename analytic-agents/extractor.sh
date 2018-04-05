#!/bin/bash


# Intent: Eventually this script will look into the consumption table,
#         and extract consumption data for some interval of time (e.g.
#         last week,  quarter, day, hour or whatever), and deliver it as a
#         nice, pseudo-anynomized csv file in google cloud storage, ready
#         for consumption by authorized users and agents.

# Setting up the source coordinate
SOURCE_ACCOUNT=pantel-2decb
SOURCE_DATASET=data_consumption
SOURCE_TABLE=hourly_consumption
SOURCE_TABLE="${SOURCE_DATASET}.${SOURCE_TABLE}"


# The hourly_consumption table contains data where the
# timestamp-column is a "datime" typed column, not a
# timestamp typed coumn.  That makes it a little tricky for
# us to use time series based query mechanisms, so therefore
# we add a procedure for converting that table into
# a timestamp-based version.
TRANSFORMED_INPUT="data_consumption.hourly_consumption_timestamped"
CONVERT_TO_TIMESTAMP_CENTRIC_FORMAT="no"

if [ "$CONVERT_TO_TIMESTAMP_CENTRIC_FORMAT" =  "yes" ] ; then
    bq rm -f "$TRANSFORMED_INPUT"
    CONVERSION_SQL="select msisdn, bytes, timestamp(timestamp) as tstamp  from data_consumption.hourly_consumption"
    bq query --destination_table "${TRANSFORMED_INPUT}" "${CONVERSION_SQL}"
fi

# Doing queries of various kinds, internal to the database
DESTINATION_TABLE="todays_consumption"
DESTINATION_TABLE="${SOURCE_DATASET}.${DESTINATION_TABLE}"


# Don't do this while experimenting  with getting exports.

bq rm -f "$DESTINATION_TABLE"
# # XXX Can't figure out how to interpolate using variable in next l inee
# read -r -d '' QUERY <<'EOF'
# 'select * from data_consumption.hourly_consumption'
# EOF

# now() returns microseconds since epoch.
# 604800000 is number of milliseconds in a week, and
# msec_to_timestamp converts from millliseconds since epoch to timestamp.
QUERY='
SELECT msisdn, bytes, tstamp, timestamp_to_sec(tstamp) as secsSinceEpoch 
FROM  data_consumption.hourly_consumption_timestamped
WHERE
  tstamp BETWEEN msec_to_timestamp((now()/1000)-604800000)
  AND current_timestamp()
  ORDER BY msisdn,tstamp
'
echo "Query is $QUERY"
bq query  --destination_table "$DESTINATION_TABLE" "$QUERY"


# Setting up the destination coordinate
BUCKET_NAME=rmz-test-bucket
FILENAME=hourly-consumption.csv
DESTINATION_BUCKET="gs://${BUCKET_NAME}/${FILENAME}"
bq extract  $DESTINATION_TABLE $DESTINATION_BUCKET

