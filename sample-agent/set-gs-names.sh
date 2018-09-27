#!/bin/bash


if [[ -z "$PROJECT_ID" ]] ; then
    echo "$0 PROJECT_ID variable not set, cannot determine google filestore coordinates"
    exit 1
fi


if [[ -z "$EXPORT_ID" ]] ; then
    echo "$0 EXPORT_ID variable not set, cannot determine google filestore coordinates"
    exit 1
fi


PURCHASES_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-purchases.csv"
SUB_2_MSISSDN_MAPPING_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID-sub2msisdn.csv"
CONSUMPTION_GS="gs://${PROJECT_ID}-dataconsumption-export/$EXPORT_ID.csv"
RESULT_SEGMENT_PSEUDO_GS="gs://${PROJECT_ID}-dataconsumption-export/${EXPORT_ID}-resultsegment-pseudoanonymized.csv"
RESULT_SEGMENT_CLEAR_GS="gs://${PROJECT_ID}-dataconsumption-export/${EXPORT_ID}-resultsegment-cleartext.csv"
