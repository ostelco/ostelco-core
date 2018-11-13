#!/bin/bash

FN="all-of-es2plus.json"
rm -f "$FN"
touch "$FN"

for f in ES2*.json ; do
    echo >> $FN
    echo $f >> $FN
    cat $f >> $FN
done
