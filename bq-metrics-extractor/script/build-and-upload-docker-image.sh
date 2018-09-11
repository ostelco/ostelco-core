#!/bin/sh

##
## Build a new jar file, then a new docker image, then
## upload the docker image to a google docker
## repository.
##


# Exit on failure
set -e

# Check for dependencies
DEPENDENCIES="gradle docker gcloud"
for dep in $DEPENDENCIES ; do
    if [[ -z "$(type $dep)" ]] ; then
	echo "Could not find dependency $dep, bailing out"
	exit 1
    fi
done

# Set destination

GCLOUD_PROJECT_NAME="pantel-2decb"
CONTAINER_NAME="bq-metrics-extractor"
GCLOUD_REPO_NAME="eu.gcr.io"



# Log into the appropriate google account and prepare to build&upload
# XXX Couldn't figure out how to make this work well in a script, but
#     that should be solved, therefore I'm keeping the dead code instead
#     of doing the right thing according to the project coding standard
#     and killing it off.
# gcloud auth login
# gcloud auth configure-docker

# Build the java .jar application from sources
gradle build

# Build the docker container
CONTAINER_ID=$(docker build . | grep "Successfully built" | awk '{print $3}')
echo "Built container $CONTAINER_ID"

# Tag and push the docker container to the google repo
echo "Tagging and pushing container"
THE_TAG="${GCLOUD_REPO_NAME}/${GCLOUD_PROJECT_NAME}/${CONTAINER_NAME}"
docker tag ${CONTAINER_ID} ${THE_TAG}
docker push ${THE_TAG}
