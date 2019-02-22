#!/usr/bin/env bash

##
## Deploy prime directly from workstation
##


DEPENDENCIES="./gradlew docker sed grep tr awk gcloud"
for DEP in $DEPENDENCIES; do
    if [[ -z "$(which $DEP)" ]] ; then
	echo "$0  ERROR: Missing dependency $DEP"
	exit 1
    fi
fi

# On error fail.
set -e

# Check that the script ss run from project root
# (figure that out by looking for itself :-)
if [[ ! -f prime/script/deploy.sh ]] ; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

#
# Use the  kubectl context containing the dev-cluster
#
DEV_CLUSTER_CONTEXT="$(kubectl config get-contexts --output name | grep dev-cluster)"
kubectl config use-context "$DEV_CLUSTER_CONTEXT"

#
# Get the GCP project id by asking gcloud
#
GCP_PROJECT_ID="$(gcloud config get-value project -q)"

#
# Get the version, sha and tag tat we'll use to
# identify the docker image we're about to deploy.
#
PRIME_VERSION="$(./gradlew prime:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG="${PRIME_VERSION}-${SHORT_SHA}-dev"

#
# Report what the variables we're using are
#
echo GCP_PROJECT_ID=${GCP_PROJECT_ID}
echo PRIME_VERSION=${PRIME_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG=${TAG}


#
#  Build the  prime subproject
#
./gradlew prime:clean prime:build

#
# Build tag and push the docker image
#
docker build -t eu.gcr.io/${GCP_PROJECT_ID}/prime:${TAG} prime
docker push eu.gcr.io/${GCP_PROJECT_ID}/prime:${TAG}


#
# Then deploy using kubectl.
#
echo "Deploying prime to GKE"

sed -e 's/PRIME_VERSION/'"${TAG}"'/g; s/_GCP_PROJECT_ID/'"${GCP_PROJECT_ID}"'/g' prime/infra/dev/prime.yaml | kubectl apply -f -