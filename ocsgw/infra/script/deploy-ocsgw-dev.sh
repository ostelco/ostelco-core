#!/usr/bin/env bash

set -e

if [ ! -f ocsgw/infra/script/deploy-ocsgw-dev.sh ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

kubectl config use-context $(kubectl config get-contexts --output name | grep dev-cluster)

PROJECT_ID="$(gcloud config get-value project -q)"
OCSGW_VERSION="$(gradle ocsgw:properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')"
SHORT_SHA="$(git log -1 --pretty=format:%h)"
TAG_OCS="${OCSGW_VERSION}-${SHORT_SHA}-dev"
TLS_PROXY_VERSION="$(cat ./tls-proxy/VERSION)"
TAG_TLS_PROXY="${TLS_PROXY_VERSION}-${SHORT_SHA}-dev"

echo PROJECT_ID=${PROJECT_ID}
echo OCSGW_VERSION=${OCSGW_VERSION}
echo SHORT_SHA=${SHORT_SHA}
echo TAG_OCS=${TAG_OCS}
echo TAG_TLS_PROXY=${TAG_TLS_PROXY}
echo TLS_PROXY_VERSION=${TLS_PROXY_VERSION}


#gradle ocsgw:clean ocsgw:build
#docker build -t eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS} ocsgw
#docker push eu.gcr.io/${PROJECT_ID}/ocsgw:${TAG_OCS}

docker build -t eu.gcr.io/${PROJECT_ID}/tls-proxy:${TAG_TLS_PROXY} tls-proxy
docker push eu.gcr.io/${PROJECT_ID}/tls-proxy:${TAG_TLS_PROXY}

docker build -t eu.gcr.io/${PROJECT_ID}/tcp-test:${TAG_TLS_PROXY} tls-proxy/test/tcp-container
docker push eu.gcr.io/${PROJECT_ID}/tcp-test:${TAG_TLS_PROXY}

echo "Deploying ocsgw and TLS proxy to GKE"

sed -e s/OCSGW_VERSION/${TAG_OCS}/g ocsgw/infra/dev/ocsgw.yaml | sed -e s/TLS_PROXY_VERSION/${TAG_TLS_PROXY}/g | kubectl apply -f -