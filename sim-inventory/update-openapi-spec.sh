#!/bin/bash

DESTINATION=resources/openapi-sim-inventory--spec.yaml

curl  http://localhost:8080/openapi.yaml > $DESTINATION


