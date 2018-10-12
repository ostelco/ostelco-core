#!/bin/bash

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /dataflow-pipelines.jar \
        --project=$PROJECT \
        --runner=DataflowRunner \
        --stagingLocation=gs://data-traffic/staging/ \
        --region=europe-west1 \
        --jobName=$JOB_NAME \
        --pubsubTopic=$PUBSUB_TOPIC \
        --dataset=$DATASET \
        --update=$UPDATING
