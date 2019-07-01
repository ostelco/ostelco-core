#!/usr/bin/env bash

##
## Upgrade prime directly from workstation to configure canary traffic routing
##

#### sanity checks
if [ -z $1 ] || [ -z $2 ]; then
   echo "ERROR: missing (some) input parameters. Aborting!"
   exit 1
fi


DEPENDENCIES="grep gcloud helm kubectl"
for DEP in ${DEPENDENCIES}; do
    if [[ -z "$(which ${DEP})" ]] ; then
	echo "$0  ERROR: Missing dependency $DEP"
	exit 1
    fi
done

# On error fail.
set -e

# Check that the script ss run from project root
# (figure that out by looking for itself :-)
if [[ ! -f prime/script/change-prime-direct-canary.sh ]] ; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi
####

SERVICE_NAME=$1
CANARY_TYPE=$2
HELM_SET_FLAG=" --set services.$SERVICE_NAME.canary"

case ${CANARY_TYPE} in

  full-weight)
    echo "Setting prime direct traffic for [ $SERVICE_NAME ] service to %100."
    # HELM_SET_FLAG="${HELM_SET_FLAG}.weight=${WEIGHT_OR_HEADER} --set  services.$SERVICE_NAME.canary.headers={}"
    HELM_SET_FLAG="${HELM_SET_FLAG}.weight=100,${HELM_SET_FLAG}.headers=null" 
    ;;

  zero-weight)
    echo "Setting prime direct traffic for [ $SERVICE_NAME ] service to %0."
    # HELM_SET_FLAG="${HELM_SET_FLAG}.weight=${WEIGHT_OR_HEADER} --set  services.$SERVICE_NAME.canary.headers={}"
    HELM_SET_FLAG="${HELM_SET_FLAG}.weight=0,${HELM_SET_FLAG}.headers=null" 
    ;;

  header)
    echo "Setting prime direct traffic for [ $SERVICE_NAME ] service to be based on header."
    HELM_SET_FLAG="${HELM_SET_FLAG}.headers.x-mode=prime-direct"
    ;;

  *)
    echo "ERROR: unknown canary type $CANARY_TYPE"
    exit -1
    ;;
esac


#
# Use the  kubectl context containing the dev-cluster
#
K8S_CONTEXT="$(kubectl config get-contexts --output name | grep pi-ostelco-dev)"
kubectl config use-context ${K8S_CONTEXT}


HELM_RELEASE_NAME="prime-direct"
HELM_CHART="ostelco/prime"
HELM_CHART_VERSION="1.0.1"

#
# Then deploy using helm.
#
echo "Upgrading prime-direct to GKE"

helm repo add ostelco https://storage.googleapis.com/pi-ostelco-helm-charts-repo/
helm repo update
helm upgrade ${HELM_RELEASE_NAME} ${HELM_CHART} --version ${HELM_CHART_VERSION} --reuse-values ${HELM_SET_FLAG}
