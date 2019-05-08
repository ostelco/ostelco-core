# Module Dataflow pipelines

## Setup

1. In Google Cloud Platform, need to setup a project with PubSubIO, Dataflow and BigQueryIO.
2. Keep the `prime-service-account.json` auth file in config folder.

## Package
 
    gradle clean build

## Deploy to GCP

    docker-compose up --build