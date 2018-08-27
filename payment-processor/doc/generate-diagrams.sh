#! /usr/bin/env bash

# Convert 'puml' files.
# Usage:
#    generate-diagrams.sh [<image-format>]
# where '<image-format>' can be 'svg', 'png' or 'eps'.
# Default is 'svg'.

FRMT=${1:-svg}
test -d diagrams || mkdir diagrams

for i in puml/*.puml
do
    b=$(basename $i .puml)
    echo "converting $i -> diagrams/$b.$FRMT"
    plantuml -t$FRMT -pipe < $i > diagrams/$b.$FRMT
done
