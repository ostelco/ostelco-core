#!/bin/bash -x 

##
## Utility script that can be used to see if the sm-dp+ emulator
## is behaving halfway decently.  Obviously we should also have
## unit tests, integration tests etc., but this script has proven to
## be handy while developing the emulator, so I'm including it for
## completeness/posterity.
##

#ES2PLUS_ENDPOINT="http://0.0.0.0:18080"
ES2PLUS_ENDPOINT="http://127.0.0.1:8080"
FUNCTION_REQUESTER_IDENTIFIER=Dunderhonning
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
#until [[ ! -z "$CURL_RESULT" ]] ; do
  #CURL_RESULT=$(
  curl \
    -vvv \
    --header "X-Admin-Protocol: gsma/rsp/v2.0.0" \
    --header "Content-Type: application/json" \
    --header "Accept: application/json" \
    --request POST \
    --data "$CMD_PAYLOAD" \
    --insecure \
    $CMD_URL
  # )
  ## XXX MISSING: Something that verifies that the result                                                                                                                                                                                  
  ##     is legit.                                                                                                                                                                                                                         
#done


