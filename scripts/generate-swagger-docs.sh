#!/usr/bin/env bash

# Generate swagger documentation for the prime API.
# The output will be placed i the directory  docs/prime-api
# Anything that happened to be in that directory previously will
# be overwritten

# Get the basedir of the ostelco core in the
# local filesystem
BASEDIR=$($(cd $(dirname "$0")/.. ); pwd)

# Setting up source and destination directories
SWAGGER_DOC_DIR=${BASEDIR}/docs/prime-api
SWAGGER_GEN_DIR=${BASEDIR}/.swagger_gen_dir
SWAGGER_YAML_FILE=${BASEDIR}/prime/infra/prod/prime-api.yaml

rm -rf $SWAGGER_DOC_DIR
mkdir $SWAGGER_DOC_DIR

rm -rf $SWAGGER_GEN_DIR
mkdir $SWAGGER_GEN_DIR
cd $SWAGGER_GEN_DIR

swagger-codegen generate -i "${SWAGGER_YAML_FILE}" -l swagger
swagger-gen -d $SWAGGER_DOC_DIR swagger.json

rm -rf ${SWAGGER_GEN_DIR}
