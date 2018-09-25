#!/bin/bash

# This script is used to inject an image tag for PRIME taken from the PRIME_TAG environment variable
# into the prime-prod-values.yaml (the values file used for production deployment of the PRIME helm chart).
# The script is used in the pipeline after new PRIME image is created and before making a PR from develop into master.

# This script should NOT be used to inject secrets into the values file as this file is version controlled in git.

rm -f prime-prod-values.yaml temp.yml  
( echo "cat <<EOF >prime-prod-values.yaml";
  cat prime-prod-values-template.yaml;
  echo "EOF";
) >temp.yml
. temp.yml