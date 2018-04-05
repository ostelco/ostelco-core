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
SOURCE_COORDINATE="${SOURCE_DATASET}.${SOURCE_TABLE}"


# The hourly_consumption table contains data where the
# timestamp-column is a "datime" typed column, not a
# timestamp typed coumn.  That makes it a little tricky for
# us to use time series based query mechanisms, so therefore
# we add a procedure for converting that table into
# a timestamp-based version.
CONVERT_TO_TIMESTAMP_CENTRIC_FORMAT="no"

if [ "$CONVERT_TO_TIMESTAMP_CENTRIC_FORMAT" =  "yes" ] ; then
    TRANSFORMED_INPUT="data_consumption.hourly_consumption_timestamped"
    bq rm -f "$TRANSFORMED_INPUT"
    CONVERSION_SQL="select msisdn, bytes, timestamp(timestamp) as tstamp  from data_consumption.hourly_consumption"
    bq query --destination_table "${TRANSFORMED_INPUT}" "${CONVERSION_SQL}"
fi


# Doing queries of various kinds, internal to the database
DESTINATION_TABLE="todays_consumption"
DESTINATION_COORDINATE="${SOURCE_DATASET}.${DESTINATION_TABLE}"


bq rm -f "$DESTINATION_COORDINATE"
# # XXX Can't figure out how to interpolate using variable in next l inee
# read -r -d '' QUERY <<'EOF'
# 'select * from data_consumption.hourly_consumption'
# EOF

QUERY='select * from data_consumption.hourly_consumption'
echo "Query is $QUERY"
bq query  --destination_table "$DESTINATION_COORDNATE" "$QUERY" limit 30


# # Setting up the destination coordinate
# BUCKET_NAME=rmz-test-bucket
# FILENAME=hourly-consumption.csv
# DESTINATION_COORDINATE="gs://${BUCKET_NAME}/${FILENAME}"
# bq extract $SOURCE_COORDINATE $DESTINATION_COORDINATE

