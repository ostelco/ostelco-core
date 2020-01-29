#!/bin/sh
# This script extracts the keys/certs needed to authenticate to the cluster endpoint
# These secrets are needed when configuring kubectl manually (without the magical gcloud command)
# They will be used by Helmsman to connect to the cluster, and therefore need to be always updated in the GCS bucket
# each time terraform is applied.

# This script will be executed as part of the circleci workflow after terraform cluster script is applied.

# defining the prefix that identifies which cluster the keys belong to.
prefix="dev_cluster"
# defining the bucket where the secrets are stored.
GCS_BUCKET=gs://pi-ostelco-dev-k8s-key-store
if [ ! -z ${PI_DEV_K8S_KEY_STORE_BUCKET} ]; then 
  GCS_BUCKET=${PI_DEV_K8S_KEY_STORE_BUCKET}
fi

if [ "$CLUSTER" = "prod" ]; then
   prefix="prod_cluster"
   GCS_BUCKET=gs://pi-ostelco-prod-k8s-key-store
   if [ ! -z ${PI_PROD_K8S_KEY_STORE_BUCKET} ]; then 
      GCS_BUCKET=${PI_PROD_K8S_KEY_STORE_BUCKET}
   fi
fi


# saving the scecrets into files
echo "Importing keys from terraform into local files for [ $CLUSTER ] cluster ... "

mkdir keys
echo $(terraform output ${prefix}_client_certificate) | base64 -d > keys/${prefix}_client_certificate.crt
echo $(terraform output ${prefix}_client_key) | base64 -d > keys/${prefix}_client_key.key
echo $(terraform output ${prefix}_ca_certificate) | base64 -d > keys/${prefix}_cluster_ca.crt
echo $(terraform output ${prefix}_endpoint) > endpoint.txt
echo $(terraform output ${prefix}_ambassador_ip) > static_ip.txt

# push secrets to GCS and cleanup the local file system
if [[ -r keys/${prefix}_client_certificate.crt ]] && [[ -r keys/${prefix}_client_key.key ]] && [[ -r keys/${prefix}_cluster_ca.crt ]];then
  echo "Keys found. Pushing keys to GCS ... "
  gsutil cp -r keys  ${GCS_BUCKET}
  if [ $? -ne 0 ] ; then
    echo "Could not copy keys to GCS bucket."
    exit 1
  else
    echo "Cleaning up local file system ... "
    #rm -fr keys && echo "Keys cleanup complete." || echo "Something went wrong during keys cleanup."
  fi

else
  echo "Something went wrong with reading terraform output variables! The files (or some of them) are empty. Deleting local keys directory, and aborting!"
  rm -fr keys
  exit 1
fi