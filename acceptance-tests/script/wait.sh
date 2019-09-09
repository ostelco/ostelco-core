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
echo "       prime  answers incoming http requests"

echo "Waiting for sm-dp+ to be minimally up"
while ! nc -z smdp-plus-emulator 8080; do
  sleep 0.1 # wait for 1/10 of the second before check again
done
echo "       sm-dp+  answers incoming http requests"


echo "Waiting for sm-dp+ to be properly up (ES2+ endpoint check)"


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

echo "SM-DP+ launched"

echo "Add profiles into SM-DP+"
# XXX Still needs a little tweaking

curl  -H 'Content-Type: text/plain' -X PUT --data-binary @-  http://prime:8080/ostelco/sim-inventory/Foo/import-batch/profilevendor/Bar?initialHssState=ACTIVATED <<EOF
ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE
8901000000000000001, 310150000000000, 4790900700,,,,,IPHONE_PROFILE_2
8901000000000000019, 310150000000001, 4790900701,,,,,IPHONE_PROFILE_2
8901000000000000027, 310150000000002, 4790900702,,,,,IPHONE_PROFILE_2
8901000000000000035, 310150000000003, 4790900703,,,,,IPHONE_PROFILE_2
8901000000000000043, 310150000000004, 4790900704,,,,,IPHONE_PROFILE_2
8901000000000000050, 310150000000005, 4790900705,,,,,IPHONE_PROFILE_2
8901000000000000068, 310150000000006, 4790900706,,,,,IPHONE_PROFILE_2
8901000000000000076, 310150000000007, 4790900707,,,,,IPHONE_PROFILE_2
8901000000000000084, 310150000000008, 4790900708,,,,,IPHONE_PROFILE_2
8901000000000000092, 310150000000009, 4790900709,,,,,IPHONE_PROFILE_2
8901000000000000100, 310150000000010, 4790900710,,,,,IPHONE_PROFILE_2
8901000000000000118, 310150000000011, 4790900711,,,,,IPHONE_PROFILE_2
8901000000000000126, 310150000000012, 4790900712,,,,,IPHONE_PROFILE_2
8901000000000000134, 310150000000013, 4790900713,,,,,IPHONE_PROFILE_2
8901000000000000142, 310150000000014, 4790900714,,,,,IPHONE_PROFILE_2
8901000000000000159, 310150000000015, 4790900715,,,,,IPHONE_PROFILE_2
8901000000000000167, 310150000000016, 4790900716,,,,,IPHONE_PROFILE_2
8901000000000000175, 310150000000017, 4790900717,,,,,IPHONE_PROFILE_2
8901000000000000183, 310150000000018, 4790900718,,,,,IPHONE_PROFILE_2
8901000000000000191, 310150000000019, 4790900719,,,,,IPHONE_PROFILE_2
8901000000000000209, 310150000000020, 4790900720,,,,,IPHONE_PROFILE_2
8901000000000000217, 310150000000021, 4790900721,,,,,IPHONE_PROFILE_2
8901000000000000225, 310150000000022, 4790900722,,,,,IPHONE_PROFILE_2
8901000000000000233, 310150000000023, 4790900723,,,,,IPHONE_PROFILE_2
8901000000000000241, 310150000000024, 4790900724,,,,,IPHONE_PROFILE_2
8901000000000000258, 310150000000025, 4790900725,,,,,IPHONE_PROFILE_2
8901000000000000266, 310150000000026, 4790900726,,,,,IPHONE_PROFILE_2
8901000000000000274, 310150000000027, 4790900727,,,,,IPHONE_PROFILE_2
8901000000000000282, 310150000000028, 4790900728,,,,,IPHONE_PROFILE_2
8901000000000000290, 310150000000029, 4790900729,,,,,IPHONE_PROFILE_2
8901000000000000308, 310150000000030, 4790900730,,,,,IPHONE_PROFILE_2
8901000000000000316, 310150000000031, 4790900731,,,,,IPHONE_PROFILE_2
8901000000000000324, 310150000000032, 4790900732,,,,,IPHONE_PROFILE_2
8901000000000000332, 310150000000033, 4790900733,,,,,IPHONE_PROFILE_2
8901000000000000340, 310150000000034, 4790900734,,,,,IPHONE_PROFILE_2
8901000000000000357, 310150000000035, 4790900735,,,,,IPHONE_PROFILE_2
8901000000000000365, 310150000000036, 4790900736,,,,,IPHONE_PROFILE_2
8901000000000000373, 310150000000037, 4790900737,,,,,IPHONE_PROFILE_2
8901000000000000381, 310150000000038, 4790900738,,,,,IPHONE_PROFILE_2
8901000000000000399, 310150000000039, 4790900739,,,,,IPHONE_PROFILE_2
8901000000000000407, 310150000000040, 4790900740,,,,,IPHONE_PROFILE_2
8901000000000000415, 310150000000041, 4790900741,,,,,IPHONE_PROFILE_2
8901000000000000423, 310150000000042, 4790900742,,,,,IPHONE_PROFILE_2
8901000000000000431, 310150000000043, 4790900743,,,,,IPHONE_PROFILE_2
8901000000000000449, 310150000000044, 4790900744,,,,,IPHONE_PROFILE_2
8901000000000000456, 310150000000045, 4790900745,,,,,IPHONE_PROFILE_2
8901000000000000464, 310150000000046, 4790900746,,,,,IPHONE_PROFILE_2
8901000000000000472, 310150000000047, 4790900747,,,,,IPHONE_PROFILE_2
8901000000000000480, 310150000000048, 4790900748,,,,,IPHONE_PROFILE_2
8901000000000000498, 310150000000049, 4790900749,,,,,IPHONE_PROFILE_2
8901000000000000506, 310150000000050, 4790900750,,,,,IPHONE_PROFILE_2
8901000000000000514, 310150000000051, 4790900751,,,,,IPHONE_PROFILE_2
8901000000000000522, 310150000000052, 4790900752,,,,,IPHONE_PROFILE_2
8901000000000000530, 310150000000053, 4790900753,,,,,IPHONE_PROFILE_2
8901000000000000548, 310150000000054, 4790900754,,,,,IPHONE_PROFILE_2
8901000000000000555, 310150000000055, 4790900755,,,,,IPHONE_PROFILE_2
8901000000000000563, 310150000000056, 4790900756,,,,,IPHONE_PROFILE_2
8901000000000000571, 310150000000057, 4790900757,,,,,IPHONE_PROFILE_2
8901000000000000589, 310150000000058, 4790900758,,,,,IPHONE_PROFILE_2
8901000000000000597, 310150000000059, 4790900759,,,,,IPHONE_PROFILE_2
8901000000000000605, 310150000000060, 4790900760,,,,,IPHONE_PROFILE_2
8901000000000000613, 310150000000061, 4790900761,,,,,IPHONE_PROFILE_2
8901000000000000621, 310150000000062, 4790900762,,,,,IPHONE_PROFILE_2
8901000000000000639, 310150000000063, 4790900763,,,,,IPHONE_PROFILE_2
8901000000000000647, 310150000000064, 4790900764,,,,,IPHONE_PROFILE_2
8901000000000000654, 310150000000065, 4790900765,,,,,IPHONE_PROFILE_2
8901000000000000662, 310150000000066, 4790900766,,,,,IPHONE_PROFILE_2
8901000000000000670, 310150000000067, 4790900767,,,,,IPHONE_PROFILE_2
8901000000000000688, 310150000000068, 4790900768,,,,,IPHONE_PROFILE_2
8901000000000000696, 310150000000069, 4790900769,,,,,IPHONE_PROFILE_2
8901000000000000704, 310150000000070, 4790900770,,,,,IPHONE_PROFILE_2
8901000000000000712, 310150000000071, 4790900771,,,,,IPHONE_PROFILE_2
8901000000000000720, 310150000000072, 4790900772,,,,,IPHONE_PROFILE_2
8901000000000000738, 310150000000073, 4790900773,,,,,IPHONE_PROFILE_2
8901000000000000746, 310150000000074, 4790900774,,,,,IPHONE_PROFILE_2
8901000000000000753, 310150000000075, 4790900775,,,,,IPHONE_PROFILE_2
8901000000000000761, 310150000000076, 4790900776,,,,,IPHONE_PROFILE_2
8901000000000000779, 310150000000077, 4790900777,,,,,IPHONE_PROFILE_2
8901000000000000787, 310150000000078, 4790900778,,,,,IPHONE_PROFILE_2
8901000000000000795, 310150000000079, 4790900779,,,,,IPHONE_PROFILE_2
8901000000000000803, 310150000000080, 4790900780,,,,,IPHONE_PROFILE_2
8901000000000000811, 310150000000081, 4790900781,,,,,IPHONE_PROFILE_2
8901000000000000829, 310150000000082, 4790900782,,,,,IPHONE_PROFILE_2
8901000000000000837, 310150000000083, 4790900783,,,,,IPHONE_PROFILE_2
8901000000000000845, 310150000000084, 4790900784,,,,,IPHONE_PROFILE_2
8901000000000000852, 310150000000085, 4790900785,,,,,IPHONE_PROFILE_2
8901000000000000860, 310150000000086, 4790900786,,,,,IPHONE_PROFILE_2
8901000000000000878, 310150000000087, 4790900787,,,,,IPHONE_PROFILE_2
8901000000000000886, 310150000000088, 4790900788,,,,,IPHONE_PROFILE_2
8901000000000000894, 310150000000089, 4790900789,,,,,IPHONE_PROFILE_2
8901000000000000902, 310150000000090, 4790900790,,,,,IPHONE_PROFILE_2
8901000000000000910, 310150000000091, 4790900791,,,,,IPHONE_PROFILE_2
8901000000000000928, 310150000000092, 4790900792,,,,,IPHONE_PROFILE_2
8901000000000000936, 310150000000093, 4790900793,,,,,IPHONE_PROFILE_2
8901000000000000944, 310150000000094, 4790900794,,,,,IPHONE_PROFILE_2
8901000000000000951, 310150000000095, 4790900795,,,,,IPHONE_PROFILE_2
8901000000000000969, 310150000000096, 4790900796,,,,,IPHONE_PROFILE_2
8901000000000000977, 310150000000097, 4790900797,,,,,IPHONE_PROFILE_2
8901000000000000985, 310150000000098, 4790900798,,,,,IPHONE_PROFILE_2
8901000000000000993, 310150000000099, 4790900799,,,,,IPHONE_PROFILE_2
EOF

echo " ... HLR preactivated rofiles loaded into prime"

java -cp '/acceptance-tests.jar' org.junit.runner.JUnitCore org.ostelco.at.TestSuite