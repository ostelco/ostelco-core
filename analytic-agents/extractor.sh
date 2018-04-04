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

# Doing queries of various kinds, internal to the database
DESTINATION_TABLE="todays_consumption"
DESTINATION_COORDINATE="${SOURCE_DATASET}.${DESTINATION_TABLE}"


bq rm -f "$DESTINATION_COORDINATE"
# XXX Can't figure out how to interpolate using variable in next l inee
QUERY='select * from data_consumption.hourly_consumption'
bq query  --destination_table "$DESTINATION_COORDNATE" "$QUERY" limit 1000


# # Setting up the destination coordinate
# BUCKET_NAME=rmz-test-bucket
# FILENAME=hourly-consumption.csv
# DESTINATION_COORDINATE="gs://${BUCKET_NAME}/${FILENAME}"
# bq extract $SOURCE_COORDINATE $DESTINATION_COORDINATE

