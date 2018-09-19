# This docker image contains Python and Gcloud
# It is used for generating proto files for the swagger API spec and updating the Google endpoints.
# It is pushed manually as: eu.gcr.io/pi-ostelco-dev/python-gcloud
FROM python:3.6.6-jessie

RUN pip install grpcio grpcio-tools && apt-get update && apt-get install lsb-release && \
    export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)" && \
    echo "deb http://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - && \
    apt-get update -y && apt-get install google-cloud-sdk -y