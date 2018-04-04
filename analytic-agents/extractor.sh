#!/bin/bash


# Setting up the source coordinate
SOURCE_ACCOUNT=pantel-2decb
SOURCE_DATASET=data_consumption
SOURCE_TABLE=hourly_consumption
SOURCE_COORDINATE="${SOURCE_ACCOUNT}:${SOURCE_DATASET}.${SOURCE_TABLE}"

# Doing queries of various kinds, internal to the database.
# bq query --destination_table [PROJECT_ID]:[DATASET].[TABLE] --use_legacy_sql=false '[QUERY]'

# Setting up the destination coordinate
BUCKET_NAME=rmz-test-bucket
FILENAME=hourly-consumption.csv
DESTINATION_COORDINATE="gs://${BUCKET_NAME}/${FILENAME}"
bq extract $SOURCE_COORDINATE $DESTINATION_COORDINATE

