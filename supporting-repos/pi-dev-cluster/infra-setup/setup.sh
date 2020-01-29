#!/bin/bash

### utils

varIsSet()
{
    varname=$1
    required=$2
    default=$3
    if [ -z ${!varname} ]; then
      if  ${required}; then
         echo "ERROR: variable ${varname} is required but not set."
         exit 1;
      else
         if [ ! -z ${default} ]; then
            eval "$varname=$default"
            echo "INFO: Setting ${varname} to default value: ${default}"
         else
            echo "WARNING: varibale ${varname} is not set. Proceeding without it."
        fi 
      fi
    fi     
}

varIsSet PROJECT true
# varIsSet GOOGLE_CREDENTIALS true
varIsSet BUCKETS_LOCATION false europe-west1
varIsSet BUCKETS_LOCATION_SG false asia-southeast1
varIsSet TARGET_ENV false dev
varIsSet DATAFLOW_REGION true 

# GCloud setup
echo "INFO: seting up gcloud project and auth ..."
export CLOUDSDK_CORE_PROJECT=${PROJECT}
# echo $GOOGLE_CREDENTIALS > /tmp/gcloud-service-key.json
# export GOOGLE_APPLICATION_CREDENTIALS=/tmp/gcloud-service-key.json
#gcloud auth activate-service-account --key-file=/tmp/gcloud-service-key.json
gcloud auth login
gcloud config set project ${PROJECT}

# cloning ostelco-prime locally
echo "INFO: clonging the ostelco-core git repo ..."
git clone https://github.com/ostelco/ostelco-core.git /ostelco-core/

# creating Google Cloud Buckets

echo "INFO: creating cloud storage buckets ..."
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION} gs://${PROJECT}-prime-files/
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION} gs://${PROJECT}-terraform-state/
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION} gs://${PROJECT}-k8s-key-store/
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION} gs://${PROJECT}-gradle-cache/
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION} gs://${PROJECT}-data-traffic/
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION} gs://${PROJECT}-svc-acct-keys/
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION} gs://${PROJECT}-ekyc-scandata-global/
gsutil mb -c REGIONAL -l ${BUCKETS_LOCATION_SG} gs://${PROJECT}-ekyc-scandata-sg/

# # creating prime-service-account
echo "INFO: creating prime service account ..."
PRIME_SERVICE_ACCOUNT_NAME=prime-service-account
gcloud iam service-accounts create ${PRIME_SERVICE_ACCOUNT_NAME} --display-name ${PRIME_SERVICE_ACCOUNT_NAME} --project ${PROJECT}

echo "INFO: granting permissions to the prime service account ..."
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/servicemanagement.serviceController
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/iap.httpsResourceAccessor
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/iam.serviceAccountTokenCreator
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/cloudtrace.agent 
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/editor
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/cloudsql.admin  
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/cloudkms.cryptoKeyEncrypterDecrypter

echo "INFO: generating PRIME service account key and pushing it to google cloud bucket ..."
gcloud iam service-accounts keys create prime-sa-key.json --iam-account ${PRIME_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com  
gsutil cp prime-sa-key.json gs://${PROJECT}-svc-acct-keys/
rm prime-sa-key.json

echo "INFO: creating DNS service account ..."
DNS_SERVICE_ACCOUNT_NAME=dns-admin-service-account
gcloud iam service-accounts create ${DNS_SERVICE_ACCOUNT_NAME} --display-name ${DNS_SERVICE_ACCOUNT_NAME} --project ${PROJECT}

echo "INFO: granting permisions to the DNS service acount ..."
gcloud projects add-iam-policy-binding ${PROJECT} --member serviceAccount:${DNS_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com --role roles/dns.admin

echo "INFO: generating DNS service accoutn key and pushing it to google cloud bucket ..."
gcloud iam service-accounts keys create dns-sa-key.json --iam-account ${DNS_SERVICE_ACCOUNT_NAME}@${PROJECT}.iam.gserviceaccount.com  
gsutil cp dns-sa-key.json gs://${PROJECT}-svc-acct-keys/
rm dns-sa-key.json


# creating pubsub topics

echo "INFO: creating pubsub topics ..."
gcloud pubsub topics create purchase-info data-traffic active-users stripe-event
gcloud pubsub topics create ocs-ccr ocs-cca ocs-activate

# creating pubsub subscriptions
echo "INFO: creating pubsub subscriptions ..."
gcloud pubsub subscriptions create test-pseudo --topic data-traffic
gcloud pubsub subscriptions create purchase-info-sub --topic purchase-info
gcloud pubsub subscriptions create stripe-event-store-sub stripe-event-report-sub --topic stripe-event
gcloud pubsub subscriptions create ocs-ccr-sub --topic ocs-ccr
gcloud pubsub subscriptions create ocsgw-cca-sub --topic ocs-cca
gcloud pubsub subscriptions create ocsgw-activate-sub --topic ocs-activate

# deploying Endpoints

echo "INFO: generating protobuf descriptor files ... "
python -m grpc_tools.protoc \
  --include_imports \
  --include_source_info \
  --proto_path=/ostelco-core/ocs-grpc-api/src/main/proto \
  --descriptor_set_out=ocs_descriptor.pb \
  ocs.proto

python -m grpc_tools.protoc \
  --include_imports \
  --include_source_info \
  --proto_path=/ostelco-core/analytics-grpc-api/src/main/proto \
  --descriptor_set_out=metrics_descriptor.pb \
  prime_metrics.proto

echo "INFO: deploying cloud endpoints ..."
gcloud endpoints services deploy ocs_descriptor.pb /ostelco-core/prime/infra/${TARGET_ENV}/ocs-api.yaml
gcloud endpoints services deploy metrics_descriptor.pb /ostelco-core/prime/infra/${TARGET_ENV}/metrics-api.yaml
gcloud endpoints services deploy /ostelco-core/prime/infra/${TARGET_ENV}/prime-client-api.yaml
gcloud endpoints services deploy /ostelco-core/prime/infra/${TARGET_ENV}/prime-huston-api.yaml
gcloud endpoints services deploy /ostelco-core/prime/infra/${TARGET_ENV}/prime-webhooks.yaml  # alvin

# deploying Dataflows

echo "INFO: deploying active-users and purchase-records dataflow jobs ..."
gcloud dataflow jobs run active-users \
    --gcs-location gs://dataflow-templates/2018-11-26-00/PubSub_to_BigQuery \
    --region ${DATAFLOW_REGION} \
    --parameters inputTopic=projects/${PROJECT}/topics/active-users,outputTableSpec=${PROJECT}:ocs_gateway_dev.raw_activeusers

gcloud dataflow jobs run purchase-records \
    --gcs-location gs://dataflow-templates/2018-11-26-00/PubSub_to_BigQuery \
    --region ${DATAFLOW_REGION} \
    --parameters inputTopic=projects/${PROJECT}/topics/purchase-info,outputTableSpec=${PROJECT}:purchases.raw_purchases

# java -Dfile.encoding=UTF-8 -jar /dataflow-pipelines.jar --project=${PROJECT} \
#         --runner=DataflowRunner \
#         --stagingLocation=gs://${PROJECT}-data-traffic/staging/ \
#         --region=${DATAFLOW_REGION} \
#         --jobName=data-traffic \
#         --pubsubTopic=data-traffic \
#         --dataset=data_consumption \
#         --update=true

echo "INFO: the script setup is complete!"
