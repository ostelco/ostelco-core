#!/usr/bin/env bash

for MSISDN in {178..190}
do
    echo firebase --project pantel-2decb  --data '0' database:set /v2/balance/4790300${MSISDN}
done
