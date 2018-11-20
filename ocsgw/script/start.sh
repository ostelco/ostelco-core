#!/bin/sh

set -e
CONFIGURATION_BUCKET="ocsgw-${VPC_ENV}-${INSTANCE}-bucket"

# for acceptance tests and local testing the
# configuration is mounted before container
# is launched. In GCP we will mount the configuration
# inside the container.
if [ ! -d /config ]; then
  # mount config from gcloud bucket
  mkdir /config
  gcsfuse ${CONFIGURATION_BUCKET} /config
fi

# this is set here to not interfere with gcsfuse credentials
export GOOGLE_APPLICATION_CREDENTIALS=/config/pantel-prod.json

# Start app for production
exec java \
    -Dfile.encoding=UTF-8 \
    -Dlogback.configurationFile=/config/logback.xml \
    -jar /ocsgw.jar
