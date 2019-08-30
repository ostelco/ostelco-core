#!/bin/bash

set -e

echo "AT waiting ocsgw to launch on 3868..."

while ! nc -z 172.16.238.3 3868; do
  sleep 0.1 # wait for 1/10 of the second before check again
done
echo "      ocs  answers incoming http requests"


echo "AT waiting Prime to launch on 8080..."

while ! nc -z prime 8080; do
  sleep 0.1 # wait for 1/10 of the second before check again
done
echo "       prime+  answers incoming http requests"

echo "Waiting for sm-dp+ to be minimally up"
while ! nc -z smdp-plus-emulator 8080; do
  sleep 0.1 # wait for 1/10 of the second before check again
done
echo "       sm-dp+  answers incoming http requests"


echo "Waiting for sm-dp+ to be properly up"


ES2PLUS_ENDPOINT="http://smdp-plus-emulator:8080"

ICCID="8901000000000000001"
FUNCTION_REQUESTER_EMULATOR="Dunderhonning"
GET_PROFILE_STATUS_PATH="/gsma/rsp2/es2plus/getProfileStatus"
GET_PROFILE_STATUS_PAYLOAD=$(cat <<EOF
{
    "header":{
	"functionRequesterIdentifier": "${FUNCTION_REQUESTER_IDENTIFIER}",
	"functionCallIdentifier": "TX-568"},
    "iccidList":[{"iccid":"${ICCID}"}]
}
EOF
)

PROFILE_STATUS_CMD_URL="${ES2PLUS_ENDPOINT}${GET_PROFILE_STATUS_PATH}"
PROFILE_STATUS_CMD_PAYLOAD=$GET_PROFILE_STATUS_PAYLOAD

CMD_URL="${ES2PLUS_ENDPOINT}${GET_PROFILE_STATUS_PATH}"
CMD_PAYLOAD=$PROFILE_STATUS_CMD_PAYLOAD



## Loop until the result of curling the sm-dp+ emulator is
## satisfactory.
CURL_RESULT=""
until [[ ! -z "$CURL_RESULT" ]] ; do
  CURL_RESULT=$(curl \
    -vvv \
    --header "X-Admin-Protocol: gsma/rsp/v2.0.0" \
    --header "Content-Type: application/json" \
    --request POST \
    --data "$CMD_PAYLOAD" \
    --insecure \
    $CMD_URL)
  ## XXX MISSING: Something that verifies that the result
  ##     is legit.
done



echo "Prime launched"
java -cp '/acceptance-tests.jar' org.junit.runner.JUnitCore org.ostelco.at.TestSuite