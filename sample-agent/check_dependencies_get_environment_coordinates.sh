#!/bin/bash

## Intended to be sourced by other programs


#
# Check for dependencies
#

DEPENDENCIES="gcloud kubectl gsutil"

for dep in $DEPENDENCIES ; do
    if [[ -z $(which $dep) ]] ; then
	echo "ERROR: Could not find dependency $dep"
    fi
done

#
#  Figure out relevant parts of the environment and check their
#  sanity.
#


PROJECT_ID=$(gcloud config get-value project)

if [[ -z "$PROJECT_ID" ]] ; then
    echo "ERROR: Unknown google project ID"
    exit 1
fi


EXPORTER_PODNAME=$(kubectl get pods | grep exporter- | awk '{print $1}')
if [[ -z "$EXPORTER_PODNAME" ]] ; then
    echo "ERROR: Unknown exporter podname"
    exit 1
fi


