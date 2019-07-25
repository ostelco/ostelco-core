#!/bin/bash

# exit if failed
set -e

echo "prime waiting for neo4j to launch on 7687..."

while ! nc -z neo4j 7687; do
  sleep 1 # wait for 1 second before check again
done

echo "neo4j launched"

echo "prime waiting Datastore emulator to launch on datastore-emulator:8081..."

ds=$(curl --silent  http://datastore-emulator:8081  | head -n1)
until [[ $ds == 'Ok' ]] ; do
    printf 'prime waiting for Datastore emulator to launch...'
    sleep 5
    ds=$(curl --silent http://datastore-emulator:8081  | head -n1)
done

echo "Datastore emulator launched"

echo "prime waiting pubsub emulator to launch on pubsub-emulator:8085..."

ds=$(curl --silent  http://pubsub-emulator:8085  | head -n1)
until [[ $ds == 'Ok' ]] ; do
    printf 'prime waiting for pubsub emulator to launch...'
    sleep 5
    ds=$(curl --silent http://pubsub-emulator:8085  | head -n1)
done

echo "Pubsub emulator launched"

echo "Creating topics...."

# For Analytics
curl  -X PUT pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/topics/active-users
curl  -X PUT pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/topics/data-traffic
curl  -X PUT pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/topics/purchase-info

# For Stripe
curl -X PUT pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/topics/stripe-event

# For OCS API
curl  -X PUT pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/topics/ocs-ccr
curl  -X PUT pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/topics/ocs-cca
curl  -X PUT pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/topics/ocs-activate

echo "Done creating topics"

echo "Creating subscriptions...."

# For Analytics
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/data-traffic","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/test-pseudo
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/purchase-info","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/purchase-info-sub

# For Stripe
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/stripe-event","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/stripe-event-store-sub
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/stripe-event","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/stripe-event-report-sub
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/stripe-event","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/stripe-event-recurring-payment-sub

# For OCS API
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/ocs-ccr","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/ocs-ccr-sub
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/ocs-cca","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/ocsgw-cca-sub
curl -X PUT -H "Content-Type: application/json" -d '{"topic":"projects/'${GCP_PROJECT_ID}'/topics/ocs-activate","ackDeadlineSeconds":10}' pubsub-emulator:8085/v1/projects/${GCP_PROJECT_ID}/subscriptions/ocsgw-activate-sub
 
echo "Done creating subscriptions"

# Forward the local port 9090 to datastore-emulator:8081
if ! hash socat 2>/dev/null
then
  echo "socat not installed."
  exit 1
fi

socat TCP-LISTEN:9090,fork TCP:datastore-emulator:8081 &

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    -jar /prime.jar server /config/config.yaml
