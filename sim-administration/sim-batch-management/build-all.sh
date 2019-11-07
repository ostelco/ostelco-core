#!/bin/bash


go build

if [ "$?" -ne "0" ]; then
  echo "Sorry compilation failed aborting build."
  exit 1
fi




go test ./...

if [ "$?" -ne "0" ]; then
  echo "Sorry, one or more tests failed, aborting build."
  exit 1
fi


