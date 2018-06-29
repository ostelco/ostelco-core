#!/usr/bin/env bash

plantuml -tsvg -pipe < puml/purchase-flow.puml > diagrams/purchase-flow.svg
plantuml -tsvg -pipe < puml/purchase-state-diagram.puml > diagrams/purchase-state-diagram.svg
