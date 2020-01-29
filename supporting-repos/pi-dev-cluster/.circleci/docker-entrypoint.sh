#!/bin/sh

echo "logging Terraform version ..."
terraform version

if [ "$CLUSTER" = "dev" ] && [ ! -z "${PI_DEV_GOOGLE_CREDENTIALS}" ]; then
  echo $PI_DEV_GOOGLE_CREDENTIALS > /tmp/credentials.json
elif [ "$CLUSTER" = "prod" ] && [ ! -z "${PI_PROD_GOOGLE_CREDENTIALS}" ]; then  
  echo $PI_PROD_GOOGLE_CREDENTIALS > /tmp/credentials.json
else
  echo "Appropriate Google credentials have not been set in the environment. Aborting!"
  exit 1
fi

if [ $? != 0 ]; then
  echo "FAILED to write Google credentials into /tmp/credentials.json. Aborting!"
  exit 1
else
  echo "Successfully populated /tmp/credentials.json"
  
  gcloud auth activate-service-account --key-file /tmp/credentials.json

  if [ $? != 0 ]; then
      echo "FAILED to authenticate to Google cloud. Aborting!"
      exit 1
  else
    echo "Successfully authenticated to Google cloud."
  fi
fi

exec "$@"

