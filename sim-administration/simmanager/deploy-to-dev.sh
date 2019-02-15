#!/usr/bin/env bash


##
##  Utility script to help build a new  docker image of the sim manager
##  and to deploy it to  a kubernetes cluster.
##


DEPENDENCIES="kubectl docker gcloud"
for DEP in $DEPENDENCIDES ; do 
    if  [[ -z "$(which  $DEP)" ]] ; then
	echo "$0 ERROR: Missing dependency $DEP"
	exit 1
    fi
done

TARGET_CLUSTER="dev-cluster"


# Find the context of the  target cluster that we're deploying to
# and set that as the context to use.
TARGET_CLUSTER_CONTEXT="$(kubectl config get-contexts --output name | grep "$TARGET_CLUSTER")"

if [[ -z "$TARGET_CLUSTER_CONTEXT" ]] ; then
    echo "$0  Could not determine cluster context for target-cluster = '$TARGET_CLUSTER'"
    echo "$0  available clusters are:"
    for cluster in  $(kubectl config get-contexts --output name ) ; do 
	echo "    $cluster"
    done
    echo "$0  Perhaps you should run 'gcloud container clusters get-credentials $TARGET_CLUSTER'"
    echo "$0  to inform kubectl that you intend to use the cluster $TARGET_CLUSTER"
    exit 1
fi

# From now on,  exit on errors
set -e


kubectl config use-context "$TARGET_CLUSTER_CONTEXT"


# Extract the project ID from gcloud and
# latest tag from git.  We'll use both of these when naming the
# docker image we're building.
PROJECT_ID=$(gcloud config get-value project -q 2> /dev/null)
TAG=$(git log -1 --pretty=format:%h)-dev



# Then build a new docker image and pushing it to the
# registry for the project.
echo "Building eu.gcr.io/${PROJECT_ID}/simmanager:${TAG}"

docker build -t eu.gcr.io/${PROJECT_ID}/simmanager:${TAG} .
docker push eu.gcr.io/${PROJECT_ID}/simmanager:${TAG}

# finally deploying it
echo "Deploying eu.gcr.io/${PROJECT_ID}/simmanager:${TAG} to GKE"

# A bit of ghetto-style templating here to create the actual
# config for the kubectl...
sed -e s/SIMMANAGER_VERSION/${TAG}/g simmanager-deploy.yaml | kubectl apply -f -
