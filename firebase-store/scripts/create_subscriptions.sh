#!/usr/bin/env bash

export MSISDN=
export EMAIL=

export URL_ENCODED_EMAIL=$(echo "$EMAIL" | sed 's/\./%2E/g' | sed 's/@/%40/g')
echo firebase --project pantel-2decb  --data "\"$MSISDN\"" database:set /v2/subscriptions/"$URL_ENCODED_EMAIL"
